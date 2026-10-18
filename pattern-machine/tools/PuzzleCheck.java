package io.github.foadsf.patternmachine;
import java.util.*;
public class PuzzleCheck {
    public static void main(String[] a) {
        int[] expectChoices = {2,3,3,3,3,3,3,4,4};
        for (int stage = 0; stage <= 12; stage++) {
            int maxLen = 0, minLen = 99;
            for (int seed = 0; seed < 2000; seed++) {
                Puzzle p = Puzzle.make(stage, new Random(seed * 31L + stage));
                int n = p.seq.length; maxLen = Math.max(maxLen, n); minLen = Math.min(minLen, n);
                if (n > 9) throw new AssertionError("too long stage " + stage);
                Set<Integer> ch = new HashSet<>(); for (int c : p.choices) ch.add(c);
                if (ch.size() != p.choices.length) throw new AssertionError("duplicate choice s" + stage);
                for (int g : p.gaps) if (!ch.contains(p.seq[g])) throw new AssertionError("answer not offered s" + stage);
                if (stage <= 8 && p.choices.length != expectChoices[stage]) throw new AssertionError("choice count s" + stage + " " + p.choices.length);
                // the full row has exactly the intended rule: its shortest period is the unit
                if (Puzzle.period(p.seq, n) != p.unit) throw new AssertionError("period " + Puzzle.period(p.seq, n) + " != unit " + p.unit + " s" + stage);
                // what she can see (before the first gap) already shows the unit at least... twice for end gaps
                int firstGap = p.gaps[0];
                if (firstGap < 2 * p.unit - (p.kind == 6 ? p.unit : 0)) throw new AssertionError("not enough shown s" + stage);
                if (p.kind == 5) { // colour-only: one shape throughout, and a same-colour wrong-shape decoy usually present
                    int sh = Tokens.shapeOf(p.seq[0]); for (int t : p.seq) if (Tokens.shapeOf(t) != sh) throw new AssertionError("shape varies s5");
                }
                if (p.kind == 0) { if (Tokens.shapeOf(p.seq[0]) == Tokens.shapeOf(p.seq[1]) || Tokens.colourOf(p.seq[0]) == Tokens.colourOf(p.seq[1])) throw new AssertionError("s0 not doubly distinct"); }
            }
            Puzzle ex = Puzzle.make(stage, new Random(7));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < ex.seq.length; i++) { boolean gap = false; for (int g : ex.gaps) gap |= g == i; sb.append(gap ? "?" : String.valueOf((char)('A' + idx(ex, ex.seq[i])))); }
            System.out.printf("stage %-2d len %d-%d choices %d  e.g. %s%n", stage, minLen, maxLen, ex.choices.length, sb);
        }
        // period(): the machine's rule-finder for her own patterns
        check(Puzzle.period(new int[]{1,2,1,2,1}, 5) == 2, "*.*.* -> 2");
        check(Puzzle.period(new int[]{1,2,3}, 3) == 0, "abc -> none");
        check(Puzzle.period(new int[]{1,1,1,1}, 4) == 1, "aaaa -> 1");
        check(Puzzle.period(new int[]{1,1,2,1,1,2}, 6) == 3, "aab aab -> 3");
        check(Puzzle.period(new int[]{1,2,1,3}, 4) == 0, "abac -> none");
        System.out.println("period checks OK");
    }
    static int idx(Puzzle p, int t) { List<Integer> seen = new ArrayList<>(); for (int x : p.seq) if (!seen.contains(x)) seen.add(x); return seen.indexOf(t); }
    static void check(boolean c, String w) { if (!c) throw new AssertionError(w); }
}
