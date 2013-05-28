package org.openscoring.standalone;

import com.sun.jersey.spi.container.servlet.ServletContainer;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

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

        final ServletHolder holder = new ServletHolder();
        holder.setInitParameter("com.sun.jersey.config.property.packages",
                "org.openscoring.server");
        holder.setInitParameter("com.sun.jersey.api.json.POJOMappingFeature",
                "true");
        holder.setServlet(new ServletContainer());

        handler.addServlet(holder, "/*");
        server.setHandler(handler);

        server.start();
        server.join();
    }
}
