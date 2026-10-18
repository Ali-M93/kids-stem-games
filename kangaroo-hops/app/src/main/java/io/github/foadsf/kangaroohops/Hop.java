package io.github.foadsf.kangaroohops;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * One Kangaroo Hops puzzle. Plain Java (no Android types), so it can be tested on any JVM.
 *
 * Stages, easiest first:
 *   0  count from zero: start at 0, hop 1-5          (0-10 line)
 *   1  add 1 or 2, start 1-6
 *   2  add 3-5, landing at most 10
 *   3  take away 1 or 2, start 3-10
 *   4  take away 3-5
 *   5  which is more? two numbers 0-10, two choices
 *   6  add 2-5 on the 0-20 line, not crossing ten
 *   7  add 3-9 crossing ten (8 + 5)
 *   8  take away 2-9 on the 0-20 line
 * Past stage 8, each puzzle picks one of stages 5-8.
 */
final class Hop {
    static final int LAST_STAGE = 8;
    static final int ADD = 0, TAKE = 1, MORE = 2;

    final int stage, kind, op;
    final int max;          // the line runs 0..max (10 or 20)
    final int start, delta; // ADD / TAKE: kangaroo starts at start, hops delta
    final int a, b;         // MORE: the two numbers compared
    final int answer;
    final int[] choices;

    private Hop(int stage, int kind, int op, int max, int start, int delta, int a, int b,
                int answer, int[] choices) {
        this.stage = stage;
        this.kind = kind;
        this.op = op;
        this.max = max;
        this.start = start;
        this.delta = delta;
        this.a = a;
        this.b = b;
        this.answer = answer;
        this.choices = choices;
    }

    /** Where the kangaroo lands (ADD/TAKE). */
    int landing() {
        return op == ADD ? start + delta : start - delta;
    }

    static Hop make(int stage, Random r) {
        int k = stage > LAST_STAGE ? 5 + r.nextInt(LAST_STAGE - 4) : stage;
        int max = k >= 6 ? 20 : 10;
        if (k == 5) {
            int x = r.nextInt(11), y;
            do {
                y = r.nextInt(11);
            } while (y == x || Math.abs(x - y) > 6);  // not equal, and not trivially far apart
            int big = Math.max(x, y);
            return new Hop(stage, k, MORE, max, 0, 0, x, y, big, r.nextBoolean() ? new int[]{x, y} : new int[]{y, x});
        }
        int start, delta, op;
        switch (k) {
            case 0: op = ADD; start = 0; delta = 1 + r.nextInt(5); break;
            case 1: op = ADD; start = 1 + r.nextInt(6); delta = 1 + r.nextInt(2); break;
            case 2: op = ADD; delta = 3 + r.nextInt(3); start = r.nextInt(10 - delta + 1); break;
            case 3: op = TAKE; delta = 1 + r.nextInt(2); start = 3 + r.nextInt(8); break;
            case 4: op = TAKE; delta = 3 + r.nextInt(3); start = delta + r.nextInt(10 - delta + 1); break;
            case 6: {
                op = ADD;
                // stay on one side of ten: 11-15 + 2-5, or 1-5 + 2-5
                delta = 2 + r.nextInt(4);
                int lo = r.nextBoolean() ? 1 : 11;
                start = lo + r.nextInt(10 - delta);
                break;
            }
            case 7: {
                op = ADD;
                delta = 3 + r.nextInt(7);                           // 3..9
                int minStart = Math.max(1, 11 - delta);             // so it crosses ten
                start = minStart + r.nextInt(Math.max(1, 10 - minStart));
                break;
            }
            default: op = TAKE; delta = 2 + r.nextInt(8); start = 10 + r.nextInt(11); break;
        }
        int ans = op == ADD ? start + delta : start - delta;
        // Choices: the answer and its near neighbours (the classic off-by-one slips), or the
        // start number ("forgot to hop") when the answer sits at the end of the line.
        List<Integer> ch = new ArrayList<>();
        ch.add(ans);
        int[] near = {ans - 1, ans + 1, ans - 2, ans + 2, ans - 3, ans + 3};
        for (int n : near) {
            if (ch.size() == 3) break;
            if (n >= 0 && n <= max && !ch.contains(n)) ch.add(n);
        }
        Collections.shuffle(ch, r);
        int[] c = new int[ch.size()];
        for (int i = 0; i < c.length; i++) c[i] = ch.get(i);
        return new Hop(stage, k, op, max, start, delta, 0, 0, ans, c);
    }
}
