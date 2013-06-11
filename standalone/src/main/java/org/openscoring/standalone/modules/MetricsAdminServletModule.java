package org.openscoring.standalone.modules;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.servlets.AdminServlet;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import org.openscoring.server.modules.OpenscoringServletModule;
import org.openscoring.standalone.metrics.ModelCacheMetric;
import org.openscoring.standalone.metrics.ModelHealthCheck;

public class MetricsAdminServletModule extends OpenscoringServletModule {

    @Override
    protected void configureServlets() {
        super.configureServlets();

        bind(AdminServlet.class).asEagerSingleton();
        serve("/metrics").with(AdminServlet.class);

        Multibinder<HealthCheck> healthCheckMultibinder = Multibinder.newSetBinder(binder(),
                HealthCheck.class);

        healthCheckMultibinder.addBinding().to(ModelHealthCheck.class);

        bind(ModelCacheMetric.class).asEagerSingleton();
    }

    @Provides
    @Singleton
    protected MetricRegistry provideMetricRegistry() {
        return new MetricRegistry();
    }

    @Provides
    @Singleton
    protected HealthCheckRegistry provideHealthCheckRegistry() {
        return new HealthCheckRegistry();
    }
}