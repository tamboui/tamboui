/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.checkbox.Checkbox;
import dev.tamboui.widgets.checkbox.CheckboxState;
import dev.tamboui.widgets.form.BooleanFieldState;
import dev.tamboui.widgets.form.FieldType;
import dev.tamboui.widgets.form.FormState;
import dev.tamboui.widgets.form.SelectFieldState;
import dev.tamboui.widgets.form.ValidationResult;
import dev.tamboui.widgets.form.Validator;
import dev.tamboui.widgets.input.TextInput;
import dev.tamboui.widgets.input.TextInputState;
import dev.tamboui.widgets.select.Select;
import dev.tamboui.widgets.select.SelectState;
import dev.tamboui.widgets.toggle.Toggle;
import dev.tamboui.widgets.toggle.ToggleState;

import static dev.tamboui.toolkit.Toolkit.handleTextInputKey;

/**
 * A form field element that pairs a label with an input control.
 * <p>
 * FormFieldElement reduces boilerplate by combining a label and input into a single
 * element with consistent layout and styling. It supports multiple field types
 * including text inputs, checkboxes, toggles, and select dropdowns.
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * // With TextInputState
 * formField("Full name", nameState)
 *     .labelWidth(14)
 *     .rounded()
 *     .borderColor(Color.DARK_GRAY)
 *     .focusedBorderColor(Color.CYAN)
 *
 * // With FormState
 * formField("Email", FORM.textField("email"))
 *     .placeholder("you@example.com")
 *     .validate(Validators.required(), Validators.email())
 * }</pre>
 *
 * <h2>CSS Child Selectors</h2>
 * <ul>
 *   <li>{@code FormFieldElement-label} - The label text style (default: dim)</li>
 *   <li>{@code FormFieldElement-input} - The input wrapper (delegates to child element's CSS)</li>
 *   <li>{@code FormFieldElement-error} - The inline error message style (default: red)</li>
 * </ul>
 *
 * @see dev.tamboui.widgets.form.FormState
 * @see dev.tamboui.widgets.form.Validators
 */
public final class FormFieldElement extends StyledElement<FormFieldElement> {

    private static final Style DEFAULT_LABEL_STYLE = Style.EMPTY.dim();
    private static final Style DEFAULT_ERROR_STYLE = Style.EMPTY.fg(Color.RED);
    private static final int DEFAULT_LABEL_WIDTH = 12;
    private static final int DEFAULT_SPACING = 1;

    // Core state
    private String label;
    private TextInputState textState;
    private BooleanFieldState booleanState;
    private SelectFieldState selectState;
    private FieldType fieldType = FieldType.TEXT;

    // Layout
    private int labelWidth = DEFAULT_LABEL_WIDTH;
    private int spacing = DEFAULT_SPACING;

    // Styling
    private Style labelStyle;
    private Style errorStyle;
    private String placeholder = "";
    private BorderType borderType;
    private Color borderColor;
    private Color focusedBorderColor;
    private Color errorBorderColor;

    // Checkbox styling
    private String checkedSymbol;
    private String uncheckedSymbol;
    private Color checkedColor;
    private Color uncheckedColor;

    // Password/masked input
    private Character maskChar;

    // Validation
    private final List<Validator> validators = new ArrayList<>();
    private boolean showInlineErrors = false;
    private ValidationResult lastValidationResult = ValidationResult.valid();

    // FormState integration (for persistent validation state)
    private FormState formState;
    private String fieldName;

    // Callbacks
    private Runnable onSubmit;

    // Navigation
    private boolean arrowNavigation = false;

    /**
     * Creates a new form field with the given label and text input state.
     *
     * @param label the field label
     * @param state the text input state
     */
    public FormFieldElement(String label, TextInputState state) {
        this.label = label != null ? label : "";
        this.textState = state != null ? state : new TextInputState();
        this.fieldType = FieldType.TEXT;
        this.focusable = true;
    }

    /**
     * Creates a new form field with the given label and a new text input state.
     *
     * @param label the field label
     */
    public FormFieldElement(String label) {
        this(label, new TextInputState());
    }

