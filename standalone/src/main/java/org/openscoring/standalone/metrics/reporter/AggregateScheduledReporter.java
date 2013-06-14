package org.openscoring.standalone.metrics.reporter;

import com.codahale.metrics.ScheduledReporter;

import java.util.Set;
import java.util.concurrent.TimeUnit;

public class AggregateScheduledReporter {
    private final Set<ScheduledReporter> scheduledReporters;

    public AggregateScheduledReporter(Set<ScheduledReporter> scheduledReporters) {
        this.scheduledReporters = scheduledReporters;
    }

    public void start(long period, TimeUnit unit) {
        for (ScheduledReporter reporter: scheduledReporters) {
            reporter.start(period, unit);
        }
    }

    public void stop() {
        for (ScheduledReporter reporter: scheduledReporters) {
            reporter.stop();
        }
    }
}
