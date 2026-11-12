package io.github.junjun1z.inventorytodo.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Logging through SLF4J (bundled with every supported Minecraft version). */
public final class Log {

    private static final Logger LOG = LoggerFactory.getLogger("inventorytodo");

    private Log() {}

    public static void info(String s) {
        LOG.info(s);
    }

    public static void warn(String s) {
        LOG.warn(s);
    }

    public static void warn(String s, Throwable t) {
        LOG.warn(s, t);
    }

    public static void error(String s, Throwable t) {
        LOG.error(s, t);
    }

    public static void debug(String s) {
        LOG.debug(s);
    }
}
