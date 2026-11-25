package com.lilifriends.games;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
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
 * Lili's Science: the grade-1 science book («العلوم للصف الأول الابتدائي») stop by stop, in Lili's
 * voice. Pictures are the phone's emoji and simple drawings (a plant, a child), never the book's
 * pages; every round is picked afresh. Lili's recordings are in {@code assets/science}; praise,
 * «try again» and the boy/girl question are the letters' ones.
 */
public class ScienceActivity extends Activity {
    public static final String EXTRA_PREFS = "prefs", EXTRA_STAGE = "stage";

    private static final int T_SORT = 0, T_NEEDS = 1, T_GROW = 2, T_PLANT = 3, T_ANIMAL = 4, T_BODY = 5, T_SENSES = 6,
            T_PICK = 7, T_MORE = 8, T_WORD = 9;

    /** One stop of the map: its title, the label on the map, what it teaches and how. */
    private static final class Stop {
        final String title, label;
        final int type, mode;

        Stop(String title, String label, int type, int mode) {
            this.title = title;
            this.label = label;
            this.type = type;
            this.mode = mode;
        }
    }

    /** The book's order: safety, unit 1 living things, 2 their bodies, 3 where they live, 4 matter, 5 energy and motion, 6 our earth. */
    private static final Stop[] STOPS = {
            new Stop("احتياطات السلامة", "⚠️", T_SORT, 0),
            new Stop("الكائنات الحية وغير الحية", "🐞", T_SORT, 1),
            new Stop("احتياجات الكائنات الحية", "💧", T_NEEDS, 0),
            new Stop("النمو والتكاثر", "🌱", T_GROW, 0),
            new Stop("النبات والحيوان", "🌳", T_SORT, 2),
            new Stop("أجزاء النبات", "🌻", T_PLANT, 0),
            new Stop("أجزاء جسم الحيوان", "🐦", T_ANIMAL, 0),
            new Stop("أجزاء جسم الإنسان", "🧒", T_BODY, 0),
            new Stop("الحواس الخمس", "👂", T_SENSES, 0),
            new Stop("البيئة اليابسة والمائية", "🐟", T_SORT, 3),
            // Unit 4: matter
            new Stop("المادة: صلب ولين", "🧸", T_SORT, 4),
            new Stop("المادة: ثقيل وخفيف", "🪶", T_SORT, 5),
            new Stop("المادة: ناعم وخشن", "🌵", T_SORT, 6),
            new Stop("أيهما أثقل؟", "⚖️", T_MORE, 0),
            new Stop("الطفو والغطس", "⛵", T_SORT, 7),
            // Unit 5: energy and motion
            new Stop("الحرارة: ساخن وبارد", "🔥", T_SORT, 8),
            new Stop("الكهرباء", "💡", T_PICK, 0),
            new Stop("الضوء", "🔦", T_PICK, 1),
            new Stop("الصوت", "🔔", T_PICK, 2),
            new Stop("الموقع", "🐱", T_WORD, 4),
            new Stop("الحركة: أسرع وأبطأ", "🐢", T_MORE, 1),
            new Stop("القوة: دفع وسحب", "🛒", T_SORT, 9),
            // Unit 6: our earth
            new Stop("اليابسة والماء", "🏔️", T_WORD, 0),
            new Stop("الفصول الأربعة", "🍂", T_WORD, 1),
            new Stop("تتابع الفصول", "🌸", T_WORD, 3),
            new Stop("ملابس الفصول", "🧥", T_PICK, 3),
            new Stop("الطقس", "🌧️", T_WORD, 2),
    };

    /** What each stop shows on the journey map. */
    static final String[] STOP_LABELS = new String[STOPS.length];

    static {
        for (int i = 0; i < STOPS.length; i++) STOP_LABELS[i] = STOPS[i].label;
    }