    /**
     * Creates a new form field with the given label and boolean state.
     *
     * @param label the field label
     * @param state the boolean field state
     * @param type the field type (CHECKBOX or TOGGLE)
     */
    public FormFieldElement(String label, BooleanFieldState state, FieldType type) {
        this.label = label != null ? label : "";
        this.booleanState = state != null ? state : new BooleanFieldState();
        this.fieldType = (type == FieldType.CHECKBOX || type == FieldType.TOGGLE) ? type : FieldType.CHECKBOX;
        this.focusable = true;
    }

    /**
     * Creates a new form field with the given label and select state.
     *
     * @param label the field label
     * @param state the select field state
     */
    public FormFieldElement(String label, SelectFieldState state) {
        this.label = label != null ? label : "";
        this.selectState = state;
        this.fieldType = FieldType.SELECT;
        this.focusable = true;
    }

    // ==================== Core Configuration ====================

    /**
     * Sets the field label.
     *
     * @param label the label text
     * @return this element for chaining
     */
    public FormFieldElement label(String label) {
        this.label = label != null ? label : "";
        return this;
    }

    /**
     * Sets the text input state.
     *
     * @param state the text input state
     * @return this element for chaining
     */
    public FormFieldElement state(TextInputState state) {
        this.textState = state != null ? state : new TextInputState();
        this.booleanState = null;
        this.selectState = null;
        this.fieldType = FieldType.TEXT;
        return this;
    }

    /**
     * Sets the boolean field state.
     *
     * @param state the boolean field state
     * @return this element for chaining
     */
    public FormFieldElement state(BooleanFieldState state) {
        this.booleanState = state != null ? state : new BooleanFieldState();
        this.textState = null;
        this.selectState = null;
        if (fieldType != FieldType.CHECKBOX && fieldType != FieldType.TOGGLE) {
            this.fieldType = FieldType.CHECKBOX;
        }
        return this;
    }

    /**
     * Sets the select field state.
     *
     * @param state the select field state
     * @return this element for chaining
     */
    public FormFieldElement state(SelectFieldState state) {
        this.selectState = state;
        this.textState = null;
        this.booleanState = null;
        this.fieldType = FieldType.SELECT;
        return this;
    }

    /**
     * Sets the initial text value for a text field.
     *
     * @param value the initial value
     * @return this element for chaining
     */
    public FormFieldElement value(String value) {
        if (textState == null) {
            textState = new TextInputState();
            booleanState = null;
            selectState = null;
            fieldType = FieldType.TEXT;
        }
        textState.setText(value != null ? value : "");
        return this;
    }

    /**
     * Sets the field type.
     *
     * @param type the field type
     * @return this element for chaining
     */
    public FormFieldElement type(FieldType type) {
        this.fieldType = type != null ? type : FieldType.TEXT;
        return this;
    }

    // ==================== Layout ====================

    /**
     * Sets the label width in characters.
     *
     * @param width the label width
     * @return this element for chaining
     */
    public FormFieldElement labelWidth(int width) {
        this.labelWidth = Math.max(0, width);
        return this;
    }

    /**
     * Sets the spacing between label and input.
     *
     * @param spacing the spacing in characters
     * @return this element for chaining
     */
    public FormFieldElement spacing(int spacing) {
        this.spacing = Math.max(0, spacing);
        return this;
    }

    // ==================== Styling ====================

    /**
     * Sets the label style.
     *
     * @param style the label style
     * @return this element for chaining
     */
    public FormFieldElement labelStyle(Style style) {
        this.labelStyle = style;
        return this;
    }

    /**
     * Sets the placeholder text for text inputs.
     *
     * @param placeholder the placeholder text
     * @return this element for chaining
     */
    public FormFieldElement placeholder(String placeholder) {
        this.placeholder = placeholder != null ? placeholder : "";
        return this;
    }

    /**
     * Uses rounded borders for the input.
     *
     * @return this element for chaining
     */
    public FormFieldElement rounded() {
        this.borderType = BorderType.ROUNDED;
        return this;
    }

    /**
     * Sets the border color for the input.
     *
     * @param color the border color
     * @return this element for chaining
     */
    public FormFieldElement borderColor(Color color) {
        this.borderColor = color;
        return this;
    }

