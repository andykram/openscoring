package org.openscoring.standalone.metrics.reporter.client;

import java.util.regex.Pattern;

abstract public class BaseReporterClient implements ReporterClient {
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");

    protected String sanitize(String s) {
        return WHITESPACE.matcher(s).replaceAll("-");
    }
}