    // Sorting: the question, the two answers, and pictures for each answer.
    private static final String[] SORT_Q = {"هَلْ هُوَ آمِنٌ أَمْ خَطِر؟", "هَلْ هُوَ حَيٌّ أَمْ غَيْرُ حَيّ؟",
            "هَلْ هُوَ نَبَاتٌ أَمْ حَيَوَان؟", "أَيْنَ يَعِيش؟", "هَلْ هُوَ صُلْبٌ أَمْ لَيِّن؟", "هَلْ هُوَ ثَقِيلٌ أَمْ خَفِيف؟",
            "هَلْ هُوَ نَاعِمٌ أَمْ خَشِن؟", "هَلْ يَطْفُو أَمْ يَغْطِس؟", "هَلْ هُوَ سَاخِنٌ أَمْ بَارِد؟", "هَلْ هُوَ دَفْعٌ أَمْ سَحْب؟"};
    private static final String[][] SORT_WORDS = {{"آمِن", "خَطِر"}, {"حَيّ", "غَيْرُ حَيّ"}, {"نَبَات", "حَيَوَان"},
            {"عَلَى الْيَابِسَة", "فِي الْمَاء"}, {"صُلْب", "لَيِّن"}, {"ثَقِيل", "خَفِيف"}, {"نَاعِم", "خَشِن"},
            {"يَطْفُو", "يَغْطِس"}, {"سَاخِن", "بَارِد"}, {"دَفْع", "سَحْب"}};
    private static final String[][][] SORT_PICS = {
            {{"🍎", "📚", "🧸", "🥛", "🪥", "🎈", "🖍️"}, {"🔥", "🔪", "⚡", "💊", "🧪", "🔌", "✂️"}},
            {{"🐱", "🐶", "🌳", "🌻", "🐟", "🐦", "🐞", "🌵"}, {"🪨", "🚗", "📚", "⚽", "🪑", "✏️", "🏠", "⏰"}},
            {{"🌳", "🌻", "🌵", "🌷", "🌴", "🌿"}, {"🐱", "🐄", "🐟", "🐦", "🐰", "🐘"}},
            {{"🐄", "🐪", "🦁", "🐰", "🐘", "🐓"}, {"🐟", "🐙", "🐬", "🦈", "🐳", "🦀"}},
            {{"🪨", "🔨", "🧱", "🥄", "🔑", "🪙"}, {"🧸", "🧽", "🧶", "🍞", "🪶", "🛏️"}},
            {{"🐘", "🚗", "🏠", "🪨", "🚚", "🛳️"}, {"🪶", "🎈", "🍃", "🦋", "✏️", "🧻"}},
            {{"🪞", "🥚", "🎈", "🧸", "🍎", "🐇"}, {"🌵", "🍍", "🪨", "🥥", "🦔", "🪵"}},
            {{"🦆", "🪵", "⛵", "🍃", "🎈", "🛟"}, {"🪨", "🔑", "🪙", "⚓", "🔨", "🥄"}},
            {{"🔥", "☕", "☀️", "🍲", "🌋", "♨️"}, {"🧊", "⛄", "🍦", "❄️", "🥶", "🐧"}},
            {{"🛒", "⚽", "🛝", "🔘", "🚪", "🛼"}, {"🪁", "🧲", "🎣", "🐕‍🦺", "🎒", "🚜"}},
    };
    // Choosing the one picture that fits: works with electricity, gives light, makes a sound, worn in winter.
    private static final String[] PICK_Q = {"أَيُّهَا يَعْمَلُ بِالْكَهْرَبَاء؟", "أَيُّهَا يُعْطِي ضَوْءًا؟", "أَيُّهَا يُصْدِرُ صَوْتًا؟",
            "مَاذَا نَلْبَسُ فِي هٰذَا الْفَصْل؟"};
    private static final String[][] PICK_YES = {{"📺", "💡", "📱", "🔌", "🖥️", "🧺"}, {"💡", "☀️", "🔦", "🕯️", "🔥", "⭐"},
            {"🔔", "🥁", "🎺", "📻", "🐓", "🎸"}, {"🧥", "🧣", "🧤", "🥾"}};
    private static final String[][] PICK_NO = {{"🪑", "📚", "🧸", "🍎", "🪨", "🥄"}, {"📚", "🪨", "🧸", "🪑", "🍞", "🧦"},
            {"🪨", "📚", "🍎", "🪑", "🧦", "🧽"}, {"🩳", "👙", "🕶️", "👒"}};
    // Which is heavier (lighter) or faster (slower): pictures from light to heavy, slow to fast.
    private static final String[][] MORE_PICS = {{"🪶", "🍎", "🐈", "🧒", "🐄", "🐘"}, {"🐌", "🐢", "🐈", "🐎", "🚗", "✈️"}};
    private static final String[][] MORE_Q = {{"أَيُّهُمَا أَثْقَل؟", "أَيُّهُمَا أَخَفّ؟"}, {"أَيُّهُمَا أَسْرَع؟", "أَيُّهُمَا أَبْطَأ؟"}};
    // Naming what is shown: land and water, seasons, weather, the season after, where the cat is.
    private static final String[][] WORD_PICS = {{"🏔️", "🌾", "🏞️", "🌊"}, {"⛄", "🌸", "🏖️", "🍂"}, {"☀️", "🌧️", "☁️", "🌨️"},
            {"⛄", "🌸", "🏖️", "🍂"}, {"", "", "", ""}};
    private static final String[][] WORD_NAMES = {{"جَبَل", "سَهْل", "نَهْر", "بَحْر"}, {"الشِّتَاء", "الرَّبِيع", "الصَّيْف", "الْخَرِيف"},
            {"مُشْمِس", "مُمْطِر", "غَائِم", "مُثْلِج"}, {"الشِّتَاء", "الرَّبِيع", "الصَّيْف", "الْخَرِيف"}, {"فَوْق", "تَحْت", "دَاخِل", "خَارِج"}};
    private static final String[] WORD_Q = {"مَا هٰذَا؟", "فِي أَيِّ فَصْلٍ نَحْنُ؟", "كَيْفَ الطَّقْس؟", "مَا الْفَصْلُ الَّذِي بَعْدَهُ؟",
            "أَيْنَ الْقِطَّة؟"};
    // What living things need, and things they do not.
    private static final String[] NEEDS = {"💧", "🍎", "🌬️", "☀️", "🏠"};
    private static final String[] NEED_NAMES = {"الْمَاء", "الْغِذَاء", "الْهَوَاء", "ضَوْءُ الشَّمْس", "الْمَأْوَى"};
    private static final String[] NOT_NEEDED = {"🎮", "🧸", "📺", "🎸", "⚽", "🎲"};
    // Growing up: three stages each.
    private static final String[][] GROWTH = {{"🥚", "🐣", "🐔"}, {"🌰", "🌱", "🌳"}, {"👶", "🧒", "🧑"}};
    // The plant's and the body's parts, as drawn.
    private static final String[] PLANT_PARTS = {"الْجَذْر", "السَّاق", "الْوَرَقَة", "الزَّهْرَة", "الثَّمَرَة"};
    private static final String[] BODY_PARTS = {"الرَّأْس", "الْعَيْن", "الْأُذُن", "الْأَنْف", "الْفَم", "الْيَد", "الْقَدَم"};
    // Animals: picture, what covers it (0 feathers, 1 fur, 2 scales), how it moves (0 flies, 1 walks, 2 swims).
    private static final String[] ANIMALS = {"🐦", "🦅", "🐟", "🐠", "🐱", "🐶", "🐰", "🐻"};
    private static final int[] COVER = {0, 0, 2, 2, 1, 1, 1, 1};
    private static final int[] MOVE = {0, 0, 2, 2, 1, 1, 1, 1};
    private static final String[] COVERS = {"رِيش", "فَرْو", "قُشُور"};
    private static final String[] MOVES = {"يَطِير", "يَمْشِي", "يَسْبَح"};
    // The senses: the question, the organ's picture and name, and something known by it.
    private static final String[] SENSE_Q = {"بِمَاذَا نَرَى؟", "بِمَاذَا نَسْمَعُ؟", "بِمَاذَا نَشُمُّ؟", "بِمَاذَا نَتَذَوَّقُ؟",
            "بِمَاذَا نَلْمِسُ؟"};
    private static final String[] ORGANS = {"👁️", "👂", "👃", "👅", "✋"};
    private static final String[] ORGAN_NAMES = {"الْعَيْن", "الْأُذُن", "الْأَنْف", "اللِّسَان", "الْجِلْد"};
    private static final String[] SENSED = {"🌈", "🔔", "🌸", "🍋", "🧸"};

