/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.gourman;

/**
 * Receives every {@link GameEvent} a {@link GourmanGame} emits.
 *
 * <p>
 * Register programmatically with {@link GourmanGame#addListener(GameListener)}, or ship an
 * implementation on the classpath with a {@code META-INF/services/dev.tamboui.demo.gourman.GameListener}
 * entry and the launcher discovers it via {@link java.util.ServiceLoader} — that is how an
 * external publisher (e.g. one that forwards events to Kafka) plugs in without code changes.
 * </p>
 *
 * <p>
 * Listeners are invoked synchronously on the game loop thread, between frames. They must return
 * quickly and never block: hand events to an async transport (a Kafka producer's send, a queue)
 * instead of doing I/O inline. A listener that throws is skipped for that event; the game and the
 * other listeners are unaffected.
 * </p>
 */
@FunctionalInterface
public interface GameListener {

    /**
     * Called for every event the game emits, in emission order.
     *
     * @param event the event that just happened
     */
    void onEvent(GameEvent event);

}
