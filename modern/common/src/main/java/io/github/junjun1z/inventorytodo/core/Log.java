package io.github.junjun1z.inventorytodo.core;

import java.lang.reflect.Method;

/**
 * Logging that works on every Minecraft version: SLF4J (1.18+), else Log4j 2 (1.12 - 1.17), else stderr.
 * Looked up by reflection so no logging library is needed at compile time.
 */
public final class Log {

    private static Object logger;
    private static Method mInfo, mWarn, mWarnT, mError, mDebug;

    static {
        String[][] candidates = { { "org.slf4j.LoggerFactory", "getLogger" },
            { "org.apache.logging.log4j.LogManager", "getLogger" } };
        for (String[] c : candidates) {
            try {
                Class<?> factory = Class.forName(c[0]);
                Object l = factory.getMethod(c[1], String.class)
                    .invoke(null, "inventorytodo");
                Class<?> lc = l.getClass();
                mInfo = lc.getMethod("info", String.class);
                mWarn = lc.getMethod("warn", String.class);
                mWarnT = lc.getMethod("warn", String.class, Throwable.class);
                mError = lc.getMethod("error", String.class, Throwable.class);
                mDebug = lc.getMethod("debug", String.class);
                mInfo.setAccessible(true);
                mWarn.setAccessible(true);
                mWarnT.setAccessible(true);
                mError.setAccessible(true);
                mDebug.setAccessible(true);
                logger = l;
                break;
            } catch (Throwable ignored) {
                logger = null;
            }
        }
    }

    private Log() {}

    private static void call(Method m, String fallbackPrefix, Object... args) {
        if (logger != null) {
            try {
                m.invoke(logger, args);
                return;
            } catch (Throwable ignored) {}
        }
        if (fallbackPrefix == null) return;
        System.err.println("[inventorytodo] " + fallbackPrefix + args[0]);
        if (args.length > 1 && args[1] instanceof Throwable) ((Throwable) args[1]).printStackTrace();
    }

    public static void info(String s) {
        call(mInfo, "", s);
    }

    public static void warn(String s) {
        call(mWarn, "WARN ", s);
    }

    public static void warn(String s, Throwable t) {
        call(mWarnT, "WARN ", s, t);
    }

    public static void error(String s, Throwable t) {
        call(mError, "ERROR ", s, t);
    }

    public static void debug(String s) {
        call(mDebug, null, s);
    }
}
