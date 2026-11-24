package com.campusrush.hurdle;

import com.campusrush.challenge.*;
import com.campusrush.map.CampusMap;

import java.util.Random;

public class RandomEventHurdle extends Hurdle {
    private static final long serialVersionUID = 2L;

    public RandomEventHurdle(CampusMap map, Random rng) {
        super("Monsoon Rain / Muddy Pathway", "Sudden heavy rain! Do a quick calculation to find the dry route.", new MathChallenge(rng));
    }

    @Override
    public int getTimeDelayMinutes() {
        return 5;
    }
}
