package org.openscoring.standalone.metrics.statsd;

import com.codahale.metrics.*;
import org.openscoring.standalone.metrics.reporter.StructuredScheduledReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

public class StatsDReporter extends StructuredScheduledReporter {

    public static Builder forRegistry(MetricRegistry registry) {
        return new Builder(registry);
    }

    /**
     * A builder for {@link StatsDReporter} instances. Defaults to not using a prefix, using the
     * default clock, converting rates to events/second, converting durations to milliseconds, and
     * not filtering metrics.
     */
    public static class Builder {
        private final MetricRegistry registry;
        private Clock clock;
        private String prefix;
        private TimeUnit rateUnit;
        private TimeUnit durationUnit;
        private MetricFilter filter;

        private Builder(MetricRegistry registry) {
            this.registry = registry;
            this.clock = Clock.defaultClock();
            this.prefix = null;
            this.rateUnit = TimeUnit.SECONDS;
            this.durationUnit = TimeUnit.MILLISECONDS;
            this.filter = MetricFilter.ALL;
        }

        /**
         * Use the given {@link Clock} instance for the time.
         *
         * @param clock a {@link Clock} instance
         * @return {@code this}
         */
        public Builder withClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        /**
         * Prefix all metric names with the given string.
         *
         * @param prefix the prefix for all metric names
         * @return {@code this}
         */
        public Builder prefixedWith(String prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Convert rates to the given time unit.
         *
         * @param rateUnit a unit of time
         * @return {@code this}
         */
        public Builder convertRatesTo(TimeUnit rateUnit) {
            this.rateUnit = rateUnit;
            return this;
        }

        /**
         * Convert durations to the given time unit.
         *
         * @param durationUnit a unit of time
         * @return {@code this}
         */
        public Builder convertDurationsTo(TimeUnit durationUnit) {
            this.durationUnit = durationUnit;
            return this;
        }

        /**
         * Only report metrics which match the given filter.
         *
         * @param filter a {@link MetricFilter}
         * @return {@code this}
         */
        public Builder filter(MetricFilter filter) {
            this.filter = filter;
            return this;
        }

        /**
         * Builds a {@link StatsDReporter} with the given properties, sending metrics using the
         * given {@link StatsD} client.
         *
         * @param statsd a {@link StatsD} client
         * @return a {@link StatsDReporter}
         */
        public StatsDReporter build(StatsD statsd) {
            return new StatsDReporter(registry,
                                      statsd,
                                      clock,
                                      prefix,
                                      rateUnit,
                                      durationUnit,
                                      filter);
        }
    }

    public static enum StatType { COUNTER, TIMER, GAUGE }

    private static final Logger LOG = LoggerFactory.getLogger(StatsDReporter.class);

    private final StatsD statsd;

    protected StatsDReporter(MetricRegistry registry,
                             StatsD statsd,
                             Clock clock,
                             String prefix,
                             TimeUnit rateUnit,
                             TimeUnit durationUnit,
                             MetricFilter filter) {
        super(registry, clock, "stats-reporter", prefix, filter, rateUnit, durationUnit);
        this.statsd = statsd;
    }

    @Override
    public void report(SortedMap<String, Gauge> gauges,
                       SortedMap<String, Counter> counters,
                       SortedMap<String, Histogram> histograms,
                       SortedMap<String, Meter> meters,
                       SortedMap<String, Timer> timers) {
        try {
            statsd.connect();
            super.report(gauges, counters, histograms, meters, timers);
            LOG.info(String.format("Reported %d gauges, " +
                                           "%d counters, " +
                                           "%d histograms, " +
                                           "%d meters " +
                                           "and %d timers to StatsD",
                                   gauges.size(),
                                   counters.size(),
                                   histograms.size(),
                                   meters.size(),
                                   timers.size()));
        } catch (IOException e) {
            LOG.warn("Error preparing StatsD for write", e);
        } catch (RuntimeException e) {
            LOG.warn("Unable to report to StatsD", statsd, e);
        } finally {
            try {
                statsd.close();
                LOG.debug("Closed StatsD connection");
            } catch (IOException e) {
                LOG.warn("Error closing StatsD", statsd, e);
            }
        }
    }

    protected static final String formatString = "%s|%s";

    protected String format(Object o, StatType statType) {
        String fmtO = format(o);
        switch (statType) {
            case COUNTER:
                return String.format(formatString, fmtO, "c");
            case GAUGE:
                return String.format(formatString, fmtO, "g");
            case TIMER:
                return String.format(formatString, fmtO, "ms");
            default:
                return String.format(formatString, fmtO, "");
        }
    }

