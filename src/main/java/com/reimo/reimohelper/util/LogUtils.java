package com.reimo.reimohelper.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logging utilities for ReimoHelper macro system
 */
public class LogUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger("ReimoHelper");

    /**
     * Send info level log
     */
    public static void sendInfo(String message) {
        LOGGER.info(message);
    }

    /**
     * Send debug level log
     */
    public static void sendDebug(String message) {
        LOGGER.debug(message);
    }

    /**
     * Send warning level log
     */
    public static void sendWarning(String message) {
        LOGGER.warn(message);
    }

    /**
     * Send error level log
     */
    public static void sendError(String message) {
        LOGGER.error(message);
    }

    /**
     * Send debug log with formatted message
     */
    public static void sendDebug(String format, Object... args) {
        LOGGER.debug(format, args);
    }

    /**
     * Send webhook status log (placeholder)
     */
    public static void webhookStatus() {
        // Implementation for webhook status logging
    }
}
