package com.lilifriends.games;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * The four games and how the journey map talks to each. Robot Path counts open-ended levels
 * from 1 ("level" = the furthest reached); the other three have stages from 0 ("stage" = the
 * furthest reached), and past their last stage they mix all stages, shown as one final stop.
 */
final class Game {
    final String id, activity;
    final int title, picture, fill, deep;
    /** Stops on the map for stage games (last stage + the mixed one); 0 for open-ended levels. */
    final int stops;

    private Game(String id, String activity, int title, int picture, int fill, int deep, int stops) {
        this.id = id;
        this.activity = activity;
        this.title = title;
        this.picture = picture;
        this.fill = fill;
        this.deep = deep;
        this.stops = stops;
    }

    static final Game PATH = new Game("path", "io.github.foadsf.robotpath.GameActivity",
            R.string.game_path, R.drawable.home_calm, Draw.BLUE, Draw.BLUE_D, 0);
    static final Game HOPS = new Game("hops", "io.github.foadsf.kangaroohops.GameActivity",
            R.string.game_hops, R.drawable.home_jump_up, Draw.PEACH, Draw.PEACH_D, 8 + 2);
    static final Game PATTERN = new Game("pattern", "io.github.foadsf.patternmachine.GameActivity",
            R.string.game_pattern, R.drawable.home_cheer, Draw.LILAC, Draw.LILAC_D, 8 + 2);
    static final Game SCALE = new Game("scale", "io.github.foadsf.balancescale.GameActivity",
            R.string.game_scale, R.drawable.home_think, Draw.MINT, Draw.MINT_D, 7 + 2);
    static final Game[] ALL = {PATH, HOPS, PATTERN, SCALE};

    static Game byId(String id) {
        for (Game g : ALL) if (g.id.equals(id)) return g;
        return PATH;
    }

    boolean openEnded() {
        return stops == 0;
    }

    /** The furthest stop reached, 1-based. */
    int furthest(SharedPreferences p) {
        if (openEnded()) return Math.max(1, p.getInt("level", 1));
        return Math.min(stops, p.getInt("stage", 0) + 1);
    }

    /** How many stops to draw: all of them, or for open-ended games a few locked ones ahead. */
    int shown(SharedPreferences p) {
        return openEnded() ? Math.max(12, furthest(p) + 5) : stops;
    }

    /** Starts stop {@code n} (1-based) for {@code player}. */
    Intent start(Context ctx, int player, int n, SharedPreferences p) {
        Intent i = new Intent().setClassName(ctx, activity);
        i.putExtra("prefs", Store.progressFile(player, this));
        if (openEnded()) {
            i.putExtra("level", n);
        } else {
            // The last stop is "everything mixed": keep the child's real stage if it is further.
            int stage = n - 1;
            if (n == stops) stage = Math.max(stage, p.getInt("stage", 0));
            i.putExtra("stage", stage);
        }
        return i;
    }
}
