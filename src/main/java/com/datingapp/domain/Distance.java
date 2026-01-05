package com.datingapp.domain;

/**
 * Type-safe distance value object in kilometers.
 */
public record Distance(double kilometers) implements Comparable<Distance> {
    public Distance {
        if (kilometers < 0)
            throw new IllegalArgumentException("Distance cannot be negative");
    }

    public static Distance ofKilometers(double km) {
        return new Distance(km);
    }

    public boolean isGreaterThan(Distance other) {
        return this.kilometers > other.kilometers;
    }

    public boolean isLessThanOrEqual(Distance other) {
        return this.kilometers <= other.kilometers;
    }

    @Override
    public int compareTo(Distance other) {
        return Double.compare(this.kilometers, other.kilometers);
    }
}
