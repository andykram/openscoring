package org.openscoring.server;

import com.google.inject.name.Named;
import com.sun.jersey.api.NotFoundException;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.EvaluatorUtil;
import org.jpmml.evaluator.ModelEvaluatorFactory;
import org.jpmml.manager.PMMLManager;
import org.openscoring.common.EvaluationRequest;
import org.openscoring.common.EvaluationResponse;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Author: @andykram
 * Date: 6/10/13
 */
public class ModelEvaluator {
    private final Map<String, PMML> cache;

    protected ModelEvaluator(@Named("pmml-model-cache") final Map<String, PMML> cache) {
        this.cache = cache;
    }

    public EvaluationResponse evaluate(String id, EvaluationRequest request) {
        return (evaluate(id, Collections.singletonList(request))).get(0);
    }

    public List<EvaluationResponse> evaluate(String id, List<EvaluationRequest> requests) {
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

                responses.add(response);
            }
        } catch(Exception e){
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return responses;
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
        response.setResult((Map) EvaluatorUtil.decode(result));

        return response;
    }
}
