///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-tui:LATEST
//DEPS dev.tamboui:tamboui-widgets:LATEST
//DEPS dev.tamboui:tamboui-panama-backend:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
//SOURCES GourmanGame.java GourmanException.java GameEvent.java GameListener.java Ghost.java Maze.java Direction.java

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.gourman;

import java.util.ServiceLoader;

/**
 * Entry point: launches the full-screen Gourman game. Run with {@code gourman/gourman}.
 *
 * <p>
 * {@link GameListener} implementations found on the classpath via {@link ServiceLoader} (a
 * {@code META-INF/services/dev.tamboui.demo.gourman.GameListener} entry) are subscribed before the
 * game starts, so an external event publisher — e.g. one forwarding every {@link GameEvent} to
 * Kafka — plugs in by just being on the classpath.
 * </p>
 */
public final class Gourman {

    private Gourman() {
    }

    /**
     * Launches the game with every {@link ServiceLoader}-discovered listener subscribed.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        GourmanGame game = new GourmanGame();
        ServiceLoader.load(GameListener.class).forEach(game::addListener);
        game.run();
    }

}
