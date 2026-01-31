/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.css.cascade.CssStyleResolver;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.form.FieldType;
import dev.tamboui.widgets.form.FormState;
import dev.tamboui.widgets.form.ValidationResult;
import dev.tamboui.widgets.form.Validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A form container that manages multiple form fields with grouping support.
 * <p>
 * FormElement simplifies building forms by providing:
 * <ul>
 *   <li>Automatic field generation from FormState</li>
 *   <li>Consistent styling across all fields</li>
 *   <li>Field grouping with optional titles</li>
 *   <li>Centralized validation</li>
 *   <li>Submit handling</li>
 * </ul>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * FormState formState = FormState.builder()
 *     .textField("fullName", "Ada Lovelace")
 *     .textField("email", "ada@analytical.io")
 *     .booleanField("newsletter", true)
 *     .build();
 *
 * form(formState)
 *     .field("fullName", "Full name")
 *     .field("email", "Email", Validators.required(), Validators.email())
 *     .field("newsletter", "Newsletter", FieldType.CHECKBOX)
 *     .labelWidth(14)
 *     .rounded()
 *     .onSubmit(state -> saveUser(state))
 * }</pre>
 *
 * <h2>Grouping Fields</h2>
 * <pre>{@code
 * form(formState)
 *     .group("Profile")
 *         .field("fullName", "Full name")
 *         .field("email", "Email")
 *     .group("Preferences")
 *         .field("newsletter", "Newsletter", FieldType.CHECKBOX)
 *     .labelWidth(14)
 *     .rounded()
 * }</pre>
 *
 * <h2>CSS Child Selectors</h2>
 * <ul>
 *   <li>{@code FormElement-field} - Style for all form fields</li>
 *   <li>{@code FormElement-group-title} - Style for group titles</li>
 * </ul>
 *
 * @see FormState
 * @see FormFieldElement
 */
public final class FormElement extends StyledElement<FormElement> {

    private static final Style DEFAULT_GROUP_TITLE_STYLE = Style.EMPTY.bold();
    private static final int DEFAULT_LABEL_WIDTH = 12;
    private static final int DEFAULT_SPACING = 1;

    private final FormState formState;
    private final List<FieldConfig> fields = new ArrayList<>();
    private final List<GroupConfig> groups = new ArrayList<>();

    // Styling options applied to all fields
    private int labelWidth = DEFAULT_LABEL_WIDTH;
    private int spacing = DEFAULT_SPACING;
    private int fieldSpacing = 1;
    private BorderType borderType;
    private Color borderColor;
    private Color focusedBorderColor;
    private Color errorBorderColor;
    private boolean showInlineErrors = false;

    // Group styling
    private Style groupTitleStyle;

    // Callbacks
    private Consumer<FormState> onSubmit;

    // Submission options
    private boolean submitOnEnter = false;
    private boolean validateOnSubmit = true;

    // Current group being built
    private String currentGroup = null;

    /**
     * Creates a new form element with the given state.
     *
     * @param formState the form state
     */
    public FormElement(FormState formState) {
        this.formState = Objects.requireNonNull(formState, "Form state must not be null");
    }

    /**
     * Returns the FormState associated with this form.
     *
     * @return the form state
     */
    public FormState formState() {
        return formState;
    }

    // ==================== Field Configuration ====================

    /**
     * Adds a text field to the form.
     *
     * @param fieldName the field name in FormState
     * @param label the display label
     * @return this element for chaining
     */
    public FormElement field(String fieldName, String label) {
        return field(fieldName, label, FieldType.TEXT);
    }

    /**
     * Adds a text field with validators to the form.
     *
     * @param fieldName the field name in FormState
     * @param label the display label
     * @param validators the validators to apply
     * @return this element for chaining
     */
    public FormElement field(String fieldName, String label, Validator... validators) {
        return field(fieldName, label, FieldType.TEXT, validators);
    }

    /**
     * Adds a field with a specific type to the form.
     *
     * @param fieldName the field name in FormState
     * @param label the display label
     * @param type the field type
     * @return this element for chaining
     */
    public FormElement field(String fieldName, String label, FieldType type) {
        return field(fieldName, label, type, (Validator[]) null);
    }

