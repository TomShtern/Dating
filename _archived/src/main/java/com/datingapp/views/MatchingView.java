package com.datingapp.views;

import java.util.ArrayList;
import java.util.List;

import com.datingapp.model.InteractionResult;
import com.datingapp.model.User;
import com.datingapp.service.MatchingService;
import com.datingapp.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

import jakarta.annotation.security.PermitAll;

@Route(value = "", layout = MainLayout.class)
@RouteAlias(value = "matches", layout = MainLayout.class)
@PageTitle("Find Matches | Dating App")
@PermitAll
public class MatchingView extends VerticalLayout {

    private final transient MatchingService matchingService;
    private final transient UserService userService;
    private List<User> potentialMatches = new ArrayList<>();
    private int currentIndex = 0;
    private final VerticalLayout profileCard = new VerticalLayout();
    private final H2 nameAge = new H2();
    private final Span genderLabel = new Span();
    private final Paragraph bioText = new Paragraph();

    public MatchingView(MatchingService matchingService, UserService userService) {
        this.matchingService = matchingService;
        this.userService = userService;
        addClassName("matching-view");
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        buildUI();
        loadPotentialMatches();
    }

    private void buildUI() {
        profileCard.addClassName("profile-card");
        profileCard.setWidth("350px");
        profileCard.setMaxWidth("90%");
        profileCard.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)").set("border-radius", "16px")
                .set("padding", "24px");
        nameAge.getStyle().set("margin", "0");
        genderLabel.getStyle().set("color", "var(--lumo-secondary-text-color)");
        bioText.getStyle().set("margin-top", "16px");
        profileCard.add(nameAge, genderLabel, bioText);
        Button passButton = new Button("Pass", e -> handlePass());
        passButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_LARGE);
        Button likeButton = new Button("Like", e -> handleLike());
        likeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        HorizontalLayout buttons = new HorizontalLayout(passButton, likeButton);
        buttons.setJustifyContentMode(JustifyContentMode.CENTER);
        add(profileCard, buttons);
    }

    private void loadPotentialMatches() {
        User currentUser = userService.getCurrentUser();
        potentialMatches = new ArrayList<>(matchingService.getPotentialMatches(currentUser, 20));
        currentIndex = 0;
        showCurrentProfile();
    }

    private void showCurrentProfile() {
        if (currentIndex >= potentialMatches.size()) {
            showEmptyState();
            return;
        }
        User user = potentialMatches.get(currentIndex);
        nameAge.setText(user.getDisplayName() + ", " + user.getAge());
        genderLabel.setText(user.getGender());
        bioText.setText(user.getBio() != null ? user.getBio() : "No bio yet");
    }

    private void showEmptyState() {
        profileCard.removeAll();
        profileCard.add(new H3("No more people nearby!"));
        profileCard.add(new Paragraph("Check back later."));
        Button refreshButton = new Button("Refresh", e -> loadPotentialMatches());
        refreshButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        profileCard.add(refreshButton);
    }

    private void handleLike() {
        if (currentIndex >= potentialMatches.size())
            return;
        User currentUser = userService.getCurrentUser();
        User likedUser = potentialMatches.get(currentIndex);
        InteractionResult result = matchingService.likeUser(currentUser, likedUser);
        if (result == InteractionResult.MATCHED)
            showMatchDialog(likedUser);
        currentIndex++;
        showCurrentProfile();
    }

    private void handlePass() {
        if (currentIndex >= potentialMatches.size())
            return;
        User currentUser = userService.getCurrentUser();
        User passedUser = potentialMatches.get(currentIndex);
        matchingService.passUser(currentUser, passedUser);
        currentIndex++;
        showCurrentProfile();
    }

    private void showMatchDialog(User matchedUser) {
        Dialog dialog = new Dialog();
        // dialog.setModal(true); // Modal by default in newer Vaadin versions
        VerticalLayout content = new VerticalLayout();
        content.setAlignItems(FlexComponent.Alignment.CENTER);
        content.add(new H2("It's a Match!"));
        content.add(new Paragraph("You and " + matchedUser.getDisplayName() + " liked each other!"));
        Button messageButton = new Button("Send a Message", e -> {
            dialog.close();
            getUI().ifPresent(ui -> ui.navigate(ChatView.class));
        });
        messageButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button keepSwipingButton = new Button("Keep Swiping", e -> dialog.close());
        content.add(new HorizontalLayout(messageButton, keepSwipingButton));
        dialog.add(content);
        dialog.open();
    }
}