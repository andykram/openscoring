package org.openscoring.server.responses;

import lombok.Data;
import org.openscoring.server.Threshold;

import java.util.List;

/**
 * Author: @andykram
 */
@Data
public class ThresholdSummaryResponse {
    private List<String> activeFields;
    private List<String> predictedFields;
    private List<String> outputFields;
    private Threshold threshold;
}
