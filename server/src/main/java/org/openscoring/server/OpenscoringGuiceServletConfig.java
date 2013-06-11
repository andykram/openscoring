package org.openscoring.server;


import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;
import org.openscoring.server.modules.OpenscoringServletModule;

public class OpenscoringGuiceServletConfig extends GuiceServletContextListener {
    @Override
    protected Injector getInjector() {
        ServletModule servletModule = new OpenscoringServletModule();
        return Guice.createInjector(servletModule);
    }
}
