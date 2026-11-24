package com.campusrush.challenge;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class PatternSequenceChallenge extends MultipleChoiceChallenge {
    private static final long serialVersionUID = 2L;

    public PatternSequenceChallenge(Random rng) {
        this(generate(rng), rng);
    }

    private PatternSequenceChallenge(long[] seq, Random rng) {
        super(question(seq), options(seq[seq.length - 1], rng), 0, rng);
    }

    /** Returns 5 shown terms followed by the answer as the last element. */
    private static long[] generate(Random rng) {
        long[] s = new long[6];
        switch (rng.nextInt(4)) {
            case 0: { // arithmetic
                long a = rng.nextInt(10) + 2, d = rng.nextInt(7) + 2;
                for (int i = 0; i < 6; i++) s[i] = a + d * i;
                break;
            }
            case 1: { // squares plus a constant
                long k = rng.nextInt(5);
                for (int i = 0; i < 6; i++) s[i] = (long) (i + 1) * (i + 1) + k;
                break;
            }
            case 2: { // geometric
                long a = rng.nextInt(3) + 1, r = rng.nextInt(2) + 2;
                s[0] = a;
                for (int i = 1; i < 6; i++) s[i] = s[i - 1] * r;
                break;
            }
            default: { // fibonacci-like
                s[0] = rng.nextInt(3) + 1;
                s[1] = rng.nextInt(3) + 1;
                for (int i = 2; i < 6; i++) s[i] = s[i - 1] + s[i - 2];
            }
        }
        return s;
    }

    private static String question(long[] s) {
        StringBuilder sb = new StringBuilder("Find the next number in the pattern: ");
        for (int i = 0; i < 5; i++) sb.append(s[i]).append(", ");
        return sb.append("?").toString();
    }

    private static List<String> options(long answer, Random rng) {
        Set<Long> values = new LinkedHashSet<>();
        values.add(answer);
        long[] offsets = {1, -1, 2, -2, 3, -3, 4, 5};
        int i = rng.nextInt(offsets.length);
        while (values.size() < 4) {
            long v = answer + offsets[i % offsets.length];
            if (v > 0) values.add(v);
            i++;
        }
        List<String> out = new ArrayList<>();
        for (long v : values) out.add(String.valueOf(v));
        return out;
    }
}
