package com.campusrush.hurdle;

import com.campusrush.challenge.*;
import com.campusrush.map.CampusMap;

import java.util.Random;

public class PopQuizHurdle extends Hurdle {
    private static final long serialVersionUID = 2L;

    public PopQuizHurdle(CampusMap map, Random rng) {
        super("Surprise Professor Encounter", "A professor stops you for a quick oral pop quiz!", TriviaChallenge.fromCategory("JAVA", rng));
    }

    @Override
    public int getTimeDelayMinutes() {
        return 3;
    }
}
