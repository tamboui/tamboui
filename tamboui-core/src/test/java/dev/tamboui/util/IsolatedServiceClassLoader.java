/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Test classloader that exposes a controlled set of providers for a single
 * {@link java.util.ServiceLoader} service, while NOT inheriting the service files visible to
 * its parent.
 * <p>
 * Class loading is delegated to the parent, so provider classes and the service interface
 * resolve to the same {@link Class} objects the test already uses. Only the
 * {@code META-INF/services/<service>} resource is overridden, with a freshly written service
 * file listing exactly the requested providers (or no file at all when none are requested).
 * This lets tests reproduce the "no provider declared", "provider declared but broken", and
 * "provider not visible to a given loader" situations deterministically.
 */
public final class IsolatedServiceClassLoader extends ClassLoader {

    private final String serviceResource;
    private final List<URL> serviceUrls;

    /**
     * @param parent           loader used for class loading (typically the test's loader)
     * @param serviceInterface fully qualified service interface name
     * @param tempDir          directory used to write the generated service file
     * @param providerClasses  fully qualified provider class names to declare (may be empty)
     */
    public IsolatedServiceClassLoader(ClassLoader parent, String serviceInterface, Path tempDir,
            String... providerClasses) {
        super(parent);
        this.serviceResource = "META-INF/services/" + serviceInterface;
        this.serviceUrls = writeServiceFile(serviceInterface, tempDir, providerClasses);
    }

    private static List<URL> writeServiceFile(String serviceInterface, Path tempDir, String[] providerClasses) {
        if (providerClasses.length == 0) {
            return Collections.emptyList();
        }
        try {
            Path dir = Files.createTempDirectory(tempDir, "isolated");
            Path servicesDir = Files.createDirectories(dir.resolve("META-INF").resolve("services"));
            Path file = servicesDir.resolve(serviceInterface);
            Files.write(file, Arrays.asList(providerClasses));
            List<URL> urls = new ArrayList<URL>();
            urls.add(file.toUri().toURL());
            return urls;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        if (serviceResource.equals(name)) {
            return Collections.enumeration(serviceUrls);
        }
        return super.getResources(name);
    }

    @Override
    public URL getResource(String name) {
        if (serviceResource.equals(name)) {
            return serviceUrls.isEmpty() ? null : serviceUrls.get(0);
        }
        return super.getResource(name);
    }
}