    private static final int ASK = 0, SORT = 1, NEEDS_Q = 2, GROW = 3, PLANT = 4, ANIMAL = 5, BODY = 6, SENSES = 7, DONE = 8,
            PICK = 9, MORE = 10, WORD = 11;
    private static final int ROUNDS = 6;

    private SharedPreferences prefs;
    private Lessons book;
    private final Map<String, String> voice = new HashMap<>();
    private Stop stop;
    private int stage;
    private final Random rnd = new Random();
    private MediaPlayer player;
    private SciView view;
    private boolean girl, busy;

    private final List<Integer> steps = new ArrayList<>();
    private int step, round, rounds;

    // The question on screen: answer cards (words or pictures) and the right one.
    private final List<String> options = new ArrayList<>();
    private final List<Boolean> optionPics = new ArrayList<>();
    private int answer = -1, wrongPick = -1, rightPick = -1;
    private long wobbleAt;
    /** The picture asked about, and which part of a drawing. */
    private String subject = "";
    private int qa, qb, qc;
    /** What Lili says after a right answer. */
    private final List<String> reply = new ArrayList<>();
    /** Growing up: the right order, the shuffled cards, which are placed. */
    private final List<String> order = new ArrayList<>();
    private final List<String> cards = new ArrayList<>();
    private final List<Boolean> used = new ArrayList<>();
    private int placed;
    /** What Lili says when the speaker is pressed. */
    private final List<String> question = new ArrayList<>();

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        String file = getIntent().getStringExtra(EXTRA_PREFS);
        if (file == null) file = "science";
        prefs = getSharedPreferences(file, MODE_PRIVATE);
        try {
            book = new Lessons(readAsset("letters/lessons.json"));
            JSONObject o = new JSONObject(readAsset("science/sounds.json"));
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
        if (g.isEmpty() && file.endsWith("_science")) {
            g = getSharedPreferences(file.substring(0, file.length() - "_science".length()) + "_letters", MODE_PRIVATE)
                    .getString("gender", "");
            if (!g.isEmpty()) prefs.edit().putString("gender", g).apply();
        }
        girl = "g".equals(g);

        if (g.isEmpty()) steps.add(ASK);
        int[] byType = {SORT, NEEDS_Q, GROW, PLANT, ANIMAL, BODY, SENSES, PICK, MORE, WORD};
        steps.add(byType[stop.type]);
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
        view = new SciView(this);
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

    /** Lili's recording of a science word or question, as an asset path. */
    private String m(String text) {
        String f = voice.get(text);
        return f == null ? null : "science/" + f;
    }

    /** A phrase recorded for the letters game (praise, «try again», hello…). */
    private String l(String text) {
        String f = book.phrase(text);
        return f == null ? null : "letters/" + f;
    }

    private String g(String boy, String girlForm) {
        return m(girl ? girlForm : boy);
    }

    /** A word without its vowel marks, for a card. */
    private static String plain(String s) {
        return s.replaceAll("[\\u064B-\\u0652]", "");
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
        switch (kind()) {
            case ASK:
                play(l("مَرْحَبًا، أَنَا لِيلِي!"), () -> play(l("مَنْ يَلْعَبُ مَعَ لِيلِي؟"), null));
                break;
            case GROW:
                rounds = 3;
                newGrow(true);
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

    /** {@code n} different numbers from 0 to {@code size - 1}, one of them {@code keep}, shuffled. */
    private List<Integer> pickSome(int size, int n, int keep) {
        List<Integer> pool = new ArrayList<>();
        for (int v = 0; v < size; v++) if (v != keep) pool.add(v);
        Collections.shuffle(pool, rnd);
        List<Integer> out = new ArrayList<>();
        out.add(keep);
        for (int v : pool) if (out.size() < n) out.add(v);
        Collections.shuffle(out, rnd);
        return out;
    }

    private void newQuestion(boolean first) {
        busy = false;
        wrongPick = -1;
        rightPick = -1;
        question.clear();
        reply.clear();
        options.clear();
        optionPics.clear();
        List<String> lead = new ArrayList<>();
        if (first && stage == 0) lead.add(m("هَيَّا نَتَعَلَّمُ الْعُلُوم"));
        int k = kind();
        switch (k) {
            case SORT: {
                int md = stop.mode;
                qa = rnd.nextInt(2);
                String[] pics = SORT_PICS[md][qa];
                subject = pics[rnd.nextInt(pics.length)];
                for (int i = 0; i < 2; i++) {
                    options.add(plain(SORT_WORDS[md][i]));
                    optionPics.add(false);
                }
                answer = qa;
                question.add(m(SORT_Q[md]));
                reply.add(m(SORT_WORDS[md][qa]));
                break;
            }
            case NEEDS_Q: {
                qa = rnd.nextInt(NEEDS.length);
                List<Integer> others = pickSome(NOT_NEEDED.length, 2, rnd.nextInt(NOT_NEEDED.length));
                List<String> pics = new ArrayList<>();
                pics.add(NEEDS[qa]);
                for (int v : others) pics.add(NOT_NEEDED[v]);
                Collections.shuffle(pics, rnd);
                for (String p : pics) {
                    options.add(p);
                    optionPics.add(true);
                }
                answer = pics.indexOf(NEEDS[qa]);
                subject = round % 2 == 0 ? "🌻" : "🐰";
                question.add(m("مَاذَا يَحْتَاجُ الْكَائِنُ الْحَيّ؟"));
                reply.add(m(NEED_NAMES[qa]));
                break;
            }
            case PLANT:
            case BODY: {
                String[] parts = k == PLANT ? PLANT_PARTS : BODY_PARTS;
                qa = round < parts.length ? (round + rnd.nextInt(parts.length)) % parts.length : rnd.nextInt(parts.length);
                answer = qa;
                question.add(m("أَيْنَ"));
                question.add(m(parts[qa]));
                reply.add(m(parts[qa]));
                break;
            }
            case ANIMAL: {
                qa = rnd.nextInt(ANIMALS.length);
                qb = rnd.nextInt(2); // 0 what covers it, 1 how it moves
                subject = ANIMALS[qa];
                String[] words = qb == 0 ? COVERS : MOVES;
                for (String w : words) {
                    options.add(plain(w));
                    optionPics.add(false);
                }
                answer = qb == 0 ? COVER[qa] : MOVE[qa];
                question.add(m(qb == 0 ? "مَاذَا يُغَطِّي جِسْمَهُ؟" : "كَيْفَ يَتَحَرَّك؟"));
                reply.add(m(words[answer]));
                break;
            }
            case PICK: {
                int md = stop.mode;
                // Clothes: a winter picture asks for the warm one; a summer picture for the light one.
                boolean summer = md == 3 && rnd.nextBoolean();
                String[] yes = summer ? PICK_NO[md] : PICK_YES[md], no = summer ? PICK_YES[md] : PICK_NO[md];
                String right = yes[rnd.nextInt(yes.length)];
                List<String> pics = new ArrayList<>();
                pics.add(right);
                for (int v : pickSome(no.length, 2, rnd.nextInt(no.length))) pics.add(no[v]);
                Collections.shuffle(pics, rnd);
                for (String p : pics) {
                    options.add(p);
                    optionPics.add(true);
                }
                answer = pics.indexOf(right);
                subject = md == 3 ? (summer ? "🏖️" : "⛄") : "";
                question.add(m(PICK_Q[md]));
                if (md == 3) reply.add(m(summer ? "الصَّيْف" : "الشِّتَاء"));
                break;
            }
            case MORE: {
                int md = stop.mode;
                String[] row = MORE_PICS[md];
                qa = rnd.nextInt(row.length);
                do qb = rnd.nextInt(row.length); while (qb == qa);
                int most = rnd.nextInt(2); // 0 heavier/faster, 1 lighter/slower
                qc = most;
                options.add(row[qa]);
                options.add(row[qb]);
                optionPics.add(true);
                optionPics.add(true);
                answer = (qa > qb) == (most == 0) ? 0 : 1;
                subject = "";
                question.add(m(MORE_Q[md][most]));
                break;
            }
            case WORD: {
                int md = stop.mode;
                qa = rnd.nextInt(4);
                int right = md == 3 ? (qa + 1) % 4 : qa;
                subject = WORD_PICS[md][qa];
                List<Integer> pick = pickSome(4, 3, right);
                for (int v : pick) {
                    options.add(plain(WORD_NAMES[md][v]));
                    optionPics.add(false);
                }
                answer = pick.indexOf(right);
                question.add(m(WORD_Q[md]));
                reply.add(m(WORD_NAMES[md][right]));
                break;
            }
            case SENSES: {
                qa = rnd.nextInt(ORGANS.length);
                qb = rnd.nextInt(2); // 0 «with what do we see?», 1 «which sense tells us this?»
                subject = qb == 1 ? SENSED[qa] : "";
                List<Integer> pick = pickSome(ORGANS.length, 3, qa);
                for (int v : pick) {
                    options.add(ORGANS[v]);
                    optionPics.add(true);
                }
                answer = pick.indexOf(qa);
                question.add(m(qb == 0 ? SENSE_Q[qa] : "بِأَيِّ حَاسَّةٍ نَعْرِفُهُ؟"));
                reply.add(m(ORGAN_NAMES[qa]));
                break;
            }
        }
        lead.addAll(question);
        view.invalidate();
        play(lead, null);
    }

    /** Three stages of growing up to put in order, the youngest first. */
    private void newGrow(boolean first) {
        busy = false;
        wrongPick = -1;
        String[] set = GROWTH[(round + rnd.nextInt(GROWTH.length)) % GROWTH.length];
        order.clear();
        for (String s : set) order.add(s);
        cards.clear();
        cards.addAll(order);
        for (int t = 0; t < 8 && cards.equals(order); t++) Collections.shuffle(cards, rnd);
        used.clear();
        for (int i = 0; i < cards.size(); i++) used.add(false);
        placed = 0;
        question.clear();
        question.add(g("رَتِّبْ مَرَاحِلَ النُّمُوّ", "رَتِّبِي مَرَاحِلَ النُّمُوّ"));
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
                play(cheer(book.praise), () -> {
                    round++;
                    if (round < rounds) newGrow(false);
                    else nextStep();
                });
            }
        } else {
            wrongPick = i;
            wobbleAt = SystemClock.uptimeMillis();
            view.invalidate();
            play(cheer(book.tryAgain), null);
        }
    }

    private void pick(int i) {
        if (busy || i < 0) return;
        int k = kind();
        if (k != PLANT && k != BODY && i >= options.size()) return;
        if (i == answer) {
            busy = true;
            rightPick = i;
            view.invalidate();
            List<String> after = new ArrayList<>(reply);
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

    // ---- Screen ------------------------------------------------------------------------------

    private class SciView extends View {
        private final float dp;
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint emoji = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Bitmap lili;
        private final List<float[]> boxes = new ArrayList<>();
        private int down = -1;
        private static final int B_SPEAK = -10, B_NEXT = -11, B_BOY = -13, B_GIRL = -14;

        SciView(Context ctx) {
            super(ctx);
            dp = getResources().getDisplayMetrics().density;
            text.setTypeface(Typeface.DEFAULT_BOLD);
            text.setTextAlign(Paint.Align.CENTER);
            emoji.setTextAlign(Paint.Align.CENTER);
            lili = Draw.load(getResources(), R.drawable.home_happy);
        }

        private int hit(float x, float y) {
            for (float[] b : boxes) if (x >= b[0] && x <= b[2] && y >= b[1] && y <= b[3]) return (int) b[4];
            return Integer.MIN_VALUE;
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
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
            else if (w == B_SPEAK) ask();
            else if (w == B_NEXT) {
                if (!busy || k == DONE) nextStep();
            } else if (w >= 0) {
                if (k == GROW) place(w);
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

        private void pic(Canvas c, String e, float cx, float cy, float size) {
            emoji.setTextSize(size);
            c.drawText(e, cx, cy - (emoji.ascent() + emoji.descent()) / 2f, emoji);
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

        private float wobble() {
            long since = SystemClock.uptimeMillis() - wobbleAt;
            if (wrongPick < 0 || since >= 450) return 0;
            postInvalidateOnAnimation();
            return (float) Math.sin(since / 30.0) * 10 * dp * (1 - since / 450f);
        }

        /** A stroke of its own: Draw's paint is shared, so a fill drawn between would change it. */
        private Paint pen(int colour, float width) {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(width);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
            p.setColor(colour);
            return p;
        }

        /** Colours of a part: green once found, red when wrongly tapped, else a soft highlight while pressed. */
        private int partColour(int i, int normal) {
            if (i == rightPick) return Draw.MINT_D;
            if (i == wrongPick) return 0xFFE07A7A;
            return normal;
        }

        /** The cat and a box: 0 on top, 1 under (a table), 2 inside, 3 beside it. */
        private void cat(Canvas c, int where, float l, float t, float r, float b) {
            float cx = (l + r) / 2, cy = (t + b) / 2, s = Math.min(r - l, b - t) * 0.22f;
            if (where <= 1) {
                float top = cy - s * 0.1f;
                c.drawRect(cx - s * 1.7f, top, cx + s * 1.7f, top + s * 0.22f, Draw.fill(0xFFB07A4A));
                c.drawRect(cx - s * 1.55f, top, cx - s * 1.35f, top + s * 1.5f, Draw.fill(0xFF8A5A30));
                c.drawRect(cx + s * 1.35f, top, cx + s * 1.55f, top + s * 1.5f, Draw.fill(0xFF8A5A30));
                pic(c, "🐱", cx, where == 0 ? top - s * 0.55f : top + s * 0.95f, s * 1.0f);
                return;
            }
            float bl = cx - s, bt = cy - s * 0.3f, bw = s * 2, bh = s * 1.3f;
            if (where == 2) pic(c, "🐱", cx, bt + s * 0.05f, s * 1.0f);
            c.drawRect(bl, bt, bl + bw, bt + bh, Draw.fill(0xFFE8B86B));
            c.drawRect(bl, bt, bl + bw, bt + bh * 0.18f, Draw.fill(0xFFD49A45));
            c.drawRect(bl, bt, bl + bw, bt + bh, pen(0xFF8A5A30, 2.5f * dp));
            if (where == 3) pic(c, "🐱", bl + bw + s * 0.9f, bt + bh - s * 0.45f, s * 1.0f);
        }

        /** A plant: roots, stem, leaves, a flower and a fruit; each part is tappable. */
        private void plant(Canvas c, float l, float t, float r, float b) {
            float cx = (l + r) / 2, h = b - t, ground = t + h * 0.72f, u = Math.min(r - l, h) / 10f;
            c.drawRect(l, ground, r, b, Draw.fill(0xFFD8B48A));
            // Roots
            Paint root = pen(partColour(0, 0xFF8A5A30), u * 0.22f);
            for (int i = -2; i <= 2; i++) {
                Path p = new Path();
                p.moveTo(cx, ground);
                p.quadTo(cx + i * u * 0.6f, ground + u * 1.2f, cx + i * u * 1.1f, ground + u * 2.3f);
                c.drawPath(p, root);
            }
            box(cx - u * 2.6f, ground + u * 0.2f, cx + u * 2.6f, Math.min(b, ground + u * 2.8f), 0);
            // Stem
            float top = t + h * 0.18f;
            c.drawLine(cx, ground, cx, top, pen(partColour(1, 0xFF3E9E4F), u * 0.45f));
            box(cx - u * 0.6f, top + u * 1.2f, cx + u * 0.6f, ground, 1);
            // Leaves
            for (int s = -1; s <= 1; s += 2) {
                float ly = ground - h * 0.22f - (s > 0 ? 0 : u * 0.9f);
                RectF leaf = new RectF(Math.min(cx, cx + s * u * 3f), ly - u * 0.7f, Math.max(cx, cx + s * u * 3f), ly + u * 0.7f);
                c.drawOval(leaf, Draw.fill(partColour(2, 0xFF5CC26E)));
                box(leaf.left, leaf.top - u * 0.3f, leaf.right, leaf.bottom + u * 0.3f, 2);
            }
            // Flower on top
            for (int i = 0; i < 6; i++) {
                double a = Math.PI * 2 * i / 6;
                c.drawCircle(cx + (float) Math.cos(a) * u * 0.9f, top + (float) Math.sin(a) * u * 0.9f, u * 0.65f,
                        Draw.fill(partColour(3, 0xFFFF8FA3)));
            }
            c.drawCircle(cx, top, u * 0.55f, Draw.fill(Draw.YELLOW_D));
            box(cx - u * 1.7f, top - u * 1.7f, cx + u * 1.7f, top + u * 1.6f, 3);
            // A fruit hanging from a side branch
            float fx = cx + u * 2.6f, fy = top + h * 0.18f;
            c.drawLine(cx, fy - u, fx, fy - u * 0.2f, pen(0xFF3E9E4F, u * 0.25f));
            c.drawCircle(fx, fy + u * 0.5f, u * 0.85f, Draw.fill(partColour(4, 0xFFFF6B6B)));
            c.drawCircle(fx - u * 0.3f, fy + u * 0.2f, u * 0.2f, Draw.fill(0x88FFFFFF));
            box(fx - u * 1.2f, fy - u * 0.6f, fx + u * 1.2f, fy + u * 1.6f, 4);
        }

        /** A child: head with eyes, ears, nose and mouth, two hands and two feet; each part is tappable. */
        private void child(Canvas c, float l, float t, float r, float b) {
            float cx = (l + r) / 2, h = b - t, u = Math.min((r - l) / 6f, h / 10f);
            float hy = t + u * 2.0f, hr = u * 1.6f;
            // Body
            c.drawRoundRect(new RectF(cx - u * 1.3f, hy + hr, cx + u * 1.3f, hy + hr + u * 3.6f), u * 0.6f, u * 0.6f,
                    Draw.fill(Draw.BLUE));
            // Arms and hands
            for (int s = -1; s <= 1; s += 2) {
                float hx = cx + s * u * 2.6f, hyy = hy + hr + u * 3.0f;
                c.drawLine(cx + s * u * 1.2f, hy + hr + u * 0.6f, hx, hyy, pen(0xFFFFD9B3, u * 0.55f));
                c.drawCircle(hx, hyy, u * 0.55f, Draw.fill(partColour(5, 0xFFFFC499)));
                box(hx - u * 0.9f, hyy - u * 0.9f, hx + u * 0.9f, hyy + u * 0.9f, 5);
            }
            // Legs and feet
            float legTop = hy + hr + u * 3.5f, footY = Math.min(b - u * 0.6f, legTop + u * 2.4f);
            for (int s = -1; s <= 1; s += 2) {
                float lx = cx + s * u * 0.7f;
                c.drawLine(lx, legTop, lx, footY, pen(0xFF5B3E8C, u * 0.6f));
                RectF foot = new RectF(lx - u * 0.7f + s * u * 0.3f, footY - u * 0.35f, lx + u * 0.7f + s * u * 0.3f, footY + u * 0.45f);
                c.drawOval(foot, Draw.fill(partColour(6, 0xFFE8607E)));
                box(foot.left - u * 0.3f, foot.top - u * 0.4f, foot.right + u * 0.3f, foot.bottom + u * 0.4f, 6);
            }
            // Ears, then the head over them
            for (int s = -1; s <= 1; s += 2) {
                float ex = cx + s * hr * 1.0f;
                c.drawCircle(ex, hy + u * 0.1f, u * 0.45f, Draw.fill(partColour(2, 0xFFFFC499)));
                box(ex - u * 0.7f, hy - u * 0.6f, ex + u * 0.7f, hy + u * 0.8f, 2);
            }
            c.drawCircle(cx, hy, hr, Draw.fill(partColour(0, 0xFFFFD9B3)));
            c.drawArc(new RectF(cx - hr, hy - hr, cx + hr, hy + hr * 0.2f), 180, 180, true, Draw.fill(0xFF7A4B2A));
            box(cx - hr, hy - hr - u * 0.2f, cx + hr, hy - hr * 0.45f, 0);
            // Eyes, nose, mouth
            for (int s = -1; s <= 1; s += 2) {
                c.drawCircle(cx + s * u * 0.6f, hy + u * 0.1f, u * 0.2f, Draw.fill(partColour(1, Draw.PLUM)));
                box(cx + s * u * 0.6f - u * 0.4f, hy - u * 0.3f, cx + s * u * 0.6f + u * 0.4f, hy + u * 0.45f, 1);
            }
            c.drawCircle(cx, hy + u * 0.6f, u * 0.17f, Draw.fill(partColour(3, 0xFFE8A07A)));
            box(cx - u * 0.3f, hy + u * 0.4f, cx + u * 0.3f, hy + u * 0.8f, 3);
            c.drawArc(new RectF(cx - u * 0.5f, hy + u * 0.6f, cx + u * 0.5f, hy + u * 1.2f), 20, 140, false,
                    pen(partColour(4, 0xFFE8607E), u * 0.16f));
            box(cx - u * 0.6f, hy + u * 0.85f, cx + u * 0.6f, hy + u * 1.35f, 4);
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
                return;
            }
            if (k == DONE) {
                float ph = Math.min(h * 0.45f, w * 0.6f);
                Draw.picture(c, lili, w / 2 - ph / 2, pad, w / 2 + ph / 2, pad + ph);
                float y = pad + ph + pad;
                for (int i = 0; i < 3; i++) Draw.star(c, w / 2 + (i - 1) * 54 * dp, y + 26 * dp, 22 * dp, Draw.YELLOW_D);
                label(c, girl ? "أَحْسَنْتِ!" : "أَحْسَنْتَ!", w / 2, y + 90 * dp, 40 * dp, w - 2 * pad, Draw.PLUM);
                next(c, w / 2, y + 150 * dp + btnH / 2, btnH * 2.4f, btnH, "تَمّ");
                return;
            }
            // A question: counter, speaker, the picture, then the answers.
            label(c, (round + 1) + " / " + rounds, w / 2, pad + 12 * dp, 18 * dp, w, 0xFF9AA0BC);
            float spT = pad + 30 * dp;
            if (side) speaker(c, pad + sr, pad + sr, sr);
            else speaker(c, w / 2, spT + sr, sr);
            float top = side ? pad + 30 * dp : spT + 2 * sr + pad;
            float wob = wobble();
            if (k == PLANT || k == BODY) {
                Draw.card(c, pad, top, w - pad, h - pad - 8 * dp, 28 * dp, 0xFFFFFFFF, 0xFFE6DCCB, 8 * dp);
                c.save();
                c.translate(wob, 0);
                if (k == PLANT) plant(c, pad + 12 * dp, top + 12 * dp, w - pad - 12 * dp, h - pad - 20 * dp);
                else child(c, pad + 12 * dp, top + 12 * dp, w - pad - 12 * dp, h - pad - 20 * dp);
                c.restore();
                return;
            }
            if (k == GROW) {
                int n = cards.size();
                float cw = Math.min(130 * dp, (w - pad * (n + 1)) / n);
                float ch = Math.min(cw, (h - top - 3 * pad - 30 * dp) / 2);
                float x0 = (w - (n * cw + (n - 1) * pad)) / 2;
                label(c, "←", w / 2, top + 10 * dp, 22 * dp, 60 * dp, Draw.PLUM);
                float slotT = top + 30 * dp;
                for (int s = 0; s < n; s++) {
                    float l = x0 + (n - 1 - s) * (cw + pad), r = l + cw;
                    if (s < placed) {
                        Draw.card(c, l, slotT, r, slotT + ch - 6 * dp, 20 * dp, Draw.MINT, Draw.MINT_D, 6 * dp);
                        pic(c, order.get(s), (l + r) / 2, slotT + ch / 2 - 3 * dp, ch * 0.55f);
                    } else {
                        c.drawRoundRect(new RectF(l, slotT, r, slotT + ch - 6 * dp), 20 * dp, 20 * dp, pen(0xFFC9CDE0, 3 * dp));
                    }
                }
                float cardT = slotT + ch + 2 * pad;
                for (int i = 0; i < n; i++) {
                    if (used.get(i)) continue;
                    float l = x0 + (n - 1 - i) * (cw + pad), r = l + cw;
                    c.save();
                    if (i == wrongPick) c.translate(wob, 0);
                    button(c, l, cardT, r, cardT + ch, i == wrongPick ? 0xFFFFB3B3 : Draw.LILAC,
                            i == wrongPick ? 0xFFE07A7A : Draw.LILAC_D, i);
                    pic(c, cards.get(i), (l + r) / 2, cardT + ch / 2 - 3 * dp, ch * 0.55f);
                    c.restore();
                }
                return;
            }
            // The picture asked about (or the question written, for «with what do we see?»).
            float optH = Math.min(150 * dp, Math.max(96 * dp, h * 0.2f));
            float sceneB = h - pad - optH - pad;
            Draw.card(c, pad, top, w - pad, sceneB - 8 * dp, 28 * dp, 0xFFFFFFFF, 0xFFE6DCCB, 8 * dp);
            float cx = w / 2, cy = (top + sceneB) / 2;
            float s = Math.min(w - 2 * pad, sceneB - top) * 0.55f;
            if (k == WORD && stop.mode == 4) {
                // A box, and a cat on it, under it (a table), in it or beside it.
                cat(c, qa, pad, top, w - pad, sceneB - 8 * dp);
            } else if (k == MORE || (k == PICK && stop.mode < 3)) {
                String q = k == MORE ? MORE_Q[stop.mode][qc] : PICK_Q[stop.mode];
                label(c, plain(q), cx, cy, Math.min(44 * dp, s * 0.25f), w - 4 * pad, Draw.PLUM);
            } else if (k == SENSES && qb == 0) {
                label(c, plain(SENSE_Q[qa]), cx, cy, Math.min(48 * dp, s * 0.3f), w - 4 * pad, Draw.PLUM);
            } else if (k == NEEDS_Q) {
                pic(c, subject, cx, cy + s * 0.08f, s * 0.6f);
                label(c, plain("مَاذَا يَحْتَاجُ؟"), cx, top + 26 * dp, 22 * dp, w - 4 * pad, Draw.PLUM);
            } else if (rightPick < 0 || k != SORT) {
                pic(c, subject, cx, cy, s * 0.75f);
            } else {
                pic(c, subject, cx, cy, s * 0.75f);
            }
            // Answers: words or pictures, in one row from the right.
            int n = options.size();
            float cw = Math.min(220 * dp, (w - pad * (n + 1)) / n);
            float ch = optH;
            float x0 = (w - (n * cw + (n - 1) * pad)) / 2;
            float ot = sceneB + pad - 8 * dp;
            for (int i = 0; i < n; i++) {
                float l = x0 + (n - 1 - i) * (cw + pad), r = l + cw;
                int fill = i == rightPick ? Draw.MINT : i == wrongPick ? 0xFFFFB3B3 : Draw.LILAC;
                int deep = i == rightPick ? Draw.MINT_D : i == wrongPick ? 0xFFE07A7A : Draw.LILAC_D;
                c.save();
                if (i == wrongPick) c.translate(wob, 0);
                button(c, l, ot, r, ot + ch, fill, deep, i);
                if (optionPics.get(i)) pic(c, options.get(i), (l + r) / 2, ot + ch / 2 - 3 * dp, Math.min(cw, ch) * 0.55f);
                else label(c, options.get(i), (l + r) / 2, ot + ch / 2 - 3 * dp, ch * 0.3f, cw * 0.85f, 0xFFFFFFFF);
                c.restore();
            }
        }
    }
}
