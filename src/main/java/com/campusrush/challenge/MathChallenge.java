package com.campusrush.challenge;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
public class MathChallenge extends MultipleChoiceChallenge {
    private static final long serialVersionUID = 2L;

    public MathChallenge(Random rng) {
        this(rng.nextInt(12) + 8, rng.nextInt(9) + 6, rng);
    }

    private MathChallenge(int a, int b, Random rng) {
        super("Quick Math Hurdle! Calculate: " + a + " x " + b + " = ?", buildOptions(a * b, rng), 0, rng);
    }

    private static List<String> buildOptions(int answer, Random rng) {
        Set<Integer> values = new LinkedHashSet<>();
        values.add(answer);                       
        int[] offsets = {10, -10, 12, -12, 20, -20, 5, -5, 1, -1};
        int i = rng.nextInt(offsets.length);
        while (values.size() < 4) {
            values.add(answer + offsets[i % offsets.length]);
            i++;
        }
        List<String> out = new ArrayList<>();
        for (int v : values) out.add(String.valueOf(v));
        return out;
    }
}
