/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.ratty;

/**
 * 3D object file formats supported by Ratty Graphics Protocol.
 */
public enum ObjectFormat {
    /**
     * Wavefront OBJ format (.obj files).
     */
    OBJ("obj"),

    /**
     * GL Transmission Format (.glb files).
     */
    GLB("glb");

    private final String formatString;

    ObjectFormat(String formatString) {
        this.formatString = formatString;
    }

    /**
     * Returns the format string used in RGP commands.
     *
     * @return the format string
     */
    public String formatString() {
        return formatString;
    }

    @Override
    public String toString() {
        return formatString;
    }
}
