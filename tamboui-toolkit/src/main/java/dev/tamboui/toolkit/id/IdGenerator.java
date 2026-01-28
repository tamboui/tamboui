package dev.tamboui.toolkit.id;

import dev.tamboui.toolkit.element.Element;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates unique IDs for elements and components.
 * <p>
 * IDs are formed by combining the class name with a unique counter.
 * This ensures that each instance receives a distinct identifier.
 */
public class IdGenerator {
    private static final IdGenerator DEFAULT_INSTANCE = new IdGenerator();

    private final AtomicLong ID_COUNTER = new AtomicLong(0);

    private IdGenerator() {

    }

    /**
     * Generates a unique ID for the given element.
     * @param element the element to generate an ID for
     * @return a unique ID string
     */
    public String generateId(Element element) {
        return "_auto_" + element.getClass().getSimpleName() + "_" + ID_COUNTER.getAndIncrement();
    }

    /**
     * Generates a new unique ID for the given element using the default instance.
     * @param element the element to generate an ID for
     * @return a unique ID string
     */
    public static String newId(Element element) {
        return DEFAULT_INSTANCE.generateId(element);
    }

    /**
     * Creates a new ID generator instance with its own counter.
     *
     * @return a new ID generator
     */
    public static IdGenerator create() {
        return new IdGenerator();
    }

}
