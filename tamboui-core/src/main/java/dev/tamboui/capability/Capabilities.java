/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.capability;

import dev.tamboui.util.SafeServiceLoader;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Convenience entrypoints for capability inspection.
 */
public final class Capabilities {

    private Capabilities() {
        // utility
    }

    /**
     * Detects capabilities from all discovered providers.
     *
     * @return the aggregated capability report
     */
    public static CapabilityReport detect() {
        List<CapabilityProvider> providers = new ArrayList<CapabilityProvider>();
        List<String> providerLoadErrors = new ArrayList<String>();

        providers.addAll(SafeServiceLoader.load(CapabilityProvider.class, err ->
                providerLoadErrors.add(String.valueOf(err.getMessage()))));
        providers.sort(Comparator.comparing(CapabilityProvider::source));

        CapabilityReportBuilder builder = new CapabilityReportBuilder();

        for (int i = 0; i < providerLoadErrors.size(); i++) {
            builder.feature("tamboui-core", "capabilityProvider.loadError." + i, providerLoadErrors.get(i));
        }

        for (CapabilityProvider provider : providers) {
            try {
                provider.contribute(builder);
            } catch (Exception e) {
                builder.feature(provider.source(), "error", e.getClass().getName() + ": " + e.getMessage());
            } catch (LinkageError e) {
                builder.feature(provider.source(), "error", e.getClass().getName() + ": " + e.getMessage());
            }
        }

        return builder.build();
    }

    /**
     * Detects capabilities and prints the report to the given stream.
     *
     * @param out the output stream to print to
     */
    public static void print(PrintStream out) {
        detect().print(out);
    }

    /**
     * Prints the capability report to standard output.
     *
     * @param args command-line arguments (ignored)
     */
    public static void main(String[] args) {
        print(System.out);
    }
}


