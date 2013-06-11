package org.openscoring.server.modules;

import com.google.common.collect.Maps;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import org.dmg.pmml.PMML;
import org.openscoring.server.ModelService;

import java.util.Map;

public class OpenscoringServletModule extends JerseyServletModule {
    @Override
    protected void configureServlets() {
        Map<String, String> config = Maps.newHashMap();
        config.put("com.sun.jersey.api.json.POJOMappingFeature", "true");

        bind(ModelService.class);
        serve("/*").with(GuiceContainer.class, config);
    }

    @Provides
    @Named("pmml-model-cache")
    @Singleton
    protected Map<String, PMML> provideModelCache() {
        return Maps.newConcurrentMap();
    }
}
