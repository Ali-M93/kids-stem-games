package io.github.foadsf.kangaroohops;
import java.util.*;
public class HopCheck {
    public static void main(String[] a) {
        for (int stage = 0; stage <= 12; stage++) {
            Map<String,Integer> seen = new TreeMap<>();
            for (int seed = 0; seed < 3000; seed++) {
                Hop h = Hop.make(stage, new Random(seed * 131L + stage));
                Set<Integer> c = new HashSet<>(); for (int x : h.choices) c.add(x);
                check(c.size() == h.choices.length, "dup choice", stage);
                check(c.contains(h.answer), "answer missing", stage);
                for (int x : h.choices) check(x >= 0 && x <= h.max, "choice off the line " + x, stage);
                if (h.op == Hop.MORE) {
                    check(h.choices.length == 2 && h.a != h.b && h.answer == Math.max(h.a, h.b), "more", stage);
                } else {
                    check(h.choices.length == 3, "3 choices", stage);
                    check(h.start >= 0 && h.start <= h.max && h.landing() >= 0 && h.landing() <= h.max, "on line", stage);
                    check(h.answer == h.landing(), "answer = landing", stage);
                    check(h.delta >= 1, "delta", stage);
                    if (h.kind == 0) check(h.start == 0 && h.delta <= 5, "s0", stage);
                    if (h.kind <= 4) check(h.max == 10, "10 line", stage);
                    if (h.kind == 6) check((h.start <= 10) == (h.landing() <= 10), "s6 must not cross ten", stage);
                    if (h.kind == 7) check(h.start < 10 && h.landing() > 10, "s7 must cross ten", stage);
                }
                String key = h.op == Hop.MORE ? "more" : (h.op == Hop.ADD ? "+" : "-") + h.delta;
                seen.merge(key, 1, Integer::sum);
            }
            Hop e = Hop.make(stage, new Random(3));
            System.out.printf("stage %-2d e.g. %s  ops %s%n", stage,
                e.op == Hop.MORE ? "more of " + e.a + "," + e.b : e.start + (e.op == Hop.ADD ? "+" : "-") + e.delta + "=" + e.answer + " choices " + Arrays.toString(e.choices), seen.keySet());
        }
        check(Numbers.digits("fa", 17).equals("۱۷"), "persian 17", -1);
        check(Numbers.digits("nl", 17).equals("17"), "nl 17", -1);
        check(Numbers.word("nl", 20).equals("twintig") && Numbers.word("fa", 0).equals("صفر"), "words", -1);
        System.out.println("digits/words OK: " + Numbers.digits("fa", 20) + " " + Numbers.word("fa", 7));
    }
    static void check(boolean c, String w, int s) { if (!c) throw new AssertionError(w + " stage " + s); }
}
