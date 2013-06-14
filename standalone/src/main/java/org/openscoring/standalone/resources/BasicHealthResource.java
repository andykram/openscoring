package org.openscoring.standalone.resources;


import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

@Singleton
@Path("/health")
@Produces(MediaType.TEXT_PLAIN)
public class BasicHealthResource {
    protected final HealthCheckRegistry healthCheckRegistry;

    @Inject
    public BasicHealthResource(HealthCheckRegistry healthCheckRegistry) {
        this.healthCheckRegistry = healthCheckRegistry;
    }

    public static final Predicate<HealthCheck.Result> failedHealthCheck = new Predicate<HealthCheck.Result>() {
        @Override
        public boolean apply(final HealthCheck.Result input) {
            try {
                return (input != null) && !input.isHealthy();
            } catch (Exception e) {
                return true;
            }
        }
    };

    @GET
    public Response getHealthCheck() {
        Map<String, HealthCheck.Result> results = healthCheckRegistry.runHealthChecks();
        Iterable<HealthCheck.Result> failingChecks = Iterables.filter(results.values(), failedHealthCheck);

        if (Iterables.isEmpty(failingChecks)) {
            return Response.ok("OK", MediaType.TEXT_PLAIN_TYPE).build();
        } else {
            return Response.serverError().build();
        }
    }
}
