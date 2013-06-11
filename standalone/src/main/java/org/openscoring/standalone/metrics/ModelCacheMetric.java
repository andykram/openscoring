package org.openscoring.standalone.metrics;

import com.codahale.metrics.CachedGauge;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.UniformReservoir;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.dmg.pmml.PMML;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ModelCacheMetric {
    private final Map<String, PMML> map;
    private final Histogram mapSizeHistory = new Histogram(new UniformReservoir());
    private final Gauge<Integer> mapSizeGauge = new CachedGauge<Integer>(1, TimeUnit.MINUTES) {
        @Override
        protected Integer loadValue() {
            int mapSize = map.size();
            mapSizeHistory.update(mapSize);
            return mapSize;
        }
    };

    @Inject
    protected ModelCacheMetric(@Named("pmml-model-cache") final Map<String, PMML> map,
            MetricRegistry registry) {
        this.map = map;

        registry.register(MetricRegistry.name(getClass(), "model-cache", "size"), this.mapSizeGauge);
        registry.register(MetricRegistry.name(getClass(), "model-cache", "historical-size"),
                this.mapSizeHistory);
    }
}
