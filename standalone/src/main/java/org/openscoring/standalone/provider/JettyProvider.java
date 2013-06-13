package org.openscoring.standalone.provider;

import com.codahale.metrics.servlets.AdminServletContextListener;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.GuiceServletContextListener;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import com.typesafe.config.Config;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.openscoring.standalone.servlets.GuiceAdminServletContextListener;

import java.util.Map;

public class JettyProvider implements Provider<Server> {
    private final Injector injector;
    private final String path;
    private final int port;
    private final AdminServletContextListener adminServletContextListener;

    @Inject
    protected JettyProvider(Injector injector,
                            @Named("openscoring") Config config,
                            GuiceAdminServletContextListener adminServletContextListener) {
        this.injector = injector;
        this.path = config.getString("path");
        this.port = config.getInt("port");
        this.adminServletContextListener = adminServletContextListener;
    }

    @Override
    public Server get() {
        final Server server = new Server(port);
        final ContextHandlerCollection handlers = new ContextHandlerCollection();

        final ServletContextHandler servletHandler = new ServletContextHandler();
        servletHandler.setContextPath(path);
        servletHandler.addFilter(GuiceFilter.class, "/*", null);

        servletHandler.addEventListener(new GuiceServletContextListener() {
            @Override
            protected Injector getInjector() {
                return injector;
            }
        });
        servletHandler.addEventListener(adminServletContextListener);
        servletHandler.addServlet(DefaultServlet.class, "/");

        final ServletContextHandler healthHandler = new ServletContextHandler();
        final ServletHolder healthHolder = new ServletHolder();
        healthHolder.setInitParameters(ImmutableMap.<String, String>of(
                "com.sun.jersey.config.property.packages", "org.openscoring.standalone.health",
                "com.sun.jersey.api.json.POJOMappingFeature", "true"
        ));
        healthHolder.setServlet(new ServletContainer());
        healthHandler.addServlet(healthHolder, "/*");

        handlers.setHandlers(new Handler[]{ healthHandler, servletHandler });
        server.setHandler(handlers);

        return server;
    }
}
