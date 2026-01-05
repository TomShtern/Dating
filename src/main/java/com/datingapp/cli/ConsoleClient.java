package com.datingapp.cli;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import com.datingapp.domain.Distance;
import com.datingapp.domain.Location;
import com.datingapp.domain.Match;
import com.datingapp.domain.Profile;
import com.datingapp.domain.Prospect;
import com.datingapp.domain.SwipeDirection;
import com.datingapp.domain.User;
import com.datingapp.domain.UserId;
import com.datingapp.domain.event.EventPublisher;
import com.datingapp.domain.event.MatchCreatedEvent;
import com.datingapp.domain.matching.DistanceStrategy;
import com.datingapp.domain.matching.MatchScorer;
import com.datingapp.domain.matching.MatchingService;
import com.datingapp.domain.repository.MatchRepository;
import com.datingapp.domain.repository.SwipeRepository;
import com.datingapp.domain.repository.UserRepository;
import com.datingapp.infrastructure.persistence.inmemory.InMemoryMatchRepository;
import com.datingapp.infrastructure.persistence.inmemory.InMemorySwipeRepository;
import com.datingapp.infrastructure.persistence.inmemory.InMemoryUserRepository;

/**
 * Command-line client for manual verification of Phase 0 domain logic.
 */
public class ConsoleClient {
    private final UserRepository userRepo;
    private final SwipeRepository swipeRepo;
    private final MatchRepository matchRepo;
    private final MatchingService matchingService;
    private final Scanner scanner = new Scanner(System.in);

    private User currentUser;

    public ConsoleClient() {
        this.userRepo = new InMemoryUserRepository();
        this.swipeRepo = new InMemorySwipeRepository();
        this.matchRepo = new InMemoryMatchRepository();

        // Simple event publisher that logs to console
        EventPublisher eventPublisher = event -> {
            if (event instanceof MatchCreatedEvent m) {
                System.out.println("\n[EVENT] WE HAVE A MATCH! " + m.userA() + " & " + m.userB());
            }
        };

        MatchScorer scorer = new MatchScorer(List.of(
                new DistanceStrategy(Distance.ofKilometers(100))));

        this.matchingService = new MatchingService(
                scorer, userRepo, swipeRepo, matchRepo, eventPublisher);

        setupSampleData();
    }

    public static void main(String[] args) {
        new ConsoleClient().run();
    }

    private void setupSampleData() {
        // Create some discoverable users
        createUserRecord("Bob", 25, new Location(40.7128, -74.0060)); // NYC
        createUserRecord("Charlie", 28, new Location(40.7306, -73.9352)); // Brooklyn
        createUserRecord("Diana", 22, new Location(34.0522, -118.2437)); // LA (Too far)
    }

    private void createUserRecord(String name, int age, Location loc) {
        UserId id = UserId.generate();
        Profile p = new Profile(id, name, "Hi I'm " + name,
                LocalDate.now().minusYears(age), Collections.emptySet(),
                null, loc, List.of("url"));
        User u = new User(id, p);
        userRepo.save(u);
    }

    public void run() {
        System.out.println("=== Dating App Console - Phase 0 Prototype ===");

        System.out.println("First, let's create YOUR profile.");
        currentUser = registerUser();
        userRepo.save(currentUser);

        while (true) {
            System.out.println("\n--- Main Menu ---");
            System.out.println("1. Find Prospects & Swipe");
            System.out.println("2. View My Matches");
            System.out.println("3. Exit");
            System.out.print("> ");

            String choice = scanner.nextLine();
            switch (choice) {
                case "1" -> swipeLoop();
                case "2" -> showMatches();
                case "3" -> {
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private User registerUser() {
        System.out.print("Name: ");
        String name = scanner.nextLine();
        System.out.print("Age: ");
        int age = Integer.parseInt(scanner.nextLine());

        // Default to NYC for testing
        Location loc = new Location(40.7128, -74.0060);

        UserId id = UserId.generate();
        Profile p = new Profile(id, name, "Self bio",
                LocalDate.now().minusYears(age), Collections.emptySet(),
                null, loc, List.of("url"));
        return new User(id, p);
    }

    private void swipeLoop() {
        Distance radius = Distance.ofKilometers(100);
        List<Prospect> prospects = matchingService.findProspects(currentUser, radius, 5, Collections.emptySet());
        if (prospects.isEmpty()) {
            System.out.println("No prospects found nearby.");
            return;
        }

        for (Prospect p : prospects) {
            System.out.println("\n--- Prospect ---");
            System.out.println("Name: " + p.displayName() + " (" + p.age() + ")");
            System.out.println("Distance: " + Math.round(p.distance().kilometers()) + " km");
            System.out.println("Score: " + Math.round(p.score() * 100) + "%");
            System.out.println("L) Like  P) Pass  Q) Quit");
            System.out.print("> ");

            String choice = scanner.nextLine().toUpperCase();
            if (choice.equals("Q"))
                break;

            SwipeDirection dir = choice.equals("L") ? SwipeDirection.LIKE : SwipeDirection.DISLIKE;
            Optional<Match> match = matchingService.processSwipe(currentUser.getId(), p.userId(), dir);

            if (match.isPresent()) {
                System.out.println("!!! MATCH FOUND !!!");
            } else if (dir == SwipeDirection.LIKE) {
                System.out.println("Liked! Waiting for their response...");
                // Simulate mutual like for testing "Bob"
                if (p.displayName().equals("Bob")) {
                    System.out.println("(Simulating: Bob likes you back...)");
                    matchingService.processSwipe(p.userId(), currentUser.getId(), SwipeDirection.LIKE);
                }
            }
        }
    }

    private void showMatches() {
        List<Match> matches = matchRepo.findByUser(currentUser.getId());
        if (matches.isEmpty()) {
            System.out.println("No matches yet.");
        } else {
            System.out.println("\n--- Your Matches ---");
            for (Match m : matches) {
                UserId otherId = m.otherUser(currentUser.getId());
                User other = userRepo.findById(otherId).orElseThrow();
                System.out.println("- " + other.getProfile().displayName() + " (Matched at: " + m.getCreatedAt() + ")");
            }
        }
    }
}
