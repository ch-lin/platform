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
