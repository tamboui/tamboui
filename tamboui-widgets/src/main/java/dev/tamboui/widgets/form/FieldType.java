/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.form;

/**
 * The type of form field input.
 * <p>
 * Different field types render different UI components:
 * <ul>
 *   <li>{@link #TEXT} - Single-line text input (default)</li>
 *   <li>{@link #TEXT_AREA} - Multi-line text input</li>
 *   <li>{@link #CHECKBOX} - Boolean checkbox</li>
 *   <li>{@link #TOGGLE} - On/off toggle switch</li>
 *   <li>{@link #SELECT} - Dropdown selection</li>
 * </ul>
 */
public enum FieldType {

    /**
     * Single-line text input (default).
     */
    TEXT,

    /**
     * Multi-line text input for longer content.
     */
    TEXT_AREA,

    /**
     * Boolean checkbox for yes/no values.
     */
    CHECKBOX,

    /**
     * On/off toggle switch for boolean values.
     */
    TOGGLE,

    /**
     * Dropdown selection from a list of options.
     */
    SELECT
}
