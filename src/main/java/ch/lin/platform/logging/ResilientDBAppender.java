package ch.lin.platform.logging;

import ch.qos.logback.classic.db.DBAppender;

public class ResilientDBAppender extends DBAppender {

    private boolean isStarting = false;

    @Override
    public void start() {
        isStarting = true;
        try {
            super.start();
        } catch (Exception e) {
            addWarn("Suppressed DBAppender startup exception: " + e.getMessage(), e);
        } finally {
            isStarting = false;
        }
    }

    @Override
    public void addError(String msg, Throwable ex) {
        if (isStarting) {
            // Downgrade ERROR to WARN during startup to prevent Spring Boot from crashing
            addWarn("Suppressed DBAppender startup error: " + msg, ex);
        } else {
            super.addError(msg, ex);
        }
    }

    @Override
    public void addError(String msg) {
        if (isStarting) {
            addWarn("Suppressed DBAppender startup error: " + msg);
        } else {
            super.addError(msg);
        }
    }
}
