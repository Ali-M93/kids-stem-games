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
        /** The word cut into syllables, as in the book's «ركّب المقاطع», or null. */
        final String[] parts;

        Item(String text, String sound, String parts) {
            this.text = text;
            this.sound = sound;
            this.parts = parts == null || parts.isEmpty() ? null : parts.split("\\|");
        }
    }

    static final class Lesson {
        final String title, big;
        /** A page of the book to look at (listening, conversation and songs), or null. */
        final String pic;
        final List<Item> intro, shortVowels, longVowels, sukun, words, sentences;
        /** «Put another letter in place of the red one»: the word, and the new word it makes. */
        final List<Item[]> swaps = new ArrayList<>();
        /** For each swap: which letter (counted in {@link #letters}) is red, and its new letter. */
        final List<String[]> swapHow = new ArrayList<>();

        Lesson(JSONObject o) throws JSONException {
            title = o.getString("title");
            big = o.getString("big");
            String p = o.optString("pic", "");
            pic = p.isEmpty() ? null : p;
            intro = items(o.optJSONArray("intro"));
            shortVowels = items(o.optJSONArray("short"));
            longVowels = items(o.optJSONArray("long"));
            sukun = items(o.optJSONArray("sukun"));
            words = items(o.optJSONArray("words"));
            sentences = items(o.optJSONArray("sents"));
            JSONArray sw = o.optJSONArray("swap");
            for (int i = 0; sw != null && i < sw.length(); i++) {
                JSONArray s = sw.getJSONArray(i);
                swaps.add(new Item[]{new Item(s.getString(0), s.getString(1), null),
                        new Item(s.getString(2), s.getString(3), null)});
                swapHow.add(new String[]{s.getString(4), s.getString(5)});
            }
        }

        /** Words to spell letter by letter: three to five letters long. */
        List<Item> spellable() {
            List<Item> out = new ArrayList<>();
            for (Item it : words) {
                int n = letters(it.text).size();
                if (n >= 3 && n <= 5) out.add(it);
            }
            return out;
        }

        /** Words that can be built from their syllables. */
        List<Item> buildable() {
            List<Item> out = new ArrayList<>();
            for (Item it : words) if (it.parts != null) out.add(it);
            return out;
        }

        /** Sentences that can be put back together from their words. */
        List<Item> orderable() {
            List<Item> out = new ArrayList<>();
            for (Item it : sentences) if (it.parts != null) out.add(it);
            return out;
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

    /** A word cut into letters, each with its marks (حركات، شدة، سكون). */
    static List<String> letters(String word) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < word.length(); ) {
            int cp = word.codePointAt(i);
            int n = Character.charCount(cp);
            boolean mark = Character.getType(cp) == Character.NON_SPACING_MARK;
            if (!mark && cur.length() > 0) {
                out.add(cur.toString());
                cur.setLength(0);
            }
            if (!Character.isWhitespace(cp)) cur.appendCodePoint(cp);
            i += n;
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
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
            out.add(new Item(p.getString(0), p.getString(1), p.length() > 2 ? p.getString(2) : null));
        }
        return out;
    }
}