    /**
     * Adds a field with a specific type and validators to the form.
     *
     * @param fieldName the field name in FormState
     * @param label the display label
     * @param type the field type
     * @param validators the validators to apply
     * @return this element for chaining
     */
    public FormElement field(String fieldName, String label, FieldType type, Validator... validators) {
        FieldConfig config = new FieldConfig(fieldName, label, type != null ? type : FieldType.TEXT, currentGroup);
        if (validators != null) {
            config.validators.addAll(Arrays.asList(validators));
        }
        fields.add(config);
        return this;
    }

    /**
     * Adds a field with a placeholder.
     *
     * @param fieldName the field name in FormState
     * @param label the display label
     * @param placeholder the placeholder text
     * @return this element for chaining
     */
    public FormElement field(String fieldName, String label, String placeholder) {
        FieldConfig config = new FieldConfig(fieldName, label, FieldType.TEXT, currentGroup);
        config.placeholder = placeholder;
        fields.add(config);
        return this;
    }

    // ==================== Grouping ====================

    /**
     * Starts a new field group.
     * <p>
     * All fields added after this call will belong to this group until
     * another group is started or {@link #endGroup()} is called.
     *
     * @param title the group title
     * @return this element for chaining
     */
    public FormElement group(String title) {
        this.currentGroup = title;
        if (!groups.stream().anyMatch(g -> g.title.equals(title))) {
            groups.add(new GroupConfig(title));
        }
        return this;
    }

    /**
     * Ends the current field group.
     * <p>
     * Fields added after this call will not belong to any group.
     *
     * @return this element for chaining
     */
    public FormElement endGroup() {
        this.currentGroup = null;
        return this;
    }

    // ==================== Styling ====================

    /**
     * Sets the label width for all fields.
     *
     * @param width the label width in characters
     * @return this element for chaining
     */
    public FormElement labelWidth(int width) {
        this.labelWidth = Math.max(0, width);
        return this;
    }

    /**
     * Sets the spacing between label and input for all fields.
     *
     * @param spacing the spacing in characters
     * @return this element for chaining
     */
    public FormElement spacing(int spacing) {
        this.spacing = Math.max(0, spacing);
        return this;
    }

    /**
     * Sets the vertical spacing between fields.
     *
     * @param spacing the spacing in rows
     * @return this element for chaining
     */
    public FormElement fieldSpacing(int spacing) {
        this.fieldSpacing = Math.max(0, spacing);
        return this;
    }

    /**
     * Uses rounded borders for all field inputs.
     *
     * @return this element for chaining
     */
    public FormElement rounded() {
        this.borderType = BorderType.ROUNDED;
        return this;
    }

    /**
     * Sets the border color for all field inputs.
     *
     * @param color the border color
     * @return this element for chaining
     */
    public FormElement borderColor(Color color) {
        this.borderColor = color;
        return this;
    }

    /**
     * Sets the border color when focused for all field inputs.
     *
     * @param color the focused border color
     * @return this element for chaining
     */
    public FormElement focusedBorderColor(Color color) {
        this.focusedBorderColor = color;
        return this;
    }

    /**
     * Sets the border color when validation fails.
     *
     * @param color the error border color
     * @return this element for chaining
     */
    public FormElement errorBorderColor(Color color) {
        this.errorBorderColor = color;
        return this;
    }

    /**
     * Sets whether to show inline error messages.
     *
     * @param show true to show inline errors
     * @return this element for chaining
     */
    public FormElement showInlineErrors(boolean show) {
        this.showInlineErrors = show;
        return this;
    }

    /**
     * Sets the style for group titles.
     *
     * @param style the group title style
     * @return this element for chaining
     */
    public FormElement groupTitleStyle(Style style) {
        this.groupTitleStyle = style;
        return this;
    }

    // ==================== Callbacks ====================

    /**
     * Sets the callback to invoke when the form is submitted.
     *
     * @param onSubmit the submit callback
     * @return this element for chaining
     */
    public FormElement onSubmit(Consumer<FormState> onSubmit) {
        this.onSubmit = onSubmit;
        return this;
    }

    /**
     * Sets whether pressing Enter in any field should trigger form submission.
     * <p>
     * When enabled, pressing Enter in any text field will call {@link #submit()}.
     * Default is {@code false}.
     *
     * @param enabled true to submit on Enter
     * @return this element for chaining
     */
    public FormElement submitOnEnter(boolean enabled) {
        this.submitOnEnter = enabled;
        return this;
    }

