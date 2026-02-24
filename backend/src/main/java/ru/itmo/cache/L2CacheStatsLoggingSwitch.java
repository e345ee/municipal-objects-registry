package ru.itmo.cache;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class L2CacheStatsLoggingSwitch {

    private final AtomicBoolean enabled;

    public L2CacheStatsLoggingSwitch(Environment env) {
        boolean initial = Boolean.parseBoolean(env.getProperty("cache.l2.stats.logging.enabled", "false"));
        this.enabled = new AtomicBoolean(initial);
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public void setEnabled(boolean value) {
        enabled.set(value);
    }
}
