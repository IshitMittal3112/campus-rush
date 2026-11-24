package com.campusrush.challenge;

import com.campusrush.util.AppLogger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Data-driven question bank loaded from questions.csv (category|question|a|b|c|d|correct#).
 * Designers can add questions without touching Java code.
 */
public final class QuestionBank {
    private static final Map<String, List<QuizQuestion>> BY_CATEGORY = new HashMap<>();
    private static boolean loaded = false;

    private QuestionBank() {}

    private static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        InputStream is = QuestionBank.class.getClassLoader().getResourceAsStream("questions.csv");
        if (is == null) {
            AppLogger.warning("questions.csv not found - using one built-in question per category.");
        } else {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                int lineNo = 0;
                while ((line = reader.readLine()) != null) {
                    lineNo++;
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    String[] p = line.split("\\|");
                    try {
                        if (p.length < 7) throw new IllegalArgumentException("expected 7 fields");
                        int correct = Integer.parseInt(p[6].trim()) - 1;
                        List<String> opts = Arrays.asList(p[2].trim(), p[3].trim(), p[4].trim(), p[5].trim());
                        if (correct < 0 || correct > 3) throw new IllegalArgumentException("bad correct option");
                        add(new QuizQuestion(p[0].trim().toUpperCase(), p[1].trim(), opts, correct));
                    } catch (RuntimeException ex) {
                        AppLogger.warning("Skipping bad question on line " + lineNo + ": " + ex.getMessage());
                    }
                }
            } catch (Exception e) {
                AppLogger.severe("Failed to read questions.csv: " + e.getMessage(), e);
            }
        }
        for (String cat : new String[]{"JAVA", "LOGIC", "CAMPUS"}) {
            if (!BY_CATEGORY.containsKey(cat)) {
                add(new QuizQuestion(cat, "Which keyword prevents a class from being subclassed?",
                        Arrays.asList("static", "abstract", "final", "private"), 2));
            }
        }
        AppLogger.info("Question bank loaded: " + BY_CATEGORY.keySet());
    }

    private static void add(QuizQuestion q) {
        BY_CATEGORY.computeIfAbsent(q.getCategory(), k -> new ArrayList<>()).add(q);
    }

    public static int size(String category) {
        ensureLoaded();
        return BY_CATEGORY.getOrDefault(category.toUpperCase(), Collections.emptyList()).size();
    }

    public static QuizQuestion random(String category, Random rng) {
        ensureLoaded();
        List<QuizQuestion> list = BY_CATEGORY.get(category.toUpperCase());
        if (list == null || list.isEmpty()) list = BY_CATEGORY.values().iterator().next();
        return list.get(rng.nextInt(list.size()));
    }
}
