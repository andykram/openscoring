package org.openscoring.server.responses;

import org.openscoring.common.EvaluationResponse;

import java.util.Map;

public class VersionedEvaluationResponse {
    protected Map<Integer, EvaluationResponse> result = null;
    protected String modelName;

    public VersionedEvaluationResponse() {}
    public VersionedEvaluationResponse(String modelName) {
        this.modelName = modelName;
    }
    public VersionedEvaluationResponse(String modelName, Map<Integer, EvaluationResponse> result) {
        this(modelName);
        this.result = result;
    }

    public Map<Integer, EvaluationResponse> getResult() {
        return this.result;
    }

    public void setResult(Map<Integer, EvaluationResponse> result) {
        this.result = result;
    }

    public String getModelName() {
        return this.modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
}
