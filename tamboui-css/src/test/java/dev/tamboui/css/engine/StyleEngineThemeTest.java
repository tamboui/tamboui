package dev.tamboui.css.engine;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StyleEngineThemeTest {

    @Test
    void testSetThemeVariables() {
        StyleEngine engine = StyleEngine.create();

        Map<String, String> variables = new HashMap<>();
        variables.put("primary", "#0000ff");
        variables.put("secondary", "#00ff00");

        engine.setThemeVariables(variables);

        // Verify variables were stored
        String primary = engine.resolveVariable("primary");
        assertEquals("#0000ff", primary);
    }

    @Test
    void testResolveThemeVariable() {
        StyleEngine engine = StyleEngine.create();

        Map<String, String> variables = new HashMap<>();
        variables.put("primary", "#0000ff");

        engine.setThemeVariables(variables);

        // Verify variable can be resolved
        String value = engine.resolveVariable("primary");
        assertEquals("#0000ff", value);
    }

    @Test
    void testResolveMissingVariable() {
        StyleEngine engine = StyleEngine.create();

        Map<String, String> variables = new HashMap<>();
        variables.put("primary", "#0000ff");

        engine.setThemeVariables(variables);

        // Non-existent variable should return null
        String value = engine.resolveVariable("nonexistent");
        assertNull(value);
    }
}
