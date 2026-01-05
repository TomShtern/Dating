package com.datingapp.views;

import org.springframework.security.core.context.SecurityContextHolder;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * Main layout with navigation drawer for authenticated pages.
 */
public class MainLayout extends AppLayout {

    public MainLayout() {
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 logo = new H1("❤️ Dating App");
        logo.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.MEDIUM);

        // Logout button
        Button logoutButton = new Button("Logout", e -> {
            SecurityContextHolder.clearContext();
            getUI().ifPresent(ui -> {
                ui.getSession().close();
                ui.getPage().setLocation("/login");
            });
        });

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), logo);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(logo);
        header.setWidthFull();
        header.addClassNames(LumoUtility.Padding.Vertical.NONE, LumoUtility.Padding.Horizontal.MEDIUM);

        // Add logout to right side
        header.add(logoutButton);

        addToNavbar(header);
    }

    private void createDrawer() {
        VerticalLayout drawer = new VerticalLayout();

        drawer.add(
                new RouterLink("🔥 Find Matches", MatchingView.class),
                new RouterLink("💬 Messages", ChatView.class),
                new RouterLink("👤 My Profile", ProfileView.class));

        drawer.setSpacing(true);
        drawer.setPadding(true);

        addToDrawer(drawer);
    }
}
