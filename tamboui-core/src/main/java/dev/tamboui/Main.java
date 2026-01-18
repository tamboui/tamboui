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

    public static void main(String[] args) {
        Capabilities.print(System.out);
    }
}