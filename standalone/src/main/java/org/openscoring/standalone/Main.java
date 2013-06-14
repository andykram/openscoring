package org.openscoring.standalone;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.openscoring.standalone.metrics.reporter.AggregateScheduledReporter;
import org.openscoring.standalone.modules.ConfigModule;
import org.openscoring.standalone.modules.MetricsModule;
import org.openscoring.standalone.modules.ScheduledReporterModule;
import org.openscoring.standalone.modules.ServerModule;
import org.openscoring.standalone.modules.ServletResourceModule;

import java.util.concurrent.TimeUnit;

@Slf4j
public class Main {
    public static void main(String[] args) throws Exception {
        final Config baseConfig = ConfigFactory.load();
        final Injector injector = Guice.createInjector(
                new ConfigModule(baseConfig),
                new ServerModule(),
                new MetricsModule(),
                new ServletResourceModule(baseConfig),
                new ScheduledReporterModule()
        );

        final Server server = injector.getInstance(Server.class);
        final int reporterInterval = baseConfig.getInt("openscoring.metrics.reporterIntervalSeconds");
        final AggregateScheduledReporter reporter = injector.getInstance(AggregateScheduledReporter.class);

        server.addLifeCycleListener(new AbstractLifeCycle.AbstractLifeCycleListener() {
            @Override
            public void lifeCycleStarted(LifeCycle event) {
                reporter.start(reporterInterval, TimeUnit.SECONDS);
            }

            @Override
            public void lifeCycleStopped(LifeCycle event) {
                reporter.stop();
            }
        });

        server.start();
        server.join();
    }
}
