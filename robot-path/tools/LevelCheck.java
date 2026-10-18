package io.github.foadsf.robotpath;
public class LevelCheck {
    public static void main(String[] a) {
        StringBuilder sb = new StringBuilder();
        for (int n = 1; n <= 80; n++) {
            Level l = Level.get(n);
            int[] sol = l.solution();
            // replay the solution: it must reach the goal in exactly `optimal` legal steps
            int c = l.start;
            for (int d : sol) { c = l.step(c, d); if (c < 0) throw new AssertionError("bad step L" + n); }
            if (c != l.goal || sol.length != l.optimal) throw new AssertionError("solution wrong L" + n);
            if (l.optimal > l.slots()) throw new AssertionError("too few slots L" + n);
            int rocks = 0; for (boolean b : l.rock) if (b) rocks++;
            if (n <= 24 || n % 10 == 0) sb.append(String.format("L%-3d %dx%d opt=%d slots=%d rocks=%d%n", n, l.w, l.h, l.optimal, l.slots(), rocks));
            if (Level.get(n).goal != l.goal) throw new AssertionError("not deterministic L" + n);
        }
        System.out.print(sb);
        // negative control: a walled-off board must be rejected
        try { java.lang.reflect.Method m = Level.class.getDeclaredMethod("hand", int.class, int.class, int.class, String[].class);
              m.setAccessible(true); m.invoke(null, 99, 3, 1, new String[]{"S#G"});
              System.out.println("NEGATIVE CONTROL FAILED: unsolvable level accepted"); }
        catch (java.lang.reflect.InvocationTargetException e) { System.out.println("negative control OK: " + e.getCause().getMessage()); }
        catch (Exception e) { System.out.println("control error " + e); }
    }
}
