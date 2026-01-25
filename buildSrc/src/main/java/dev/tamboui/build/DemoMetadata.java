/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.build;

import java.io.Serializable;
import java.util.Set;

/**
 * Immutable metadata for a demo project.
 * This is used to pass information from demo projects to the documentation build.
 */
public record DemoMetadata(
        String id,
        String displayName,
        String description,
        String module,
        Set<String> tags,
        String castFileName
) implements Serializable {

    /**
     * Creates metadata with default empty values for optional fields.
     */
    public static DemoMetadata of(String id, String displayName, String castFileName) {
        return new DemoMetadata(id, displayName, "", "Other", Set.of(), castFileName);
    }
}
