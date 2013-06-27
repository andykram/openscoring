package org.openscoring.standalone.modules;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jersey.InstrumentedResourceMethodDispatchAdapter;
import com.codahale.metrics.servlet.InstrumentedFilter;
import com.codahale.metrics.servlets.HealthCheckServlet;
import com.codahale.metrics.servlets.MetricsServlet;
import com.codahale.metrics.servlets.PingServlet;
import com.codahale.metrics.servlets.ThreadDumpServlet;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.servlet.ServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import com.typesafe.config.Config;
import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.dmg.pmml.PMML;
import org.openscoring.server.ModelService;
import org.openscoring.standalone.resources.BasicHealthResource;

import javax.ws.rs.core.UriBuilder;
import java.util.Map;

public class ServletResourceModule extends ServletModule {

    protected Config metricsConfig;

    public ServletResourceModule(Config baseConfig) {
        this.metricsConfig = baseConfig.getConfig("openscoring.metrics");
    }

    @Override
    protected void configureServlets() {
        bind(JacksonJsonProvider.class).in(Singleton.class);

        bind(ThreadDumpServlet.class).in(Singleton.class);
        bind(MetricsServlet.class).in(Singleton.class);
        bind(PingServlet.class).in(Singleton.class);
        bind(HealthCheckServlet.class).in(Singleton.class);
        bind(InstrumentedFilter.class).in(Singleton.class);
        bind(ModelService.class);
        bind(BasicHealthResource.class);

        filter("/*").through(InstrumentedFilter.class);
        serve(metricsPath("threads")).with(ThreadDumpServlet.class);
        serve(metricsPath("metrics")).with(MetricsServlet.class);
        serve(metricsPath("ping")).with(PingServlet.class);
        serve(metricsPath("healthcheck")).with(HealthCheckServlet.class);
        serve("/*").with(GuiceContainer.class);
    }

    protected String metricsPath(String pathPart) {
        final String metricsPath = metricsConfig.getString("path");
        final UriBuilder uriBuilder = UriBuilder.fromPath(metricsPath).path(pathPart);
        return uriBuilder.build().getRawPath();
    }

    @Provides
    @Singleton
    @Named("pmml-model-cache")
    protected Table<String, Integer, PMML> provideModelCache() {
        return HashBasedTable.create();
    }

    @Provides
    @Singleton
    protected InstrumentedResourceMethodDispatchAdapter provideInstrumentedResourceMethodDispatchAdapter(MetricRegistry registry) {
        return new InstrumentedResourceMethodDispatchAdapter(registry);
    }
}