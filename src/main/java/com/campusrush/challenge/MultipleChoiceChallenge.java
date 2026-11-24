package com.campusrush.challenge;

import com.campusrush.core.GameConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public abstract class MultipleChoiceChallenge implements Challenge {
    private static final long serialVersionUID = 1L;

    private final String question;
    private final List<String> options;
    private final String correctText;

    protected MultipleChoiceChallenge(String question, List<String> options, int correctIndex, Random rng) {
        this.question = question;
        this.correctText = options.get(correctIndex);
        List<String> shuffled = new ArrayList<>(options);
        if (rng != null) Collections.shuffle(shuffled, rng);
        this.options = shuffled;
    }

    @Override
    public String getQuestionText() { return question; }

    @Override
    public List<String> getOptions() { return Collections.unmodifiableList(options); }

    @Override
    public boolean validateAnswer(String userAnswer) {
        if (userAnswer == null) return false;
        String clean = userAnswer.trim();
        if (clean.equalsIgnoreCase(correctText)) return true;
        if (clean.matches("[1-9]")) {
            int idx = Integer.parseInt(clean) - 1;
            return idx < options.size() && options.get(idx).equalsIgnoreCase(correctText);
        }
        return false;
    }

    @Override
    public int getBonusPoints() { return GameConfig.HURDLE_SOLVED_POINTS; }

    @Override
    public String getCorrectAnswerDisplay() { return correctText; }
}
