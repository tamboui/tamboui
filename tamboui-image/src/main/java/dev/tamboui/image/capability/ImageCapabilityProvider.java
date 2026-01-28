/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.image.capability;

import dev.tamboui.capability.CapabilityProvider;
import dev.tamboui.capability.CapabilityReportBuilder;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Capability contributor for {@code tamboui-image}.
 */
public final class ImageCapabilityProvider implements CapabilityProvider {

    /**
     * Creates a new image capability provider.
     */
    public ImageCapabilityProvider() {
    }

    @Override
    public String source() {
        return "tamboui-image";
    }

    @Override
    public void contribute(CapabilityReportBuilder report) {
        TerminalImageCapabilities caps = TerminalImageCapabilities.detect();
        Set<TerminalImageProtocol> supported = caps.supportedProtocols();

        report.feature(source(), "best_image_protocol", caps.bestSupport());
        report.feature(source(), "image_protocols", supported.stream().map(Enum::name).collect(Collectors.joining(", ")));
        for (TerminalImageProtocol protocol : TerminalImageProtocol.values()) {
            report.feature(source(), "image_protocol." + protocol.name().toLowerCase(), caps.supports(protocol));
        }
        report.feature(source(), "supports_native_images", caps.supportsNativeImages());
    }
}


