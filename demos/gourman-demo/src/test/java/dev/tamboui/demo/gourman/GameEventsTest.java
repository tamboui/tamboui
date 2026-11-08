/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.gourman;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GameEventsTest {

    @Test
    void startingAGamePublishesGameStarted() {
        GourmanGame game = new GourmanGame();
        List<GameEvent> events = new CopyOnWriteArrayList<>();
        game.addListener(events::add);
        game.newGame();
        assertThat(events).containsExactly(new GameEvent.GameStarted(1, 3));
    }

    @Test
    void movingOntoAPelletPublishesMoveEatAndScore() {
        GourmanGame game = new GourmanGame();
        List<GameEvent> events = new CopyOnWriteArrayList<>();
        game.newGame();
        game.addListener(events::add);
        // Gourman starts at (13, 23) facing LEFT; (12, 23) holds a pellet.
        game.moveGourman(System.nanoTime());
        assertThat(events).containsExactly(
                new GameEvent.GourmanMoved(12, 23, Direction.LEFT),
                new GameEvent.PelletEaten(12, 23, 243),
                new GameEvent.ScoreChanged(10, 10));
    }

    @Test
    void stateAccessorsTrackTheGame() {
        GourmanGame game = new GourmanGame();
        game.newGame();
        assertThat(game.score()).isZero();
        assertThat(game.lives()).isEqualTo(3);
        assertThat(game.level()).isEqualTo(1);
        assertThat(game.pelletsRemaining()).isEqualTo(244);
        assertThat(game.paused()).isFalse();
        assertThat(game.gameOver()).isFalse();
        game.moveGourman(System.nanoTime());
        assertThat(game.score()).isEqualTo(10);
        assertThat(game.pelletsRemaining()).isEqualTo(243);
        game.pause();
        // pause() only suspends active play; the game is still in the READY countdown here.
        game.resume();
        assertThat(game.paused()).isFalse();
    }

    @Test
    void aThrowingListenerIsIsolatedFromTheGameAndOtherListeners() {
        GourmanGame game = new GourmanGame();
        List<GameEvent> events = new CopyOnWriteArrayList<>();
        game.newGame();
        game.addListener(event -> {
            throw new IllegalStateException("misbehaving listener");
        });
        game.addListener(events::add);
        game.moveGourman(System.nanoTime());
        assertThat(events).isNotEmpty();
    }

}