    /**
     * Sets the border color when focused.
     *
     * @param color the focused border color
     * @return this element for chaining
     */
    public FormFieldElement focusedBorderColor(Color color) {
        this.focusedBorderColor = color;
        return this;
    }

    /**
     * Sets the border color when validation fails.
     *
     * @param color the error border color
     * @return this element for chaining
     */
    public FormFieldElement errorBorderColor(Color color) {
        this.errorBorderColor = color;
        return this;
    }

    // ==================== Checkbox/Toggle Styling ====================

    /**
     * Sets the symbol to display when checkbox is checked.
     *
     * @param symbol the checked symbol (e.g., "[x]", "●", "✓")
     * @return this element for chaining
     */
    public FormFieldElement checkedSymbol(String symbol) {
        this.checkedSymbol = symbol;
        return this;
    }

    /**
     * Sets the symbol to display when checkbox is unchecked.
     *
     * @param symbol the unchecked symbol (e.g., "[ ]", "○", "")
     * @return this element for chaining
     */
    public FormFieldElement uncheckedSymbol(String symbol) {
        this.uncheckedSymbol = symbol;
        return this;
    }

    /**
     * Sets the color when checkbox is checked.
     *
     * @param color the checked color
     * @return this element for chaining
     */
    public FormFieldElement checkedColor(Color color) {
        this.checkedColor = color;
        return this;
    }

    /**
     * Sets the color when checkbox is unchecked.
     *
     * @param color the unchecked color
     * @return this element for chaining
     */
    public FormFieldElement uncheckedColor(Color color) {
        this.uncheckedColor = color;
        return this;
    }

    /**
     * Sets the checkbox to use bullet-style symbols (● / ○).
     *
     * @return this element for chaining
     */
    public FormFieldElement bulletStyle() {
        this.checkedSymbol = "●";
        this.uncheckedSymbol = "○";
        return this;
    }

    // ==================== Password/Masked Input ====================

    /**
     * Masks the input text with the default mask character ('*').
     * <p>
     * When enabled, the actual text is stored but displayed as mask characters.
     * Useful for password fields.
     *
     * @return this element for chaining
     */
    public FormFieldElement masked() {
        return masked('*');
    }

    /**
     * Masks the input text with the specified character.
     * <p>
     * When enabled, the actual text is stored but displayed as the mask character.
     * Useful for password fields.
     *
     * @param maskChar the character to display instead of actual text
     * @return this element for chaining
     */
    public FormFieldElement masked(char maskChar) {
        this.maskChar = maskChar;
        return this;
    }

    /**
     * Sets the error message style.
     *
     * @param style the error style
     * @return this element for chaining
     */
    public FormFieldElement errorStyle(Style style) {
        this.errorStyle = style;
        return this;
    }

    // ==================== Validation ====================

    /**
     * Adds validators to this field.
     *
     * @param validators the validators to add
     * @return this element for chaining
     */
    public FormFieldElement validate(Validator... validators) {
        this.validators.addAll(Arrays.asList(validators));
        return this;
    }

    /**
     * Sets whether to show inline error messages.
     *
     * @param show true to show inline errors
     * @return this element for chaining
     */
    public FormFieldElement showInlineErrors(boolean show) {
        this.showInlineErrors = show;
        return this;
    }

    /**
     * Associates this field with a FormState for persistent validation state.
     * When set, validation results are stored in the FormState instead of locally.
     *
     * @param formState the form state
     * @param fieldName the field name in the form state
     * @return this element for chaining
     */
    public FormFieldElement formState(FormState formState, String fieldName) {
        this.formState = formState;
        this.fieldName = fieldName;
        // Auto-apply masking for password fields
        if (formState != null && formState.isMaskedField(fieldName)) {
            masked();
        }
        return this;
    }

    /**
     * Validates the current field value.
     *
     * @return the validation result
     */
    public ValidationResult validateField() {
        ValidationResult result;
        if (validators.isEmpty()) {
            result = ValidationResult.valid();
        } else {
            String value = getCurrentTextValue();
            result = ValidationResult.valid();
            for (Validator validator : validators) {
                ValidationResult r = validator.validate(value);
                if (!r.isValid()) {
                    result = r;
                    break;
                }
            }
        }

        // Store in FormState if available, otherwise store locally
        if (formState != null && fieldName != null) {
            formState.setValidationResult(fieldName, result);
        } else {
            lastValidationResult = result;
        }
        return result;
    }

