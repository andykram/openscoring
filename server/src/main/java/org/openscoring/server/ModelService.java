/*
 * Copyright (c) 2013 University of Tartu
 */
package org.openscoring.server;

import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
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

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Singleton
@Path("model")
public class ModelService {

    private final Map<String, PMML> cache;

    @Inject
    protected ModelService(@Named("pmml-model-cache") final Map<String, PMML> cache) {
        this.cache = cache;
    }

	@PUT
    @Timed
    @Metered
	@Path("{id}")
	@Consumes({MediaType.APPLICATION_XML, MediaType.TEXT_XML})
	@Produces(MediaType.TEXT_PLAIN)
	public String deploy(@PathParam("id") String id, @Context HttpServletRequest request){
		PMML pmml;

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

		cache.put(id, pmml);

		return "Model " + id + " deployed successfully";
	}

	@GET
    @Timed
	@Produces(MediaType.APPLICATION_JSON)
	public List<String> getDeployedIds(){
		List<String> result = new ArrayList<String>(cache.keySet());

		return result;
	}

	@GET
    @Timed
	@Path("{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public SummaryResponse getSummary(@PathParam("id") String id){
		PMML pmml = cache.get(id);
		if(pmml == null){
			throw new NotFoundException();
		}

		SummaryResponse response = new SummaryResponse();

		try {
			PMMLManager pmmlManager = new PMMLManager(pmml);

			Evaluator evaluator = (Evaluator)pmmlManager.getModelManager(null, ModelEvaluatorFactory.getInstance());

			response.setActiveFields(toValueList(evaluator.getActiveFields()));
			response.setPredictedFields(toValueList(evaluator.getPredictedFields()));
			response.setOutputFields(toValueList(evaluator.getOutputFields()));
		} catch(Exception e){
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return response;
	}

	@POST
	@Path("{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public EvaluationResponse evaluate(@PathParam("id") String id, EvaluationRequest request){
		return (evaluateBatch(id, Collections.singletonList(request))).get(0);
	}

	@POST
    @Timed
    @Metered
	@Path("{id}/batch")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public List<EvaluationResponse> evaluateBatch(@PathParam("id") String id, List<EvaluationRequest> requests){
		PMML pmml = cache.get(id);
		if(pmml == null){
			throw new NotFoundException();
		}

		List<EvaluationResponse> responses = new ArrayList<EvaluationResponse>();

		try {
			PMMLManager pmmlManager = new PMMLManager(pmml);

			Evaluator evaluator = (Evaluator)pmmlManager.getModelManager(null, ModelEvaluatorFactory.getInstance());

			for(EvaluationRequest request : requests){
				EvaluationResponse response = evaluate(evaluator, request);
                log.info("Evaluating model {} for request {} with result {}", id, request.toString(),
                        response.toString());

				responses.add(response);
			}
		} catch(Exception e){
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}

		return responses;
	}

	@DELETE
    @Timed
	@Path("{id}")
	@Produces(MediaType.TEXT_PLAIN)
	public String undeploy(@PathParam("id") String id){
		PMML pmml = cache.remove(id);
		if(pmml == null){
			throw new NotFoundException();
		}

		return "Model " + id + " undeployed successfully";
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