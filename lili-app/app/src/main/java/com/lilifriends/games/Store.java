package com.lilifriends.games;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Players on this device. Each player has a small id and an avatar picture; their progress in
 * each game lives in its own SharedPreferences file, "p<id>_<game>", which the game opens.
 */
final class Store {
    static final int MAX_PLAYERS = 6;
    private static final String FILE = "players", KEY_IDS = "ids", KEY_NEXT = "next";

    private final Context ctx;
    private final SharedPreferences prefs;

    Store(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        prefs = this.ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    /** Player ids, in the order they were added. */
    List<Integer> players() {
        List<Integer> out = new ArrayList<>();
        String s = prefs.getString(KEY_IDS, "");
        for (String part : s.split(",")) {
            if (part.isEmpty()) continue;
            try {
                out.add(Integer.parseInt(part));
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }

    int avatar(int id) {
        return prefs.getInt("avatar_" + id, id % Draw.AVATARS);
    }

    /** Adds a player with the first avatar nobody is using yet; returns the new id, or -1. */
    int add() {
        List<Integer> ids = players();
        if (ids.size() >= MAX_PLAYERS) return -1;
        boolean[] used = new boolean[Draw.AVATARS];
        for (int id : ids) used[avatar(id) % Draw.AVATARS] = true;
        int pick = 0;
        while (pick < Draw.AVATARS - 1 && used[pick]) pick++;
        int id = prefs.getInt(KEY_NEXT, 1);
        ids.add(id);
        prefs.edit().putInt(KEY_NEXT, id + 1).putInt("avatar_" + id, pick).putString(KEY_IDS, join(ids)).apply();
        return id;
    }

    /** Removes a player and wipes their progress in every game. */
    void remove(int id) {
        List<Integer> ids = players();
        ids.remove(Integer.valueOf(id));
        prefs.edit().remove("avatar_" + id).putString(KEY_IDS, join(ids)).apply();
        for (Game g : Game.ALL) {
            String name = progressFile(id, g);
            ctx.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().apply();
            File f = new File(ctx.getApplicationInfo().dataDir, "shared_prefs/" + name + ".xml");
            //noinspection ResultOfMethodCallIgnored
            f.delete();
        }
    }

    static String progressFile(int player, Game g) {
        return "p" + player + "_" + g.id;
    }

    SharedPreferences progress(int player, Game g) {
        return ctx.getSharedPreferences(progressFile(player, g), Context.MODE_PRIVATE);
    }

    private static String join(List<Integer> ids) {
        StringBuilder sb = new StringBuilder();
        for (int id : ids) {
            if (sb.length() > 0) sb.append(',');
            sb.append(id);
        }
        return sb.toString();
    }
}
