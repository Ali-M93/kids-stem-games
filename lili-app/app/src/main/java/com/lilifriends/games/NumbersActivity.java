package com.lilifriends.games;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Lili's Numbers: the grade-1 maths book («الرياضيات للصف الأول الابتدائي») stop by stop, in Lili's
 * voice. Unlike the reading book, nothing here is a page of the book: every exercise is drawn and
 * made up afresh (things to count, patterns, number lines, sums), so a child never learns the
 * answers by heart. Lili's recordings are the number names and short instructions
 * ({@code assets/math}); praise, «try again» and the boy/girl question are the letters' ones.
 */
public class NumbersActivity extends Activity {
    public static final String EXTRA_PREFS = "prefs", EXTRA_STAGE = "stage";

    // What a stop teaches.
    private static final int T_PATTERN = 0, T_NUM = 1, T_COMPARE = 2, T_NEIGHBOR = 3, T_ORDER = 4,
            T_ORDINAL = 5, T_ADD = 6, T_SUB = 7;

    /** One stop of the map: its title, the label on the map, what it teaches and its numbers. */
    private static final class Stop {
        final String title, label;
        final int type, max;
        final int[] nums;

        Stop(String title, String label, int type, int max, int... nums) {
            this.title = title;
            this.label = label;
            this.type = type;
            this.max = max;
            this.nums = nums;
        }
    }

    /** The book's order: chapter 1 (patterns, numbers to 20), chapter 2 (compare and order), chapter 3 (sums to 10). */
    private static final Stop[] STOPS = {
            new Stop("الأنماط", "▲●", T_PATTERN, 0),
            new Stop("العددان ١ و ٢", "١ ٢", T_NUM, 2, 1, 2),
            new Stop("العددان ٣ و ٤", "٣ ٤", T_NUM, 4, 3, 4),
            new Stop("العددان ٥ و ٦", "٥ ٦", T_NUM, 6, 5, 6),
            new Stop("الأعداد ٧ و ٨ و ٩", "٧-٩", T_NUM, 9, 7, 8, 9),
            new Stop("العدد صفر", "٠", T_NUM, 9, 0),
            new Stop("العدد ١٠", "١٠", T_NUM, 10, 10),
            new Stop("الأعداد من ١١ إلى ١٥", "١١-١٥", T_NUM, 15, 11, 12, 13, 14, 15),
            new Stop("الأعداد من ١٦ إلى ٢٠", "١٦-٢٠", T_NUM, 20, 16, 17, 18, 19, 20),
            new Stop("مقارنة الأعداد حتى ٩", "أكبر", T_COMPARE, 9),
            new Stop("السابق والتالي حتى ٩", "قبل", T_NEIGHBOR, 9),
            new Stop("ترتيب الأعداد حتى ٩", "رتّب", T_ORDER, 9),
            new Stop("العدد الترتيبي", "الأول", T_ORDINAL, 10),
            new Stop("مقارنة الأعداد حتى ٢٠", "٢٠<", T_COMPARE, 20),
            new Stop("السابق والتالي حتى ٢٠", "بعد", T_NEIGHBOR, 20),
            new Stop("ترتيب الأعداد حتى ٢٠", "٢٠", T_ORDER, 20),
            new Stop("الجمع حتى ١٠", "+", T_ADD, 10),
            new Stop("الطرح حتى ١٠", "−", T_SUB, 10),
    };

    /** What each stop shows on the journey map. */
    static final String[] STOP_LABELS = new String[STOPS.length];

    static {
        for (int i = 0; i < STOPS.length; i++) STOP_LABELS[i] = STOPS[i].label;
    }

    private static final String[] NAMES = {"صِفْر", "وَاحِد", "اِثْنَان", "ثَلَاثَة", "أَرْبَعَة", "خَمْسَة", "سِتَّة",
            "سَبْعَة", "ثَمَانِيَة", "تِسْعَة", "عَشَرَة", "أَحَدَ عَشَرَ", "اِثْنَا عَشَرَ", "ثَلَاثَةَ عَشَرَ", "أَرْبَعَةَ عَشَرَ",
            "خَمْسَةَ عَشَرَ", "سِتَّةَ عَشَرَ", "سَبْعَةَ عَشَرَ", "ثَمَانِيَةَ عَشَرَ", "تِسْعَةَ عَشَرَ", "عِشْرُون"};
    private static final String[] ORDINALS = {"الْأَوَّل", "الثَّانِي", "الثَّالِث", "الرَّابِع", "الْخَامِس", "السَّادِس",
            "السَّابِع", "الثَّامِن", "التَّاسِع", "الْعَاشِر"};

    private static final int ASK = 0, INTRO = 1, TRACE = 2, COUNT = 3, FIND = 4, PATTERN = 5, COMPARE = 6,
            NEIGHBOR = 7, ORDER = 8, ORDINAL = 9, ADD = 10, SUB = 11, DONE = 12;
    private static final int ROUNDS = 6;

    private SharedPreferences prefs;
    private Lessons book;
    private final Map<String, String> voice = new HashMap<>();
    private Stop stop;
    private int stage;
    private final Random rnd = new Random();
    private MediaPlayer player;
    private NumView view;
    private boolean girl, busy;

    private final List<Integer> steps = new ArrayList<>();
    private int step, round, rounds, traceIdx;

    // The question on screen. Choices are shown as cards; {@code answer} is the right one.
    private final List<String> options = new ArrayList<>();
    private int answer = -1, wrongPick = -1, rightPick = -1;
    private long wobbleAt;
    /** Numbers of the question: what to count, the two to compare, a+b, a-b, the line's start… */
    private int qa, qb, qc;
    /** A pattern: shapes (shape*10+colour) with the last one missing. */
    private final List<Integer> pattern = new ArrayList<>();
    private final List<Integer> shapeOptions = new ArrayList<>();
    /** Putting numbers in order: the right order, the shuffled cards, which are used. */
    private final List<Integer> order = new ArrayList<>();
    private final List<Integer> cards = new ArrayList<>();
    private final List<Boolean> used = new ArrayList<>();
    private int placed;
    /** What Lili says when the speaker is pressed. */
    private final List<String> question = new ArrayList<>();

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        String file = getIntent().getStringExtra(EXTRA_PREFS);
        if (file == null) file = "numbers";
        prefs = getSharedPreferences(file, MODE_PRIVATE);
        try {
            book = new Lessons(readAsset("letters/lessons.json"));
            JSONObject o = new JSONObject(readAsset("math/sounds.json"));
            for (Iterator<String> it = o.keys(); it.hasNext(); ) {
                String k = it.next();
                voice.put(k, o.getString(k));
            }
        } catch (Exception e) {
            finish();
            return;
        }
        int last = STOPS.length - 1;
        stage = Math.max(0, Math.min(getIntent().getIntExtra(EXTRA_STAGE, Math.min(prefs.getInt("stage", 0), last)), last));
        stop = STOPS[stage];