    /**
     * Returns the last validation result.
     *
     * @return the last validation result
     */
    public ValidationResult lastValidation() {
        // Get from FormState if available, otherwise return local
        if (formState != null && fieldName != null) {
            return formState.validationResult(fieldName);
        }
        return lastValidationResult;
    }

    private String getCurrentTextValue() {
        if (textState != null) {
            return textState.text();
        }
        return "";
    }

    // ==================== Callbacks ====================

    /**
     * Sets a callback to be invoked when Enter is pressed.
     *
     * @param onSubmit the callback
     * @return this element for chaining
     */
    public FormFieldElement onSubmit(Runnable onSubmit) {
        this.onSubmit = onSubmit;
        return this;
    }

    /**
     * Enables arrow key navigation for this field.
     * <p>
     * When enabled, pressing Up/Down arrows in text fields and boolean fields
     * will return {@link EventResult#FOCUS_PREVIOUS}/{@link EventResult#FOCUS_NEXT}
     * to navigate between fields. Select fields still use Up/Down for selection.
     *
     * @param enabled true to enable arrow navigation
     * @return this element for chaining
     */
    public FormFieldElement arrowNavigation(boolean enabled) {
        this.arrowNavigation = enabled;
        return this;
    }

    // ==================== Sizing ====================

    @Override
    public int preferredWidth() {
        // Label width + spacing + minimum input width + border
        int inputWidth = Math.max(10, placeholder.length());
        int borderWidth = borderType != null ? 2 : 0;
        return labelWidth + spacing + inputWidth + borderWidth;
    }

    @Override
    public int preferredHeight() {
        // For bordered inputs: 3 rows; for non-bordered: 1 row
        // Add 1 row if showing inline errors
        int baseHeight = borderType != null ? 3 : 1;
        return showInlineErrors && !lastValidation().isValid() ? baseHeight + 1 : baseHeight;
    }

    // ==================== Event Handling ====================

    @Override
    public boolean isFocusable() {
        return focusable;
    }

    @Override
    public EventResult handleKeyEvent(KeyEvent event, boolean focused) {
        if (!focused) {
            return EventResult.UNHANDLED;
        }

        // Handle based on field type
        switch (fieldType) {
            case TEXT:
            case TEXT_AREA:
                return handleTextFieldKey(event);

            case CHECKBOX:
            case TOGGLE:
                return handleBooleanFieldKey(event);

            case SELECT:
                return handleSelectFieldKey(event);

            default:
                return EventResult.UNHANDLED;
        }
    }

    private EventResult handleTextFieldKey(KeyEvent event) {
        if (textState == null) {
            return EventResult.UNHANDLED;
        }

        // Handle Enter key - call onSubmit and validate
        if (event.isConfirm()) {
            validateField();
            if (onSubmit != null) {
                onSubmit.run();
            }
            return EventResult.HANDLED;
        }

        // Arrow navigation for text fields
        if (arrowNavigation) {
            if (event.isUp()) {
                return EventResult.FOCUS_PREVIOUS;
            }
            if (event.isDown()) {
                return EventResult.FOCUS_NEXT;
            }
        }

        boolean handled = handleTextInputKey(textState, event);
        if (handled) {
            // Re-validate on change if we have validators
            if (!validators.isEmpty()) {
                validateField();
            }
        }
        return handled ? EventResult.HANDLED : EventResult.UNHANDLED;
    }

