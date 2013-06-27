package org.openscoring.server.responses;

import java.util.Map;

import com.google.common.collect.Maps;
import org.openscoring.common.SummaryResponse;

public class VersionedSummaryResponse {
    protected Map<Integer, SummaryResponse> summaryResponses = Maps.newHashMap();
    protected String modelName;

    public VersionedSummaryResponse() {}

    public VersionedSummaryResponse(String modelName) {
        this.modelName = modelName;
    }

    public void setSummaryResponses(Map<Integer, SummaryResponse> summaryResponses) {
        this.summaryResponses = summaryResponses;
    }

    public Map<Integer, SummaryResponse> getSummaryResponses() {
        return this.summaryResponses;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getModelName() {
        return this.modelName;
    }

    public void setSummaryResponse(Integer version, SummaryResponse response) {
        summaryResponses.put(version, response);
    }
}
