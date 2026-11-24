package com.campusrush.challenge;

import java.util.List;
import java.util.Random;

/** A quiz question drawn from the data-driven question bank. */
public class TriviaChallenge extends MultipleChoiceChallenge {
    private static final long serialVersionUID = 2L;

    public TriviaChallenge(QuizQuestion q, Random rng) {
        super(q.getText(), q.getOptions(), q.getCorrectIndex(), rng);
    }

    public TriviaChallenge(String question, List<String> options, int correctIndex, Random rng) {
        super(question, options, correctIndex, rng);
    }

    /** Convenience: random question from a category of questions.csv. */
    public static TriviaChallenge fromCategory(String category, Random rng) {
        return new TriviaChallenge(QuestionBank.random(category, rng), rng);
    }
}
