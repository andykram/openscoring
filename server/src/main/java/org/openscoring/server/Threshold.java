package org.openscoring.server;

import lombok.Data;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * Author: @andykram
 */
@Data
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties({"scoreKey"})
public class Threshold {
    @JsonProperty
    private final Float trueIfAbove;
    @JsonProperty
    private final Float trueIfBelow;
    private final String scoreKey;
}