        // The child already told Lili in the letters game (same player): ask only if not.
        String forced = getIntent().getStringExtra("gender");
        if (forced != null) prefs.edit().putString("gender", forced).commit();
        String g = prefs.getString("gender", "");
        if (g.isEmpty() && file.endsWith("_numbers")) {
            g = getSharedPreferences(file.substring(0, file.length() - "_numbers".length()) + "_letters", MODE_PRIVATE)
                    .getString("gender", "");
            if (!g.isEmpty()) prefs.edit().putString("gender", g).apply();
        }
        girl = "g".equals(g);

        if (g.isEmpty()) steps.add(ASK);
        switch (stop.type) {
            case T_NUM:
                steps.add(INTRO);
                steps.add(TRACE);
                steps.add(COUNT);
                steps.add(FIND);
                break;
            case T_PATTERN:
                steps.add(PATTERN);
                break;
            case T_COMPARE:
                steps.add(COMPARE);
                break;
            case T_NEIGHBOR:
                steps.add(NEIGHBOR);
                break;
            case T_ORDER:
                steps.add(ORDER);
                break;
            case T_ORDINAL:
                steps.add(ORDINAL);
                break;
            case T_ADD:
                steps.add(ADD);
                break;
            default:
                steps.add(SUB);
        }
        steps.add(DONE);

