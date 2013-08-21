package org.openscoring.server.responses;

import lombok.Data;
import org.openscoring.common.EvaluationResponse;
import org.openscoring.server.Threshold;

import java.util.Map;

/**
 * Author: @andykram
 */
@Data
public class ThresholdEvaluationResponse {
    private Map<String, Object> result;
    private Threshold threshold;

    public static ThresholdEvaluationResponse fromEvaluationResponse(final EvaluationResponse response) {
        final ThresholdEvaluationResponse r = new ThresholdEvaluationResponse();
        r.setResult(response.getResult());
        return r;
    }
}
