package org.openscoring.standalone.metrics;

import com.codahale.metrics.*;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.dmg.pmml.PMML;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ModelCacheMetricSet implements MetricSet {
    protected final Table<String, Integer, PMML> table;
    protected final Histogram mapSizeHistory = new Histogram(new UniformReservoir());
    protected final Map<String, Metric> metricMap;

    protected final Gauge<Integer> mapSizeGauge = new CachedGauge<Integer>(1, TimeUnit.MINUTES) {
        @Override
        protected Integer loadValue() {
            int mapSize = table.size();
            mapSizeHistory.update(mapSize);
            return mapSize;
        }
    };

    @Inject
    protected ModelCacheMetricSet(@Named("pmml-model-cache") final Table<String, Integer, PMML> table) {
        final Map<String, Metric> metricMap = Maps.newHashMapWithExpectedSize(2);

        metricMap.put(MetricRegistry.name(getClass(), "model-cache", "size"), this.mapSizeGauge);
        metricMap.put(MetricRegistry.name(getClass(), "model-cache", "historical-size"),
                      this.mapSizeHistory);

        this.metricMap = metricMap;
        this.table = table;
    }

    @Override
    public Map<String, Metric> getMetrics() {
        return metricMap;
    }
}
