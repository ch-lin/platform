/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2025 Che-Hung Lin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package ch.lin.platform.logging;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.db.ConnectionSource;
import ch.qos.logback.core.db.dialect.SQLDialectCode;
import ch.qos.logback.core.status.Status;

class ResilientDBAppenderTest {

    @Test
    void shouldDowngradeErrorToWarnDuringStartup() {
        ResilientDBAppender appender = new ResilientDBAppender();
        LoggerContext context = new LoggerContext();
        appender.setContext(context);

        // Supply a dummy connection source that triggers an error during the start phase
        appender.setConnectionSource(new ConnectionSource() {
            private boolean started = false;

            @Override
            public void start() {
                started = true;
            }

            @Override
            public void stop() {
                started = false;
            }

            @Override
            public boolean isStarted() {
                return started;
            }

            @Override
            public java.sql.Connection getConnection() throws java.sql.SQLException {
                return null;
            }

            @Override
            public SQLDialectCode getSQLDialectCode() {
                // Simulate an error occurring during super.start()
                appender.addError("Simulated startup error");
                return SQLDialectCode.UNKNOWN_DIALECT;
            }

            @Override
            public boolean supportsGetGeneratedKeys() {
                return false;
            }

            @Override
            public boolean supportsBatchUpdates() {
                return false;
            }
        });

        appender.start();

        List<Status> statuses = context.getStatusManager().getCopyOfStatusList();

        // Verify that we intercepted the error and logged a WARN instead
        boolean hasExpectedWarn = statuses.stream()
                .anyMatch(s -> s.getLevel() == Status.WARN && s.getMessage().contains("Suppressed DBAppender startup error"));
        boolean hasError = statuses.stream()
                .anyMatch(s -> s.getLevel() == Status.ERROR);

        assertTrue(hasExpectedWarn, "Should contain a suppressed warning status.");
        assertFalse(hasError, "Should not contain any error statuses during startup.");
    }

    @Test
    void shouldDowngradeErrorWithExceptionToWarnDuringStartup() {
        ResilientDBAppender appender = new ResilientDBAppender();
        LoggerContext context = new LoggerContext();
        appender.setContext(context);

        appender.setConnectionSource(new ConnectionSource() {
            private boolean started = false;

            @Override
            public void start() {
                started = true;
            }

            @Override
            public void stop() {
                started = false;
            }

            @Override
            public boolean isStarted() {
                return started;
            }

            @Override
            public java.sql.Connection getConnection() throws java.sql.SQLException {
                return null;
            }

            @Override
            public SQLDialectCode getSQLDialectCode() {
                appender.addError("Simulated startup error with exception", new RuntimeException("DB offline"));
                return SQLDialectCode.UNKNOWN_DIALECT;
            }

            @Override
            public boolean supportsGetGeneratedKeys() {
                return false;
            }

            @Override
            public boolean supportsBatchUpdates() {
                return false;
            }
        });

        appender.start();

        List<Status> statuses = context.getStatusManager().getCopyOfStatusList();

        boolean hasExpectedWarn = statuses.stream()
                .anyMatch(s -> s.getLevel() == Status.WARN && s.getMessage().contains("Suppressed DBAppender startup error"));
        boolean hasError = statuses.stream()
                .anyMatch(s -> s.getLevel() == Status.ERROR);

        assertTrue(hasExpectedWarn, "Should contain a suppressed warning status.");
        assertFalse(hasError, "Should not contain any error statuses during startup.");
    }

    @Test
    void shouldNotDowngradeErrorAfterStartup() {
        ResilientDBAppender appender = new ResilientDBAppender();
        LoggerContext context = new LoggerContext();
        appender.setContext(context);

        // Simulate an error occurring during normal runtime (outside of start())
        appender.addError("Normal runtime error");

        List<Status> statuses = context.getStatusManager().getCopyOfStatusList();

        // Verify that the error was NOT downgraded
        boolean hasWarn = statuses.stream().anyMatch(s -> s.getLevel() == Status.WARN);
        boolean hasExpectedError = statuses.stream()
                .anyMatch(s -> s.getLevel() == Status.ERROR && s.getMessage().equals("Normal runtime error"));

        assertFalse(hasWarn, "Should not downgrade to WARN outside of startup.");
        assertTrue(hasExpectedError, "Should contain the standard error status.");
    }

    @Test
    void shouldNotDowngradeErrorWithExceptionAfterStartup() {
        ResilientDBAppender appender = new ResilientDBAppender();
        LoggerContext context = new LoggerContext();
        appender.setContext(context);

        // Simulate an error with an exception occurring during normal runtime
        appender.addError("Normal runtime error with exception", new RuntimeException("DB offline"));

        List<Status> statuses = context.getStatusManager().getCopyOfStatusList();

        boolean hasExpectedError = statuses.stream()
                .anyMatch(s -> s.getLevel() == Status.ERROR && s.getMessage().equals("Normal runtime error with exception"));

        assertTrue(hasExpectedError, "Should contain the standard error status.");
    }

    @Test
    void shouldCompleteStartWithoutException() {
        ResilientDBAppender appender = new ResilientDBAppender();
        LoggerContext context = new LoggerContext();
        appender.setContext(context);

        // Provide a mocked ConnectionSource that returns a known dialect.
        // This prevents DBAppender from trying to connect to a real database during startup,
        // allowing super.start() to complete successfully and cover the normal path.
        appender.setConnectionSource(new ConnectionSource() {
            private boolean started = false;

            @Override
            public void start() {
                started = true;
            }

            @Override
            public void stop() {
                started = false;
            }

            @Override
            public boolean isStarted() {
                return started;
            }

            @Override
            public java.sql.Connection getConnection() {
                return null;
            }

            @Override
            public SQLDialectCode getSQLDialectCode() {
                return SQLDialectCode.H2_DIALECT;
            }

            @Override
            public boolean supportsGetGeneratedKeys() {
                return true;
            }

            @Override
            public boolean supportsBatchUpdates() {
                return true;
            }
        });

        appender.start();

        List<Status> statuses = context.getStatusManager().getCopyOfStatusList();
        boolean hasError = statuses.stream()
                .anyMatch(s -> s.getLevel() == Status.ERROR);

        assertTrue(appender.isStarted(), "Appender should have successfully started.");
        assertFalse(hasError, "Should not contain any error statuses when starting successfully.");
    }
}
