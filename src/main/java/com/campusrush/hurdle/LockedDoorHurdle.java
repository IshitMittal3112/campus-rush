package com.campusrush.hurdle;

import com.campusrush.challenge.*;
import com.campusrush.map.CampusMap;

import java.util.Random;

public class LockedDoorHurdle extends Hurdle {
    private static final long serialVersionUID = 2L;

    public LockedDoorHurdle(CampusMap map, Random rng) {
        super("Locked Gate Passcode", "The shortcut gate is locked! Crack the access pattern.", new PatternSequenceChallenge(rng));
    }

    @Override
    public int getTimeDelayMinutes() {
        return 4;
    }
}