        Typeface face = Typeface.createFromAsset(getAssets(), "fonts/fredoka_semibold.ttf");
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setFitsSystemWindows(true);
        root.setBackgroundColor(Draw.SKY_TOP);
        TopBar bar = new TopBar(this, face, stop.title, -1);
        bar.setOnBack(this::finish);
        root.addView(bar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        view = new NumView(this);
        root.addView(view, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
        startStep(Math.max(0, Math.min(getIntent().getIntExtra("step", 0), steps.size() - 1)));
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopSound();
    }

    private String readAsset(String name) throws IOException {
        try (InputStream in = getAssets().open(name)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString("UTF-8");
        }
    }

    // ---- Sound -------------------------------------------------------------------------------

    /** Lili's recording of a maths word or instruction, as an asset path. */
    private String m(String text) {
        String f = voice.get(text);
        return f == null ? null : "math/" + f;
    }

    /** A phrase recorded for the letters game (praise, «try again», hello…). */
    private String l(String text) {
        String f = book.phrase(text);
        return f == null ? null : "letters/" + f;
    }

    private String g(String boy, String girlForm) {
        return m(girl ? girlForm : boy);
    }

    private static String digits(int n) {
        String s = Integer.toString(n);
        StringBuilder b = new StringBuilder();
        for (char ch : s.toCharArray()) b.append((char) ('٠' + (ch - '0')));
        return b.toString();
    }

    private String name(int n) {
        return n >= 0 && n < NAMES.length ? m(NAMES[n]) : null;
    }

    private void stopSound() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    /** Plays the recordings one after another, then runs {@code after}. */
    private void play(List<String> sounds, Runnable after) {
        playFrom(new ArrayList<>(sounds), 0, after);
    }

    private void play(String sound, Runnable after) {
        List<String> one = new ArrayList<>();
        one.add(sound);
        play(one, after);
    }

    private void playFrom(List<String> sounds, int i, Runnable after) {
        stopSound();
        while (i < sounds.size() && sounds.get(i) == null) i++;
        if (i >= sounds.size()) {
            if (after != null) after.run();
            return;
        }
        final int nextI = i + 1;
        MediaPlayer mp = new MediaPlayer();
        try {
            AssetFileDescriptor fd = getAssets().openFd(sounds.get(i));
            mp.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            fd.close();
            mp.setOnCompletionListener(x -> {
                if (player != x) return;
                x.release();
                player = null;
                playFrom(sounds, nextI, after);
            });
            mp.prepare();
            player = mp;
            mp.start();
        } catch (IOException | RuntimeException e) {
            mp.release();
            playFrom(sounds, nextI, after);
        }
    }

    private static final String[] BOY_ONLY = {"أَحْسَنْتَ!", "حَاوِلْ مَرَّةً أُخْرَى", "أَحْسَنْتَ يَا بَطَل!"};
    private static final String[] GIRL_ONLY = {"أَحْسَنْتِ!", "حَاوِلِي مَرَّةً أُخْرَى", "أَحْسَنْتِ يَا بَطَلَة!"};

    private String cheer(List<Lessons.Item> list) {
        List<Lessons.Item> fit = new ArrayList<>();
        for (Lessons.Item it : list) {
            boolean other = false;
            for (String s : girl ? BOY_ONLY : GIRL_ONLY) if (s.equals(it.text)) other = true;
            if (!other) fit.add(it);
        }
        if (fit.isEmpty()) fit = list;
        return fit.isEmpty() ? null : "letters/" + fit.get(rnd.nextInt(fit.size())).sound;
    }

    private void ask() {
        play(question, null);
    }

    // ---- Flow --------------------------------------------------------------------------------

    private int kind() {
        return steps.get(step);
    }

    private void startStep(int s) {
        step = s;
        round = 0;
        busy = false;
        rounds = ROUNDS;
        List<String> lead = new ArrayList<>();
        switch (kind()) {
            case ASK:
                play(l("مَرْحَبًا، أَنَا لِيلِي!"), () -> play(l("مَنْ يَلْعَبُ مَعَ لِيلِي؟"), null));
                break;
            case INTRO:
                question.clear();
                for (int n : stop.nums) question.add(name(n));
                lead.add(m("هَيَّا نَتَعَلَّمُ عَدَدًا جَدِيدًا"));
                lead.addAll(question);
                play(lead, null);
                break;
            case TRACE:
                traceIdx = 0;
                view.clearTrace();
                question.clear();
                question.add(name(stop.nums[0]));
                lead.add(g("اُكْتُبِ الْعَدَد", "اُكْتُبِي الْعَدَد"));
                lead.addAll(question);
                play(lead, null);
                break;
            case ORDER:
                rounds = 4;
                newOrder(true);
                break;
            case DONE:
                int reached = prefs.getInt("stage", 0);
                if (stage + 1 > reached) prefs.edit().putInt("stage", stage + 1).apply();
                play(cheer(book.praise), null);
                break;
            default:
                newQuestion(true);
        }
        view.invalidate();
    }

    private void nextStep() {
        if (step + 1 < steps.size()) startStep(step + 1);
        else finish();
    }

    private void chooseGender(boolean g) {
        if (kind() != ASK || busy) return;
        busy = true;
        girl = g;
        prefs.edit().putString("gender", g ? "g" : "b").apply();
        play(l(g ? "بِنْت" : "وَلَد"), this::nextStep);
    }

    /** A number to ask about: mostly this stop's own numbers, sometimes an earlier one. */
    private int pickNumber() {
        int lo = stop.nums.length > 0 && stop.nums[0] == 0 ? 0 : 1;
        if (stop.nums.length > 0 && rnd.nextInt(3) > 0) return stop.nums[rnd.nextInt(stop.nums.length)];
        return lo + rnd.nextInt(stop.max - lo + 1);
    }

    /** Three different number cards, one of them {@code right}, all within 0..20. */
    private void numberOptions(int right, int lo, int hi) {
        options.clear();
        List<Integer> pool = new ArrayList<>();
        for (int d = -3; d <= 3; d++) {
            int v = right + d;
            if (d != 0 && v >= lo && v <= hi) pool.add(v);
        }
        Collections.shuffle(pool, rnd);
        List<Integer> pick = new ArrayList<>();
        pick.add(right);
        for (int v : pool) if (pick.size() < 3) pick.add(v);
        Collections.shuffle(pick, rnd);
        for (int v : pick) options.add(digits(v));
        answer = pick.indexOf(right);
    }

    private void newQuestion(boolean first) {
        busy = false;
        wrongPick = -1;
        rightPick = -1;
        question.clear();
        List<String> lead = new ArrayList<>();
        switch (kind()) {
            case COUNT:
                qa = pickNumber();
                numberOptions(qa, 0, Math.max(stop.max, 3));
                if (first) lead.add(g("عُدَّ وَاخْتَرْ", "عُدِّي وَاخْتَارِي"));
                question.add(m("كَمْ عَدَدُهَا؟"));
                break;
            case FIND:
                qa = pickNumber();
                numberOptions(qa, 0, Math.max(stop.max, 3));
                if (first) lead.add(m("أَيْنَ الْعَدَد؟"));
                question.add(name(qa));
                break;
            case PATTERN:
                newPattern();
                if (first) lead.add(g("أَكْمِلِ النَّمَط", "أَكْمِلِي النَّمَط"));
                question.add(m("مَا الشَّكْلُ التَّالِي؟"));
                break;
            case COMPARE:
                qa = rnd.nextInt(stop.max + 1);
                do qb = rnd.nextInt(stop.max + 1); while (qb == qa);
                qc = rnd.nextInt(2); // 0: which is bigger, 1: which is smaller
                options.clear();
                options.add(digits(qa));
                options.add(digits(qb));
                answer = (qa > qb) == (qc == 0) ? 0 : 1;
                question.add(m(qc == 0 ? "أَيُّهُمَا أَكْبَر؟" : "أَيُّهُمَا أَصْغَر؟"));
                break;
            case NEIGHBOR:
                qa = rnd.nextInt(stop.max - 1); // the line shows qa, qa+1, qa+2
                qb = rnd.nextInt(3); // the blank: 0 before, 1 between, 2 after
                numberOptions(qa + qb, 0, stop.max);
                question.add(m(qb == 0 ? "مَا الْعَدَدُ الَّذِي قَبْلَهُ؟" : qb == 1 ? "مَا الْعَدَدُ الَّذِي بَيْنَهُمَا؟"
                        : "مَا الْعَدَدُ الَّذِي بَعْدَهُ؟"));
                break;
            case ORDINAL:
                qa = rnd.nextInt(10);
                options.clear();
                answer = qa;
                question.add(m("مَنِ الَّذِي فِي الْمَكَانِ"));
                question.add(m(ORDINALS[qa]));
                break;
            case ADD:
                qa = 1 + rnd.nextInt(stop.max - 1);
                qb = 1 + rnd.nextInt(stop.max - qa);
                numberOptions(qa + qb, 0, stop.max);
                if (first) lead.add(m("هَيَّا نَجْمَعُ"));
                question.add(name(qa));
                question.add(m("زَائِد"));
                question.add(name(qb));
                question.add(m("كَمِ الْمَجْمُوعُ؟"));
                break;
            case SUB:
                qa = 2 + rnd.nextInt(stop.max - 1);
                qb = 1 + rnd.nextInt(qa - 1);
                numberOptions(qa - qb, 0, stop.max);
                if (first) lead.add(g("اِطْرَحْ", "اِطْرَحِي"));
                question.add(name(qa));
                question.add(m("نَاقِص"));
                question.add(name(qb));
                question.add(m("كَمْ يَبْقَى؟"));
                break;
        }
        lead.addAll(question);
        view.invalidate();
        play(lead, null);
    }

    /** Shapes repeating as AB, AAB, ABB or ABC, shown seven long and the eighth to choose. */
    private void newPattern() {
        int[][] units = {{0, 1}, {0, 0, 1}, {0, 1, 1}, {0, 1, 2}};
        int[] u = units[Math.min(units.length - 1, round < 2 ? rnd.nextInt(2) : rnd.nextInt(units.length))];
        List<Integer> kinds = new ArrayList<>();
        for (int s = 0; s < 4; s++) kinds.add(s);
        Collections.shuffle(kinds, rnd);
        int[] colours = {rnd.nextInt(4), rnd.nextInt(4), rnd.nextInt(4)};
        pattern.clear();
        for (int i = 0; i < 8; i++) {
            int k = u[i % u.length];
            pattern.add(kinds.get(k) * 10 + (colours[k] + k) % 4);
        }
        int right = pattern.get(7);
        shapeOptions.clear();
        shapeOptions.add(right);
        for (int s : kinds) {
            int v = s * 10 + rnd.nextInt(4);
            if (shapeOptions.size() < 3 && s != right / 10) shapeOptions.add(v);
        }
        Collections.shuffle(shapeOptions, rnd);
        options.clear();
        for (int i = 0; i < shapeOptions.size(); i++) options.add("");
        answer = shapeOptions.indexOf(right);
    }

    private void pick(int i) {
        if (busy || i < 0) return;
        int k = kind();
        if (k != ORDINAL && i >= options.size()) return;
        if (i == answer) {
            busy = true;
            rightPick = i;
            view.invalidate();
            List<String> after = new ArrayList<>();
            if (k == COUNT || k == FIND) after.add(name(qa));
            else if (k == NEIGHBOR) after.add(name(qa + qb));
            else if (k == ADD || k == SUB) {
                after.add(name(qa));
                after.add(m(k == ADD ? "زَائِد" : "نَاقِص"));
                after.add(name(qb));
                after.add(m("يُسَاوِي"));
                after.add(name(k == ADD ? qa + qb : qa - qb));
            } else if (k == COMPARE) after.add(name(i == 0 ? qa : qb));
            else if (k == ORDINAL) after.add(m(ORDINALS[qa]));
            after.add(cheer(book.praise));
            play(after, () -> {
                round++;
                if (round < rounds) newQuestion(false);
                else nextStep();
            });
        } else {
            wrongPick = i;
            wobbleAt = SystemClock.uptimeMillis();
            view.invalidate();
            play(cheer(book.tryAgain), this::ask);
        }
    }

    /** Four (or five) different numbers to put from the smallest to the biggest. */
    private void newOrder(boolean first) {
        busy = false;
        wrongPick = -1;
        int n = stop.max > 9 ? 5 : 4;
        List<Integer> all = new ArrayList<>();
        int lo = stop.max > 9 && rnd.nextBoolean() ? 6 : 0;
        for (int v = lo; v <= stop.max; v++) all.add(v);
        Collections.shuffle(all, rnd);
        order.clear();
        order.addAll(all.subList(0, n));
        Collections.sort(order);
        cards.clear();
        cards.addAll(order);
        for (int t = 0; t < 8 && cards.equals(order); t++) Collections.shuffle(cards, rnd);
        used.clear();
        for (int i = 0; i < n; i++) used.add(false);
        placed = 0;
        question.clear();
        question.add(g("رَتِّبِ الْأَعْدَادَ مِنَ الْأَصْغَرِ إِلَى الْأَكْبَر", "رَتِّبِي الْأَعْدَادَ مِنَ الْأَصْغَرِ إِلَى الْأَكْبَر"));
        view.invalidate();
        if (first) ask();
    }

    private void place(int i) {
        if (busy || i < 0 || i >= cards.size() || used.get(i)) return;
        if (cards.get(i).equals(order.get(placed))) {
            used.set(i, true);
            placed++;
            wrongPick = -1;
            view.invalidate();
            if (placed == order.size()) {
                busy = true;
                List<String> after = new ArrayList<>();
                for (int v : order) after.add(name(v));
                after.add(cheer(book.praise));
                play(after, () -> {
                    round++;
                    if (round < rounds) newOrder(false);
                    else nextStep();
                });
            } else {
                play(name(cards.get(i)), null);
            }
        } else {
            wrongPick = i;
            wobbleAt = SystemClock.uptimeMillis();
            view.invalidate();
            play(cheer(book.tryAgain), null);
        }
    }

    private void traced() {
        List<String> after = new ArrayList<>();
        after.add(name(stop.nums[traceIdx]));
        after.add(cheer(book.praise));
        busy = true;
        play(after, () -> {
            busy = false;
            if (traceIdx + 1 < stop.nums.length) {
                traceIdx++;
                view.clearTrace();
                question.clear();
                question.add(name(stop.nums[traceIdx]));
                ask();
            } else {
                nextStep();
            }
        });
    }

    // ---- Screen ------------------------------------------------------------------------------

    private static final int[] COLOURS = {0xFFFF6B6B, Draw.BLUE_D, Draw.MINT_D, Draw.LILAC_D};

    private class NumView extends View {
        private final float dp;
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Bitmap lili;
        private final List<float[]> boxes = new ArrayList<>();
        private int down = -1;
        private static final int B_SPEAK = -10, B_NEXT = -11, B_CLEAR = -12, B_BOY = -13, B_GIRL = -14;
        // Tracing a number, as the letters game traces a letter.
        private final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path finger = new Path();
        private final List<float[]> touches = new ArrayList<>();
        private float[] traceBox;
        private String traceFor = "";
        private float[] samples = new float[0];
        private int sampleCount;
        private boolean done, tracing;

        NumView(Context ctx) {
            super(ctx);
            dp = getResources().getDisplayMetrics().density;
            text.setTypeface(Typeface.DEFAULT_BOLD);
            text.setTextAlign(Paint.Align.CENTER);
            lili = Draw.load(getResources(), R.drawable.home_happy);
        }

        void clearTrace() {
            finger.reset();
            touches.clear();
            done = false;
            invalidate();
        }

        private String traceText() {
            return digits(stop.nums[traceIdx]);
        }

        private void inkPaint(String s, float bw, float bh) {
            ink.setTypeface(Typeface.DEFAULT_BOLD);
            ink.setTextAlign(Paint.Align.CENTER);
            ink.setStyle(Paint.Style.FILL);
            ink.setTextSize(Math.min(bw, bh) * 0.8f);
            float tw = ink.measureText(s);
            if (tw > bw * 0.9f && tw > 0) ink.setTextSize(ink.getTextSize() * bw * 0.9f / tw);
        }

        private void buildSamples(String s, float l, float t, float r, float b) {
            int bw = Math.max(1, Math.round(r - l)), bh = Math.max(1, Math.round(b - t));
            Bitmap mask = Bitmap.createBitmap(bw, bh, Bitmap.Config.ALPHA_8);
            Canvas mc = new Canvas(mask);
            inkPaint(s, bw, bh);
            mc.drawText(s, bw / 2f, bh / 2f - (ink.ascent() + ink.descent()) / 2f, ink);
            int stepPx = Math.max(3, Math.round(Math.min(bw, bh) / 70f));
            float[] pts = new float[(bw / stepPx + 1) * (bh / stepPx + 1) * 2];
            int n = 0;
            for (int y = 0; y < bh; y += stepPx) {
                for (int x = 0; x < bw; x += stepPx) {
                    if (Color.alpha(mask.getPixel(x, y)) > 128) {
                        pts[n++] = l + x;
                        pts[n++] = t + y;
                    }
                }
            }
            mask.recycle();
            samples = pts;
            sampleCount = n / 2;
        }

        private float coverage() {
            if (sampleCount == 0) return 0;
            float rad = 22 * dp, r2 = rad * rad;
            int hitCount = 0;
            for (int i = 0; i < sampleCount; i++) {
                float sx = samples[2 * i], sy = samples[2 * i + 1];
                for (float[] p : touches) {
                    float dx = p[0] - sx, dy = p[1] - sy;
                    if (dx * dx + dy * dy <= r2) {
                        hitCount++;
                        break;
                    }
                }
            }
            return hitCount / (float) sampleCount;
        }

        private boolean traceTouch(MotionEvent e) {
            float x = e.getX(), y = e.getY();
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (traceBox == null || done || busy || x < traceBox[0] || x > traceBox[2] || y < traceBox[1] || y > traceBox[3]) return false;
                    tracing = true;
                    finger.moveTo(x, y);
                    touches.add(new float[]{x, y});
                    invalidate();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (!tracing) return false;
                    finger.lineTo(x, y);
                    touches.add(new float[]{x, y});
                    invalidate();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (!tracing) return false;
                    tracing = false;
                    if (coverage() >= 0.72f) {
                        done = true;
                        traced();
                    }
                    invalidate();
                    return true;
            }
            return false;
        }

        private int hit(float x, float y) {
            for (float[] b : boxes) if (x >= b[0] && x <= b[2] && y >= b[1] && y <= b[3]) return (int) b[4];
            return Integer.MIN_VALUE;
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            if (kind() == TRACE && (tracing || e.getActionMasked() == MotionEvent.ACTION_DOWN) && traceTouch(e)) return true;
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    down = hit(e.getX(), e.getY());
                    invalidate();
                    return true;
                case MotionEvent.ACTION_UP:
                    int w = down;
                    down = -1;
                    invalidate();
                    if (w != Integer.MIN_VALUE && hit(e.getX(), e.getY()) == w) act(w);
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    down = -1;
                    invalidate();
                    return true;
            }
            return true;
        }

