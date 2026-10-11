/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui;

import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.tui.error.TuiException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RenderThreadLenientTest {

    @Test
    @DisplayName("checkRenderThread passes when no render loop is active")
    void passesWhenNoRenderLoopIsActive() {
        assertThatCode(RenderThread::checkRenderThread).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("checkRenderThread enforces while a render loop is active on another thread")
    void enforcesWhileARenderLoopIsActiveElsewhere() throws Exception {
        CountDownLatch marked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread loop = new Thread(() -> {
            RenderThread.markAsRenderThread();
            marked.countDown();
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            RenderThread.clearRenderThread();
        });
        loop.start();
        marked.await();
        try {
            assertThatThrownBy(RenderThread::checkRenderThread).isInstanceOf(TuiException.class);
        } finally {
            release.countDown();
            loop.join();
        }
        // The loop has ended, so the check is lenient again
        assertThatCode(RenderThread::checkRenderThread).doesNotThrowAnyException();
    }
}
