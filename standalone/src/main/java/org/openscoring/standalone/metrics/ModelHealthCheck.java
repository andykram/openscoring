package org.openscoring.standalone.metrics;

import com.codahale.metrics.health.HealthCheck;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.dmg.pmml.PMML;

import java.util.Map;

public class ModelHealthCheck extends HealthCheck {

    @Inject @Named("pmml-models-cache") private Map<String, PMML> modelsCache;

    @Override
    protected Result check() throws Exception {
        if (modelsCache != null) {
            int numModels = modelsCache.size();
            return Result.healthy(String.format("Models cache contains %d models", numModels));
        } else {
            return Result.unhealthy("No models cache available!");
        }
    }
}