        private void act(int w) {
            int k = kind();
            if (w == B_BOY || w == B_GIRL) chooseGender(w == B_GIRL);
            else if (w == B_CLEAR) clearTrace();
            else if (w == B_SPEAK) ask();
            else if (w == B_NEXT) {
                if (k == TRACE && !done && traceIdx + 1 < stop.nums.length) {
                    traceIdx++;
                    clearTrace();
                    question.clear();
                    question.add(name(stop.nums[traceIdx]));
                    ask();
                } else if (!busy || k == DONE || k == INTRO) {
                    nextStep();
                }
            } else if (w >= 0) {
                if (k == ORDER) place(w);
                else pick(w);
            }
        }

        private void box(float l, float t, float r, float b, int what) {
            boxes.add(new float[]{l, t, r, b, what});
        }

        private void label(Canvas c, String s, float cx, float cy, float size, float maxW, int color) {
            text.setTextSize(size);
            float tw = text.measureText(s);
            if (tw > maxW && tw > 0) text.setTextSize(size * maxW / tw);
            text.setColor(color);
            c.drawText(s, cx, cy - (text.ascent() + text.descent()) / 2f, text);
        }

        private void button(Canvas c, float l, float t, float r, float b, int fill, int deep, int what) {
            float s = down == what ? 0.94f : 1f;
            c.save();
            c.scale(s, s, (l + r) / 2, (t + b) / 2);
            Draw.card(c, l, t, r, b - 6 * dp, 20 * dp, fill, deep, 6 * dp);
            c.restore();
            box(l, t, r, b, what);
        }

