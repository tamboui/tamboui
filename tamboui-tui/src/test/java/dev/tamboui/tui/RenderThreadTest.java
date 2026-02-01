/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import dev.tamboui.tui.error.TuiException;

import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

class RenderThreadTest {

    @AfterEach
    void cleanup() {
        // Ensure render thread is cleared after each test
        RenderThread.clearRenderThread();
    }

    @Test
    @DisplayName("isRenderThread returns false when no render thread is set")
    void isRenderThread_returnsFalse_whenNoRenderThreadSet() {
        RenderThread.clearRenderThread();
        assertThat(RenderThread.isRenderThread()).isFalse();
    }

    @Test
    @DisplayName("isRenderThread returns true when called from the render thread")
    void isRenderThread_returnsTrue_whenCalledFromRenderThread() {
        RenderThread.setRenderThread(Thread.currentThread());
        assertThat(RenderThread.isRenderThread()).isTrue();
    }

    @Test
    @DisplayName("isRenderThread returns false when called from a different thread")
    void isRenderThread_returnsFalse_whenCalledFromDifferentThread() throws Exception {
        RenderThread.setRenderThread(Thread.currentThread());

        AtomicBoolean isRenderThread = new AtomicBoolean(true);
        CountDownLatch latch = new CountDownLatch(1);

        Thread otherThread = new Thread(() -> {
            isRenderThread.set(RenderThread.isRenderThread());
            latch.countDown();
        });
        otherThread.start();
        latch.await(1, TimeUnit.SECONDS);

        assertThat(isRenderThread.get()).isFalse();
    }

    @Test
    @DisplayName("checkRenderThread succeeds when no render thread is set (allows testing)")
    void checkRenderThread_succeeds_whenNoRenderThreadSet() {
        RenderThread.clearRenderThread();

        // Should not throw - allows unit tests to run without special setup
        assertThatCode(RenderThread::checkRenderThread).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("checkRenderThread throws with informative message when called from wrong thread")
    void checkRenderThread_throwsInformativeException_whenCalledFromWrongThread() throws Exception {
        RenderThread.setRenderThread(Thread.currentThread());

        AtomicReference<Throwable> caught = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Thread otherThread = new Thread(() -> {
            try {
                RenderThread.checkRenderThread();
            } catch (Throwable t) {
                caught.set(t);
            }
            latch.countDown();
        }, "test-thread");
        otherThread.start();
        latch.await(1, TimeUnit.SECONDS);

        assertThat(caught.get())
            .isInstanceOf(TuiException.class)
            .hasMessageContaining("test-thread")
            .hasMessageContaining("render thread");
    }

    @Test
    @DisplayName("checkRenderThread succeeds when on render thread")
    void checkRenderThread_succeeds_whenOnRenderThread() {
        RenderThread.setRenderThread(Thread.currentThread());

        // Should not throw
        assertThatCode(RenderThread::checkRenderThread).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("clearRenderThread resets the render thread reference")
    void clearRenderThread_resetsReference() {
        RenderThread.setRenderThread(Thread.currentThread());
        assertThat(RenderThread.isRenderThread()).isTrue();

        RenderThread.clearRenderThread();
        assertThat(RenderThread.isRenderThread()).isFalse();
    }
}
