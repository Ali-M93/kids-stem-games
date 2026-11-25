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
            T_ORDINAL = 5, T_ADD = 6, T_SUB = 7, T_CLASSIFY = 8,
            T_GRAPH = 9, T_DAYS = 10, T_CLOCK = 11, T_FRACTION = 12, T_LENGTH = 13, T_SHAPES = 14, T_SOLIDS = 15, T_TENS = 16,
            T_STORY = 17, T_BOND = 18, T_FACTS = 19, T_KIND = 20, T_SPATIAL = 21, T_HUNDRED = 22;

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

    /**
     * The book's order: 1 patterns and numbers to 20, 2 compare and order, 3 sums to 10, 4 sorting and
     * picture graphs, 5-6 sums to 20, 7 days, clock, halves, lengths, shapes, 8 tens and ones to 99,
     * 9 sums to 99 without carrying.
     */
    private static final Stop[] STOPS = {
            // 1: patterns and numbers to 20
            new Stop("الأنماط", "▲●", T_PATTERN, 0),
            new Stop("العددان ١ و ٢", "١ ٢", T_NUM, 2, 1, 2),
            new Stop("العددان ٣ و ٤", "٣ ٤", T_NUM, 4, 3, 4),
            new Stop("العددان ٥ و ٦", "٥ ٦", T_NUM, 6, 5, 6),
            new Stop("الأعداد ٧ و ٨ و ٩", "٧-٩", T_NUM, 9, 7, 8, 9),
            new Stop("العدد صفر", "٠", T_NUM, 9, 0),
            new Stop("العدد ١٠", "١٠", T_NUM, 10, 10),
            new Stop("الأعداد من ١١ إلى ١٥", "١١-١٥", T_NUM, 15, 11, 12, 13, 14, 15),
            new Stop("الأعداد من ١٦ إلى ٢٠", "١٦-٢٠", T_NUM, 20, 16, 17, 18, 19, 20),
            // 2: comparing and ordering
            new Stop("مقارنة الأعداد حتى ٩", "أكبر", T_COMPARE, 9),
            new Stop("السابق والتالي حتى ٩", "قبل", T_NEIGHBOR, 9),
            new Stop("ترتيب الأعداد حتى ٩", "رتّب", T_ORDER, 9),
            new Stop("العدد الترتيبي", "الأول", T_ORDINAL, 10),
            new Stop("مقارنة الأعداد حتى ٢٠", "٢٠<", T_COMPARE, 20),
            new Stop("السابق والتالي حتى ٢٠", "بعد", T_NEIGHBOR, 20),
            new Stop("ترتيب الأعداد حتى ٢٠", "٢٠", T_ORDER, 20),
            // 3: adding and taking away to 10
            new Stop("قصص الجمع", "قصة", T_STORY, 10, 3),
            new Stop("الجمع حتى ١٠", "+", T_ADD, 10),
            new Stop("جمع الصفر وجمع العدد مع نفسه", "٠+", T_ADD, 10, 1),
            new Stop("تكوين الأعداد حتى ٦", "٦=", T_BOND, 6),
            new Stop("تكوين الأعداد حتى ١٠", "١٠=", T_BOND, 10),
            new Stop("مفهوم الطرح", "−", T_SUB, 10),
            new Stop("الحقائق المترابطة", "+−", T_FACTS, 10),
            new Stop("طرح الصفر وطرح الكل", "−٠", T_SUB, 10, 1),
            new Stop("خطة حل المسألة", "مسألة", T_STORY, 10, 0),
            // 4: sorting and picture graphs
            new Stop("التصنيف بالشكل", "صنّف", T_CLASSIFY, 0),
            new Stop("التصنيف باللون", "لون", T_CLASSIFY, 1),
            new Stop("التصنيف بالنوع", "نوع", T_KIND, 0),
            new Stop("التمثيل بالصور", "مخطط", T_GRAPH, 0),
            // 5: adding to 20
            new Stop("الجمع بإضافة ١ و ٢ و ٣", "+١", T_ADD, 20, 3),
            new Stop("الجمع على خط الأعداد", "+خط", T_ADD, 20, 4),
            new Stop("جمع العدد مع نفسه", "٥+٥", T_ADD, 20, 2),
            new Stop("الجمع بتكوين العدد ١٠", "+٢٠", T_ADD, 20),
            new Stop("مسائل الجمع", "قصة+", T_STORY, 20, 1),
            // 6: taking away to 20
            new Stop("الطرح بالعد إلى الوراء", "−١", T_SUB, 20, 2),
            new Stop("الطرح على خط الأعداد", "−خط", T_SUB, 20, 3),
            new Stop("الطرح حتى ٢٠", "−٢٠", T_SUB, 20),
            new Stop("الحقائق المترابطة حتى ٢٠", "+−٢٠", T_FACTS, 20),
            new Stop("مسائل الطرح", "قصة−", T_STORY, 20, 2),
            // 7: geometry and measuring
            new Stop("أيام الأسبوع", "أيام", T_DAYS, 0),
            new Stop("الوقت بالساعات الكاملة", "ساعة", T_CLOCK, 0, 0),
            new Stop("النصف", "½", T_FRACTION, 0, 0),
            new Stop("الربع", "¼", T_FRACTION, 0, 1),
            new Stop("الوقت بنصف الساعة", "٣٠:", T_CLOCK, 0, 1),
            new Stop("الأطوال", "أطول", T_LENGTH, 0),
            new Stop("المجسمات", "مجسم", T_SOLIDS, 0),
            new Stop("الأشكال المستوية", "أشكال", T_SHAPES, 0),
            new Stop("البعد المكاني", "فوق", T_SPATIAL, 0),
            // 8: numbers to 99
            new Stop("العشرات والآحاد", "٩٩", T_TENS, 99),
            new Stop("لوحة المئة: خمسات وعشرات", "١٠٠", T_HUNDRED, 100, 10),
            new Stop("لوحة المئة: اثنينات", "٢،٤", T_HUNDRED, 100, 2),
            new Stop("مقارنة الأعداد حتى ٩٩", "٩٩<", T_COMPARE, 99),
            new Stop("السابق والتالي حتى ٩٩", "قبل٩٩", T_NEIGHBOR, 99),
            new Stop("ترتيب الأعداد حتى ٩٩", "رتّب٩٩", T_ORDER, 99),
            // 9: adding and taking away to 99, no carrying
            new Stop("جمع العشرات", "+١٠", T_ADD, 99, 5),
            new Stop("طرح العشرات", "−١٠", T_SUB, 99, 5),
            new Stop("الجمع حتى ٩٩", "+٩٩", T_ADD, 99),
            new Stop("الطرح حتى ٩٩", "−٩٩", T_SUB, 99),
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

    private static final String[] DAY_NAMES = {"السَّبْت", "الْأَحَد", "الِاثْنَيْن", "الثُّلَاثَاء", "الْأَرْبِعَاء", "الْخَمِيس", "الْجُمُعَة"};
    private static final String[] HOURS = {"", "الْوَاحِدَة", "الثَّانِيَة", "الثَّالِثَة", "الرَّابِعَة", "الْخَامِسَة", "السَّادِسَة",
            "السَّابِعَة", "الثَّامِنَة", "التَّاسِعَة", "الْعَاشِرَة", "الْحَادِيَةَ عَشْرَة", "الثَّانِيَةَ عَشْرَة"};
    /** Shapes by their drawing code: 0 circle, 1 square, 2 triangle, 4 rectangle (3, the star, has no name). */
    private static final String[] SHAPE_NAMES = {"دَائِرَة", "مُرَبَّع", "مُثَلَّث", "", "مُسْتَطِيل"};
    private static final String[] SOLID_NAMES = {"مُكَعَّب", "أُسْطُوَانَة", "كُرَة", "مَخْرُوط", "مُتَوَازِي مُسْتَطِيلَات"};

    /** Little stories to add (birds, balloons, fish, roses) and to take away (apples, cars, stars, pencils). */
    private static final String[] ADD_STORIES = {
            "فِي الشَّجَرَةِ عَصَافِير، وَجَاءَتْ عَصَافِيرُ أُخْرَى. كَمْ عُصْفُورًا صَارَ فِي الشَّجَرَة؟",
            "عِنْدَ لِيلِي بَالُونَات، وَأَعْطَاهَا أَبُوهَا بَالُونَاتٍ أُخْرَى. كَمْ بَالُونًا صَارَ عِنْدَهَا؟",
            "فِي الْحَوْضِ سَمَكَات، وَوَضَعْنَا سَمَكَاتٍ أُخْرَى. كَمْ سَمَكَةً صَارَ فِي الْحَوْض؟",
            "فِي الْحَدِيقَةِ وُرُود، وَزَرَعْنَا وُرُودًا أُخْرَى. كَمْ وَرْدَةً صَارَ فِي الْحَدِيقَة؟"};
    private static final String[] ADD_PICS = {"🐦", "🎈", "🐟", "🌹"};
    private static final String[] SUB_STORIES = {
            "فِي السَّلَّةِ تُفَّاحَات، وَأَكَلَتْ لِيلِي بَعْضَهَا. كَمْ تُفَّاحَةً بَقِيَ فِي السَّلَّة؟",
            "فِي الشَّارِعِ سَيَّارَات، وَذَهَبَ بَعْضُهَا. كَمْ سَيَّارَةً بَقِيَ فِي الشَّارِع؟",
            "فِي السَّمَاءِ نُجُوم، وَاخْتَفَى بَعْضُهَا خَلْفَ الْغَيْم. كَمْ نَجْمًا بَقِيَ؟",
            "عِنْدَ لِيلِي أَقْلَام، وَأَعْطَتْ صَدِيقَتَهَا بَعْضَهَا. كَمْ قَلَمًا بَقِيَ عِنْدَهَا؟"};
    private static final String[] SUB_PICS = {"🍎", "🚗", "⭐", "✏️"};
    private static final String[] KIND_NAMES = {"حَيَوَانَات", "فَوَاكِه", "وَسَائِلُ نَقْل"};
    private static final String[][] KIND_PICS = {{"🐱", "🐶", "🐟", "🐦", "🐰", "🐄"},
            {"🍎", "🍌", "🍇", "🍊", "🍓", "🍉"}, {"🚗", "🚌", "✈️", "🚲", "🚂", "⛵"}};
    /** Where the ball is: above, under, right, left, in, out, in front, behind. */
    private static final String[] PLACES = {"فَوْق", "تَحْت", "يَمِين", "يَسَار", "دَاخِل", "خَارِج", "أَمَام", "خَلْف"};

    private static final int ASK = 0, INTRO = 1, TRACE = 2, COUNT = 3, FIND = 4, PATTERN = 5, COMPARE = 6,
            NEIGHBOR = 7, ORDER = 8, ORDINAL = 9, ADD = 10, SUB = 11, DONE = 12, CLASSIFY = 13,
            GRAPH = 14, DAYS = 15, CLOCK = 16, FRACTION = 17, LENGTH = 18, SHAPES = 19, SOLIDS = 20, TENS = 21,
            STORY = 22, BOND = 23, FACTS = 24, KIND = 25, SPATIAL = 26, HUNDRED = 27;
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
    /** A story's sum adds (or takes away). */
    private boolean storyAdd;

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
            case T_CLASSIFY:
                steps.add(CLASSIFY);
                break;
            case T_GRAPH:
                steps.add(GRAPH);
                break;
            case T_DAYS:
                steps.add(DAYS);
                break;
            case T_CLOCK:
                steps.add(CLOCK);
                break;
            case T_FRACTION:
                steps.add(FRACTION);
                break;
            case T_LENGTH:
                steps.add(LENGTH);
                break;
            case T_SHAPES:
                steps.add(SHAPES);
                break;
            case T_SOLIDS:
                steps.add(SOLIDS);
                break;
            case T_TENS:
                steps.add(TENS);
                break;
            case T_STORY:
                steps.add(STORY);
                break;
            case T_BOND:
                steps.add(BOND);
                break;
            case T_FACTS:
                steps.add(FACTS);
                break;
            case T_KIND:
                steps.add(KIND);
                break;
            case T_SPATIAL:
                steps.add(SPATIAL);
                break;
            case T_HUNDRED:
                steps.add(HUNDRED);
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

    /** A word without its vowel marks, for a card. */
    private static String plain(String s) {
        return s.replaceAll("[\\u064B-\\u0652]", "");
    }

    private static String digits(int n) {
        String s = Integer.toString(n);
        StringBuilder b = new StringBuilder();
        for (char ch : s.toCharArray()) b.append((char) ('٠' + (ch - '0')));
        return b.toString();
    }

    private static final String[] TEN_NAMES = {"", "", "عِشْرُون", "ثَلَاثُون", "أَرْبَعُون", "خَمْسُون", "سِتُّون", "سَبْعُون",
            "ثَمَانُون", "تِسْعُون"};
    private static final String[] AND_TENS = {"", "", "وَعِشْرُون", "وَثَلَاثُون", "وَأَرْبَعُون", "وَخَمْسُون", "وَسِتُّون",
            "وَسَبْعُون", "وَثَمَانُون", "وَتِسْعُون"};

    /** Lili saying a number up to a hundred: «خَمْسَة» then «وَثَلَاثُون» for ٣٥. */
    private List<String> names(int n) {
        List<String> out = new ArrayList<>();
        if (n >= 0 && n < NAMES.length) out.add(m(NAMES[n]));
        else if (n == 100) out.add(m("مِئَة"));
        else if (n > 20 && n < 100 && n % 10 == 0) out.add(m(TEN_NAMES[n / 10]));
        else if (n > 20 && n < 100) {
            out.add(m(NAMES[n % 10]));
            out.add(m(AND_TENS[n / 10]));
        }
        return out;
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

    private void play(List<String> sounds, Runnable after, boolean unused) {
        play(sounds, after);
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
        if (question.isEmpty() && kind() == CLASSIFY) {
            play(g("صَنِّفِ الْأَشْيَاء", "صَنِّفِي الْأَشْيَاء"), null);
            return;
        }
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
                for (int n : stop.nums) question.addAll(names(n));
                lead.add(m("هَيَّا نَتَعَلَّمُ عَدَدًا جَدِيدًا"));
                lead.addAll(question);
                play(lead, null);
                break;
            case TRACE:
                traceIdx = 0;
                view.clearTrace();
                question.clear();
                question.addAll(names(stop.nums[0]));
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
    /** How this stop varies its exercise (the numbers after max in its definition). */
    private int mode() {
        return stop.type != T_NUM && stop.nums.length > 0 ? stop.nums[0] : 0;
    }

    private int pickNumber() {
        int lo = stop.nums.length > 0 && stop.nums[0] == 0 ? 0 : 1;
        if (stop.nums.length > 0 && rnd.nextInt(3) > 0) return stop.nums[rnd.nextInt(stop.nums.length)];
        return lo + rnd.nextInt(stop.max - lo + 1);
    }

    /** Three different number cards, one of them {@code right}, all within 0..20. */
    private void numberOptions(int right, int lo, int hi) {
        options.clear();
        List<Integer> pool = new ArrayList<>();
        int[] near = right > 20 ? new int[]{-10, -2, -1, 1, 2, 10} : new int[]{-3, -2, -1, 1, 2, 3};
        for (int d : near) {
            int v = right + d;
            if (v >= lo && v <= hi) pool.add(v);
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
        int k = kind();
        switch (k) {
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
                question.addAll(names(qa));
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
            case CLASSIFY: {
                // One thing at a time into its basket: by its shape (stop.max 0) or its colour (1).
                rounds = 8;
                qc = stop.max;
                List<Integer> pool = new ArrayList<>();
                for (int v = 0; v < 4; v++) pool.add(v);
                Collections.shuffle(pool, rnd);
                shapeOptions.clear();
                shapeOptions.addAll(pool.subList(0, 3));
                answer = rnd.nextInt(3);
                int right = shapeOptions.get(answer);
                qa = qc == 0 ? right * 10 + rnd.nextInt(4) : rnd.nextInt(4) * 10 + right;
                options.clear();
                for (int i = 0; i < 3; i++) options.add("");
                question.add(g("صَنِّفِ الْأَشْيَاء", "صَنِّفِي الْأَشْيَاء"));
                if (!first) question.clear();
                break;
            }
            case GRAPH: {
                // A picture graph: three kinds of shape, each a row of as many as there are.
                List<Integer> kinds = new ArrayList<>();
                for (int v = 0; v < 4; v++) kinds.add(v);
                Collections.shuffle(kinds, rnd);
                List<Integer> counts = new ArrayList<>();
                for (int v = 1; v <= 7; v++) counts.add(v);
                Collections.shuffle(counts, rnd);
                pattern.clear();
                shapeOptions.clear();
                for (int i = 0; i < 3; i++) {
                    shapeOptions.add(kinds.get(i) * 10 + i);
                    pattern.add(counts.get(i));
                }
                qc = rnd.nextInt(2); // 0 most, 1 fewest
                int best = 0;
                for (int i = 1; i < 3; i++) {
                    if (qc == 0 ? pattern.get(i) > pattern.get(best) : pattern.get(i) < pattern.get(best)) best = i;
                }
                answer = best;
                options.clear();
                for (int i = 0; i < 3; i++) options.add("");
                question.add(m(qc == 0 ? "أَيُّهَا أَكْثَر؟" : "أَيُّهَا أَقَلّ؟"));
                break;
            }
            case DAYS: {
                qa = rnd.nextInt(7);
                qb = rnd.nextInt(2); // 0 the day after, 1 the day before
                int right = (qa + (qb == 0 ? 1 : 6)) % 7;
                List<Integer> pick = new ArrayList<>();
                pick.add(right);
                for (int d : new int[]{(right + 2) % 7, (right + 5) % 7, (right + 3) % 7}) {
                    if (pick.size() < 3 && d != qa && !pick.contains(d)) pick.add(d);
                }
                Collections.shuffle(pick, rnd);
                options.clear();
                shapeOptions.clear();
                for (int d : pick) {
                    options.add(plain(DAY_NAMES[d]));
                    shapeOptions.add(d);
                }
                answer = pick.indexOf(right);
                question.add(m(DAY_NAMES[qa]));
                question.add(m(qb == 0 ? "مَا الْيَوْمُ الَّذِي بَعْدَهُ؟" : "مَا الْيَوْمُ الَّذِي قَبْلَهُ؟"));
                break;
            }
            case CLOCK: {
                qa = 1 + rnd.nextInt(12);
                qb = mode() == 0 ? 0 : round < 2 ? 1 : rnd.nextInt(2); // o'clock, or half past too
                List<int[]> pick = new ArrayList<>();
                pick.add(new int[]{qa, qb});
                pick.add(new int[]{qa % 12 + 1, qb});
                pick.add(qb == 1 ? new int[]{qa, 0} : new int[]{(qa + 5) % 12 + 1, mode() == 0 ? 0 : rnd.nextInt(2)});
                List<Integer> idx = new ArrayList<>();
                for (int i = 0; i < 3; i++) idx.add(i);
                Collections.shuffle(idx, rnd);
                options.clear();
                for (int i : idx) options.add(digits(pick.get(i)[0]) + ":" + (pick.get(i)[1] == 1 ? "٣٠" : "٠٠"));
                answer = idx.indexOf(0);
                question.add(m("كَمِ السَّاعَةُ؟"));
                break;
            }
            case FRACTION: {
                qc = mode(); // 0 half, 1 quarter
                shapeOptions.clear();
                for (int v = 0; v < 3; v++) shapeOptions.add(v); // 0 half, 1 quarter, 2 not equal parts
                Collections.shuffle(shapeOptions, rnd);
                qa = rnd.nextInt(2); // a square or a circle
                options.clear();
                for (int i = 0; i < 3; i++) options.add("");
                answer = shapeOptions.indexOf(qc);
                question.add(m(qc == 0 ? "أَيْنَ النِّصْف؟" : "أَيْنَ الرُّبْع؟"));
                break;
            }
            case LENGTH: {
                qa = 3 + rnd.nextInt(7);
                do qb = 3 + rnd.nextInt(7); while (Math.abs(qb - qa) < 2);
                qc = rnd.nextInt(2); // 0 longer, 1 shorter
                options.clear();
                options.add("");
                options.add("");
                answer = (qa > qb) == (qc == 0) ? 0 : 1;
                question.add(m(qc == 0 ? "أَيُّهُمَا أَطْوَل؟" : "أَيُّهُمَا أَقْصَر؟"));
                break;
            }
            case SHAPES:
            case SOLIDS: {
                List<Integer> pool = new ArrayList<>();
                if (k == SHAPES) {
                    pool.add(0);
                    pool.add(1);
                    pool.add(2);
                    pool.add(4);
                } else {
                    for (int v = 0; v < 5; v++) pool.add(v);
                }
                Collections.shuffle(pool, rnd);
                shapeOptions.clear();
                shapeOptions.addAll(pool.subList(0, 3));
                answer = rnd.nextInt(3);
                qa = shapeOptions.get(answer);
                options.clear();
                for (int i = 0; i < 3; i++) options.add("");
                if (first) lead.add(g("اِخْتَرِ الشَّكْل", "اِخْتَارِي الشَّكْل"));
                question.add(m(k == SHAPES ? SHAPE_NAMES[qa] : SOLID_NAMES[qa]));
                break;
            }
            case TENS: {
                qa = 21 + rnd.nextInt(79);
                qb = rnd.nextInt(3); // 0 how many tens, 1 how many ones, 2 find the number
                if (qb == 2) {
                    numberOptions(qa, 10, 99);
                    if (first) lead.add(m("أَيْنَ الْعَدَد؟"));
                    question.addAll(names(qa));
                } else {
                    int right = qb == 0 ? qa / 10 : qa % 10;
                    numberOptions(right, 0, 9);
                    question.add(m(qb == 0 ? "كَمْ عَدَدُ الْعَشَرَات؟" : "كَمْ عَدَدُ الْآحَاد؟"));
                }
                break;
            }
            case STORY: {
                int md = mode(); // 0 either, 1 add to 20, 2 take away to 20, 3 add to 10
                storyAdd = md == 1 || md == 3 || (md == 0 && rnd.nextBoolean());
                qc = rnd.nextInt(4);
                if (stop.max > 10 && storyAdd) {
                    qa = 2 + rnd.nextInt(8);
                    int lo = Math.max(2, 11 - qa);
                    qb = lo + rnd.nextInt(10 - lo);
                } else if (stop.max > 10) {
                    qa = 11 + rnd.nextInt(8);
                    int lo = Math.max(2, qa - 9);
                    qb = lo + rnd.nextInt(10 - lo);
                } else if (storyAdd) {
                    qa = 1 + rnd.nextInt(8);
                    qb = 1 + rnd.nextInt(9 - qa);
                } else {
                    qa = 3 + rnd.nextInt(8);
                    qb = 1 + rnd.nextInt(qa - 1);
                }
                numberOptions(storyAdd ? qa + qb : qa - qb, 0, 20);
                question.add(m(storyAdd ? ADD_STORIES[qc] : SUB_STORIES[qc]));
                break;
            }
            case BOND: {
                // A number made of two parts: the whole on top, the parts below, one missing.
                qa = 2 + rnd.nextInt(stop.max - 1);
                qb = 1 + rnd.nextInt(qa - 1);
                qc = rnd.nextInt(4) == 0 ? 0 : 2; // which is missing: 0 the whole, 2 a part
                numberOptions(qc == 0 ? qa : qa - qb, 0, stop.max);
                question.add(m("مَا الْعَدَدُ النَّاقِص؟"));
                break;
            }
            case FACTS: {
                // One family of facts: a + b = c, b + a = c, c − a = b, c − b = a.
                if (stop.max > 10) {
                    qa = 2 + rnd.nextInt(8);
                    int lo = Math.max(2, 11 - qa);
                    qb = lo + rnd.nextInt(10 - lo);
                } else {
                    qa = 1 + rnd.nextInt(8);
                    qb = 1 + rnd.nextInt(9 - qa);
                }
                qc = round < 4 ? round : rnd.nextInt(4);
                int right = qc < 2 ? qa + qb : qc == 2 ? qb : qa;
                numberOptions(right, 0, stop.max);
                question.add(m("مَا الْعَدَدُ النَّاقِص؟"));
                break;
            }
            case KIND: {
                rounds = 8;
                qa = rnd.nextInt(3);
                qb = rnd.nextInt(KIND_PICS[qa].length);
                shapeOptions.clear();
                for (int v = 0; v < 3; v++) shapeOptions.add(v);
                Collections.shuffle(shapeOptions, rnd);
                options.clear();
                for (int v : shapeOptions) options.add(plain(KIND_NAMES[v]));
                answer = shapeOptions.indexOf(qa);
                if (first) question.add(g("صَنِّفْ حَسَبَ النَّوْع", "صَنِّفِي حَسَبَ النَّوْع"));
                break;
            }
            case SPATIAL: {
                qa = rnd.nextInt(PLACES.length);
                List<Integer> pool = new ArrayList<>();
                for (int v = 0; v < PLACES.length; v++) if (v != qa) pool.add(v);
                Collections.shuffle(pool, rnd);
                // Its opposite is always one of the others: above/under, right/left…
                int opposite = qa ^ 1;
                pool.remove(Integer.valueOf(opposite));
                shapeOptions.clear();
                shapeOptions.add(qa);
                shapeOptions.add(opposite);
                shapeOptions.add(pool.get(0));
                Collections.shuffle(shapeOptions, rnd);
                options.clear();
                for (int v : shapeOptions) options.add(plain(PLACES[v]));
                answer = shapeOptions.indexOf(qa);
                question.add(m("أَيْنَ الْكُرَةُ؟"));
                break;
            }
            case HUNDRED: {
                // The hundred board: counting in tens, fives or twos, one number hidden.
                qb = mode() == 2 ? 2 : (round % 2 == 0 ? 10 : 5);
                int count = qb == 2 ? 20 : 100 / qb;
                qa = qb * (2 + rnd.nextInt(count - 2));
                numberOptions(qa, 1, 100);
                question.add(m(qb == 2 ? "نَعُدُّ بِالِاثْنَيْن" : qb == 5 ? "نَعُدُّ بِالْخَمْسَات" : "نَعُدُّ بِالْعَشَرَات"));
                question.add(m("مَا الْعَدَدُ النَّاقِص؟"));
                break;
            }
            case ORDINAL:
                qa = rnd.nextInt(10);
                options.clear();
                answer = qa;
                question.add(m("مَنِ الَّذِي فِي الْمَكَانِ"));
                question.add(m(ORDINALS[qa]));
                break;
            case ADD:
                if (mode() == 1) {
                    // Adding zero, and a number to itself.
                    qa = rnd.nextInt(6);
                    qb = rnd.nextBoolean() ? 0 : qa;
                    if (rnd.nextBoolean()) {
                        int t = qa;
                        qa = qb;
                        qb = t;
                    }
                } else if (mode() == 2) {
                    // Doubles and one more or one less: ٦ + ٦, ٦ + ٧.
                    qa = 1 + rnd.nextInt(9);
                    qb = Math.max(1, Math.min(10, qa + rnd.nextInt(3) - 1));
                } else if (mode() == 3) {
                    // Counting on 1, 2 or 3 along the number line.
                    qa = rnd.nextInt(17);
                    qb = 1 + rnd.nextInt(3);
                } else if (mode() == 4) {
                    qa = 1 + rnd.nextInt(14);
                    qb = 1 + rnd.nextInt(Math.min(9, 20 - qa));
                } else if (mode() == 5) {
                    // Whole tens: ٣٠ + ٢٠.
                    qa = 10 * (1 + rnd.nextInt(8));
                    qb = 10 * (1 + rnd.nextInt(9 - qa / 10));
                } else if (stop.max > 20) {
                    // Chapter 9: no carrying — the ones add up to nine at most.
                    qa = 10 + rnd.nextInt(80);
                    int bt = rnd.nextInt(10 - qa / 10), bo = rnd.nextInt(10 - qa % 10);
                    if (bt == 0 && bo == 0) bo = Math.min(1, 9 - qa % 10);
                    if (bt == 0 && bo == 0) bt = Math.min(1, 9 - qa / 10);
                    qb = bt * 10 + bo;
                } else if (stop.max > 10) {
                    // The book's chapter 5: two one-digit numbers that make a teen, like ٨ + ٥.
                    qa = 2 + rnd.nextInt(8);
                    int lo = Math.max(2, 11 - qa);
                    qb = lo + rnd.nextInt(10 - lo);
                } else {
                    qa = 1 + rnd.nextInt(stop.max - 1);
                    qb = 1 + rnd.nextInt(stop.max - qa);
                }
                numberOptions(qa + qb, 0, stop.max);
                if (first) lead.add(m("هَيَّا نَجْمَعُ"));
                question.addAll(names(qa));
                question.add(m("زَائِد"));
                question.addAll(names(qb));
                question.add(m("كَمِ الْمَجْمُوعُ؟"));
                break;
            case SUB:
                if (mode() == 1) {
                    // Taking away nothing, and taking away all.
                    qa = 1 + rnd.nextInt(10);
                    qb = rnd.nextBoolean() ? 0 : qa;
                } else if (mode() == 2) {
                    // Counting back 1, 2 or 3 along the number line.
                    qa = 3 + rnd.nextInt(18);
                    qb = 1 + rnd.nextInt(3);
                } else if (mode() == 3) {
                    qa = 5 + rnd.nextInt(16);
                    qb = 1 + rnd.nextInt(Math.min(9, qa));
                } else if (mode() == 5) {
                    qa = 10 * (2 + rnd.nextInt(8));
                    qb = 10 * (1 + rnd.nextInt(qa / 10 - 1));
                } else if (stop.max > 20) {
                    // Chapter 9: no borrowing — each digit taken from a bigger or equal one.
                    qa = 21 + rnd.nextInt(79);
                    qb = rnd.nextInt(qa / 10 + 1) * 10 + rnd.nextInt(qa % 10 + 1);
                    if (qb == 0) qb = qa % 10 > 0 ? 1 : 10;
                } else if (stop.max > 10) {
                    // Chapter 6: a teen take away one digit, leaving one digit, like ١٤ − ٦.
                    qa = 11 + rnd.nextInt(8);
                    int lo = Math.max(2, qa - 9);
                    qb = lo + rnd.nextInt(10 - lo);
                } else {
                    qa = 2 + rnd.nextInt(stop.max - 1);
                    qb = 1 + rnd.nextInt(qa - 1);
                }
                numberOptions(qa - qb, 0, stop.max);
                if (first) lead.add(mode() == 2 ? m("نَعُدُّ إِلَى الْوَرَاء") : g("اِطْرَحْ", "اِطْرَحِي"));
                question.addAll(names(qa));
                question.add(m("نَاقِص"));
                question.addAll(names(qb));
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
            if (k == COUNT || k == FIND) after.addAll(names(qa));
            else if (k == NEIGHBOR) after.addAll(names(qa + qb));
            else if (k == ADD || k == SUB) {
                after.addAll(names(qa));
                after.add(m(k == ADD ? "زَائِد" : "نَاقِص"));
                after.addAll(names(qb));
                after.add(m("يُسَاوِي"));
                after.addAll(names(k == ADD ? qa + qb : qa - qb));
            } else if (k == COMPARE) after.addAll(names(i == 0 ? qa : qb));
            else if (k == DAYS) after.add(m(DAY_NAMES[shapeOptions.get(i)]));
            else if (k == CLOCK) {
                after.add(m("السَّاعَةُ"));
                after.add(m(HOURS[qa]));
                if (qb == 1) after.add(m("وَالنِّصْف"));
            } else if (k == SHAPES) after.add(m(SHAPE_NAMES[qa]));
            else if (k == SOLIDS) after.add(m(SOLID_NAMES[qa]));
            else if (k == TENS) after.addAll(names(qb == 2 ? qa : qb == 0 ? qa / 10 : qa % 10));
            else if (k == STORY) after.addAll(sum(qa, storyAdd, qb, storyAdd ? qa + qb : qa - qb));
            else if (k == BOND) after.addAll(sum(qb, true, qa - qb, qa));
            else if (k == FACTS) {
                if (qc == 0) after.addAll(sum(qa, true, qb, qa + qb));
                else if (qc == 1) after.addAll(sum(qb, true, qa, qa + qb));
                else if (qc == 2) after.addAll(sum(qa + qb, false, qa, qb));
                else after.addAll(sum(qa + qb, false, qb, qa));
            } else if (k == KIND) after.add(m(KIND_NAMES[qa]));
            else if (k == SPATIAL) after.add(m(PLACES[qa]));
            else if (k == HUNDRED) after.addAll(names(qa));
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

    /** «ثَلَاثَة زَائِد اِثْنَان يُسَاوِي خَمْسَة». */
    private List<String> sum(int a, boolean plus, int b, int c) {
        List<String> out = new ArrayList<>(names(a));
        out.add(m(plus ? "زَائِد" : "نَاقِص"));
        out.addAll(names(b));
        out.add(m("يُسَاوِي"));
        out.addAll(names(c));
        return out;
    }

    /** Four (or five) different numbers to put from the smallest to the biggest. */
    private void newOrder(boolean first) {
        busy = false;
        wrongPick = -1;
        int n = stop.max > 20 ? 4 : stop.max > 9 ? 5 : 4;
        List<Integer> all = new ArrayList<>();
        int lo = stop.max > 20 ? 10 : stop.max > 9 && rnd.nextBoolean() ? 6 : 0;
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
                for (int v : order) after.addAll(names(v));
                after.add(cheer(book.praise));
                play(after, () -> {
                    round++;
                    if (round < rounds) newOrder(false);
                    else nextStep();
                });
            } else {
                play(names(cards.get(i)), null);
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
        after.addAll(names(stop.nums[traceIdx]));
        after.add(cheer(book.praise));
        busy = true;
        play(after, () -> {
            busy = false;
            if (traceIdx + 1 < stop.nums.length) {
                traceIdx++;
                view.clearTrace();
                question.clear();
                question.addAll(names(stop.nums[traceIdx]));
                ask();
            } else {
                nextStep();
            }
        });
    }

    // ---- Screen ------------------------------------------------------------------------------

    /** Red, blue, green, purple, and grey for the baskets that sort by shape. */
    private static final int[] COLOURS = {0xFFFF6B6B, Draw.BLUE_D, Draw.MINT_D, Draw.LILAC_D, 0xFFB8BCCB};

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
                    question.addAll(names(stop.nums[traceIdx]));
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
                case 4:
                    c.drawRoundRect(new RectF(cx - r * 1.2f, cy - r * 0.65f, cx + r * 1.2f, cy + r * 0.65f), r * 0.15f, r * 0.15f, Draw.fill(colour));
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
            if (n > 20 || (stop.max > 20 && n >= 10)) {
                blocks(c, n, l, t, r, b);
                return;
            }
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

        private final Paint emoji = new Paint(Paint.ANTI_ALIAS_FLAG);

        /** A picture from the phone's emoji font, centred. */
        private void pic(Canvas c, String e, float cx, float cy, float size, int alpha) {
            emoji.setTextAlign(Paint.Align.CENTER);
            emoji.setTextSize(size);
            emoji.setAlpha(alpha);
            c.drawText(e, cx, cy - (emoji.ascent() + emoji.descent()) / 2f, emoji);
            emoji.setAlpha(255);
        }

        /** {@code n} pictures in rows of five, the last {@code crossed} faded and crossed out. */
        private void pics(Canvas c, String e, int n, float l, float t, float r, float b, int crossed, int sizeFor) {
            if (n <= 0) return;
            int sc = Math.max(n, sizeFor);
            float cell = Math.min(Math.min((r - l) / Math.min(5, sc), (b - t) / ((sc + 4) / 5)), 80 * dp);
            int cols = Math.min(5, n), rows = (n + 4) / 5;
            float x0 = (l + r) / 2 + cols * cell / 2, y0 = (t + b) / 2 - rows * cell / 2;
            for (int i = 0; i < n; i++) {
                float cx = x0 - (i % 5 + 0.5f) * cell, cy = y0 + (i / 5 + 0.5f) * cell;
                boolean gone = i >= n - crossed;
                pic(c, e, cx, cy, cell * 0.78f, gone ? 90 : 255);
                if (gone) {
                    float rad = cell * 0.36f;
                    c.drawLine(cx - rad, cy - rad, cx + rad, cy + rad, pen(0xFF5B3E8C, rad * 0.2f));
                    c.drawLine(cx + rad, cy - rad, cx - rad, cy + rad, pen(0xFF5B3E8C, rad * 0.2f));
                }
            }
        }

        /**
         * A number line from {@code lo} to {@code hi}, growing to the left as Arabic is read, with
         * hops from {@code from} by {@code hops} (to the left when adding, to the right when taking away).
         */
        private void numberLine(Canvas c, int lo, int hi, int from, int hops, boolean add, float l, float r, float y) {
            int n = hi - lo;
            float stepW = (r - l - 20 * dp) / n;
            float x0 = r - 10 * dp;
            c.drawLine(l, y, r, y, pen(Draw.PLUM, 3 * dp));
            for (int v = lo; v <= hi; v++) {
                float x = x0 - (v - lo) * stepW;
                c.drawLine(x, y - 6 * dp, x, y + 6 * dp, pen(Draw.PLUM, 2 * dp));
                boolean mark = v == from || (rightPick >= 0 && v == from + (add ? hops : -hops));
                label(c, digits(v), x, y + 24 * dp, mark ? 24 * dp : 18 * dp, stepW * 0.9f, mark ? Draw.PINK_D : 0xFF7A7F9A);
            }
            float fx = x0 - (from - lo) * stepW;
            c.drawCircle(fx, y, 7 * dp, Draw.fill(Draw.PINK_D));
            for (int i = 0; i < hops; i++) {
                float a = fx + (add ? -i : i) * stepW, b = fx + (add ? -(i + 1) : i + 1) * stepW;
                RectF arc = new RectF(Math.min(a, b), y - stepW * 0.9f, Math.max(a, b), y + stepW * 0.9f);
                c.drawArc(arc, 180, 180, false, pen(Draw.BLUE_D, 3 * dp));
            }
            float ex = fx + (add ? -hops : hops) * stepW;
            if (rightPick < 0) label(c, "؟", ex, y - stepW * 1.3f - 8 * dp, 22 * dp, 40 * dp, Draw.BLUE_D);
            else c.drawCircle(ex, y, 7 * dp, Draw.fill(Draw.MINT_D));
        }

        /** Tens as rods of ten little cubes, then the ones, as the book shows numbers to 99. */
        private void blocks(Canvas c, int n, float l, float t, float r, float b) {
            int tens = n / 10, ones = n % 10;
            int cols = tens + (ones > 0 ? 1 : 0);
            float cell = Math.min((b - t) / 10.5f, (r - l) / (cols * 1.6f + 0.5f));
            float total = cols * cell * 1.6f;
            float x = (l + r) / 2 + total / 2 - cell * 1.3f, y0 = (t + b) / 2 - 5 * cell;
            for (int i = 0; i < tens; i++) {
                for (int j = 0; j < 10; j++) cube(c, x, y0 + j * cell, cell, Draw.BLUE_D);
                x -= cell * 1.6f;
            }
            for (int j = 0; j < ones; j++) cube(c, x, y0 + (9 - j) * cell, cell, 0xFFFF6B6B);
        }

        private void cube(Canvas c, float x, float y, float s, int colour) {
            c.drawRect(x, y, x + s, y + s, Draw.fill(colour));
            c.drawRect(x, y, x + s, y + s, Draw.stroke(0x55FFFFFF, Math.max(1, s * 0.08f)));
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

        /** The book's solids: cube, cylinder, ball, cone and box. */
        private void solid(Canvas c, int kind, float cx, float cy, float r) {
            int front = Draw.BLUE, top = 0xFFB6DDF7, side = Draw.BLUE_D;
            Paint edge = pen(0xFF2F5D86, r * 0.05f);
            switch (kind) {
                case 0:
                case 4: {
                    float w = kind == 0 ? r * 1.2f : r * 1.7f, h = kind == 0 ? r * 1.2f : r * 0.9f, d = r * 0.45f;
                    float l = cx - w / 2 - d / 2, t = cy - h / 2 + d / 2;
                    Path tp = new Path();
                    tp.moveTo(l, t);
                    tp.lineTo(l + d, t - d);
                    tp.lineTo(l + w + d, t - d);
                    tp.lineTo(l + w, t);
                    tp.close();
                    Path sp = new Path();
                    sp.moveTo(l + w, t);
                    sp.lineTo(l + w + d, t - d);
                    sp.lineTo(l + w + d, t + h - d);
                    sp.lineTo(l + w, t + h);
                    sp.close();
                    c.drawPath(tp, Draw.fill(top));
                    c.drawPath(sp, Draw.fill(side));
                    c.drawRect(l, t, l + w, t + h, Draw.fill(front));
                    c.drawPath(tp, edge);
                    c.drawPath(sp, edge);
                    c.drawRect(l, t, l + w, t + h, edge);
                    break;
                }
                case 1: {
                    float w = r * 0.8f, h = r * 1.5f, e = r * 0.28f;
                    c.drawRect(cx - w, cy - h / 2, cx + w, cy + h / 2, Draw.fill(front));
                    c.drawOval(new RectF(cx - w, cy + h / 2 - e, cx + w, cy + h / 2 + e), Draw.fill(front));
                    c.drawOval(new RectF(cx - w, cy - h / 2 - e, cx + w, cy - h / 2 + e), Draw.fill(top));
                    c.drawOval(new RectF(cx - w, cy - h / 2 - e, cx + w, cy - h / 2 + e), edge);
                    c.drawLine(cx - w, cy - h / 2, cx - w, cy + h / 2, edge);
                    c.drawLine(cx + w, cy - h / 2, cx + w, cy + h / 2, edge);
                    c.drawArc(new RectF(cx - w, cy + h / 2 - e, cx + w, cy + h / 2 + e), 0, 180, false, edge);
                    break;
                }
                case 2:
                    c.drawCircle(cx, cy, r, Draw.fill(front));
                    c.drawCircle(cx - r * 0.35f, cy - r * 0.35f, r * 0.25f, Draw.fill(0x88FFFFFF));
                    c.drawArc(new RectF(cx - r, cy - r * 0.25f, cx + r, cy + r * 0.25f), 0, 180, false, edge);
                    c.drawCircle(cx, cy, r, edge);
                    break;
                default: {
                    float w = r * 0.85f, e = r * 0.25f, base = cy + r * 0.75f;
                    Path cp = new Path();
                    cp.moveTo(cx, cy - r);
                    cp.lineTo(cx + w, base);
                    cp.lineTo(cx - w, base);
                    cp.close();
                    c.drawOval(new RectF(cx - w, base - e, cx + w, base + e), Draw.fill(side));
                    c.drawPath(cp, Draw.fill(front));
                    c.drawArc(new RectF(cx - w, base - e, cx + w, base + e), 0, 180, false, edge);
                    c.drawLine(cx, cy - r, cx + w, base, edge);
                    c.drawLine(cx, cy - r, cx - w, base, edge);
                }
            }
        }

        /** A square or circle cut in two halves, four quarters, or two parts that are not equal. */
        private void fraction(Canvas c, int kind, boolean round, float cx, float cy, float r) {
            Paint line = pen(Draw.PLUM, Math.max(2, r * 0.06f));
            RectF box = new RectF(cx - r, cy - r, cx + r, cy + r);
            int fillC = 0xFFFFB347;
            if (round) {
                c.drawCircle(cx, cy, r, Draw.fill(0xFFFFFFFF));
                if (kind == 0) c.drawArc(box, 90, 180, true, Draw.fill(fillC));
                else if (kind == 1) c.drawArc(box, 180, 90, true, Draw.fill(fillC));
                else c.drawArc(box, 120, 120, false, Draw.fill(fillC));
                c.drawCircle(cx, cy, r, line);
                if (kind == 0) c.drawLine(cx, cy - r, cx, cy + r, line);
                else if (kind == 1) {
                    c.drawLine(cx, cy - r, cx, cy + r, line);
                    c.drawLine(cx - r, cy, cx + r, cy, line);
                } else c.drawLine(cx - r * 0.5f, cy - r * 0.866f, cx - r * 0.5f, cy + r * 0.866f, line);
            } else {
                c.drawRect(box, Draw.fill(0xFFFFFFFF));
                if (kind == 0) c.drawRect(cx - r, cy - r, cx, cy + r, Draw.fill(fillC));
                else if (kind == 1) c.drawRect(cx - r, cy - r, cx, cy, Draw.fill(fillC));
                else c.drawRect(cx - r, cy - r, cx - r * 0.45f, cy + r, Draw.fill(fillC));
                c.drawRect(box, line);
                if (kind == 0) c.drawLine(cx, cy - r, cx, cy + r, line);
                else if (kind == 1) {
                    c.drawLine(cx, cy - r, cx, cy + r, line);
                    c.drawLine(cx - r, cy, cx + r, cy, line);
                } else c.drawLine(cx - r * 0.45f, cy - r, cx - r * 0.45f, cy + r, line);
            }
        }

        /** A ball and a box (or a table): the ball above, under, right, left, in, out, in front or behind. */
        private void spatial(Canvas c, int where, float l, float t, float r, float b) {
            float cx = (l + r) / 2, cy = (t + b) / 2;
            float s = Math.min(r - l, b - t) * 0.22f;
            float br = s * 0.42f;
            int ballC = 0xFFFF6B6B;
            if (where <= 1) {
                // A table: the ball on it or under it.
                float top = cy - s * 0.2f;
                c.drawRect(cx - s * 1.6f, top, cx + s * 1.6f, top + s * 0.22f, Draw.fill(0xFFB07A4A));
                c.drawRect(cx - s * 1.45f, top, cx - s * 1.25f, top + s * 1.5f, Draw.fill(0xFF8A5A30));
                c.drawRect(cx + s * 1.25f, top, cx + s * 1.45f, top + s * 1.5f, Draw.fill(0xFF8A5A30));
                float by = where == 0 ? top - br : top + s * 1.5f - br;
                ball(c, cx, by, br, ballC);
                return;
            }
            // A box, open at the top.
            float bl = cx - s, bt = cy - s * 0.6f, bw = s * 2, bh = s * 1.3f;
            if (where == 7) ball(c, cx + s * 0.4f, bt - br * 0.2f, br, ballC); // behind: half hidden
            if (where == 4) ball(c, cx, bt + br * 0.6f, br, ballC);
            c.drawRect(bl, bt, bl + bw, bt + bh, Draw.fill(0xFFE8B86B));
            c.drawRect(bl, bt, bl + bw, bt + bh * 0.18f, Draw.fill(0xFFD49A45));
            c.drawRect(bl, bt, bl + bw, bt + bh, pen(0xFF8A5A30, 2.5f * dp));
            if (where == 2) ball(c, bl + bw + br * 1.6f, bt + bh - br, br, ballC);
            if (where == 3) ball(c, bl - br * 1.6f, bt + bh - br, br, ballC);
            if (where == 5) ball(c, bl + bw + br * 2.6f, bt + bh - br, br, ballC);
            if (where == 6) ball(c, cx, bt + bh - br * 0.2f, br, ballC);
        }

        private void ball(Canvas c, float x, float y, float r, int colour) {
            c.drawCircle(x, y, r, Draw.fill(colour));
            c.drawCircle(x - r * 0.35f, y - r * 0.35f, r * 0.25f, Draw.fill(0x88FFFFFF));
            c.drawArc(new RectF(x - r, y - r * 0.3f, x + r, y + r * 0.3f), 0, 180, false, pen(0x88FFFFFF, r * 0.1f));
        }

        /** A pencil {@code len} long (out of nine), pointing left. */
        private void pencil(Canvas c, float cx, float cy, float maxW, int len, int colour) {
            float w = maxW * len / 9f, h = Math.max(10 * dp, maxW * 0.09f);
            float l = cx - w / 2, r = cx + w / 2;
            float tip = h * 1.2f;
            c.drawRect(l + tip, cy - h / 2, r, cy + h / 2, Draw.fill(colour));
            c.drawRect(r - h * 0.6f, cy - h / 2, r, cy + h / 2, Draw.fill(0xFFFFB3C6));
            Path p = new Path();
            p.moveTo(l + tip, cy - h / 2);
            p.lineTo(l, cy);
            p.lineTo(l + tip, cy + h / 2);
            p.close();
            c.drawPath(p, Draw.fill(0xFFF3D7A6));
            c.drawCircle(l + tip * 0.15f, cy, h * 0.12f, Draw.fill(Draw.PLUM));
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
            int kind = kind();
            boolean pictures = kind == FRACTION || kind == SHAPES || kind == SOLIDS;
            boolean stacked = kind == LENGTH;
            float cw = stacked ? Math.min(w - 2 * pad, 420 * dp) : Math.min(pictures ? 220 * dp : 170 * dp, (w - pad * (n + 1)) / n);
            float ch = stacked ? (bottom - top - pad) / n
                    : Math.min(bottom - top, pictures ? cw * 1.15f : Math.max(90 * dp, cw * 0.8f));
            float x0 = (w - (n * cw + (n - 1) * pad)) / 2;
            float top0 = top;
            float wob = wobble();
            for (int i = 0; i < n; i++) {
                int col = n - 1 - i; // right to left
                float l = stacked ? (w - cw) / 2 : x0 + col * (cw + pad), r = l + cw;
                top = stacked ? top0 + i * (ch + pad) : top0;
                int fill = i == rightPick ? Draw.MINT : i == wrongPick ? 0xFFFFB3B3 : Draw.LILAC;
                int deep = i == rightPick ? Draw.MINT_D : i == wrongPick ? 0xFFE07A7A : Draw.LILAC_D;
                c.save();
                if (i == wrongPick) c.translate(wob, 0);
                button(c, l, top, r, top + ch, fill, deep, i);
                int kk = kind();
                float ox = (l + r) / 2, oy = top + ch / 2 - 3 * dp, orr = Math.min(cw * 0.36f, ch * 0.32f);
                if (kk == GRAPH || kk == SHAPES) {
                    c.drawCircle(ox, oy, orr * 1.45f, Draw.fill(0xFFFFFFFF));
                    shape(c, kk == GRAPH ? shapeOptions.get(i) : shapeOptions.get(i) * 10 + 1, ox, oy, orr);
                } else if (kk == SOLIDS) {
                    c.drawCircle(ox, oy, orr * 1.5f, Draw.fill(0xFFFFFFFF));
                    solid(c, shapeOptions.get(i), ox, oy, orr);
                } else if (kk == FRACTION) {
                    fraction(c, shapeOptions.get(i), qa == 1, ox, oy, orr);
                } else if (kk == LENGTH) {
                    c.drawRoundRect(new RectF(l + 10 * dp, top + 10 * dp, r - 10 * dp, top + ch - 18 * dp), 14 * dp, 14 * dp, Draw.fill(0xFFFFFFFF));
                    pencil(c, ox, oy, cw - 40 * dp, i == 0 ? qa : qb, i == 0 ? 0xFFFF6B6B : Draw.BLUE_D);
                } else if (kk == KIND) {
                    pic(c, KIND_PICS[shapeOptions.get(i)][0], ox, top + ch * 0.36f, ch * 0.34f, 255);
                    label(c, options.get(i), ox, top + ch * 0.74f, ch * 0.2f, cw * 0.85f, 0xFFFFFFFF);
                } else if (kind() == CLASSIFY) {
                    float rr = Math.min(cw, ch) * 0.28f;
                    float bx = (l + r) / 2, by = top + ch / 2 - 3 * dp;
                    c.drawCircle(bx, by, rr * 1.45f, Draw.fill(0xFFFFFFFF));
                    if (qc == 0) shape(c, shapeOptions.get(i) * 10 + 4, bx, by, rr);
                    else c.drawCircle(bx, by, rr * 1.1f, Draw.fill(COLOURS[shapeOptions.get(i)]));
                } else if (kind() == PATTERN) {
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
                    things(c, v, l, cardB + pad, r, areaB, COLOURS[i % 4], 0);
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
                boolean pictures = k == FRACTION || k == LENGTH || k == SHAPES || k == SOLIDS;
                float optH = k == ORDINAL ? 0 : pictures ? h * (side ? 0.55f : 0.45f)
                        : Math.min(130 * dp, Math.max(90 * dp, h * 0.16f));
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
                    things(c, qa, l + 18 * dp, t + 18 * dp, r - 18 * dp, b - 26 * dp, COLOURS[qa % 4], 0);
                    break;
                case CLASSIFY: {
                    Draw.card(c, l, t, r, b - 8 * dp, 28 * dp, 0xFFFFFFFF, 0xFFE6DCCB, 8 * dp);
                    float rr = Math.min(r - l, b - t) * 0.2f;
                    if (rightPick < 0) shape(c, qa, cx, (t + b) / 2 - 4 * dp, rr);
                    label(c, qc == 0 ? "حَسَبَ الشَّكْل" : "حَسَبَ اللَّوْن", cx, t + 26 * dp, 22 * dp, r - l - 24 * dp, Draw.PLUM);
                    break;
                }
                case STORY: {
                    Draw.card(c, l, t, r, b - 8 * dp, 28 * dp, 0xFFFFFFFF, 0xFFE6DCCB, 8 * dp);
                    String e = storyAdd ? ADD_PICS[qc] : SUB_PICS[qc];
                    float inT = t + 14 * dp, inB = b - 24 * dp;
                    if (storyAdd) {
                        float mid = (l + r) / 2;
                        pics(c, e, qa, mid + 20 * dp, inT, r - 12 * dp, inB, 0, Math.max(qa, qb));
                        label(c, "+", mid, (inT + inB) / 2, 36 * dp, 36 * dp, Draw.PLUM);
                        pics(c, e, qb, l + 12 * dp, inT, mid - 20 * dp, inB, 0, Math.max(qa, qb));
                    } else {
                        pics(c, e, qa, l + 12 * dp, inT, r - 12 * dp, inB, qb, qa);
                    }
                    break;
                }
                case BOND: {
                    Draw.card(c, l, t, r, b - 8 * dp, 28 * dp, 0xFFFFFFFF, 0xFFE6DCCB, 8 * dp);
                    float rr = Math.min((r - l) / 6, (b - t) / 5.5f);
                    float topY = t + 16 * dp + rr, lowY = b - 24 * dp - rr;
                    float px = (r - l) / 4;
                    c.drawLine(cx, topY, cx + px, lowY, pen(0xFFC9CDE0, 4 * dp));
                    c.drawLine(cx, topY, cx - px, lowY, pen(0xFFC9CDE0, 4 * dp));
                    int[] vals = {qa, qb, qa - qb};
                    float[][] at = {{cx, topY}, {cx + px, lowY}, {cx - px, lowY}};
                    for (int i = 0; i < 3; i++) {
                        boolean hidden = rightPick < 0 && i == qc;
                        int fill = i == 0 ? Draw.YELLOW : COLOURS[i - 1];
                        if (hidden) {
                            c.drawCircle(at[i][0], at[i][1], rr, Draw.fill(0xFFFFFFFF));
                            c.drawCircle(at[i][0], at[i][1], rr, pen(Draw.YELLOW_D, 4 * dp));
                            label(c, "؟", at[i][0], at[i][1], rr, rr * 1.4f, Draw.YELLOW_D);
                        } else {
                            c.drawCircle(at[i][0], at[i][1], rr, Draw.fill(fill));
                            label(c, digits(vals[i]), at[i][0], at[i][1] - 2 * dp, rr, rr * 1.4f, 0xFFFFFFFF);
                        }
                    }
                    break;
                }
                case FACTS: {
                    float eqH = Math.min(70 * dp, (b - t) * 0.3f);
                    Draw.card(c, l, t, r, b - eqH - 8 * dp, 28 * dp, 0xFFFFFFFF, 0xFFE6DCCB, 8 * dp);
                    // The family's three numbers in a little house of facts.
                    float rr = Math.min((r - l) / 8, (b - eqH - t) / 5);
                    float topY = t + 20 * dp + rr, lowY = b - eqH - 24 * dp - rr;
                    int[] vals = {qa + qb, qa, qb};
                    float[][] at = {{cx, topY}, {cx + rr * 1.6f, lowY}, {cx - rr * 1.6f, lowY}};
                    for (int i = 0; i < 3; i++) {
                        c.drawCircle(at[i][0], at[i][1], rr, Draw.fill(i == 0 ? Draw.YELLOW : COLOURS[i]));
                        label(c, digits(vals[i]), at[i][0], at[i][1] - 2 * dp, rr, rr * 1.4f, 0xFFFFFFFF);
                    }
                    int x1 = qc == 0 ? qa : qc == 1 ? qb : qa + qb, x2 = qc == 0 ? qb : qc == 1 ? qa : qc == 2 ? qa : qb;
                    int res = qc < 2 ? qa + qb : qc == 2 ? qb : qa;
                    String eq = digits(x1) + (qc < 2 ? " + " : " − ") + digits(x2) + " = " + (rightPick >= 0 ? digits(res) : "؟");
                    label(c, "\u200F" + eq, cx, b - eqH / 2, eqH * 0.7f, r - l, Draw.PLUM);
                    break;
                }
                case KIND: {
                    Draw.card(c, l, t, r, b - 8 * dp, 28 * dp, 0xFFFFFFFF, 0xFFE6DCCB, 8 * dp);
                    label(c, "حَسَبَ النَّوْع", cx, t + 26 * dp, 22 * dp, r - l - 24 * dp, Draw.PLUM);
                    if (rightPick < 0) pic(c, KIND_PICS[qa][qb], cx, (t + b) / 2, Math.min(r - l, b - t) * 0.42f, 255);
                    break;
                }
                case SPATIAL: {
                    Draw.card(c, l, t, r, b - 8 * dp, 28 * dp, 0xFFFFFFFF, 0xFFE6DCCB, 8 * dp);
                    spatial(c, qa, l, t, r, b - 8 * dp);
                    break;
                }
                case HUNDRED: {
                    Draw.card(c, l, t, r, b - 8 * dp, 20 * dp, 0xFFFFFFFF, 0xFFE6DCCB, 6 * dp);
                    int rows = qb == 2 ? 4 : 10;
                    float cell = Math.min((r - l - 16 * dp) / 10, (b - t - 24 * dp) / rows);
                    float gx = cx + 5 * cell, gy = (t + b) / 2 - rows * cell / 2 - 4 * dp;
                    for (int v = 1; v <= rows * 10; v++) {
                        int row = (v - 1) / 10, col = (v - 1) % 10;
                        float x = gx - (col + 1) * cell, y = gy + row * cell;
                        boolean on = v % qb == 0;
                        boolean hidden = v == qa && rightPick < 0;
                        RectF box = new RectF(x + 1.5f, y + 1.5f, x + cell - 1.5f, y + cell - 1.5f);
                        c.drawRoundRect(box, 4 * dp, 4 * dp, Draw.fill(hidden ? 0xFFFFFFFF : on ? Draw.YELLOW : 0xFFF4EEE4));
                        if (hidden) {
                            c.drawRoundRect(box, 4 * dp, 4 * dp, pen(Draw.PINK_D, 2.5f * dp));
                            label(c, "؟", x + cell / 2, y + cell / 2, cell * 0.6f, cell * 0.8f, Draw.PINK_D);
                        } else {
                            label(c, digits(v), x + cell / 2, y + cell / 2, cell * 0.42f, cell * 0.85f,
                                    on ? 0xFFFFFFFF : 0xFF9AA0BC);
                        }
                    }
                    break;
                }
                case GRAPH: {
                    Draw.card(c, l, t, r, b - 8 * dp, 28 * dp, 0xFFFFFFFF, 0xFFE6DCCB, 8 * dp);
                    float rowH = (b - t - 30 * dp) / 3, cell = Math.min(rowH * 0.8f, (r - l - 40 * dp) / 8.5f);
                    for (int i = 0; i < 3; i++) {
                        float y = t + 15 * dp + rowH * (i + 0.5f);
                        float x = r - 20 * dp - cell * 0.6f;
                        shape(c, shapeOptions.get(i), x, y, cell * 0.36f);
                        c.drawLine(x - cell * 0.7f, y - rowH * 0.4f, x - cell * 0.7f, y + rowH * 0.4f, Draw.stroke(0xFFC9CDE0, 2 * dp));
                        for (int j = 0; j < pattern.get(i); j++) {
                            shape(c, shapeOptions.get(i), x - cell * (1.4f + j), y, cell * 0.36f);
                        }
                    }
                    break;
                }
                case DAYS: {
                    float s = Math.min(r - l, (b - t) * 1.6f);
                    float ct = (t + b) / 2 - s * 0.25f, cb = (t + b) / 2 + s * 0.25f;
                    Draw.card(c, cx - s / 2, ct, cx + s / 2, cb - 8 * dp, 28 * dp, Draw.YELLOW, Draw.YELLOW_D, 8 * dp);
                    label(c, plain(DAY_NAMES[qa]), cx, (ct + cb) / 2 - 4 * dp, (cb - ct) * 0.4f, s * 0.8f, 0xFFFFFFFF);
                    label(c, qb == 0 ? "← بَعْدَهُ ؟" : "قَبْلَهُ ؟ →", cx, cb + 22 * dp, 22 * dp, r - l, Draw.PLUM);
                    break;
                }
                case CLOCK: {
                    float rr = Math.min(r - l, b - t) * 0.42f, cy = (t + b) / 2;
                    c.drawCircle(cx, cy + 4 * dp, rr, Draw.fill(0x22000000));
                    c.drawCircle(cx, cy, rr, Draw.fill(0xFFFFFFFF));
                    c.drawCircle(cx, cy, rr, Draw.stroke(Draw.PINK_D, rr * 0.08f));
                    for (int hh = 1; hh <= 12; hh++) {
                        double a = Math.PI * 2 * hh / 12 - Math.PI / 2;
                        label(c, digits(hh), cx + (float) Math.cos(a) * rr * 0.78f, cy + (float) Math.sin(a) * rr * 0.78f,
                                rr * 0.17f, rr * 0.3f, Draw.PLUM);
                    }
                    double ha = Math.PI * 2 * ((qa % 12) + qb * 0.5) / 12 - Math.PI / 2;
                    double ma = qb == 1 ? Math.PI / 2 : -Math.PI / 2;
                    c.drawLine(cx, cy, cx + (float) Math.cos(ha) * rr * 0.45f, cy + (float) Math.sin(ha) * rr * 0.45f,
                            Draw.stroke(Draw.PLUM, rr * 0.07f));
                    c.drawLine(cx, cy, cx + (float) Math.cos(ma) * rr * 0.68f, cy + (float) Math.sin(ma) * rr * 0.68f,
                            Draw.stroke(Draw.BLUE_D, rr * 0.045f));
                    c.drawCircle(cx, cy, rr * 0.05f, Draw.fill(Draw.PLUM));
                    break;
                }
                case FRACTION:
                case LENGTH:
                case SHAPES:
                case SOLIDS: {
                    float s = Math.min(r - l, b - t) * 0.8f;
                    Draw.card(c, cx - s / 2, t, cx + s / 2, t + s - 8 * dp, 36 * dp, Draw.YELLOW, Draw.YELLOW_D, 8 * dp);
                    String q = k == FRACTION ? (qc == 0 ? "النِّصْف" : "الرُّبْع") : k == LENGTH ? (qc == 0 ? "الْأَطْوَل" : "الْأَقْصَر") : "؟";
                    label(c, q, cx, t + s / 2 - 4 * dp, s * (q.length() > 2 ? 0.3f : 0.6f), s * 0.8f, 0xFFFFFFFF);
                    break;
                }
                case TENS: {
                    Draw.card(c, l, t, r, b - 8 * dp, 28 * dp, 0xFFFFFFFF, 0xFFE6DCCB, 8 * dp);
                    float lt = t + 34 * dp;
                    if (qb != 2) label(c, qb == 0 ? "الْعَشَرَات" : "الْآحَاد", cx, t + 20 * dp, 20 * dp, r - l, Draw.PLUM);
                    blocks(c, qa, l + 16 * dp, lt, r - 16 * dp, b - 24 * dp);
                    break;
                }
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
                        int body = i == rightPick ? Draw.MINT_D : i == wrongPick ? 0xFFE07A7A : COLOURS[i % 4];
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
                    boolean line = (add && (mode() == 3 || mode() == 4)) || (!add && (mode() == 2 || mode() == 3));
                    if (line) {
                        // A window of ten steps around the hops, so the numbers stay big enough to read.
                        int end = add ? qa + qb : qa - qb;
                        int lo = Math.max(0, Math.min(Math.min(qa, end) - 1, 10));
                        numberLine(c, lo, lo + 10, qa, qb, add, l + 8 * dp, r - 8 * dp, (inT + inB) / 2 + 10 * dp);
                    } else if (add) {
                        float mid = (l + r) / 2;
                        things(c, qa, mid + 24 * dp, inT, r - 12 * dp, inB, COLOURS[0], 0, Math.max(qa, qb));
                        label(c, "+", mid, (inT + inB) / 2, 40 * dp, 40 * dp, Draw.PLUM);
                        things(c, qb, l + 12 * dp, inT, mid - 24 * dp, inB, COLOURS[1], 0, Math.max(qa, qb));
                    } else {
                        things(c, qa, l + 12 * dp, inT, r - 12 * dp, inB, COLOURS[2], qb);
                    }
                    String eq = digits(qa) + (add ? " + " : " − ") + digits(qb) + " = " + (rightPick >= 0 ? digits(add ? qa + qb : qa - qb) : "؟");
                    // Written as the book writes it: the first number on the right.
                    label(c, "\u200F" + eq, cx, b - eqH / 2, eqH * 0.7f, r - l, Draw.PLUM);
                    break;
                }
            }
        }
    }
}
