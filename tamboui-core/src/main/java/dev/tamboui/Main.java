package dev.tamboui;

import dev.tamboui.capability.Capabilities;

/**
 * Main entry point for the TamboUI core jar.
 * Used for printing diagnostic info.
 * 
 * See Capabilities for more details.
 * 
 * @see dev.tamboui.capability.Capabilities
 * 
 */
public class Main {

    /** Private constructor to prevent instantiation. */
    private Main() {
    }

    /**
     * Main entry point that prints diagnostic information.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        Capabilities.print(System.out);
    }
}