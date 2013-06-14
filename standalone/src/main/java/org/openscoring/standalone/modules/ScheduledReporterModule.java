package org.openscoring.standalone.modules;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.typesafe.config.Config;
import org.openscoring.standalone.metrics.reporter.AggregateScheduledReporter;
import org.openscoring.standalone.metrics.statsd.StatsD;
import org.openscoring.standalone.metrics.statsd.StatsDReporter;

import java.util.Set;

public class ScheduledReporterModule extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder<ScheduledReporter> scheduledReporterMultibinder = Multibinder.newSetBinder(
                binder(),
                ScheduledReporter.class);

        scheduledReporterMultibinder.addBinding().to(StatsDReporter.class);
    }

    @Provides
    @Singleton
    protected AggregateScheduledReporter provideAggregateScheduledReporter(Set<ScheduledReporter> reporters) {
        return new AggregateScheduledReporter(reporters);
    }

    @Provides
    protected StatsD provideStatsD(@Named("statsd") Config config) {
        final String host = config.getString("host");
        final int port = config.getInt("port");
        return new StatsD(host, port);
    }

    @Provides
    protected StatsDReporter provideStatsDReporter(MetricRegistry registry, StatsD statsD) {
        return StatsDReporter.forRegistry(registry).build(statsD);
    }
}