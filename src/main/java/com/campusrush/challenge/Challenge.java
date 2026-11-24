package com.campusrush.challenge;

import java.io.Serializable;
import java.util.List;
public interface Challenge extends Serializable {
    String getQuestionText();
    List<String> getOptions();
    boolean validateAnswer(String userAnswer);
    int getBonusPoints();
    String getCorrectAnswerDisplay();
}
