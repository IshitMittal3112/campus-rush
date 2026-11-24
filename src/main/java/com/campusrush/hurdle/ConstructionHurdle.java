package com.campusrush.hurdle;

import com.campusrush.challenge.*;
import com.campusrush.map.CampusMap;

import java.util.Random;

public class ConstructionHurdle extends Hurdle {
    private static final long serialVersionUID = 2L;

    public ConstructionHurdle(CampusMap map, Random rng) {
        super("Construction Detour", "Roadwork blocks the path! Solve the signboard puzzle to find the detour.", TriviaChallenge.fromCategory("LOGIC", rng));
    }

    @Override
    public int getTimeDelayMinutes() {
        return 4;
    }
}
