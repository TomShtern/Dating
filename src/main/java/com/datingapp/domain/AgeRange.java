package com.datingapp.domain;

/**
 * Value object representing an age range for matching preferences.
 */
public record AgeRange(int min, int max) {
    public AgeRange {
        if (min < 18)
            throw new IllegalArgumentException("Minimum age must be 18+");
        if (max < min)
            throw new IllegalArgumentException("Max age must be greater than or equal to min age");
        if (max > 120)
            throw new IllegalArgumentException("Max age exceeded");
    }

    public boolean contains(int age) {
        return age >= min && age <= max;
    }

    public static AgeRange of(int min, int max) {
        return new AgeRange(min, max);
    }
}
