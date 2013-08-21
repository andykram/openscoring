package org.openscoring.server.responses;

import lombok.Data;
import org.openscoring.common.EvaluationResponse;
import org.openscoring.server.Threshold;

import java.util.Map;

@Data
public class VersionedEvaluationResponse {
    private final String modelName;
    private Map<Integer, EvaluationResponse> result;
    private Threshold threshold;
}
