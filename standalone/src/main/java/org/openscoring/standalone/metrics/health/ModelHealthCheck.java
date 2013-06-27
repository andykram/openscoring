package org.openscoring.standalone.metrics.health;

import com.codahale.metrics.health.HealthCheck;
import com.google.common.collect.Table;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.dmg.pmml.PMML;

import java.util.Map;

public class ModelHealthCheck extends HealthCheck {

    private final Table<String, Integer, PMML> modelsCache;

    @Inject
    protected ModelHealthCheck(@Named("pmml-model-cache") final Table<String, Integer, PMML> modelsCache) {
        this.modelsCache = modelsCache;
    }

    @Override
    protected Result check() throws Exception {
        if (modelsCache != null) {
            if (modelsCache.isEmpty()) {
                return Result.healthy("No models in cache!");
            } else {
                return Result.healthy("Models cache contains models");
            }
        } else {
            return Result.unhealthy("No models cache available!");
        }
    }
}
