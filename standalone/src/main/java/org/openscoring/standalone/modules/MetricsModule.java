package org.openscoring.standalone.modules;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import org.openscoring.standalone.metrics.ModelCacheMetricSet;
import org.openscoring.standalone.metrics.health.ModelHealthCheck;

import java.util.Map;
import java.util.Set;

public class MetricsModule extends AbstractModule {
    @Override
    protected void configure() {
        final Multibinder<MetricSet> metricSets = Multibinder.newSetBinder(binder(), MetricSet.class);
        metricSets.addBinding().to(ModelCacheMetricSet.class);

        MapBinder<String, HealthCheck> healthChecks = MapBinder.newMapBinder(binder(),
                                                                             String.class,
                                                                             HealthCheck.class);
        healthChecks.addBinding("models-cache").to(ModelHealthCheck.class);
    }

    @Provides
    @Singleton
    protected MetricRegistry provideMetricRegistry(Set<MetricSet> metrics) {
        final MetricRegistry metricRegistry = new MetricRegistry();
        for (MetricSet metric: metrics) {
            metricRegistry.registerAll(metric);
        }
        return metricRegistry;
    }

    @Provides
    @Singleton
    protected HealthCheckRegistry provideHealthCheckRegistry(Map<String, HealthCheck> healthChecks) {
        final HealthCheckRegistry healthCheckRegistry = new HealthCheckRegistry();
        for (Map.Entry<String, HealthCheck> entry: healthChecks.entrySet()) {
            healthCheckRegistry.register(entry.getKey(), entry.getValue());
        }
        return healthCheckRegistry;
    }
}
