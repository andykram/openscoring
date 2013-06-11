package org.openscoring.standalone;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import org.openscoring.standalone.modules.MetricsAdminServletModule;

public class GuiceServletConfig extends GuiceServletContextListener {
    @Override
    protected Injector getInjector() {
        return Guice.createInjector(
                new MetricsAdminServletModule()
        );
    }
}
