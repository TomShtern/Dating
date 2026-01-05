package com.datingapp.views;

import java.time.format.DateTimeFormatter;
import java.util.List;

import com.datingapp.model.Match;
import com.datingapp.model.Message;
import com.datingapp.model.User;
import com.datingapp.service.ChatService;
import com.datingapp.service.MatchingService;
import com.datingapp.service.UserService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;

@Route(value = "chat", layout = MainLayout.class)
@PageTitle("Messages | Dating App")
@PermitAll
public class ChatView extends VerticalLayout {

    private final transient ChatService chatService;
    private final transient MatchingService matchingService;
    private User currentUser;
    private Match selectedMatch;
    private final VerticalLayout matchesList = new VerticalLayout();
    private final VerticalLayout messagesContainer = new VerticalLayout();
    private final TextField messageInput = new TextField();
    private final Button sendButton = new Button("Send");

    public ChatView(ChatService chatService, MatchingService matchingService, UserService userService) {
        this.chatService = chatService;
        this.matchingService = matchingService;
        addClassName("chat-view");
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        currentUser = userService.getCurrentUser();
        buildUI();
        loadMatches();
    }

    private void buildUI() {
        matchesList.setPadding(true);
        matchesList.setSpacing(true);
        matchesList.setWidth("300px");
        VerticalLayout leftPanel = new VerticalLayout();
        leftPanel.add(new H3("My Matches"));
        leftPanel.add(matchesList);
        leftPanel.setSizeFull();
        leftPanel.setPadding(true);
        messagesContainer.setSizeFull();
        messagesContainer.setPadding(true);
        messagesContainer.setSpacing(true);
        Scroller scroller = new Scroller(messagesContainer);
        scroller.setSizeFull();
        messageInput.setPlaceholder("Type a message...");
        messageInput.setWidthFull();
        sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sendButton.addClickListener(e -> sendMessage());
        messageInput.addKeyPressListener(Key.ENTER, e -> sendMessage());
        HorizontalLayout inputBar = new HorizontalLayout(messageInput, sendButton);
        inputBar.setWidthFull();
        inputBar.expand(messageInput);
        inputBar.setPadding(true);
        VerticalLayout rightPanel = new VerticalLayout();
        rightPanel.setSizeFull();
        rightPanel.setPadding(false);
        rightPanel.setSpacing(false);
        rightPanel.add(scroller, inputBar);
        rightPanel.expand(scroller);
        SplitLayout splitLayout = new SplitLayout(leftPanel, rightPanel);
        splitLayout.setSizeFull();
        splitLayout.setSplitterPosition(25);
        add(splitLayout);
        expand(splitLayout);
        showEmptyConversation();
    }

    private void loadMatches() {
        matchesList.removeAll();
        List<Match> matches = matchingService.getMatchesForUser(currentUser);
        if (matches.isEmpty()) {
            matchesList.add(new Paragraph("No matches yet."));
            return;
        }
        for (Match match : matches) {
            User otherUser = match.getOtherUser(currentUser);
            Message lastMessage = chatService.getLatestMessage(match);
            Div matchCard = new Div();
            matchCard.getStyle().set("padding", "12px").set("border-radius", "8px").set("cursor", "pointer")
                    .set("border", "1px solid var(--lumo-contrast-10pct)");
            matchCard.add(new Span(otherUser.getDisplayName()));
            if (lastMessage != null) {
                Span preview = new Span(truncate(lastMessage.getContent(), 30));
                preview.getStyle().set("color", "var(--lumo-secondary-text-color)").set("display", "block")
                        .set("font-size", "small");
                matchCard.add(preview);
            }
            matchCard.addClickListener(e -> selectMatch(match));
            matchesList.add(matchCard);
        }
    }

    private void selectMatch(Match match) {
        selectedMatch = match;
        loadConversation();
    }

    private void loadConversation() {
        messagesContainer.removeAll();
        if (selectedMatch == null) {
            showEmptyConversation();
            return;
        }
        User otherUser = selectedMatch.getOtherUser(currentUser);
        messagesContainer.add(new H3("Chat with " + otherUser.getDisplayName()));
        List<Message> messages = chatService.getChatHistory(selectedMatch.getId(), currentUser);
        for (Message message : messages) {
            messagesContainer.add(createMessageBubble(message));
        }
        messagesContainer.getElement().executeJs("this.scrollTop = this.scrollHeight");
    }

    private Div createMessageBubble(Message message) {
        boolean isMine = message.getSender().getId().equals(currentUser.getId());
        Div bubble = new Div();
        bubble.setText(message.getContent());
        bubble.getStyle().set("padding", "10px 14px").set("border-radius", "16px").set("max-width", "70%");
        if (isMine)
            bubble.getStyle().set("background-color", "var(--lumo-primary-color)").set("color", "white")
                    .set("margin-left", "auto");
        else
            bubble.getStyle().set("background-color", "var(--lumo-contrast-10pct)").set("margin-right", "auto");
        Span time = new Span(message.getSentAt().format(DateTimeFormatter.ofPattern("HH:mm")));
        time.getStyle().set("font-size", "x-small").set("color", "var(--lumo-secondary-text-color)")
                .set("display", "block").set("text-align", isMine ? "right" : "left");
        Div wrapper = new Div(bubble, time);
        wrapper.getStyle().set("display", "flex").set("flex-direction", "column");
        if (isMine)
            wrapper.getStyle().set("align-items", "flex-end");
        return wrapper;
    }

    private void sendMessage() {
        if (selectedMatch == null)
            return;
        String content = messageInput.getValue().trim();
        if (content.isEmpty())
            return;
        chatService.sendMessage(currentUser, selectedMatch.getId(), content);
        messageInput.clear();
        loadConversation();
    }

    private void showEmptyConversation() {
        messagesContainer.removeAll();
        messagesContainer.add(new Paragraph("Select a match to start chatting!"));
    }

    private String truncate(String text, int maxLength) {
        if (text == null)
            return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}