    private EventResult handleBooleanFieldKey(KeyEvent event) {
        if (booleanState == null) {
            return EventResult.UNHANDLED;
        }

        // Toggle on space or enter
        if (event.isConfirm() || event.character() == ' ') {
            booleanState.toggle();
            return EventResult.HANDLED;
        }

        // Arrow navigation for boolean fields
        if (arrowNavigation) {
            if (event.isUp()) {
                return EventResult.FOCUS_PREVIOUS;
            }
            if (event.isDown()) {
                return EventResult.FOCUS_NEXT;
            }
        }

        // For toggle in inline choice mode, allow left/right to switch
        // Layout: "● Yes / ○ No" - Yes (true) is on left, No (false) is on right
        if (fieldType == FieldType.TOGGLE) {
            if (event.isRight() && booleanState.value()) {
                // Currently on "Yes" (left), move right to "No"
                booleanState.setValue(false);
                return EventResult.HANDLED;
            }
            if (event.isLeft() && !booleanState.value()) {
                // Currently on "No" (right), move left to "Yes"
                booleanState.setValue(true);
                return EventResult.HANDLED;
            }
        }

        return EventResult.UNHANDLED;
    }

    private EventResult handleSelectFieldKey(KeyEvent event) {
        if (selectState == null) {
            return EventResult.UNHANDLED;
        }

        // Navigate with up/down or left/right
        if (event.isUp() || event.isLeft()) {
            selectState.selectPrevious();
            return EventResult.HANDLED;
        }
        if (event.isDown() || event.isRight()) {
            selectState.selectNext();
            return EventResult.HANDLED;
        }

        return EventResult.UNHANDLED;
    }

    // ==================== Rendering ====================

    @Override
    protected void renderContent(Frame frame, Rect area, RenderContext context) {
        if (area.isEmpty()) {
            return;
        }

        boolean isFocused = elementId != null && context.isFocused(elementId);

        // Resolve styles with priority: explicit > CSS > default
        Style effectiveLabelStyle = resolveEffectiveStyle(context, "label", labelStyle, DEFAULT_LABEL_STYLE);
        Style effectiveErrorStyle = resolveEffectiveStyle(context, "error", errorStyle, DEFAULT_ERROR_STYLE);

        // Calculate layout
        int effectiveLabelWidth = Math.min(labelWidth, area.width() - 1);
        int inputX = area.x() + effectiveLabelWidth + spacing;
        int inputWidth = area.width() - effectiveLabelWidth - spacing;

        // Determine if we need error row
        boolean hasError = !lastValidation().isValid();
        int inputHeight = borderType != null ? 3 : 1;
        int totalHeight = showInlineErrors && hasError ? inputHeight + 1 : inputHeight;

        // Render label
        Rect labelArea = new Rect(area.x(), area.y(), effectiveLabelWidth, inputHeight);
        renderLabel(frame, labelArea, effectiveLabelStyle);

        // Render input
        Rect inputArea = new Rect(inputX, area.y(), inputWidth, inputHeight);
        renderInput(frame, inputArea, context, isFocused, hasError);

        // Render inline error if enabled
        if (showInlineErrors && hasError && area.height() > inputHeight) {
            Rect errorArea = new Rect(inputX, area.y() + inputHeight, inputWidth, 1);
            renderError(frame, errorArea, effectiveErrorStyle);
        }
    }

    private void renderLabel(Frame frame, Rect area, Style style) {
        if (area.isEmpty() || label.isEmpty()) {
            return;
        }

        // Truncate label if too long
        String displayLabel = label;
        if (displayLabel.length() > area.width()) {
            displayLabel = displayLabel.substring(0, area.width());
        }

        // Render label text (vertically centered if bordered input)
        int labelY = borderType != null ? area.y() + 1 : area.y();
        frame.buffer().setString(area.x(), labelY, displayLabel, style);
    }

    private void renderInput(Frame frame, Rect area, RenderContext context, boolean focused, boolean hasError) {
        if (area.isEmpty()) {
            return;
        }

        switch (fieldType) {
            case TEXT:
            case TEXT_AREA:
                renderTextInput(frame, area, context, focused, hasError);
                break;

            case CHECKBOX:
                renderCheckbox(frame, area, context, focused);
                break;

            case TOGGLE:
                renderToggle(frame, area, context, focused);
                break;

            case SELECT:
                renderSelect(frame, area, context, focused);
                break;
        }
    }

