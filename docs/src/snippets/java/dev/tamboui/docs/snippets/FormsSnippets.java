package dev.tamboui.docs.snippets;

import dev.tamboui.style.Color;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.FormElement;
import dev.tamboui.toolkit.elements.FormFieldElement;
import dev.tamboui.widgets.form.BooleanFieldState;
import dev.tamboui.widgets.form.FieldType;
import dev.tamboui.widgets.form.FormState;
import dev.tamboui.widgets.form.SelectFieldState;
import dev.tamboui.widgets.form.ValidationResult;
import dev.tamboui.widgets.form.Validator;
import dev.tamboui.widgets.form.Validators;
import dev.tamboui.widgets.input.TextInputState;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Code snippets for forms.adoc documentation.
 * Each method contains tagged regions that are included in the documentation.
 */
@SuppressWarnings({"unused", "UnnecessaryLocalVariable"})
public class FormsSnippets {

    // tag::overview[]
    // Centralized state for all form fields
    FormState form = FormState.builder()
        .textField("fullName", "Ada Lovelace")
        .textField("email", "ada@example.com")
        .booleanField("subscribe", true)
        .selectField("country", Arrays.asList("USA", "UK", "Germany"), 0)
        .build();

    // Simple field declarations
    Element overviewExample = formField("Full Name", form.textField("fullName"))
        .labelWidth(12)
        .rounded()
        .borderColor(Color.DARK_GRAY)
        .focusedBorderColor(Color.CYAN);
    // end::overview[]

    void basicUsage() {
        // tag::basic-usage[]
        // With TextInputState
        TextInputState nameState = new TextInputState("John");
        formField("Name", nameState);

        // Creates internal state automatically
        formField("Email")
            .placeholder("you@example.com");
        // end::basic-usage[]
    }

    void textFields() {
        // tag::text-fields[]
        TextInputState emailState = new TextInputState("");
        formField("Email", emailState)
            .placeholder("you@example.com")
            .rounded();
        // end::text-fields[]
    }

    void passwordFieldsBuilder() {
        // tag::password-fields-builder[]
        FormState form = FormState.builder()
            .textField("username", "")
            .maskedField("password", "")  // automatically masked with '*'
            .build();
        // end::password-fields-builder[]
    }

    void passwordFieldsMasked() {
        TextInputState passwordState = new TextInputState("");
        TextInputState pinState = new TextInputState("");
        // tag::password-fields-masked[]
        formField("Password", passwordState)
            .placeholder("Enter password")
            .masked()          // displays '*' for each character
            .rounded();

        // Or with a custom mask character
        formField("PIN", pinState)
            .masked('\u25CF');       // displays a filled circle for each character
        // end::password-fields-masked[]
    }

    void booleanFields() {
        // tag::boolean-fields[]
        BooleanFieldState subscribeState = new BooleanFieldState(false);

        // Checkbox style: [x] or [ ]
        formField("Subscribe", subscribeState);

        // Toggle style: [ON] or [OFF]
        formField("Dark Mode", subscribeState, FieldType.TOGGLE);
        // end::boolean-fields[]
    }

    void selectFields() {
        // tag::select-fields[]
        SelectFieldState countryState = new SelectFieldState("USA", "UK", "Germany");
        formField("Country", countryState);
        // end::select-fields[]
    }

    void stylingOptions() {
        TextInputState emailState = new TextInputState("");
        // tag::styling-options[]
        formField("Email", emailState)
            .labelWidth(14)           // Fixed label width for alignment
            .spacing(2)               // Gap between label and input
            .rounded()                // Rounded border
            .borderColor(Color.DARK_GRAY)
            .focusedBorderColor(Color.CYAN)
            .errorBorderColor(Color.RED);
        // end::styling-options[]
    }

    void formStateCreation() {
        // tag::form-state-creation[]
        FormState form = FormState.builder()
            // Text fields
            .textField("username", "")
            .textField("email", "user@example.com")

            // Boolean fields
            .booleanField("newsletter", true)
            .booleanField("darkMode", false)

            // Select fields
            .selectField("country", Arrays.asList("USA", "UK", "Germany"), 0)
            .selectField("role", Arrays.asList("Admin", "User", "Guest"))  // defaults to index 0

            .build();
        // end::form-state-creation[]
    }

    void accessingValues() {
        FormState form = FormState.builder()
            .textField("username", "")
            .textField("email", "user@example.com")
            .booleanField("newsletter", true)
            .selectField("country", Arrays.asList("USA", "UK", "Germany"), 0)
            .build();
        // tag::accessing-values[]
        // Get state objects for UI binding
        TextInputState usernameState = form.textField("username");
        BooleanFieldState newsletterState = form.booleanField("newsletter");
        SelectFieldState countryState = form.selectField("country");

        // Get/set text values directly
        String email = form.textValue("email");
        form.setTextValue("email", "new@example.com");

        // Get/set boolean values
        boolean subscribed = form.booleanValue("newsletter");
        form.setBooleanValue("newsletter", false);

        // Get/set select values
        String country = form.selectValue("country");
        int countryIndex = form.selectIndex("country");
        form.selectIndex("country", 2);  // Select "Germany"

        // Get all text values as map
        Map<String, String> allText = form.textValues();
        // end::accessing-values[]
    }

