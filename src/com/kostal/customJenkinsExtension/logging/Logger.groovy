package com.kostal.customJenkinsExtension.logging

import com.cloudbees.groovy.cps.NonCPS

/**
 * Enable logging data to Jenkins' central log.
**/
class Logger implements Serializable {
    private static final long serialVersionUID = 1L
    private static final String DEFAULT_CLASS = 'com.kostal.pipelineworks.logging.Logger'
    private final String className
    private final transient java.util.logging.Logger logger

    /**
     * Create a new logger
     *
     * @param className The name of the class you are logging for
    **/
    Logger(String className=DEFAULT_CLASS) {
        this.className = className
        this.logger = Logger.getLogger(className)
    }

    /**
     * Create a java.util.logging.Logger
     *
     * @param className The name of the class you are logging on behalf of
    **/
    @NonCPS
    static java.util.logging.Logger getLogger(String className=DEFAULT_CLASS) {
        return java.util.logging.Logger.getLogger(className)
    }

    /**
     * Log a message at the FINE level
     *
     * Use this for messages of little importance that could be useful when
     * debugging.
     *
     * @param message The message to log
     * @param className The name of the class to log on behalf of
    **/
    static void fine(String message, String className=DEFAULT_CLASS) {
        getLogger(className).fine(message)
    }

    /**
     * Log a message at the FINE level
     *
     * Use this for messages of little importance that could be useful when
     * debugging.
     *
     * @param message The message to log
    **/
    @NonCPS
    void logFine(String message) {
        logger.fine(message)
    }

    /**
     * Log a message at the INFO level
     *
     * Use this for informative messages that are of general use, but are not
     * errors and don't require specific action.
     *
     * @param message The message to log
     * @param className The name of the class to log on behalf of
    **/
    static void info(String message, String className=DEFAULT_CLASS) {
        getLogger(className).info(message)
    }

    /**
     * Log a message at the INFO level
     *
     * Use this for informative messages that are of general use, but are not
     * errors and don't require specific action.
     *
     * @param message The message to log
    **/
    @NonCPS
    void logInfo(String message) {
        logger.info(message)
    }

    /**
     * Log a message at the WARNING level
     *
     * Use this for warnings about potential issues.
     *
     * @param message The message to log
     * @param className The name of the class to log on behalf of
    **/
    static void warning(String message, String className=DEFAULT_CLASS) {
        getLogger(className).warning(message)
    }

    /**
     * Log a message at the WARNING level
     *
     * Use this for warnings about potential issues.
     *
     * @param message The message to log
    **/
    @NonCPS
    void logWarning(String message) {
        logger.warning(message)
    }

    /**
     * Logs a message at the SEVERE level
     *
     * Use this to report severe issues that require action as soon as
     * possible.
     *
     * @param message The message to log
     * @param className The name of the class to log on behalf of
    **/
    static void severe(String message, String className=DEFAULT_CLASS) {
        getLogger(className).severe(message)
    }

    /**
     * Logs a message at the SEVERE level
     *
     * Use this to report severe issues that require action as soon as
     * possible.
     *
     * @param message The message to log
    **/
    @NonCPS
    void logSevere(String message) {
        logger.severe(message)
    }
}
