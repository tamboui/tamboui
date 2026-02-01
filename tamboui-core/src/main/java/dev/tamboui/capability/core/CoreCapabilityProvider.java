/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.capability.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dev.tamboui.capability.CapabilityProvider;
import dev.tamboui.capability.CapabilityReportBuilder;
import dev.tamboui.terminal.BackendProvider;
import dev.tamboui.util.SafeServiceLoader;

/**
 * Core capability contributor: prints what core assumes/uses and what it can infer from the environment.
 */
public final class CoreCapabilityProvider implements CapabilityProvider {

    /**
     * Creates a new core capability provider.
     */
    public CoreCapabilityProvider() {
    }

    @Override
    public String source() {
        return "tamboui-core";
    }

    @Override
    public void contribute(CapabilityReportBuilder report) {
        report.property(source(), "java.version");
        report.property(source(), "java.vendor");
        report.property(source(), "os.name");
        report.property(source(), "os.arch");
        report.property(source(), "os.version");
        report.property(source(), "tamboui.backend");

        report.env(source(), "TERM");
        report.env(source(), "COLORTERM");
        report.env(source(), "TERM_PROGRAM");
        report.env(source(), "TERM_PROGRAM_VERSION");
        report.env(source(), "LC_ALL");
        report.env(source(), "LANG");
        report.env(source(), "TAMBOUI_BACKEND");

        List<String> loadErrors = new ArrayList<String>();
        List<BackendProvider> providers = SafeServiceLoader.load(BackendProvider.class, err -> {
            loadErrors.add(String.valueOf(err.getMessage()));
        });

        report.feature(source(), "backend.provider_count", providers.size());
        report.feature(source(), "backend.provider_names", String.join(",", providers.stream().map(BackendProvider::name).collect(Collectors.toList())));
        for (int i = 0; i < loadErrors.size(); i++) {
            report.feature(source(), "backend.provider_error." + i, loadErrors.get(i));
        }
    }

}


