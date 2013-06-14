package org.openscoring.standalone.provider;

import com.codahale.metrics.servlets.AdminServletContextListener;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.GuiceServletContextListener;
import com.typesafe.config.Config;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.openscoring.standalone.servlets.GuiceAdminServletContextListener;

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
        final ServletContextHandler handler = new ServletContextHandler();
        handler.setContextPath(path);
        handler.addFilter(GuiceFilter.class, "/*", null);

        handler.addEventListener(new GuiceServletContextListener() {
            @Override
            protected Injector getInjector() {
                return injector;
            }
        });
        handler.addEventListener(adminServletContextListener);
        handler.addServlet(DefaultServlet.class, "/" );

        server.setHandler(handler);

        return server;
    }
}
