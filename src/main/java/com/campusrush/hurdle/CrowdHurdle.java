package com.campusrush.hurdle;

import com.campusrush.challenge.*;
import com.campusrush.map.CampusMap;

import java.util.Random;

public class CrowdHurdle extends Hurdle {
    private static final long serialVersionUID = 2L;

    public CrowdHurdle(CampusMap map, Random rng) {
        super("Campus Fest Crowd Surge", "A heavy student crowd blocks the pathway! Answer a campus question to squeeze through.", new CampusTriviaChallenge(map, rng));
    }

    @Override
    public int getTimeDelayMinutes() {
        return 3;
    }
}