        private void speaker(Canvas c, float cx, float cy, float r) {
            button(c, cx - r, cy - r, cx + r, cy + r, Draw.YELLOW, Draw.YELLOW_D, B_SPEAK);
            float u = r / 6f, y = cy - 3 * dp;
            c.drawRect(cx - 3.2f * u, y - 1.3f * u, cx - 1.6f * u, y + 1.3f * u, Draw.fill(Draw.PLUM));
            Path p = new Path();
            p.moveTo(cx - 1.8f * u, y - 1.3f * u);
            p.lineTo(cx + 0.6f * u, y - 3.0f * u);
            p.lineTo(cx + 0.6f * u, y + 3.0f * u);
            p.lineTo(cx - 1.8f * u, y + 1.3f * u);
            p.close();
            c.drawPath(p, Draw.fill(Draw.PLUM));
            c.drawArc(new RectF(cx + 0.2f * u, y - 2.2f * u, cx + 2.6f * u, y + 2.2f * u), -50, 100, false,
                    Draw.stroke(Draw.PLUM, 0.6f * u));
        }

        private void next(Canvas c, float cx, float cy, float w, float h, String s) {
            button(c, cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2, Draw.MINT, Draw.MINT_D, B_NEXT);
            label(c, s, cx, cy - 3 * dp, h * 0.42f, w * 0.8f, 0xFFFFFFFF);
        }

        /** A shape of the pattern games: circle, square, triangle or star, in one of four colours. */
        private void shape(Canvas c, int code, float cx, float cy, float r) {
            int colour = COLOURS[code % 10 % COLOURS.length];
            switch (code / 10) {
                case 0:
                    c.drawCircle(cx, cy, r, Draw.fill(colour));
                    break;
                case 1:
                    c.drawRoundRect(new RectF(cx - r * 0.9f, cy - r * 0.9f, cx + r * 0.9f, cy + r * 0.9f), r * 0.2f, r * 0.2f, Draw.fill(colour));
                    break;
                case 2:
                    Path p = new Path();
                    p.moveTo(cx, cy - r);
                    p.lineTo(cx + r, cy + r * 0.8f);
                    p.lineTo(cx - r, cy + r * 0.8f);
                    p.close();
                    c.drawPath(p, Draw.fill(colour));
                    break;
                default:
                    Draw.star(c, cx, cy, r * 1.1f, colour);
            }
        }

        /** {@code n} round things to count, in rows of five like the book's ten-frames. */
        private void things(Canvas c, int n, float l, float t, float r, float b, int colour, int crossed) {
            things(c, n, l, t, r, b, colour, crossed, n);
        }

        /** As above, but sized as if there were {@code sizeFor} things, so two groups side by side look alike. */
        private void things(Canvas c, int n, float l, float t, float r, float b, int colour, int crossed, int sizeFor) {
            if (n <= 0) {
                // Zero: an empty plate.
                float cx = (l + r) / 2, cy = (t + b) / 2, pr = Math.min(r - l, b - t) * 0.35f;
                c.drawOval(new RectF(cx - pr, cy - pr * 0.45f, cx + pr, cy + pr * 0.45f), Draw.fill(0xFFE6DCCB));
                c.drawOval(new RectF(cx - pr * 0.7f, cy - pr * 0.3f, cx + pr * 0.7f, cy + pr * 0.3f), Draw.fill(0xFFF4EEE4));
                return;
            }
            int cols = Math.min(5, n), rows = (n + 4) / 5;
            int sc = Math.max(n, sizeFor);
            float cell = Math.min((r - l) / Math.min(5, sc), (b - t) / ((sc + 4) / 5));
            cell = Math.min(cell, 90 * dp);
            float rad = cell * 0.36f;
            float x0 = (l + r) / 2 + cols * cell / 2, y0 = (t + b) / 2 - rows * cell / 2;
            for (int i = 0; i < n; i++) {
                int row = i / 5, col = i % 5;
                // Right to left, as the book counts.
                float cx = x0 - (col + 0.5f) * cell, cy = y0 + (row + 0.5f) * cell;
                c.drawCircle(cx, cy + rad * 0.12f, rad, Draw.fill(0x22000000));
                c.drawCircle(cx, cy, rad, Draw.fill(colour));
                c.drawCircle(cx - rad * 0.35f, cy - rad * 0.35f, rad * 0.22f, Draw.fill(0x66FFFFFF));
                c.drawLine(cx, cy - rad, cx + rad * 0.25f, cy - rad * 1.35f, Draw.stroke(0xFF7A4B2A, rad * 0.16f));
                if (i >= n - crossed) {
                    Paint x = Draw.stroke(0xFF5B3E8C, rad * 0.22f);
                    c.drawLine(cx - rad, cy - rad, cx + rad, cy + rad, x);
                    c.drawLine(cx + rad, cy - rad, cx - rad, cy + rad, x);
                }
            }
        }

