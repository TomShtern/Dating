package com.datingapp.views;

import com.datingapp.model.User;
import com.datingapp.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;

@Route(value = "profile", layout = MainLayout.class)
@PageTitle("My Profile | Dating App")
@PermitAll
public class ProfileView extends VerticalLayout {

    private final transient UserService userService;
    private User currentUser;
    private final TextField displayName = new TextField("Display Name");
    private final IntegerField age = new IntegerField("Age");
    private final ComboBox<String> gender = new ComboBox<>("Gender");
    private final TextArea bio = new TextArea("Bio");
    private boolean editMode = false;

    public ProfileView(UserService userService) {
        this.userService = userService;
        addClassName("profile-view");
        setMaxWidth("600px");
        getStyle().set("margin", "0 auto");
        setPadding(true);
        setSpacing(true);
        loadUserData();
    }

    private void loadUserData() {
        currentUser = userService.getCurrentUser();
        removeAll();
        add(new H2("My Profile"));
        displayName.setValue(currentUser.getDisplayName());
        displayName.setWidthFull();
        displayName.setReadOnly(!editMode);
        age.setValue(currentUser.getAge());
        age.setWidthFull();
        age.setReadOnly(!editMode);
        age.setMin(18);
        gender.setItems("Male", "Female", "Other");
        gender.setValue(currentUser.getGender());
        gender.setWidthFull();
        gender.setReadOnly(!editMode);
        bio.setValue(currentUser.getBio() != null ? currentUser.getBio() : "");
        bio.setWidthFull();
        bio.setMaxLength(500);
        bio.setHeight("150px");
        bio.setReadOnly(!editMode);
        add(displayName, age, gender, bio);
        HorizontalLayout buttons = new HorizontalLayout();
        if (editMode) {
            Button saveButton = new Button("Save", e -> saveProfile());
            saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            Button cancelButton = new Button("Cancel", e -> {
                editMode = false;
                loadUserData();
            });
            buttons.add(saveButton, cancelButton);
        } else {
            Button editButton = new Button("Edit Profile", e -> {
                editMode = true;
                loadUserData();
            });
            editButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            buttons.add(editButton);
        }
        add(buttons);
    }

    private void saveProfile() {
        try {
            userService.updateProfile(currentUser.getId(), displayName.getValue(), age.getValue(), gender.getValue(),
                    bio.getValue());
            Notification.show("Profile updated!", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            editMode = false;
            loadUserData();
        } catch (Exception e) {
            Notification.show("Error: " + e.getMessage(), 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}