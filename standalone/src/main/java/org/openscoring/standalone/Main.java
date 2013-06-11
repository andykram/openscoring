package org.openscoring.standalone;

import com.google.inject.Injector;
import com.google.inject.servlet.GuiceFilter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.openscoring.standalone.servlets.GuiceAdminServletContextListener;

@Slf4j
public class Main {
    public static void main(String[] args) throws Exception {
        final Config config = ConfigFactory.load().getConfig("openscoring");
        final int port = config.getInt("port");
        final String path = config.getString("path");
        log.info("Starting OpenScoring standalone on port {}, path {}", port, path);

        final Server server = new Server(port);

        final ServletContextHandler handler = new ServletContextHandler();
        handler.setContextPath(path);

        final GuiceServletConfig listener = new GuiceServletConfig();
        final Injector injector = listener.getInjector();

        handler.addEventListener(listener);
        handler.addEventListener(injector.getInstance(GuiceAdminServletContextListener.class));
        handler.addFilter(GuiceFilter.class, "/*", null);
        handler.addServlet(DefaultServlet.class, "/");

        server.setHandler(handler);

        server.start();
        server.join();
    }
}
