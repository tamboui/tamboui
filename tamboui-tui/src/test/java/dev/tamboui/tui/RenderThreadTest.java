/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.tui.error.TuiException;

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
        RenderThread.markAsRenderThread();
        assertThat(RenderThread.isRenderThread()).isTrue();
    }

    @Test
    @DisplayName("isRenderThread returns false when called from a different thread")
    void isRenderThread_returnsFalse_whenCalledFromDifferentThread() throws Exception {
        RenderThread.markAsRenderThread();

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
    @DisplayName("checkRenderThread throws when current thread is not marked as render thread")
    void checkRenderThread_throws_whenNotMarked() {
        RenderThread.clearRenderThread();

        assertThatThrownBy(RenderThread::checkRenderThread).isInstanceOf(TuiException.class);
    }

    @Test
    @DisplayName("checkRenderThread throws with informative message when called from wrong thread")
    void checkRenderThread_throwsInformativeException_whenCalledFromWrongThread() throws Exception {
        RenderThread.markAsRenderThread();

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
        RenderThread.markAsRenderThread();

        // Should not throw
        assertThatCode(RenderThread::checkRenderThread).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("clearRenderThread resets the render thread reference")
    void clearRenderThread_resetsReference() {
        RenderThread.markAsRenderThread();
        assertThat(RenderThread.isRenderThread()).isTrue();

        RenderThread.clearRenderThread();
        assertThat(RenderThread.isRenderThread()).isFalse();
    }
}
