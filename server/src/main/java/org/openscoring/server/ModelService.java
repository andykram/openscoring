/*
 * Copyright (c) 2013 University of Tartu
 */
package org.openscoring.server;

import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.sun.jersey.api.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.dmg.pmml.*;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.EvaluatorUtil;
import org.jpmml.evaluator.ModelEvaluatorFactory;
import org.jpmml.manager.PMMLManager;
import org.openscoring.common.EvaluationRequest;
import org.openscoring.common.EvaluationResponse;
import org.openscoring.common.SummaryResponse;
import org.openscoring.server.responses.ThresholdEvaluationResponse;
import org.openscoring.server.responses.ThresholdSummaryResponse;
import org.openscoring.server.responses.VersionedEvaluationResponse;
import org.openscoring.server.responses.VersionedSummaryResponse;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.*;

@Slf4j
@Singleton
@Path("model")
public class ModelService {

    private static final String SCORE_KEY_NAME = "score";
    private final Table<String, Integer, PMML> cache;

    @Inject
    protected ModelService(@Named("pmml-model-cache") final Table<String, Integer, PMML> cache) {
        this.cache = cache;
    }

	@PUT
    @Timed
    @Metered(name="deploy-meter")
	@Path("{id}")
	@Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML})
	@Produces(MediaType.TEXT_PLAIN)
	public Response deploy(@PathParam("id") String id, @Context HttpServletRequest request){
		Map<Integer, PMML> row = cache.row(id);
        TreeSet<Integer> sortedVersions = Sets.newTreeSet(row.keySet());
        Integer version = sortedVersions.isEmpty() ? 1 : (sortedVersions.last() + 1);

        return deploy(id, version, request);
	}

    @PUT
    @Timed
    @Metered(name="deploy-meter")
    @Path("{id}/{version}")
    @Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML})
    @Produces(MediaType.TEXT_PLAIN)
    public Response deploy(@PathParam("id") String id,
                         @PathParam("version") Integer version,
                         @Context HttpServletRequest request) {
        PMML pmml;

        if (cache.contains(id, version)) {
            return Response.status(Response.Status.CONFLICT)
                           .entity("Model versions cannot be updated")
                           .build();
        }

        try {
            InputStream is = request.getInputStream();

            try {
                pmml = IOUtil.unmarshal(is);
            } finally {
                is.close();
            }
        } catch (Exception e) {
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        if (getThreshold(pmml.getHeader()) == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("Model must have threshold")
                           .build();
        } else {
            cache.put(id, version, pmml);

            return Response.status(Response.Status.OK)
                           .entity("Model " + id + " deployed successfully")
                           .build();
        }
    }

	@GET
    @Timed
	@Produces(MediaType.APPLICATION_JSON)
	public List<String> getDeployedIds(){
        return new ArrayList<String>(cache.rowKeySet());
	}

	@GET
    @Timed
	@Path("{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public VersionedSummaryResponse getSummary(@PathParam("id") String id){
        final Map<Integer, PMML> modelVersions = cache.row(id);

		if (modelVersions.isEmpty()) {
			throw new NotFoundException();
		}

        VersionedSummaryResponse response = new VersionedSummaryResponse(id);

        for (Map.Entry<Integer, PMML> modelVersion: modelVersions.entrySet()) {
            final ThresholdSummaryResponse modelResponse = new ThresholdSummaryResponse();
            final PMML model = modelVersion.getValue();
            final Threshold threshold = getThreshold(model.getHeader());

            try {
                PMMLManager pmmlManager = new PMMLManager(model);
                Evaluator evaluator = (Evaluator)pmmlManager.getModelManager(null,
                                                                             ModelEvaluatorFactory.getInstance());
                modelResponse.setActiveFields(toValueList(evaluator.getActiveFields()));
                modelResponse.setPredictedFields(toValueList(evaluator.getPredictedFields()));
                modelResponse.setOutputFields(toValueList(evaluator.getOutputFields()));
                modelResponse.setThreshold(threshold);
            } catch (Exception ignored) {}

            response.setSummaryResponse(modelVersion.getKey(), modelResponse);
        }

		return response;
	}

	@POST
    @Timed
    @Metered(name = "evaluate-meter")
	@Path("{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public VersionedEvaluationResponse evaluate(@PathParam("id") String id, EvaluationRequest request) {
        Map<Integer, PMML> versions = cache.row(id);
        Map<Integer, ThresholdEvaluationResponse> modelResponses = Maps.newHashMap();

        VersionedEvaluationResponse response = new VersionedEvaluationResponse(id);

        for (Map.Entry<Integer, PMML> modelVersion: versions.entrySet()) {
            try {
                VersionedEvaluationResponse result = evaluateBatchVersion(id,
                                                                          modelVersion.getKey(),
                                                                          Collections.singletonList(request)).get(0);
                modelResponses.putAll(result.getResult());
            } catch (Exception ignored) {}
        }

        response.setResult(modelResponses);
        return response;
	}

    @POST
    @Timed
    @Metered(name = "evaluate-batch-meter")
    @Path("{id}/batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<VersionedEvaluationResponse> evaulateBatch(@PathParam("id") String id,
                                                           List<EvaluationRequest> requests) {
        Map<Integer, PMML> versions = cache.row(id);
        List<VersionedEvaluationResponse> responses = Lists.newArrayList();

        for (Map.Entry<Integer, PMML> modelVersion: versions.entrySet()) {
            try {
                List<VersionedEvaluationResponse> result = evaluateBatchVersion(id,
                                                                                modelVersion.getKey(),
                                                                                requests);
                responses.addAll(result);
            } catch (Exception ignored) {}
        }

        return responses;
    }

	@POST
    @Timed
    @Metered(name = "evaluate-batch-version-meter")
	@Path("{id}/{version}/batch")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public List<VersionedEvaluationResponse> evaluateBatchVersion(@PathParam("id") String id,
                                                                  @PathParam("version") Integer version,
                                                                  List<EvaluationRequest> requests){

		final PMML pmml = cache.get(id, version);
		if(pmml == null){
			throw new NotFoundException();
		}

        final Threshold threshold = getThreshold(pmml.getHeader());

		final List<VersionedEvaluationResponse> responses = Lists.newArrayList();

		try {
			final PMMLManager pmmlManager = new PMMLManager(pmml);

			final Evaluator evaluator = (Evaluator)pmmlManager.getModelManager(null,
                                                                               ModelEvaluatorFactory.getInstance());

			for(EvaluationRequest request : requests) {
                final Map<String, ?> parameters = request.getParameters();
                final List<Map.Entry<String, ?>> toTransform = Lists.newArrayListWithCapacity(parameters.size());

                for (Map.Entry<String, ?> param : parameters.entrySet()) {
                    if (param.getValue() instanceof Boolean) {
                        toTransform.add(param);
                    }
                }

                log.info("Received request parameters {} with UUID {}", request.getParameters(), request.getId());

                for (Map.Entry<String, ?> param : toTransform) {
                    Boolean boolValue = (Boolean) param.getValue();
                    ((Map)parameters).put(param.getKey(), Boolean.toString(boolValue));
                }

                log.info("Evaluating request parameters {} with UUID {}", parameters, request.getId());

				ThresholdEvaluationResponse response = ThresholdEvaluationResponse.fromEvaluationResponse(evaluate(evaluator,
                                                                                                                   request));
                String scoreKey = threshold.getScoreKey();
                Map<String, Object> result = response.getResult();

                if ((scoreKey != null) && (result.containsKey(scoreKey))) {
                    result.put(SCORE_KEY_NAME, result.get(scoreKey));
                }

                response.setThreshold(threshold);

                Map<Integer, ThresholdEvaluationResponse> responseMap = ImmutableMap.of(version, response);

                VersionedEvaluationResponse versionedResponse = new VersionedEvaluationResponse(id);
                versionedResponse.setResult(responseMap);

                log.info("Evaluated model {} for request {} with result {} with UUID {}", id,
                         parameters,
                         response.getResult(),
                         request.getId());

				responses.add(versionedResponse);
			}
		} catch(Exception e){
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return responses;
	}

    @POST
    @Timed
    @Metered(name = "evaluate-version-meter")
    @Path("{id}/{version}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public VersionedEvaluationResponse evaluateVersion(@PathParam("id") String id,
                                                       @PathParam("version") Integer version,
                                                       EvaluationRequest request) {
        return (evaluateBatchVersion(id, version, Collections.singletonList(request))).get(0);
    }

	@DELETE
    @Timed
	@Path("{id}/{version}")
	@Produces(MediaType.TEXT_PLAIN)
	public String undeploy(@PathParam("id") String id,
                           @PathParam("version") Integer version){
		PMML pmml = cache.remove(id, version);
		if (pmml == null) {
			throw new NotFoundException();
		}

        return String.format("Model %s version %d undeployed successfully", id, version);
	}

	static
	private EvaluationResponse evaluate(Evaluator evaluator, EvaluationRequest request){
		EvaluationResponse response = new EvaluationResponse();

		Map<FieldName, Object> parameters = new LinkedHashMap<FieldName, Object>();

		List<FieldName> activeFields = evaluator.getActiveFields();
		for(FieldName activeField : activeFields){
			Object value = request.getParameter(activeField.getValue());

			parameters.put(activeField, evaluator.prepare(activeField, value));
		}

		Map<FieldName, ?> result = evaluator.evaluate(parameters);

		// XXX
		response.setResult((Map)EvaluatorUtil.decode(result));

		return response;
	}

	static
	private List<String> toValueList(List<FieldName> names){
		List<String> result = new ArrayList<String>(names.size());

		for(FieldName name : names){
			result.add(name.getValue());
		}

		return result;
	}

    static private Threshold getThreshold(final Header header) {
        final List<Extension> extensions = header.getExtensions();
        Float upper = null;
        Float lower = null;
        String scoreKey = null;

        try {
            for (Extension extension : extensions) {
                if (extension.getName().equals("trueIfAbove")) {
                    lower = Float.valueOf(extension.getValue());
                } else if (extension.getName().equals("trueIfBelow")) {
                    upper = Float.valueOf(extension.getValue());
                } else if (extension.getName().equals("useAsScore")) {
                    scoreKey = extension.getValue();
                }
            }

            return new Threshold(lower, upper, scoreKey);

        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}