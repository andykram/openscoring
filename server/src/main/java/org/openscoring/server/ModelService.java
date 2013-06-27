/*
 * Copyright (c) 2013 University of Tartu
 */
package org.openscoring.server;

import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.sun.jersey.api.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.EvaluatorUtil;
import org.jpmml.evaluator.ModelEvaluatorFactory;
import org.jpmml.manager.IOUtil;
import org.jpmml.manager.PMMLManager;
import org.openscoring.common.EvaluationRequest;
import org.openscoring.common.EvaluationResponse;
import org.openscoring.common.SummaryResponse;
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
	public String deploy(@PathParam("id") String id, @Context HttpServletRequest request){
		Map<Integer, PMML> row = cache.row(id);
        TreeSet<Integer> sortedVersions = Sets.newTreeSet(row.keySet());

        return deploy(id, sortedVersions.last() + 1, request);
	}

    @PUT
    @Timed
    @Metered(name="deploy-meter")
    @Path("{id}/{version}")
    @Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML})
    @Produces(MediaType.TEXT_PLAIN)
    public String deploy(@PathParam("id") String id,
                         @PathParam("version") Integer version,
                         @Context HttpServletRequest request) {
        PMML pmml;

        if (cache.contains(id, version)) {
            return "Model versions cannot be updated";
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

        cache.put(id, version, pmml);

        return "Model " + id + " deployed successfully";
    }

	@GET
    @Timed
	@Produces(MediaType.APPLICATION_JSON)
	public List<String> getDeployedIds(){
		List<String> result = new ArrayList<String>(cache.rowKeySet());

		return result;
	}

	@GET
    @Timed
	@Path("{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public VersionedSummaryResponse getSummary(@PathParam("id") String id){
        Map<Integer, PMML> modelVersions = cache.row(id);

		if (modelVersions.isEmpty()) {
			throw new NotFoundException();
		}

        VersionedSummaryResponse response = new VersionedSummaryResponse(id);

        for (Map.Entry<Integer, PMML> modelVersion: modelVersions.entrySet()) {
            SummaryResponse modelResponse = new SummaryResponse();

            try {
                PMMLManager pmmlManager = new PMMLManager(modelVersion.getValue());
                Evaluator evaluator = (Evaluator)pmmlManager.getModelManager(null,
                                                                             ModelEvaluatorFactory.getInstance());
                modelResponse.setActiveFields(toValueList(evaluator.getActiveFields()));
                modelResponse.setPredictedFields(toValueList(evaluator.getPredictedFields()));
                modelResponse.setOutputFields(toValueList(evaluator.getOutputFields()));
            } catch (Exception e) {}

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
        Map<Integer, EvaluationResponse> modelResponses = Maps.newHashMap();

        VersionedEvaluationResponse response = new VersionedEvaluationResponse(id);

        for (Map.Entry<Integer, PMML> modelVersion: versions.entrySet()) {
            VersionedEvaluationResponse result = evaluateBatchVersion(id,
                                                                      modelVersion.getKey(),
                                                                      Collections.singletonList(request)).get(0);
            modelResponses.putAll(result.getResult());
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
            List<VersionedEvaluationResponse> result = evaluateBatchVersion(id,
                                                                            modelVersion.getKey(),
                                                                            requests);
            responses.addAll(result);
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

		PMML pmml = cache.get(id, version);
		if(pmml == null){
			throw new NotFoundException();
		}

		List<VersionedEvaluationResponse> responses = Lists.newArrayList();

		try {
			PMMLManager pmmlManager = new PMMLManager(pmml);

			Evaluator evaluator = (Evaluator)pmmlManager.getModelManager(null,
                                                                         ModelEvaluatorFactory.getInstance());

			for(EvaluationRequest request : requests){
				EvaluationResponse response = evaluate(evaluator, request);

                Map<Integer, EvaluationResponse> responseMap = Maps.newHashMap();
                responseMap.put(version, response);

                VersionedEvaluationResponse versionedResponse = new VersionedEvaluationResponse(id, responseMap);

                log.info("Evaluating model {} for request {} with result {}", id,
                         request.getParameters(),
                         response.getResult());

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
}