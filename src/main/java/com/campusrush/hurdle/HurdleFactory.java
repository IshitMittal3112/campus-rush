package com.campusrush.hurdle;

import com.campusrush.map.CampusMap;

import java.util.Random;

/** Creates a random hurdle. Seeded Random in, reproducible hurdle out. */
public final class HurdleFactory {
    private HurdleFactory() {}

    public static Hurdle random(CampusMap map, Random rng) {
        switch (rng.nextInt(5)) {
            case 0:  return new CrowdHurdle(map, rng);
            case 1:  return new LockedDoorHurdle(map, rng);
            case 2:  return new PopQuizHurdle(map, rng);
            case 3:  return new RandomEventHurdle(map, rng);
            default: return new ConstructionHurdle(map, rng);
        }
    }
}