    @Override
    protected void reportTimer(String name, Timer timer, long timestamp) throws IOException {
        final Snapshot snapshot = timer.getSnapshot();

        statsd.send(prefix(name, "max"),
                    format(convertDuration(snapshot.getMax()), StatType.TIMER),
                    timestamp);
        statsd.send(prefix(name, "mean"),
                    format(convertDuration(snapshot.getMean()), StatType.TIMER),
                    timestamp);
        statsd.send(prefix(name, "min"),
                    format(convertDuration(snapshot.getMin()), StatType.TIMER),
                    timestamp);
        statsd.send(prefix(name, "stddev"),
                    format(convertDuration(snapshot.getStdDev()), StatType.TIMER),
                    timestamp);
        statsd.send(prefix(name, "p50"),
                    format(convertDuration(snapshot.getMedian()), StatType.TIMER),
                    timestamp);
        statsd.send(prefix(name, "p75"),
                    format(convertDuration(snapshot.get75thPercentile()), StatType.TIMER),
                    timestamp);
        statsd.send(prefix(name, "p95"),
                    format(convertDuration(snapshot.get95thPercentile()), StatType.TIMER),
                    timestamp);
        statsd.send(prefix(name, "p98"),
                    format(convertDuration(snapshot.get98thPercentile()), StatType.TIMER),
                    timestamp);
        statsd.send(prefix(name, "p99"),
                    format(convertDuration(snapshot.get99thPercentile()), StatType.TIMER),
                    timestamp);
        statsd.send(prefix(name, "p999"),
                    format(convertDuration(snapshot.get999thPercentile()), StatType.TIMER),
                    timestamp);

        reportMetered(name, timer, timestamp);
    }

    @Override
    protected void reportMetered(String name, Metered meter, long timestamp) throws IOException {
        statsd.send(prefix(name, "count"),
                    format(meter.getCount(), StatType.GAUGE),
                    timestamp);
        statsd.send(prefix(name, "m1_rate"),
                    format(convertRate(meter.getOneMinuteRate()), StatType.TIMER),
                    timestamp);
        statsd.send(prefix(name, "m5_rate"),
                    format(convertRate(meter.getFiveMinuteRate()), StatType.TIMER),
                    timestamp);
        statsd.send(prefix(name, "m15_rate"),
                    format(convertRate(meter.getFifteenMinuteRate()), StatType.TIMER),
                    timestamp);
        statsd.send(prefix(name, "mean_rate"),
                    format(convertRate(meter.getMeanRate()), StatType.TIMER),
                    timestamp);
    }

    @Override
    protected void reportHistogram(String name, Histogram histogram, long timestamp) throws IOException {
        final Snapshot snapshot = histogram.getSnapshot();
        statsd.send(prefix(name, "count"), format(histogram.getCount(), StatType.GAUGE), timestamp);
        statsd.send(prefix(name, "max"), format(snapshot.getMax(), StatType.TIMER), timestamp);
        statsd.send(prefix(name, "mean"), format(snapshot.getMean(), StatType.TIMER), timestamp);
        statsd.send(prefix(name, "min"), format(snapshot.getMin(), StatType.TIMER), timestamp);
        statsd.send(prefix(name, "stddev"), format(snapshot.getStdDev(), StatType.TIMER), timestamp);
        statsd.send(prefix(name, "p50"), format(snapshot.getMedian(), StatType.TIMER), timestamp);
        statsd.send(prefix(name, "p75"), format(snapshot.get75thPercentile(), StatType.TIMER), timestamp);
        statsd.send(prefix(name, "p95"), format(snapshot.get95thPercentile(), StatType.TIMER), timestamp);
        statsd.send(prefix(name, "p98"), format(snapshot.get98thPercentile(), StatType.TIMER), timestamp);
        statsd.send(prefix(name, "p99"), format(snapshot.get99thPercentile(), StatType.TIMER), timestamp);
        statsd.send(prefix(name, "p999"), format(snapshot.get999thPercentile(), StatType.TIMER), timestamp);
    }

    @Override
    protected void reportCounter(String name, Counter counter, long timestamp) throws IOException {
        statsd.send(prefix(name, "count"), format(counter.getCount(), StatType.GAUGE), timestamp);
    }

    @Override
    protected void reportGauge(String name, Gauge gauge, long timestamp) throws IOException {
        final String value = format(gauge.getValue(), StatType.GAUGE);
        if (value != null) {
            statsd.send(prefix(name), value, timestamp);
        }
    }


}
