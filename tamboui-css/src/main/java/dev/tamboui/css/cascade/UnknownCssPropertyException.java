/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.cascade;

/**
 * Exception thrown when an unknown CSS property is encountered
 * and {@link UnknownPropertyBehavior#FAIL} is configured.
 */
public class UnknownCssPropertyException extends RuntimeException {

    /** The unknown CSS property name. */
    private final String propertyName;
    /** The value associated with the unknown property. */
    private final String propertyValue;

    /**
     * Creates a new exception for an unknown CSS property.
     *
     * @param propertyName  the unknown property name
     * @param propertyValue the property value
     */
    public UnknownCssPropertyException(String propertyName, String propertyValue) {
        super("Unknown CSS property: " + propertyName + " with value: " + propertyValue);
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
    }

    /**
     * Returns the unknown property name.
     *
     * @return the property name
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Returns the property value.
     *
     * @return the property value
     */
    public String getPropertyValue() {
        return propertyValue;
    }
}
