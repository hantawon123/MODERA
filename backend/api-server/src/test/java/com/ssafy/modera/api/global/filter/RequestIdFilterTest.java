package com.ssafy.modera.api.global.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();
    private Logger logger;
    private Level originalLevel;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(RequestIdFilter.class);
        originalLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        logger.setLevel(originalLevel);
        appender.stop();
    }

    @Test
    void logsFastSuccessfulRequestAtDebug() {
        log("GET", "/api/v1/images", 200, 12);

        assertThat(singleEvent().getLevel()).isEqualTo(Level.DEBUG);
    }

    @Test
    void logsSlowSuccessfulRequestAtWarn() {
        log("GET", "/api/v1/images", 200, 300);

        assertThat(singleEvent().getLevel()).isEqualTo(Level.WARN);
    }

    @Test
    void logsClientAndServerErrorsAtElevatedLevels() {
        log("POST", "/api/v1/auth/kakao/login", 401, 20);
        log("GET", "/api/v1/images", 500, 25);

        assertThat(appender.list)
                .extracting(ILoggingEvent::getLevel)
                .containsExactly(Level.WARN, Level.ERROR);
    }

    @Test
    void doesNotLogActuatorRequests() {
        log("GET", "/actuator/health", 200, 1);

        assertThat(appender.list).isEmpty();
    }

    private void log(String method, String uri, int status, long elapsedMillis) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(status);
        filter.logAccess(request, response, elapsedMillis);
    }

    private ILoggingEvent singleEvent() {
        assertThat(appender.list).hasSize(1);
        return appender.list.getFirst();
    }
}
