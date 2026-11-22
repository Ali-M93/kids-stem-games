package com.lilifriends.games;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * The reading lessons of the grade-1 book «قراءتي الجديدة», in the book's order, read from
 * assets/letters/lessons.json. Every item is a piece of text and the file of Lili saying it.
 */
final class Lessons {
    /** A word, syllable or sentence, and the sound of Lili saying it. */
    static final class Item {
        final String text, sound;

        Item(String text, String sound) {
            this.text = text;
            this.sound = sound;
        }
    }

    static final class Lesson {
        final String title, big;
        final List<Item> intro, shortVowels, longVowels, sukun, words, sentences;

        Lesson(JSONObject o) throws JSONException {
            title = o.getString("title");
            big = o.getString("big");
            intro = items(o.optJSONArray("intro"));
            shortVowels = items(o.optJSONArray("short"));
            longVowels = items(o.optJSONArray("long"));
            sukun = items(o.optJSONArray("sukun"));
            words = items(o.optJSONArray("words"));
            sentences = items(o.optJSONArray("sents"));
        }

        /** Syllables to hear and pick: the vowels and the stopped (sukun) syllables. */
        List<Item> sounds() {
            List<Item> out = new ArrayList<>(shortVowels);
            out.addAll(longVowels);
            out.addAll(sukun);
            return out;
        }
    }

    final List<Lesson> lessons = new ArrayList<>();
    final List<Item> praise, tryAgain;
    final JSONObject say;

    Lessons(String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        JSONArray arr = root.getJSONArray("lessons");
        for (int i = 0; i < arr.length(); i++) lessons.add(new Lesson(arr.getJSONObject(i)));
        JSONObject g = root.getJSONObject("general");
        praise = items(g.optJSONArray("ok"));
        tryAgain = items(g.optJSONArray("again"));
        say = g.optJSONObject("say") != null ? g.optJSONObject("say") : new JSONObject();
    }

    /** The sound file of a general phrase such as «اِسْمَعْ وَاخْتَرْ», or null. */
    String phrase(String text) {
        String s = say.optString(text, "");
        return s.isEmpty() ? null : s;
    }

    private static List<Item> items(JSONArray a) throws JSONException {
        List<Item> out = new ArrayList<>();
        if (a == null) return out;
        for (int i = 0; i < a.length(); i++) {
            JSONArray p = a.getJSONArray(i);
            out.add(new Item(p.getString(0), p.getString(1)));
        }
        return out;
    }
}
