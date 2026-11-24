package com.campusrush.challenge;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/** One row of questions.csv. */
public class QuizQuestion implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String category;
    private final String text;
    private final List<String> options;
    private final int correctIndex;

    public QuizQuestion(String category, String text, List<String> options, int correctIndex) {
        this.category = category;
        this.text = text;
        this.options = options;
        this.correctIndex = correctIndex;
    }

    public String getCategory() { return category; }
    public String getText() { return text; }
    public List<String> getOptions() { return Collections.unmodifiableList(options); }
    public int getCorrectIndex() { return correctIndex; }
}