    private void renderTextInput(Frame frame, Rect area, RenderContext context,
                                  boolean focused, boolean hasError) {
        if (textState == null) {
            return;
        }

        // Determine effective border color
        Color effectiveBorderColor = borderColor;
        if (hasError && errorBorderColor != null) {
            effectiveBorderColor = errorBorderColor;
        } else if (focused && focusedBorderColor != null) {
            effectiveBorderColor = focusedBorderColor;
        }

        TextInput.Builder builder = TextInput.builder()
                .style(context.currentStyle())
                .placeholder(placeholder);

        // Apply masking for password fields
        if (maskChar != null) {
            builder.masked(maskChar);
        }

        if (borderType != null || effectiveBorderColor != null) {
            Block.Builder blockBuilder = Block.builder()
                    .borders(Borders.ALL)
                    .styleResolver(styleResolver(context));
            if (borderType != null) {
                blockBuilder.borderType(borderType);
            }
            if (effectiveBorderColor != null) {
                blockBuilder.borderColor(effectiveBorderColor);
            }
            builder.block(blockBuilder.build());
        }

        TextInput widget = builder.build();

        // Show cursor when focused
        if (focused) {
            widget.renderWithCursor(area, frame.buffer(), textState, frame);
        } else {
            frame.renderStatefulWidget(widget, area, textState);
        }
    }

    private void renderCheckbox(Frame frame, Rect area, RenderContext context, boolean focused) {
        if (booleanState == null) {
            return;
        }

        // Build Checkbox widget with current styling
        Style baseStyle = context.currentStyle();
        if (focused) {
            baseStyle = baseStyle.reversed();
        }

        Checkbox.Builder builder = Checkbox.builder()
                .style(baseStyle)
                .styleResolver(styleResolver(context));

        // Apply custom symbols if set
        if (checkedSymbol != null) {
            builder.checkedSymbol(checkedSymbol);
        }
        if (uncheckedSymbol != null) {
            builder.uncheckedSymbol(uncheckedSymbol);
        }

        // Apply custom colors if set
        if (checkedColor != null) {
            builder.checkedColor(checkedColor);
        }
        if (uncheckedColor != null) {
            builder.uncheckedColor(uncheckedColor);
        }

        Checkbox checkbox = builder.build();

        // Create state from BooleanFieldState and render
        CheckboxState state = new CheckboxState(booleanState.value());
        int y = borderType != null ? area.y() + 1 : area.y();
        Rect checkboxArea = new Rect(area.x(), y, checkbox.width(), 1);

        frame.renderStatefulWidget(checkbox, checkboxArea, state);
    }

    private void renderToggle(Frame frame, Rect area, RenderContext context, boolean focused) {
        if (booleanState == null) {
            return;
        }

        // Build Toggle widget with inline choice mode
        Toggle.Builder builder = Toggle.builder()
                .inlineChoice(true)
                .style(context.currentStyle())
                .styleResolver(styleResolver(context));

        // Apply focus highlighting
        if (focused) {
            builder.selectedColor(Color.CYAN);
        }

        Toggle toggle = builder.build();

        // Create state from BooleanFieldState and render
        ToggleState state = new ToggleState(booleanState.value());
        int y = borderType != null ? area.y() + 1 : area.y();
        Rect toggleArea = new Rect(area.x(), y, toggle.width(), 1);

        frame.renderStatefulWidget(toggle, toggleArea, state);
    }

    private void renderSelect(Frame frame, Rect area, RenderContext context, boolean focused) {
        if (selectState == null) {
            return;
        }

        // Build Select widget
        Select.Builder builder = Select.builder()
                .style(context.currentStyle())
                .styleResolver(styleResolver(context));

        // Apply focus highlighting
        if (focused) {
            builder.selectedColor(Color.CYAN);
            builder.indicatorColor(Color.WHITE);
        }

        Select select = builder.build();

        // Create state from SelectFieldState and render
        SelectState state = new SelectState(selectState.options(), selectState.selectedIndex());
        int y = borderType != null ? area.y() + 1 : area.y();
        Rect selectArea = new Rect(area.x(), y, area.width(), 1);

        frame.renderStatefulWidget(select, selectArea, state);
    }

    private void renderError(Frame frame, Rect area, Style style) {
        String error = lastValidation().errorMessage();
        if (error == null || error.isEmpty()) {
            return;
        }

        // Truncate if too long
        if (error.length() > area.width()) {
            error = error.substring(0, area.width());
        }

        frame.buffer().setString(area.x(), area.y(), error, style);
    }
}
