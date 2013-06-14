package org.openscoring.standalone.modules;


import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.typesafe.config.Config;

public class ConfigModule extends AbstractModule {
    protected Config config;

    public ConfigModule(Config config) {
        this.config = config;
    }

    @Override
    protected void configure() { }

    @Provides
    @Singleton
    @Named("openscoring")
    protected Config provideConfig() {
        return config.getConfig("openscoring");
    }

    @Provides
    @Singleton
    @Named("metrics")
    protected Config provideMetricsConfig(@Named("openscoring") Config baseConfig) {
        final Config metricsConfig = baseConfig.getConfig("metrics");
        return metricsConfig;
    }

    @Provides
    @Singleton
    @Named("statsd")
    protected Config provideStatsdConfig(@Named("openscoring") Config baseConfig) {
        return baseConfig.getConfig("statsd");
    }
}