    /**
     * Sets whether to validate all fields before submitting.
     * <p>
     * When enabled (default), {@link #submit()} will only call the onSubmit callback
     * if all validations pass.
     *
     * @param enabled true to validate before submitting
     * @return this element for chaining
     */
    public FormElement validateOnSubmit(boolean enabled) {
        this.validateOnSubmit = enabled;
        return this;
    }

    /**
     * Submits the form.
     * <p>
     * If {@link #validateOnSubmit(boolean)} is enabled (default), this method
     * first validates all fields. If any validation fails, the onSubmit callback
     * is not called and this method returns {@code false}.
     * <p>
     * This method can be called from a submit button or any other trigger:
     * <pre>{@code
     * FormElement form = form(formState).onSubmit(state -> save(state));
     *
     * // In a button action:
     * button("Save", () -> form.submit());
     * }</pre>
     *
     * @return true if the form was submitted (validation passed or disabled),
     *         false if validation failed
     */
    public boolean submit() {
        if (validateOnSubmit && !validateAll()) {
            return false;
        }
        if (onSubmit != null) {
            onSubmit.accept(formState);
        }
        return true;
    }

    /**
     * Returns whether submit on Enter is enabled.
     *
     * @return true if Enter triggers submission
     */
    public boolean isSubmitOnEnter() {
        return submitOnEnter;
    }

    // ==================== Validation ====================

    /**
     * Validates all fields in the form.
     * <p>
     * Validation results are stored in the FormState so they persist across renders.
     *
     * @return true if all fields are valid, false otherwise
     */
    public boolean validateAll() {
        boolean allValid = true;
        for (FieldConfig config : fields) {
            ValidationResult result = ValidationResult.valid();
            if (!config.validators.isEmpty()) {
                String value = getFieldValue(config.fieldName, config.type);
                for (Validator validator : config.validators) {
                    result = validator.validate(value);
                    if (!result.isValid()) {
                        allValid = false;
                        break;
                    }
                }
            }
            // Store result in FormState for persistence
            formState.setValidationResult(config.fieldName, result);
        }
        return allValid;
    }

