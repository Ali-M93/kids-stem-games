package io.github.foadsf.balancescale;
import java.util.*;
public class WeighCheck {
    public static void main(String[] a) {
        for (int stage = 0; stage <= 11; stage++) {
            int minNeed = 99, maxNeed = 0, maxLeftW = 0, maxPan = 0;
            for (int seed = 0; seed < 3000; seed++) {
                Weigh w = Weigh.make(stage, new Random(seed * 977L + stage));
                int need = w.need();
                check(need >= 1, "right must start lighter (need " + need + ")", stage);
                // reachable with the basket: apples (weight 1) are always offered
                boolean hasApple = false; for (int f : w.basket) hasApple |= f == Weigh.APPLE;
                check(hasApple, "an apple in the basket makes every target reachable", stage);
                // facts cover every non-apple fruit that appears
                Set<Integer> shown = new HashSet<>(); for (int f : w.facts) shown.add(f);
                List<Integer> all = new ArrayList<>(w.left); all.addAll(w.rightGiven); for (int f : w.basket) all.add(f);
                for (int f : all) if (f != Weigh.APPLE) check(shown.contains(f), "fact card for " + Weigh.NAME[f], stage);
                minNeed = Math.min(minNeed, need); maxNeed = Math.max(maxNeed, need);
                maxLeftW = Math.max(maxLeftW, Weigh.weight(w.left));
                maxPan = Math.max(maxPan, Math.max(w.left.size(), w.rightGiven.size() + need));
            }
            Weigh e = Weigh.make(stage, new Random(5));
            System.out.printf("stage %-2d need %d-%d  left weight <= %d  fruit per pan <= %d  e.g. L%s R%s basket %s%n",
                stage, minNeed, maxNeed, maxLeftW, maxPan, names(e.left), names(e.rightGiven), names(toList(e.basket)));
        }
    }
    static List<Integer> toList(int[] x) { List<Integer> l = new ArrayList<>(); for (int i : x) l.add(i); return l; }
    static String names(List<Integer> l) { StringBuilder sb = new StringBuilder("["); for (int f : l) sb.append(Weigh.NAME[f].charAt(0)); return sb.append("]").toString(); }
    static void check(boolean c, String w, int s) { if (!c) throw new AssertionError(w + " stage " + s); }
}
