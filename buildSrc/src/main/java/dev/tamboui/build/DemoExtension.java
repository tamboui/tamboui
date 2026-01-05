package dev.tamboui.build;

import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;

/**
 * Extension interface for demo configuration.
 */
public interface DemoExtension {
    Property<String> getDisplayName();
    Property<String> getDescription();
    Property<String> getModule();
    SetProperty<String> getTags();
}
