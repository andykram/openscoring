package org.openscoring.server.responses;

import java.util.Map;

import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.Setter;
import org.openscoring.common.SummaryResponse;
import org.openscoring.server.Threshold;

public class VersionedSummaryResponse {
    @Getter
    @Setter
    private Map<Integer, ThresholdSummaryResponse> summaryResponses = Maps.newHashMap();

    @Getter
    @Setter
    private String modelName;

    public VersionedSummaryResponse() {}

    public VersionedSummaryResponse(String modelName) {
        this.modelName = modelName;
    }

    public void setSummaryResponse(Integer version, ThresholdSummaryResponse response) {
        summaryResponses.put(version, response);
    }
}
