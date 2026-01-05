package com.datingapp.views;

import com.datingapp.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("register")
@PageTitle("Register | Dating App")
@AnonymousAllowed
public class RegisterView extends VerticalLayout {

    private final UserService userService;
    private final TextField username = new TextField("Username");
    private final PasswordField password = new PasswordField("Password");
    private final PasswordField confirmPassword = new PasswordField("Confirm Password");
    private final TextField displayName = new TextField("Display Name");
    private final IntegerField age = new IntegerField("Age");
    private final ComboBox<String> gender = new ComboBox<>("Gender");
    private final TextArea bio = new TextArea("Bio (optional)");

    public RegisterView(UserService userService) {
        this.userService = userService;
        addClassName("register-view");
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setMaxWidth("400px");
        getStyle().set("margin", "0 auto");
        username.setRequired(true);
        password.setRequired(true);
        password.setMinLength(8);
        confirmPassword.setRequired(true);
        displayName.setRequired(true);
        age.setRequired(true);
        age.setMin(18);
        age.setMax(120);
        age.setValue(25);
        gender.setItems("Male", "Female", "Other");
        gender.setRequired(true);
        bio.setMaxLength(500);
        Button registerButton = new Button("Create Account", e -> register());
        registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        registerButton.setWidthFull();
        add(new H1("Create Account"), username, password, confirmPassword, displayName, age, gender, bio, registerButton, new RouterLink("Already have an account? Login", LoginView.class));
        username.setWidthFull();
        password.setWidthFull();
        confirmPassword.setWidthFull();
        displayName.setWidthFull();
        age.setWidthFull();
        gender.setWidthFull();
        bio.setWidthFull();
    }

    private void register() {
        if (username.isEmpty() || password.isEmpty() || displayName.isEmpty() || age.isEmpty() || gender.isEmpty()) {
            showError("Please fill in all required fields");
            return;
        }
        if (password.getValue().length() < 8) {
            showError("Password must be at least 8 characters");
            return;
        }
        if (!password.getValue().equals(confirmPassword.getValue())) {
            showError("Passwords do not match");
            return;
        }
        if (age.getValue() < 18) {
            showError("You must be at least 18 years old");
            return;
        }
        try {
            userService.register(username.getValue(), password.getValue(), displayName.getValue(), age.getValue(), gender.getValue(), bio.getValue());
            Notification.show("Registration successful! Please login.", 3000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            getUI().ifPresent(ui -> ui.navigate("login"));
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    private void showError(String message) {
        Notification.show(message, 3000, Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}