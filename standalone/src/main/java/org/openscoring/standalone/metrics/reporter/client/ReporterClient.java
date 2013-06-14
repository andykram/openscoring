package org.openscoring.standalone.metrics.reporter.client;


import java.io.Closeable;
import java.io.IOException;

public interface ReporterClient extends Closeable {

    public void connect() throws IllegalStateException, IOException;

    public void send(String name, String value, long timestamp) throws IOException;
}
