package com.loupsolitaire.backend.support;

import java.util.List;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * Capture les lignes de log d'une classe pendant un test (RGPD-03 : verifier
 * qu'aucune adresse email complete n'y apparait).
 */
public final class LogsCaptures implements AutoCloseable {

    private final Logger logger;
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    public LogsCaptures(Class<?> classe) {
        logger = (Logger) LoggerFactory.getLogger(classe);
        appender.start();
        logger.addAppender(appender);
    }

    /** Messages formates (arguments remplaces). */
    public List<String> messages() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    @Override
    public void close() {
        logger.detachAppender(appender);
    }
}
