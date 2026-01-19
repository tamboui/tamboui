/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

class UiThreadTest {

    @AfterEach
    void cleanup() {
        // Ensure UI thread is cleared after each test
        UiThread.clearUiThread();
    }

    @Test
    @DisplayName("isUiThread returns false when no UI thread is set")
    void isUiThread_returnsFalse_whenNoUiThreadSet() {
        UiThread.clearUiThread();
        assertThat(UiThread.isUiThread()).isFalse();
    }

    @Test
    @DisplayName("isUiThread returns true when called from the UI thread")
    void isUiThread_returnsTrue_whenCalledFromUiThread() {
        UiThread.setUiThread(Thread.currentThread());
        assertThat(UiThread.isUiThread()).isTrue();
    }

    @Test
    @DisplayName("isUiThread returns false when called from a different thread")
    void isUiThread_returnsFalse_whenCalledFromDifferentThread() throws Exception {
        UiThread.setUiThread(Thread.currentThread());

        AtomicBoolean isUiThread = new AtomicBoolean(true);
        CountDownLatch latch = new CountDownLatch(1);

        Thread otherThread = new Thread(() -> {
            isUiThread.set(UiThread.isUiThread());
            latch.countDown();
        });
        otherThread.start();
        latch.await(1, TimeUnit.SECONDS);

        assertThat(isUiThread.get()).isFalse();
    }

    @Test
    @DisplayName("checkUiThread succeeds when no UI thread is set (allows testing)")
    void checkUiThread_succeeds_whenNoUiThreadSet() {
        UiThread.clearUiThread();

        // Should not throw - allows unit tests to run without special setup
        assertThatCode(UiThread::checkUiThread).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("checkUiThread throws with informative message when called from wrong thread")
    void checkUiThread_throwsInformativeException_whenCalledFromWrongThread() throws Exception {
        UiThread.setUiThread(Thread.currentThread());

        AtomicReference<Throwable> caught = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Thread otherThread = new Thread(() -> {
            try {
                UiThread.checkUiThread();
            } catch (Throwable t) {
                caught.set(t);
            }
            latch.countDown();
        }, "test-thread");
        otherThread.start();
        latch.await(1, TimeUnit.SECONDS);

        assertThat(caught.get())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("test-thread")
            .hasMessageContaining("UI thread");
    }

    @Test
    @DisplayName("checkUiThread succeeds when on UI thread")
    void checkUiThread_succeeds_whenOnUiThread() {
        UiThread.setUiThread(Thread.currentThread());

        // Should not throw
        assertThatCode(UiThread::checkUiThread).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("clearUiThread resets the UI thread reference")
    void clearUiThread_resetsReference() {
        UiThread.setUiThread(Thread.currentThread());
        assertThat(UiThread.isUiThread()).isTrue();

        UiThread.clearUiThread();
        assertThat(UiThread.isUiThread()).isFalse();
    }
}
