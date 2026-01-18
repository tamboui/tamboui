/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.capability;

/**
 * Service-provider interface (SPI) for contributing capability information.
 * <p>
 * Assumption is that various parts of TamboUI will have a broad range of capabilities
 * and ways that it explores the environment, properties and various features.
 * 
 * Each provider is responsible for contributing its own information to the report into the following sections:
 * 
 * 1. List of relevant environment variables for this provider
 * 2. List of relevant system properties for this provider
 * 3. List of relevant capabilities/features specific to the provider (meaning a provider should not try report features coming from another module)
 * 
 * Each section is grouped by the provider source; allowing multiple providers to be interested/using the same keys/info.
 * It is up to the caller to decide how to use the report and what to do with the information.
 * 
 * Implementations should be registered via the Java {@link java.util.ServiceLoader}
 * mechanism by creating a file:
 * {@code META-INF/services/dev.tamboui.capability.CapabilityProvider}
 * containing the fully qualified class name of the implementation.
 */
public interface CapabilityProvider {

    /**
     * A stable, user-friendly source id for this provider.
     * <p>
     * The default implementation derives a name from the class name by removing
     * "CapabilityProvider" suffix and converting to lowercase.
     *
     * @return source id (e.g. "tamboui-core", "tamboui-image")
     */
    default String source() {
        String className = getClass().getSimpleName();
        if (className.endsWith("CapabilityProvider")) {
            return className.substring(0, className.length() - "CapabilityProvider".length()).toLowerCase();
        }
        return className.toLowerCase();
    }

    /**
     * Contribute information to the capability report.
     * <p>
     * Providers SHOULD use dotted keys for {@link CapabilityReportBuilder#feature(String, String, Object)}
     * to create stable namespaces (e.g. {@code image.protocol.kitty}).
     * 
     * Providers SHOULD NOT fail when contributing to the report; if something goes wrong, they should report that as a missing feature/info. 
     *
     * @param report report builder
     */
    void contribute(CapabilityReportBuilder report);
}

