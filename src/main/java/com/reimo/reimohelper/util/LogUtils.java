package com.reimo.reimohelper.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger("ReimoHelper");

    public static void sendInfo(String message) {
        LOGGER.info(message);
    }

    public static void sendDebug(String message) {
        LOGGER.debug(message);
    }

    public static void sendWarning(String message) {
        LOGGER.warn(message);
    }

    public static void sendError(String message) {
        LOGGER.error(message);
    }

    public static void sendDebug(String format, Object... args) {
        LOGGER.debug(format, args);
    }

    public static void webhookStatus() {
    }
}
