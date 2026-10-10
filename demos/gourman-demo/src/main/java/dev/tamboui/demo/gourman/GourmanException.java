/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.gourman;

/** Thrown when the game cannot start or the terminal backend fails. */
public class GourmanException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param message what failed
     * @param cause the underlying failure
     */
    public GourmanException(String message, Throwable cause) {
        super(message, cause);
    }

}
