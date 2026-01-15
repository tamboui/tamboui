/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout.cassowary;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Internal symbol type used in the simplex tableau.
 *
 * <p>Symbols represent variables in the internal tableau representation.
 * They can be external (user-defined variables), slack (for inequalities),
 * error (for non-required constraints), or dummy (temporary markers).
 */
final class Symbol {

    /**
     * The type of symbol.
     */
    enum Type {
        /** External variable defined by the user. */
        EXTERNAL,
        /** Slack variable added for inequality constraints. */
        SLACK,
        /** Error variable for non-required constraints. */
        ERROR,
        /** Dummy variable used during constraint addition. */
        DUMMY
    }

    private static final AtomicLong ID_GENERATOR = new AtomicLong();

    private final long id;
    private final Type type;

    private Symbol(Type type) {
        this.id = ID_GENERATOR.incrementAndGet();
        this.type = type;
    }

    /**
     * Creates an external symbol for a user-defined variable.
     *
     * @return a new external symbol
     */
    static Symbol external() {
        return new Symbol(Type.EXTERNAL);
    }

    /**
     * Creates a slack symbol for an inequality constraint.
     *
     * @return a new slack symbol
     */
    static Symbol slack() {
        return new Symbol(Type.SLACK);
    }

    /**
     * Creates an error symbol for a non-required constraint.
     *
     * @return a new error symbol
     */
    static Symbol error() {
        return new Symbol(Type.ERROR);
    }

    /**
     * Creates a dummy symbol.
     *
     * @return a new dummy symbol
     */
    static Symbol dummy() {
        return new Symbol(Type.DUMMY);
    }

    /**
     * Returns the unique identifier of this symbol.
     *
     * @return the symbol ID
     */
    long id() {
        return id;
    }

    /**
     * Returns the type of this symbol.
     *
     * @return the symbol type
     */
    Type type() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Symbol)) {
            return false;
        }
        Symbol symbol = (Symbol) o;
        return id == symbol.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public String toString() {
        switch (type) {
            case EXTERNAL:
                return "e" + id;
            case SLACK:
                return "s" + id;
            case ERROR:
                return "r" + id;
            case DUMMY:
                return "d" + id;
            default:
                return "?" + id;
        }
    }
}