    void usingValidators() {
        TextInputState emailState = new TextInputState("");
        // tag::using-validators[]
        formField("Email", emailState)
            .validate(Validators.required(), Validators.email())
            .errorBorderColor(Color.RED)
            .showInlineErrors(true);
        // end::using-validators[]
    }

    void customErrorMessages() {
        // tag::custom-error-messages[]
        Validators.required("Please enter your name");
        Validators.email("Invalid email format");
        Validators.minLength(3, "Username must be at least 3 characters");
        Validators.range(1, 100, "Age must be between 1 and 100");
        // end::custom-error-messages[]
    }

    void triggeringValidation() {
        TextInputState emailState = new TextInputState("");
        // tag::triggering-validation[]
        FormFieldElement field = formField("Email", emailState)
            .validate(Validators.required(), Validators.email());

        // Validate and get result
        ValidationResult result = field.validateField();

        if (!result.isValid()) {
            String error = result.errorMessage();  // "Field is required" or "Invalid email"
        }

        // Get last validation result
        ValidationResult lastResult = field.lastValidation();
        // end::triggering-validation[]
    }

    // Stub for custom validator example
    interface UserService {
        boolean exists(String value);
    }
    UserService userService;

    void customValidators() {
        TextInputState usernameState = new TextInputState("");
        // tag::custom-validators[]
        Validator usernameAvailable = value -> {
            if (userService.exists(value)) {
                return ValidationResult.invalid("Username already taken");
            }
            return ValidationResult.valid();
        };

        formField("Username", usernameState)
            .validate(Validators.required(), usernameAvailable);
        // end::custom-validators[]
    }

    void composingValidators() {
        TextInputState emailState = new TextInputState("");
        // tag::composing-validators[]
        Validator emailValidator = Validators.required()
            .and(Validators.email())
            .and(Validators.maxLength(100));

        formField("Email", emailState)
            .validate(emailValidator);
        // end::composing-validators[]
    }

    void formContainerBasic() {
        FormState formState = FormState.builder()
            .textField("fullName", "")
            .textField("email", "")
            .textField("role", "")
            .build();
        // tag::form-container-basic[]
        form(formState)
            .field("fullName", "Full Name")
            .field("email", "Email")
            .field("role", "Role")
            .labelWidth(14)
            .rounded();
        // end::form-container-basic[]
    }

    void groupingFields() {
        FormState formState = FormState.builder()
            .textField("fullName", "")
            .textField("email", "")
            .textField("phone", "")
            .booleanField("newsletter", false)
            .booleanField("darkMode", false)
            .build();
        // tag::grouping-fields[]
        form(formState)
            .group("Personal Info")
                .field("fullName", "Full Name")
                .field("email", "Email")
                .field("phone", "Phone")
            .group("Preferences")
                .field("newsletter", "Newsletter", FieldType.CHECKBOX)
                .field("darkMode", "Dark Mode", FieldType.TOGGLE)
            .labelWidth(14)
            .spacing(1);
        // end::grouping-fields[]
    }

    // Stub for submit example
    void authenticate(String user, String pass) {}

    void submitOnEnter() {
        FormState formState = FormState.builder()
            .textField("username", "")
            .textField("password", "")
            .build();
        // tag::submit-on-enter[]
        form(formState)
            .field("username", "Username")
            .field("password", "Password")
            .submitOnEnter(true)
            .onSubmit(state -> {
                String user = state.textValue("username");
                String pass = state.textValue("password");
                authenticate(user, pass);
            });
        // end::submit-on-enter[]
    }

    // Stub for programmatic submit example
    void authenticate(FormState state) {}

    void programmaticSubmit() {
        FormState formState = FormState.builder()
            .textField("username", "")
            .textField("password", "")
            .build();
        // tag::programmatic-submit[]
        FormElement loginForm = form(formState)
            .field("username", "Username", Validators.required())
            .field("password", "Password", Validators.required())
            .onSubmit(state -> authenticate(state));

        // Render form with submit button
        column(
            loginForm,
            text(" Login ").bold()  // Note: button() is not yet available
        );
        // end::programmatic-submit[]
    }

    // Stub for validation on submit example
    void save(FormState state) {}

    void validationOnSubmit() {
        FormState formState = FormState.builder()
            .textField("email", "")
            .build();
        // tag::validation-on-submit[]
        FormElement form = form(formState)
            .field("email", "Email", Validators.required(), Validators.email())
            .validateOnSubmit(true)  // default behavior
            .onSubmit(state -> {
                // This is only called if ALL validations pass
                save(state);
            });

        boolean success = form.submit();
        // success == true  -> validation passed, onSubmit was called
        // success == false -> validation failed, onSubmit was NOT called
        // end::validation-on-submit[]
    }

    void skipValidationOnSubmit() {
        FormState formState = FormState.builder()
            .textField("email", "")
            .build();
        // tag::skip-validation-on-submit[]
        form(formState)
            .validateOnSubmit(false)  // skip validation
            .onSubmit(state -> save(state));  // always called
        // end::skip-validation-on-submit[]
    }