    /**
     * Returns validation errors for all fields.
     *
     * @return a map of field names to error messages (empty for valid fields)
     */
    public Map<String, String> validationErrors() {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldConfig config : fields) {
            if (!config.validators.isEmpty()) {
                String value = getFieldValue(config.fieldName, config.type);
                for (Validator validator : config.validators) {
                    ValidationResult result = validator.validate(value);
                    if (!result.isValid()) {
                        errors.put(config.fieldName, result.errorMessage());
                        break;
                    }
                }
            }
        }
        return Collections.unmodifiableMap(errors);
    }

    private String getFieldValue(String fieldName, FieldType type) {
        switch (type) {
            case CHECKBOX:
            case TOGGLE:
                return String.valueOf(formState.booleanValue(fieldName));
            case SELECT:
                return formState.selectValue(fieldName);
            default:
                return formState.textValue(fieldName);
        }
    }

    // ==================== Sizing ====================

    @Override
    public int preferredWidth() {
        int inputWidth = 20; // Reasonable default
        int borderWidth = borderType != null ? 2 : 0;
        return labelWidth + spacing + inputWidth + borderWidth;
    }

    @Override
    public int preferredHeight() {
        if (fields.isEmpty()) {
            return 1;
        }

        int fieldHeight = borderType != null ? 3 : 1;
        int errorHeight = showInlineErrors ? 1 : 0;
        int totalHeight = 0;

        // Count unique groups for group titles
        long groupCount = groups.size();

        // Fields
        totalHeight += fields.size() * (fieldHeight + errorHeight);

        // Spacing between fields
        if (fields.size() > 1) {
            totalHeight += (fields.size() - 1) * fieldSpacing;
        }

        // Group titles (1 row each + spacing)
        totalHeight += (int) groupCount * 2;

        return totalHeight;
    }

    // ==================== Rendering ====================

    @Override
    protected void renderContent(Frame frame, Rect area, RenderContext context) {
        if (area.isEmpty() || fields.isEmpty()) {
            return;
        }

        // Resolve group title style
        Style effectiveGroupTitleStyle = resolveEffectiveStyle(
                context, "group-title", groupTitleStyle, DEFAULT_GROUP_TITLE_STYLE);

        // Build list of elements to render
        List<RenderItem> items = buildRenderItems(effectiveGroupTitleStyle);

        // Calculate heights and render
        int y = area.y();
        for (RenderItem item : items) {
            if (y >= area.y() + area.height()) {
                break;
            }

            int itemHeight = item.height();
            Rect itemArea = new Rect(area.x(), y, area.width(), Math.min(itemHeight, area.y() + area.height() - y));

            if (item.isGroupTitle()) {
                renderGroupTitle(frame, itemArea, item.groupTitle(), effectiveGroupTitleStyle);
            } else {
                renderField(frame, itemArea, item.fieldConfig(), context);
            }

            y += itemHeight + (item.isGroupTitle() ? 1 : fieldSpacing);
        }
    }

    private List<RenderItem> buildRenderItems(Style groupTitleStyle) {
        List<RenderItem> items = new ArrayList<>();
        String lastGroup = null;

        for (FieldConfig config : fields) {
            // Add group title if this is a new group
            if (config.group != null && !config.group.equals(lastGroup)) {
                items.add(new RenderItem(config.group));
                lastGroup = config.group;
            }
            items.add(new RenderItem(config));
        }

        return items;
    }

    private void renderGroupTitle(Frame frame, Rect area, String title, Style style) {
        String displayTitle = title;
        if (displayTitle.length() > area.width()) {
            displayTitle = displayTitle.substring(0, area.width());
        }
        frame.buffer().setString(area.x(), area.y(), displayTitle, style);
    }

    private void renderField(Frame frame, Rect area, FieldConfig config, RenderContext context) {
        // Create a FormFieldElement for this field
        FormFieldElement field = createFormFieldElement(config);

        // Render the field element
        field.render(frame, area, context);
    }

    private FormFieldElement createFormFieldElement(FieldConfig config) {
        FormFieldElement field;

        switch (config.type) {
            case CHECKBOX:
            case TOGGLE:
                field = new FormFieldElement(config.label,
                        formState.booleanField(config.fieldName), config.type);
                break;

            case SELECT:
                field = new FormFieldElement(config.label,
                        formState.selectField(config.fieldName));
                break;

            default:
                field = new FormFieldElement(config.label,
                        formState.textField(config.fieldName));
                if (config.placeholder != null) {
                    field.placeholder(config.placeholder);
                }
                break;
        }

        // Use FormState for validation persistence
        field.formState(formState, config.fieldName);

        // Apply common styling
        field.labelWidth(labelWidth)
             .spacing(spacing)
             .id("form-field-" + config.fieldName);

        if (borderType != null) {
            field.rounded();
        }
        if (borderColor != null) {
            field.borderColor(borderColor);
        }
        if (focusedBorderColor != null) {
            field.focusedBorderColor(focusedBorderColor);
        }
        if (errorBorderColor != null) {
            field.errorBorderColor(errorBorderColor);
        }
        if (showInlineErrors) {
            field.showInlineErrors(true);
        }

        // Apply validators
        if (!config.validators.isEmpty()) {
            field.validate(config.validators.toArray(new Validator[0]));
        }

        // Submit on Enter handling
        if (submitOnEnter) {
            field.onSubmit(() -> this.submit());
        }

        return field;
    }

    // ==================== Internal Classes ====================

    private static final class FieldConfig {
        final String fieldName;
        final String label;
        final FieldType type;
        final String group;
        final List<Validator> validators = new ArrayList<>();
        String placeholder;

        FieldConfig(String fieldName, String label, FieldType type, String group) {
            this.fieldName = fieldName;
            this.label = label;
            this.type = type;
            this.group = group;
        }
    }

    private static final class GroupConfig {
        final String title;

        GroupConfig(String title) {
            this.title = title;
        }
    }

    private final class RenderItem {
        private final String groupTitle;
        private final FieldConfig fieldConfig;

        RenderItem(String groupTitle) {
            this.groupTitle = groupTitle;
            this.fieldConfig = null;
        }

        RenderItem(FieldConfig fieldConfig) {
            this.groupTitle = null;
            this.fieldConfig = fieldConfig;
        }

        boolean isGroupTitle() {
            return groupTitle != null;
        }

        String groupTitle() {
            return groupTitle;
        }

        FieldConfig fieldConfig() {
            return fieldConfig;
        }

        int height() {
            if (isGroupTitle()) {
                return 1;
            }
            int fieldHeight = borderType != null ? 3 : 1;
            return showInlineErrors ? fieldHeight + 1 : fieldHeight;
        }
    }
}