        private float wobble() {
            long since = SystemClock.uptimeMillis() - wobbleAt;
            if (wrongPick < 0 || since >= 450) return 0;
            postInvalidateOnAnimation();
            return (float) Math.sin(since / 30.0) * 10 * dp * (1 - since / 450f);
        }

        /** The answer cards in one row along the bottom. */
        private void optionRow(Canvas c, float w, float top, float bottom, float pad) {
            int n = options.size();
            float cw = Math.min(170 * dp, (w - pad * (n + 1)) / n);
            float ch = Math.min(bottom - top, Math.max(90 * dp, cw * 0.8f));
            float x0 = (w - (n * cw + (n - 1) * pad)) / 2;
            float wob = wobble();
            for (int i = 0; i < n; i++) {
                int col = n - 1 - i; // right to left
                float l = x0 + col * (cw + pad), r = l + cw;
                int fill = i == rightPick ? Draw.MINT : i == wrongPick ? 0xFFFFB3B3 : Draw.LILAC;
                int deep = i == rightPick ? Draw.MINT_D : i == wrongPick ? 0xFFE07A7A : Draw.LILAC_D;
                c.save();
                if (i == wrongPick) c.translate(wob, 0);
                button(c, l, top, r, top + ch, fill, deep, i);
                if (kind() == PATTERN) {
                    float rr = Math.min(cw, ch) * 0.28f;
                    c.drawCircle((l + r) / 2, top + ch / 2 - 3 * dp, rr * 1.45f, Draw.fill(0xFFFFFFFF));
                    shape(c, shapeOptions.get(i), (l + r) / 2, top + ch / 2 - 3 * dp, rr);
                } else {
                    label(c, options.get(i), (l + r) / 2, top + ch / 2 - 3 * dp, ch * 0.5f, cw * 0.8f, 0xFFFFFFFF);
                }
                c.restore();
            }
        }

