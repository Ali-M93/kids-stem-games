package io.github.foadsf.robotpath;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * One board: a w×h grid with rocks, a start cell and a treat. Cells are numbered row-major,
 * {@code cell = y * w + x}, with y growing downwards.
 *
 * Levels 1-6 are drawn by hand so the first steps are gentle (two moves straight, then a turn);
 * after that they are generated from the level number, so the same number always gives the same
 * board and a parent can say "do level 9 again". Every level is checked by breadth-first search:
 * {@link #optimal} is the length of the shortest program that reaches the treat.
 */
final class Level {
    static final int UP = 0, RIGHT = 1, DOWN = 2, LEFT = 3;
    static final int[] DX = {0, 1, 0, -1};
    static final int[] DY = {-1, 0, 1, 0};

    final int number, w, h, start, goal;
    final boolean[] rock;
    final int optimal;

    private Level(int number, int w, int h, boolean[] rock, int start, int goal) {
        this.number = number;
        this.w = w;
        this.h = h;
        this.rock = rock;
        this.start = start;
        this.goal = goal;
        int[] dist = distances(w, h, rock, start);
        this.optimal = dist[goal];
        if (optimal <= 0) throw new IllegalStateException("unsolvable level " + number);
    }

    /** How many program slots she gets: the shortest solution plus room to wander. */
    int slots() {
        return Math.min(12, optimal + (optimal <= 2 ? 1 : 2));
    }

    /** The cell one step from {@code cell} in direction {@code d}, or -1 for a wall or rock. */
    int step(int cell, int d) {
        int x = cell % w + DX[d], y = cell / w + DY[d];
        if (x < 0 || y < 0 || x >= w || y >= h) return -1;
        int next = y * w + x;
        return rock[next] ? -1 : next;
    }

    /** One shortest program from the start to the treat, as directions. Used for hints. */
    int[] solution() {
        int n = w * h;
        int[] prev = new int[n], how = new int[n];
        Arrays.fill(prev, -2);
        ArrayDeque<Integer> q = new ArrayDeque<>();
        q.add(start);
        prev[start] = -1;
        while (!q.isEmpty()) {
            int c = q.poll();
            if (c == goal) break;
            for (int d = 0; d < 4; d++) {
                int nx = step(c, d);
                if (nx >= 0 && prev[nx] == -2) {
                    prev[nx] = c;
                    how[nx] = d;
                    q.add(nx);
                }
            }
        }
        int[] path = new int[optimal];
        for (int c = goal, i = optimal - 1; c != start; c = prev[c], i--) path[i] = how[c];
        return path;
    }

    static int[] distances(int w, int h, boolean[] rock, int from) {
        int[] dist = new int[w * h];
        Arrays.fill(dist, -1);
        dist[from] = 0;
        ArrayDeque<Integer> q = new ArrayDeque<>();
        q.add(from);
        while (!q.isEmpty()) {
            int c = q.poll(), x = c % w, y = c / w;
            for (int d = 0; d < 4; d++) {
                int nx = x + DX[d], ny = y + DY[d];
                if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                int n = ny * w + nx;
                if (rock[n] || dist[n] >= 0) continue;
                dist[n] = dist[c] + 1;
                q.add(n);
            }
        }
        return dist;
    }

    // ---- the levels ---------------------------------------------------------------------

    static Level get(int n) {
        switch (n) {
            case 1: return hand(1, 3, 3, "...", "S.G", "...");   // → →
            case 2: return hand(2, 3, 3, ".G.", "...", ".S.");   // ↑ ↑
            case 3: return hand(3, 3, 3, "...", ".G.", "S..");   // → ↑   (first turn)
            case 4: return hand(4, 4, 4, "....", "....", "....", "S..G");   // → → →
            case 5: return hand(5, 4, 4, "....", "..G.", "....", "S...");   // → → ↑ ↑
            case 6: return hand(6, 3, 2, "S#G", "...");  // first rock: ↓ → → ↑
            default: return generate(n);
        }
    }

    private static Level hand(int n, int w, int h, String... rows) {
        boolean[] rock = new boolean[w * h];
        int s = -1, g = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                char ch = rows[y].charAt(x);
                if (ch == '#') rock[y * w + x] = true;
                else if (ch == 'S') s = y * w + x;
                else if (ch == 'G') g = y * w + x;
            }
        }
        return new Level(n, w, h, rock, s, g);
    }

    /** Bigger boards, longer paths and more rocks as the number grows. */
    private static Level generate(int n) {
        int size = n < 12 ? 4 : n < 20 ? 5 : 6;
        int target = Math.min(3 + (n - 5) / 2, 9); // nine moves is plenty for a five-year-old
        int rocks = Math.min(size * size / 4, 1 + (n - 5) / 3);
        for (int want = target; want >= 2; want--) {
            for (int attempt = 0; attempt < 300; attempt++) {
                Random r = new Random(n * 7919L + want * 104729L + attempt);
                boolean[] rock = new boolean[size * size];
                for (int i = 0; i < rocks; i++) rock[r.nextInt(size * size)] = true;
                int start = r.nextInt(size * size);
                if (rock[start]) continue;
                int[] dist = distances(size, size, rock, start);
                List<Integer> goals = new ArrayList<>();
                for (int c = 0; c < dist.length; c++) if (dist[c] == want) goals.add(c);
                if (goals.isEmpty()) continue;
                int goal = goals.get(r.nextInt(goals.size()));
                // Past level 8, insist the shortest route is not a straight line.
                if (n > 8 && (start % size == goal % size || start / size == goal / size)) continue;
                return new Level(n, size, size, rock, start, goal);
            }
        }
        throw new IllegalStateException("no level " + n);
    }
}
