package org.openscoring.standalone.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import org.eclipse.jetty.server.Server;
import org.openscoring.standalone.provider.JettyProvider;

/**
 * Author: @andykram
 * Date: 6/11/13
 */
public class ServerModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(Server.class).toProvider(JettyProvider.class).in(Singleton.class);
    }
}