        @Override
        protected void onDraw(Canvas c) {
            boxes.clear();
            float w = getWidth(), h = getHeight(), pad = 18 * dp;
            c.drawColor(Draw.SKY_TOP);
            int k = kind();
            boolean side = w > h * 1.2f;
            float btnH = Math.max(64 * dp, Math.min(84 * dp, h * 0.11f));
            float sr = side ? 30 * dp : Math.max(36 * dp, Math.min(50 * dp, h * 0.065f));
            if (k == ASK) {
                label(c, "مَنْ يَلْعَبُ مَعَ لِيلِي؟", w / 2, pad + 30 * dp, 30 * dp, w - 2 * pad, Draw.PLUM);
                float cw = side ? Math.min((w - 3 * pad) / 2, h * 0.6f) : Math.min(w - 2 * pad, (h - 100 * dp) / 2 - pad);
                float ch = cw * 0.75f, t0 = pad + 70 * dp;
                float[][] at = side
                        ? new float[][]{{w / 2 + pad / 2, t0}, {w / 2 - pad / 2 - cw, t0}}
                        : new float[][]{{(w - cw) / 2, t0}, {(w - cw) / 2, t0 + ch + pad}};
                button(c, at[0][0], at[0][1], at[0][0] + cw, at[0][1] + ch, Draw.BLUE, Draw.BLUE_D, B_BOY);
                label(c, "وَلَد", at[0][0] + cw / 2, at[0][1] + ch / 2 - 3 * dp, ch * 0.32f, cw * 0.8f, 0xFFFFFFFF);
                button(c, at[1][0], at[1][1], at[1][0] + cw, at[1][1] + ch, Draw.PINK, Draw.PINK_D, B_GIRL);
                label(c, "بِنْت", at[1][0] + cw / 2, at[1][1] + ch / 2 - 3 * dp, ch * 0.32f, cw * 0.8f, 0xFFFFFFFF);
            } else if (k == INTRO) {
                // The new numbers, each with as many things to count beside it.
                int n = stop.nums.length;
                float row = h - pad - btnH / 2;
                float areaB = row - btnH / 2 - pad;
                float cellW = (w - pad * (n + 1)) / n;
                for (int i = 0; i < n; i++) {
                    int v = stop.nums[i];
                    float l = w - pad - (i + 1) * cellW - i * pad, r = l + cellW;
                    float cardB = pad + Math.min(cellW, (areaB - pad) * 0.5f);
                    Draw.card(c, l, pad, r, cardB - 8 * dp, 28 * dp, Draw.YELLOW, Draw.YELLOW_D, 8 * dp);
                    label(c, digits(v), (l + r) / 2, (pad + cardB) / 2 - 4 * dp, (cardB - pad) * 0.6f, cellW * 0.8f, 0xFFFFFFFF);
                    things(c, v, l, cardB + pad, r, areaB, COLOURS[i % COLOURS.length], 0);
                }
                speaker(c, w / 2 + btnH * 0.9f, row, btnH / 2);
                next(c, w / 2 - btnH * 1.1f, row, btnH * 2.2f, btnH, "هَيَّا");
            } else if (k == TRACE) {
                btnH = Math.min(btnH, (w - 2 * pad) / 4.9f);
                float bottomRow = h - pad - btnH / 2;
                float l = pad, t = pad, r = w - pad, b = h - 2 * pad - btnH;
                String s = traceText();
                if (traceBox == null || traceBox[2] != r || traceBox[3] != b || !traceFor.equals(s)) {
                    traceBox = new float[]{l, t, r, b};
                    traceFor = s;
                    buildSamples(s, l, t, r, b);
                }
                Draw.card(c, l, t, r, b - 8 * dp, 32 * dp, 0xFFFFFFFF, 0xFFE6DCCB, 8 * dp);
                inkPaint(s, r - l, b - t);
                ink.setColor(done ? Draw.MINT : 0xFFFFF0C8);
                c.drawText(s, (l + r) / 2f, (t + b) / 2f - (ink.ascent() + ink.descent()) / 2f, ink);
                c.drawPath(finger, Draw.stroke(done ? Draw.MINT_D : Draw.YELLOW_D, 18 * dp));
                if (!done) {
                    float cx = w / 2 - 1.9f * btnH;
                    button(c, cx - 0.65f * btnH, bottomRow - btnH / 2, cx + 0.65f * btnH, bottomRow + btnH / 2,
                            Draw.BLUE, Draw.BLUE_D, B_CLEAR);
                    label(c, "اِمْسَحْ", cx, bottomRow - 3 * dp, btnH * 0.32f, btnH * 1.1f, 0xFFFFFFFF);
                }
                speaker(c, w / 2 + 1.7f * btnH, bottomRow, btnH / 2);
                next(c, w / 2, bottomRow, btnH * 1.9f, btnH, "التَّالِي");
            } else if (k == ORDER) {
                label(c, (round + 1) + " / " + rounds, w / 2, pad + 12 * dp, 18 * dp, w, 0xFF9AA0BC);
                float spT = pad + 30 * dp;
                if (side) speaker(c, pad + sr, pad + sr, sr);
                else speaker(c, w / 2, spT + sr, sr);
                float top = side ? pad + 30 * dp : spT + 2 * sr + pad;
                label(c, "الْأَصْغَر", w - pad - 40 * dp, top + 10 * dp, 18 * dp, 90 * dp, Draw.PLUM);
                label(c, "الْأَكْبَر", pad + 40 * dp, top + 10 * dp, 18 * dp, 90 * dp, Draw.PLUM);
                top += 30 * dp;
                int n = cards.size();
                float cw = Math.min(120 * dp, (w - pad * (n + 1)) / n);
                float ch = Math.min(cw, (h - top - 3 * pad) / 2);
                float x0 = (w - (n * cw + (n - 1) * pad)) / 2;
                for (int s = 0; s < n; s++) {
                    float l = x0 + (n - 1 - s) * (cw + pad), r = l + cw;
                    if (s < placed) {
                        Draw.card(c, l, top, r, top + ch - 6 * dp, 20 * dp, Draw.MINT, Draw.MINT_D, 6 * dp);
                        label(c, digits(order.get(s)), (l + r) / 2, top + ch / 2 - 3 * dp, ch * 0.5f, cw * 0.8f, 0xFFFFFFFF);
                    } else {
                        c.drawRoundRect(new RectF(l, top, r, top + ch - 6 * dp), 20 * dp, 20 * dp, Draw.stroke(0xFFC9CDE0, 3 * dp));
                    }
                }
                float cardT = top + ch + 2 * pad;
                float wob = wobble();
                for (int i = 0; i < n; i++) {
                    if (used.get(i)) continue;
                    float l = x0 + (n - 1 - i) * (cw + pad), r = l + cw;
                    c.save();
                    if (i == wrongPick) c.translate(wob, 0);
                    button(c, l, cardT, r, cardT + ch, i == wrongPick ? 0xFFFFB3B3 : Draw.LILAC,
                            i == wrongPick ? 0xFFE07A7A : Draw.LILAC_D, i);
                    label(c, digits(cards.get(i)), (l + r) / 2, cardT + ch / 2 - 3 * dp, ch * 0.5f, cw * 0.8f, 0xFFFFFFFF);
                    c.restore();
                }
            } else if (k == DONE) {
                float ph = Math.min(h * 0.45f, w * 0.6f);
                Draw.picture(c, lili, w / 2 - ph / 2, pad, w / 2 + ph / 2, pad + ph);
                float y = pad + ph + pad;
                for (int i = 0; i < 3; i++) Draw.star(c, w / 2 + (i - 1) * 54 * dp, y + 26 * dp, 22 * dp, Draw.YELLOW_D);
                label(c, girl ? "أَحْسَنْتِ!" : "أَحْسَنْتَ!", w / 2, y + 90 * dp, 40 * dp, w - 2 * pad, Draw.PLUM);
                next(c, w / 2, y + 150 * dp + btnH / 2, btnH * 2.4f, btnH, "تَمّ");
            } else {
                // A question: the counter, the speaker, the picture, and the answers below.
                label(c, (round + 1) + " / " + rounds, w / 2, pad + 12 * dp, 18 * dp, w, 0xFF9AA0BC);
                float spT = pad + 30 * dp;
                if (side) speaker(c, pad + sr, pad + sr, sr);
                else speaker(c, w / 2, spT + sr, sr);
                float sceneT = side ? pad + 30 * dp : spT + 2 * sr + pad;
                float optH = k == ORDINAL ? 0 : Math.min(130 * dp, Math.max(90 * dp, h * 0.16f));
                float sceneB = h - pad - optH - (k == ORDINAL ? 0 : pad);
                scene(c, k, pad, sceneT, w - pad, sceneB);
                if (k != ORDINAL) optionRow(c, w, sceneB + pad, h - pad, pad);
            }
        }

