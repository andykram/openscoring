package org.openscoring.standalone.servlets;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.servlets.AdminServletContextListener;
import com.google.inject.Inject;

public class GuiceAdminServletContextListener extends AdminServletContextListener {

    @Inject private MetricRegistry metricRegistry;
    @Inject private HealthCheckRegistry healthCheckRegistry;

    @Override
    protected MetricRegistry getMetricRegistry() {
        return metricRegistry;
    }

    @Override
    protected HealthCheckRegistry getHealthCheckRegistry() {
        return healthCheckRegistry;
    }
}
