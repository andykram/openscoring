package org.openscoring.standalone.metrics.reporter;


import com.codahale.metrics.*;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

public abstract class StructuredScheduledReporter extends ScheduledReporter {

    protected final String prefix;
    protected final Clock clock;
    /**
     * Creates a new {@link com.codahale.metrics.ScheduledReporter} instance.
     *
     * @param registry the {@link com.codahale.metrics.MetricRegistry} containing the metrics this
     *                 reporter will report
     * @param name     the reporter's name
     * @param filter   the filter for which metrics to report
     */
    protected StructuredScheduledReporter(MetricRegistry registry,
                                          Clock clock,
                                          String name,
                                          String prefix,
                                          MetricFilter filter,
                                          TimeUnit rateUnit,
                                          TimeUnit durationUnit) {
        super(registry, name, filter, rateUnit, durationUnit);
        this.prefix = prefix;
        this.clock = clock;
    }

    @Override
    public void report(SortedMap<String, Gauge> gauges,
                       SortedMap<String, Counter> counters,
                       SortedMap<String, Histogram> histograms,
                       SortedMap<String, Meter> meters,
                       SortedMap<String, Timer> timers) {
        final long timestamp = clock.getTime() / 1000;

        try {
            for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
                reportGauge(entry.getKey(), entry.getValue(), timestamp);
            }

            for (Map.Entry<String, Counter> entry : counters.entrySet()) {
                reportCounter(entry.getKey(), entry.getValue(), timestamp);
            }

            for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
                reportHistogram(entry.getKey(), entry.getValue(), timestamp);
            }

            for (Map.Entry<String, Meter> entry : meters.entrySet()) {
                reportMetered(entry.getKey(), entry.getValue(), timestamp);
            }

            for (Map.Entry<String, Timer> entry : timers.entrySet()) {
                reportTimer(entry.getKey(), entry.getValue(), timestamp);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected String format(Object o) {
        if (o instanceof Float) {
            return format(((Float) o).doubleValue());
        } else if (o instanceof Double) {
            return format(((Double) o).doubleValue());
        } else if (o instanceof Byte) {
            return format(((Byte) o).longValue());
        } else if (o instanceof Short) {
            return format(((Short) o).longValue());
        } else if (o instanceof Integer) {
            return format(((Integer) o).longValue());
        } else if (o instanceof Long) {
            return format(((Long) o).longValue());
        }
        return null;
    }

    protected String prefix(String... components) {
        return MetricRegistry.name(prefix, components);
    }

    protected String format(long n) {
        return Long.toString(n);
    }

    protected String format(double v) {
        // the Carbon plaintext format is pretty underspecified, but it seems like it just wants
        // US-formatted digits
        return String.format(Locale.US, "%2.2f", v);
    }

    protected abstract void reportTimer(String name,
                                        Timer timer,
                                        long timestamp) throws IOException;

    protected abstract void reportMetered(String name,
                                          Metered meter,
                                          long timestamp) throws IOException;

    protected abstract void reportHistogram(String name,
                                            Histogram histogram,
                                            long timestamp) throws IOException;

    protected abstract void reportCounter(String name,
                                          Counter counter,
                                          long timestamp) throws IOException;

    protected abstract void reportGauge(String name,
                                        Gauge gauge,
                                        long timestamp) throws IOException;
}
