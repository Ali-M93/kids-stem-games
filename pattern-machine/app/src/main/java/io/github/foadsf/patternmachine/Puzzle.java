package io.github.foadsf.patternmachine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * One pattern puzzle: a row of tokens with one or two gaps, and the tokens to choose from.
 *
 * Stages, easiest first:
 *   0  AB, ask the next one, two choices, the two tokens differ in shape AND colour
 *   1  AB, three choices
 *   2  AAB
 *   3  ABB
 *   4  ABC
 *   5  AB by colour only: same shape, the colour alternates
 *   6  AB or ABC with the gap in the middle
 *   7  AABB, four choices
 *   8  ABC with the last TWO missing, filled in order
 * Past stage 8, each puzzle picks one of stages 4-8 at random.
 */
final class Puzzle {
    static final int LAST_STAGE = 8;

    final int stage;
    final int kind;        // which of stages 0-8 this puzzle actually is (differs past stage 8)
    final int[] seq;       // the full pattern
    final int[] gaps;      // indices into seq that are hidden, filled in this order
    final int[] choices;   // tokens on the buttons
    final int unit;        // length of the repeating unit, for the hint

    private Puzzle(int stage, int kind, int[] seq, int[] gaps, int[] choices, int unit) {
        this.stage = stage;
        this.kind = kind;
        this.seq = seq;
        this.gaps = gaps;
        this.choices = choices;
        this.unit = unit;
    }

    static Puzzle make(int stage, Random r) {
        int s = stage > LAST_STAGE ? 4 + r.nextInt(LAST_STAGE - 3) : stage;
        String rule;
        switch (s) {
            case 0: case 1: case 5: rule = "AB"; break;
            case 2: rule = "AAB"; break;
            case 3: rule = "ABB"; break;
            case 4: case 8: rule = "ABC"; break;
            case 6: rule = r.nextBoolean() ? "AB" : "ABC"; break;
            default: rule = "AABB";
        }
        int letters = rule.contains("C") ? 3 : 2;

        // Pick distinct tokens for A, B, C.
        int[] pick = new int[letters];
        if (s == 5) {
            int shape = r.nextInt(Tokens.SHAPES);
            List<Integer> cols = shuffled(Tokens.COLOURS, r);
            for (int i = 0; i < letters; i++) pick[i] = Tokens.token(shape, cols.get(i));
        } else {
            // Distinct shapes and distinct colours, so either attribute shows the pattern.
            List<Integer> shapes = shuffled(Tokens.SHAPES, r), cols = shuffled(Tokens.COLOURS, r);
            for (int i = 0; i < letters; i++) pick[i] = Tokens.token(shapes.get(i), cols.get(i));
        }

        int unit = rule.length();
        int len, gapsAt[];
        if (s == 6) {
            len = unit * 3;
            gapsAt = new int[]{unit + r.nextInt(unit)};          // somewhere in the second repeat
        } else if (s == 8) {
            len = unit * 2 + 2;
            gapsAt = new int[]{len - 2, len - 1};
        } else {
            len = unit * 2 + 1 + r.nextInt(unit);                 // two full repeats, then a bit
            if (unit == 2 && len < 6) len = 6;                    // AB: show five, ask the sixth
            len = Math.min(len, 9);                               // fits two rows on a small phone
            gapsAt = new int[]{len - 1};
        }
        int[] seq = new int[len];
        for (int i = 0; i < len; i++) seq[i] = pick[rule.charAt(i % unit) - 'A'];

        // Choices: every token of the pattern, plus distractors up to the stage's count.
        int count = s == 0 ? 2 : s == 7 || s == 8 ? 4 : 3;
        List<Integer> ch = new ArrayList<>();
        for (int t : pick) if (!ch.contains(t)) ch.add(t);
        while (ch.size() < count) {
            int t;
            if (s == 5) {
                // Same colour, other shape: only someone watching the colour gets it right.
                t = Tokens.token(r.nextInt(Tokens.SHAPES), Tokens.colourOf(pick[r.nextInt(letters)]));
            } else {
                t = Tokens.token(r.nextInt(Tokens.SHAPES), r.nextInt(Tokens.COLOURS));
            }
            if (!ch.contains(t)) ch.add(t);
        }
        while (ch.size() > count) ch.remove(ch.size() - 1);  // never drops a needed token: count >= letters
        Collections.shuffle(ch, r);
        int[] choices = new int[ch.size()];
        for (int i = 0; i < choices.length; i++) choices[i] = ch.get(i);
        return new Puzzle(stage, s, seq, gapsAt, choices, unit);
    }

    private static List<Integer> shuffled(int n, Random r) {
        List<Integer> l = new ArrayList<>();
        for (int i = 0; i < n; i++) l.add(i);
        Collections.shuffle(l, r);
        return l;
    }

    /**
     * The shortest repeating unit of a free-form row, or 0 if there is none. A unit counts only
     * if it shows at least twice: in ★●★●★ the unit is ★● (length 2); ★●▲ has none.
     */
    static int period(int[] t, int n) {
        for (int p = 1; p * 2 <= n; p++) {
            boolean ok = true;
            for (int i = p; i < n && ok; i++) ok = t[i] == t[i % p];
            if (ok) return p;
        }
        return 0;
    }
}