        private void scene(Canvas c, int k, float l, float t, float r, float b) {
            float cx = (l + r) / 2;
            switch (k) {
                case COUNT:
                    Draw.card(c, l, t, r, b - 8 * dp, 28 * dp, 0xFFFFFFFF, 0xFFE6DCCB, 8 * dp);
                    things(c, qa, l + 18 * dp, t + 18 * dp, r - 18 * dp, b - 26 * dp, COLOURS[qa % COLOURS.length], 0);
                    break;
                case FIND: {
                    float s = Math.min(r - l, b - t) * 0.8f;
                    Draw.card(c, cx - s / 2, t, cx + s / 2, t + s - 8 * dp, 36 * dp, Draw.YELLOW, Draw.YELLOW_D, 8 * dp);
                    label(c, "؟", cx, t + s / 2 - 4 * dp, s * 0.6f, s * 0.8f, 0xFFFFFFFF);
                    break;
                }
                case PATTERN: {
                    Draw.card(c, l, t, r, b - 8 * dp, 28 * dp, 0xFFFFFFFF, 0xFFE6DCCB, 8 * dp);
                    int n = pattern.size();
                    boolean two = (r - l) / n < 64 * dp;
                    int per = two ? 4 : n;
                    float cell = Math.min((r - l - 24 * dp) / per, (b - t - 30 * dp) / (two ? 2 : 1));
                    float rr = cell * 0.32f;
                    for (int i = 0; i < n; i++) {
                        int row = i / per, col = i % per;
                        float x = cx + per * cell / 2 - (col + 0.5f) * cell;
                        float y = (t + b) / 2 - 4 * dp + (two ? (row - 0.5f) * cell : 0);
                        if (i == n - 1 && rightPick < 0) {
                            c.drawCircle(x, y, rr * 1.2f, Draw.stroke(0xFFC9CDE0, 3 * dp));
                            label(c, "؟", x, y, rr * 1.4f, rr * 2, 0xFFC9CDE0);
                        } else {
                            shape(c, pattern.get(i), x, y, rr);
                        }
                    }
                    break;
                }
                case COMPARE: {
                    // Two baskets: the numbers and as many things.
                    float gap = 18 * dp, half = (r - l - gap) / 2;
                    for (int i = 0; i < 2; i++) {
                        int v = i == 0 ? qa : qb;
                        float bl = i == 0 ? r - half : l, br = bl + half;
                        Draw.card(c, bl, t, br, b - 8 * dp, 28 * dp, 0xFFFFFFFF, 0xFFE6DCCB, 8 * dp);
                        things(c, v, bl + 12 * dp, t + 44 * dp, br - 12 * dp, b - 20 * dp, COLOURS[i * 2], 0, Math.max(qa, qb));
                    }
                    label(c, qc == 0 ? "أَيُّهُمَا أَكْبَر؟" : "أَيُّهُمَا أَصْغَر؟", cx, t + 24 * dp, 24 * dp, r - l - 24 * dp, Draw.PLUM);
                    break;
                }
                case NEIGHBOR: {
                    // A piece of the number line: three numbers, one missing.
                    float cw = Math.min(130 * dp, (r - l - 4 * 18 * dp) / 3);
                    float y = (t + b) / 2;
                    c.drawLine(l, y + cw * 0.6f, r, y + cw * 0.6f, Draw.stroke(0xFFC9CDE0, 4 * dp));
                    for (int i = 0; i < 3; i++) {
                        float x = cx + (1 - i) * (cw + 18 * dp); // right to left: qa, qa+1, qa+2
                        RectF box = new RectF(x - cw / 2, y - cw / 2, x + cw / 2, y + cw / 2);
                        if (i == qb && rightPick < 0) {
                            c.drawRoundRect(box, 20 * dp, 20 * dp, Draw.stroke(Draw.YELLOW_D, 4 * dp));
                            label(c, "؟", x, y, cw * 0.5f, cw * 0.8f, Draw.YELLOW_D);
                        } else {
                            Draw.card(c, box.left, box.top, box.right, box.bottom, 20 * dp,
                                    i == qb ? Draw.MINT : Draw.YELLOW, i == qb ? Draw.MINT_D : Draw.YELLOW_D, 6 * dp);
                            label(c, digits(qa + i), x, y - 3 * dp, cw * 0.5f, cw * 0.8f, 0xFFFFFFFF);
                        }
                        c.drawCircle(x, y + cw * 0.6f, 5 * dp, Draw.fill(Draw.PLUM));
                    }
                    break;
                }
                case ORDINAL: {
                    // Ten friends in a queue, the first on the right by the flag.
                    float cell = Math.min((r - l - 40 * dp) / 10, (b - t) * 0.3f);
                    float y = (t + b) / 2;
                    float fx = r - 14 * dp;
                    c.drawLine(fx, y - cell * 1.2f, fx, y + cell * 0.8f, Draw.stroke(Draw.PLUM, 3 * dp));
                    Path flag = new Path();
                    flag.moveTo(fx, y - cell * 1.2f);
                    flag.lineTo(fx - cell * 0.7f, y - cell * 0.95f);
                    flag.lineTo(fx, y - cell * 0.7f);
                    flag.close();
                    c.drawPath(flag, Draw.fill(0xFFFF6B6B));
                    float wob = wobble();
                    for (int i = 0; i < 10; i++) {
                        float x = fx - 26 * dp - (i + 0.5f) * cell;
                        float rr = cell * 0.46f;
                        c.save();
                        if (i == wrongPick) c.translate(wob, 0);
                        int body = i == rightPick ? Draw.MINT_D : i == wrongPick ? 0xFFE07A7A : COLOURS[i % COLOURS.length];
                        c.drawRoundRect(new RectF(x - rr, y, x + rr, y + rr * 3.2f), rr * 0.6f, rr * 0.6f, Draw.fill(body));
                        c.drawCircle(x, y - rr * 0.2f, rr * 0.85f, Draw.fill(0xFFFFD9B3));
                        c.drawCircle(x - rr * 0.3f, y - rr * 0.3f, rr * 0.1f, Draw.fill(Draw.PLUM));
                        c.drawCircle(x + rr * 0.3f, y - rr * 0.3f, rr * 0.1f, Draw.fill(Draw.PLUM));
                        c.drawArc(new RectF(x - rr * 0.35f, y - rr * 0.2f, x + rr * 0.35f, y + rr * 0.25f), 20, 140, false,
                                Draw.stroke(Draw.PLUM, rr * 0.08f));
                        c.restore();
                        box(x - cell / 2, y - cell, x + cell / 2, y + cell * 2.2f, i);
                    }
                    break;
                }
                case ADD:
                case SUB: {
                    boolean add = k == ADD;
                    float eqH = Math.min(70 * dp, (b - t) * 0.3f);
                    Draw.card(c, l, t, r, b - eqH - 8 * dp, 28 * dp, 0xFFFFFFFF, 0xFFE6DCCB, 8 * dp);
                    float inT = t + 12 * dp, inB = b - eqH - 20 * dp;
                    if (add) {
                        float mid = (l + r) / 2;
                        things(c, qa, mid + 24 * dp, inT, r - 12 * dp, inB, COLOURS[0], 0, Math.max(qa, qb));
                        label(c, "+", mid, (inT + inB) / 2, 40 * dp, 40 * dp, Draw.PLUM);
                        things(c, qb, l + 12 * dp, inT, mid - 24 * dp, inB, COLOURS[1], 0, Math.max(qa, qb));
                    } else {
                        things(c, qa, l + 12 * dp, inT, r - 12 * dp, inB, COLOURS[2], qb);
                    }
                    String eq = digits(qa) + (add ? " + " : " − ") + digits(qb) + " = " + (rightPick >= 0 ? digits(add ? qa + qb : qa - qb) : "؟");
                    // Written as the book writes it: the first number on the right.
                    label(c, "‫" + eq + "‬", cx, b - eqH / 2, eqH * 0.7f, r - l, Draw.PLUM);
                    break;
                }
            }
        }
    }
}
