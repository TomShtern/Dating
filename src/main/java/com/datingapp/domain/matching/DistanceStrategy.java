package com.datingapp.domain.matching;

import com.datingapp.domain.Distance;
import com.datingapp.domain.Location;
import com.datingapp.domain.User;

/**
 * Concrete strategy that scores based on geographic distance.
 */
public class DistanceStrategy implements MatchStrategy {
    private final Distance maxDistance;

    public DistanceStrategy(Distance maxDistance) {
        this.maxDistance = maxDistance;
    }

    @Override
    public double score(User candidate, User requester) {
        Location candidateLoc = candidate.getProfile().location();
        Location requesterLoc = requester.getProfile().location();

        if (candidateLoc == null || requesterLoc == null) {
            return 0.0;
        }

        Distance distance = requesterLoc.distanceTo(candidateLoc);

        if (distance.isGreaterThan(maxDistance)) {
            return 0.0;
        }

        // Linear decay: 1.0 at same location, 0.0 at max distance
        return 1.0 - (distance.kilometers() / maxDistance.kilometers());
    }

    @Override
    public String name() {
        return "distance";
    }
}