    void arrowNavigation() {
        FormState formState = FormState.builder()
            .textField("username", "")
            .textField("email", "")
            .selectField("role", Arrays.asList("Admin", "User", "Guest"), 0)
            .build();
        // tag::arrow-navigation[]
        form(formState)
            .field("username", "Username")
            .field("email", "Email")
            .field("role", "Role", FieldType.SELECT)
            .arrowNavigation(true);  // Up/Down navigate between fields
        // end::arrow-navigation[]
    }

    // tag::complete-example[]
    public static class SettingsForm {

        private static final FormState FORM = FormState.builder()
            // Profile
            .textField("fullName", "Ada Lovelace")
            .textField("email", "ada@analytical.io")
            .textField("role", "Research")
            .textField("timezone", "UTC+1")
            // Preferences
            .textField("theme", "Nord")
            .booleanField("notifications", true)
            // Security
            .textField("twoFa", "Enabled")
            .build();

        public static Element render() {
            return column(
                panel("Profile", column(
                    formField("Full name", FORM.textField("fullName"))
                        .labelWidth(14).rounded()
                        .borderColor(Color.DARK_GRAY)
                        .focusedBorderColor(Color.CYAN),
                    formField("Email", FORM.textField("email"))
                        .labelWidth(14).rounded()
                        .borderColor(Color.DARK_GRAY)
                        .focusedBorderColor(Color.CYAN)
                        .validate(Validators.required(), Validators.email()),
                    formField("Role", FORM.textField("role"))
                        .labelWidth(14).rounded()
                        .borderColor(Color.DARK_GRAY)
                        .focusedBorderColor(Color.CYAN)
                ).spacing(1)).rounded().borderColor(Color.CYAN),

                panel("Preferences", column(
                    formField("Theme", FORM.textField("theme"))
                        .labelWidth(14).rounded()
                        .borderColor(Color.DARK_GRAY),
                    formField("Notifications", FORM.booleanField("notifications"))
                        .labelWidth(14)
                ).spacing(1)).rounded().borderColor(Color.GREEN),

                row(
                    text(" Save ").bold().black().onGreen(),
                    text(" Cancel ").bold().white().bg(Color.DARK_GRAY)
                ).spacing(2)
            ).spacing(1).fill();
        }
    }
    // end::complete-example[]

    void textInputStateReference() {
        // tag::text-input-state-reference[]
        TextInputState state = new TextInputState("initial");

        // Get/set text
        String text = state.text();
        state.setText("new value");

        // Cursor operations
        state.insert('c');
        state.deleteBackward();
        state.deleteForward();
        state.moveCursorLeft();
        state.moveCursorRight();
        state.clear();
        // end::text-input-state-reference[]
    }

    void booleanFieldStateReference() {
        // tag::boolean-field-state-reference[]
        BooleanFieldState state = new BooleanFieldState(false);

        // Get/set value
        boolean value = state.value();
        state.setValue(true);

        // Toggle
        state.toggle();  // Flips the value
        // end::boolean-field-state-reference[]
    }

    void selectFieldStateReference() {
        List<String> options = Arrays.asList("A", "B", "C");
        // tag::select-field-state-reference[]
        SelectFieldState state = new SelectFieldState("A", "B", "C");
        // Or with List
        SelectFieldState stateFromList = new SelectFieldState(options, 1);  // Select index 1

        // Get values
        String selected = state.selectedValue();  // "A"
        int index = state.selectedIndex();        // 0
        List<String> opts = state.options();   // ["A", "B", "C"]

        // Change selection
        state.selectIndex(2);    // Select "C"
        state.selectNext();      // Move to next option (wraps)
        state.selectPrevious();  // Move to previous option (wraps)
        // end::select-field-state-reference[]
    }

    // tag::migration-before[]
    // Individual state declarations
    private static final TextInputState FULL_NAME = new TextInputState("Ada");
    private static final TextInputState EMAIL = new TextInputState("ada@example.com");
    private static final TextInputState ROLE = new TextInputState("Research");

    // Custom helper
    private static Element formRow(String label, TextInputState state) {
        return row(
            text(label).dim().length(14),
            textInput(state).rounded().borderColor(Color.DARK_GRAY).fill()
        ).spacing(1).length(3);
    }

    // Usage
    Element beforeExample1 = formRow("Full name", FULL_NAME);
    Element beforeExample2 = formRow("Email", EMAIL);
    // end::migration-before[]

    // tag::migration-after[]
    // Centralized state
    private static final FormState FORM_STATE = FormState.builder()
        .textField("fullName", "Ada")
        .textField("email", "ada@example.com")
        .textField("role", "Research")
        .build();

    // Usage - no helper needed
    Element afterExample1 = formField("Full name", FORM_STATE.textField("fullName"))
        .labelWidth(14).rounded().borderColor(Color.DARK_GRAY);
    Element afterExample2 = formField("Email", FORM_STATE.textField("email"))
        .labelWidth(14).rounded().borderColor(Color.DARK_GRAY);
    // end::migration-after[]
}
