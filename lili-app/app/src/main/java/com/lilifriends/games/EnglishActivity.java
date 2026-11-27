package com.lilifriends.games;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
 * Lili's English: the grade-1 English book («English for Iraq, 1st Primary») unit by unit, in Lili's
 * voice. Each letter with its name, its sound and its words; the book's words (colours, classroom,
 * toys, animals, body, family, house) to hear and find; numbers to count; the book's sentences to
 * say along; a test at the end of each unit. Pictures are emoji and drawn colours; the units open
 * with their page of the book. Lili's recordings are in {@code assets/english}, the test's and
 * «watch and listen» are the science ones.
 */
public class EnglishActivity extends Activity {
    public static final String EXTRA_PREFS = "prefs", EXTRA_STAGE = "stage";

    private static final int T_LETTER = 0, T_SOUNDS = 1, T_WORD = 2, T_COUNT = 3, T_SAY = 4, T_UPPER = 5, T_ADJ = 6,
            T_ABC = 7, T_PAGE = 8, T_TEST = 9;

    /** One stop of the map: its title, the label on the map, what it teaches and which part of it. */
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

    /** The book's order: eight units, each opening with its page and ending with a test. */
    private static final Stop[] STOPS = {
            new Stop("الوحدة الأولى: Hello", "📖", T_PAGE, 1),
            new Stop("التحيات", "👋", T_SAY, 0),
            new Stop("الحرف a", "a", T_LETTER, 0),
            new Stop("الحرف b", "b", T_LETTER, 1),
            new Stop("الحرف c", "c", T_LETTER, 2),
            new Stop("الحرف d", "d", T_LETTER, 3),
            new Stop("الحرف e", "e", T_LETTER, 4),
            new Stop("الحرف f", "f", T_LETTER, 5),
            new Stop("الألوان", "🎨", T_WORD, 0),
            new Stop("أصوات الحروف a–f", "🔊", T_SOUNDS, 0),
            new Stop("اختبار الوحدة الأولى", "⭐", T_TEST, 1),
            new Stop("الوحدة الثانية: My school", "📖", T_PAGE, 2),
            new Stop("الحرف g", "g", T_LETTER, 6),
            new Stop("الحرف h", "h", T_LETTER, 7),
            new Stop("الحرف i", "i", T_LETTER, 8),
            new Stop("الحرف j", "j", T_LETTER, 9),
            new Stop("الحرف k", "k", T_LETTER, 10),
            new Stop("الحرف l", "l", T_LETTER, 11),
            new Stop("أدوات الصف", "📏", T_WORD, 1),
            new Stop("كلام الصف", "🙋", T_SAY, 1),
            new Stop("أصوات الحروف g–l", "🔊", T_SOUNDS, 1),
            new Stop("اختبار الوحدة الثانية", "⭐", T_TEST, 2),
            new Stop("الوحدة الثالثة: My toys", "📖", T_PAGE, 3),
            new Stop("الحرف m", "m", T_LETTER, 12),
            new Stop("الحرف n", "n", T_LETTER, 13),
            new Stop("الحرف o", "o", T_LETTER, 14),
            new Stop("الحرف p", "p", T_LETTER, 15),
            new Stop("الحرف q", "q", T_LETTER, 16),
            new Stop("الحرف r", "r", T_LETTER, 17),
            new Stop("الحرف s", "s", T_LETTER, 18),
            new Stop("الحرف t", "t", T_LETTER, 19),
            new Stop("الألعاب", "🧸", T_WORD, 2),
            new Stop("الأعداد 1–10", "🔢", T_COUNT, 0),
            new Stop("كم عمرك؟", "🎂", T_SAY, 2),
            new Stop("أصوات الحروف m–t", "🔊", T_SOUNDS, 2),
            new Stop("اختبار الوحدة الثالثة", "⭐", T_TEST, 3),
            new Stop("الوحدة الرابعة: Revision", "📖", T_PAGE, 4),
            new Stop("الحرف u", "u", T_LETTER, 20),
            new Stop("الحرف v", "v", T_LETTER, 21),
            new Stop("الحرف w", "w", T_LETTER, 22),
            new Stop("الحرف x", "x", T_LETTER, 23),
            new Stop("الحرف y", "y", T_LETTER, 24),
            new Stop("الحرف z", "z", T_LETTER, 25),
            new Stop("أصوات الحروف u–z", "🔊", T_SOUNDS, 3),
            new Stop("الحروف كلها", "abc", T_ABC, 0),
            new Stop("ماذا ترى؟", "👀", T_SAY, 3),
            new Stop("اختبار الوحدة الرابعة", "⭐", T_TEST, 4),
            new Stop("الوحدة الخامسة: At the zoo", "📖", T_PAGE, 5),
            new Stop("الحروف الكبيرة A–H", "A", T_UPPER, 4),
            new Stop("الحيوانات", "🦁", T_WORD, 3),
            new Stop("ألوان جديدة", "🖍️", T_WORD, 4),
            new Stop("كبير وصغير", "🐘", T_ADJ, 0),
            new Stop("في حديقة الحيوان", "🐒", T_SAY, 4),
            new Stop("اختبار الوحدة الخامسة", "⭐", T_TEST, 5),
            new Stop("الوحدة السادسة: My body", "📖", T_PAGE, 6),
            new Stop("الحروف الكبيرة I–P", "I", T_UPPER, 5),
            new Stop("أجزاء الجسم", "👂", T_WORD, 5),
            new Stop("أنا وجسمي", "🧼", T_SAY, 5),
            new Stop("اختبار الوحدة السادسة", "⭐", T_TEST, 6),
            new Stop("الوحدة السابعة: My family", "📖", T_PAGE, 7),
            new Stop("الحروف الكبيرة Q–Z", "Q", T_UPPER, 6),
            new Stop("العائلة", "👪", T_WORD, 6),
            new Stop("البيت", "🏠", T_WORD, 7),
            new Stop("صديقي", "🔑", T_SAY, 6),
            new Stop("اختبار الوحدة السابعة", "⭐", T_TEST, 7),
            new Stop("الوحدة الثامنة: Revision", "📖", T_PAGE, 8),
            new Stop("أصوات الحروف كلها", "🔊", T_SOUNDS, 7),
            new Stop("كلمات الحروف", "🍎", T_WORD, 8),
            new Stop("نعد من جديد", "🔢", T_COUNT, 0),
            new Stop("الكبيرة والصغيرة", "Aa", T_UPPER, 7),
            new Stop("اختبار الوحدة الثامنة", "⭐", T_TEST, 8),
    };

    /** What each stop shows on the journey map. */
    static final String[] STOP_LABELS = new String[STOPS.length];

    static {
        for (int i = 0; i < STOPS.length; i++) STOP_LABELS[i] = STOPS[i].label;
    }

    // The alphabet: small and capital, the letter's name and its sound as Lili recorded them.
    private static final String[] LOWER = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p",
            "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"};
    private static final String[] UPPER = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P",
            "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};
    private static final String[] NAMES = {"Ay", "Bee", "See", "Dee", "Ee", "Eff", "Jee", "Aitch", "Eye", "Jay", "Kay", "El",
            "Em", "En", "Oh", "Pee", "Kyoo", "Ar", "Ess", "Tee", "You", "Vee", "Double you", "Ex", "Why", "Zed"};
    private static final String[] SOUNDS = {"Ah", "Buh", "Kuh", "Duh", "Eh", "Fff", "Guh", "Huh", "Ih", "Juh", "Kuh", "Lll",
            "Mmm", "Nnn", "Aw", "Puh", "Kwuh", "Rrr", "Sss", "Tuh", "Uh", "Vvv", "Wuh", "Kss", "Yuh", "Zzz"};
    /** The book's two words for each letter, and their pictures. */
    private static final String[][] WORDS = {{"Ant", "Apple"}, {"Ball", "Bus"}, {"Car", "Cat"}, {"Dog", "Duck"},
            {"Egg", "Elephant"}, {"Fish", "Flower"}, {"Girl", "Goat"}, {"Horse", "House"}, {"Ink", "In"},
            {"Juice", "Jumper"}, {"Key", "Kite"}, {"Lemon", "Lion"}, {"Monkey", "Moon"}, {"Net", "Nose"},
            {"Olive", "Orange"}, {"Plane", "Pizza"}, {"Queen", "Question"}, {"Rabbit", "Robot"}, {"Snake", "Sun"},
            {"Taxi", "Tomato"}, {"Umbrella", "Up"}, {"Van", "Violin"}, {"Watch", "Window"}, {"X-ray", "X-ray"},
            {"Yo-yo", "Yo-yo"}, {"Zebra", "Zebra"}};
    private static final String[][] WORD_PICS = {{"🐜", "🍎"}, {"⚽", "🚌"}, {"🚗", "🐱"}, {"🐶", "🦆"}, {"🥚", "🐘"},
            {"🐟", "🌸"}, {"👧", "🐐"}, {"🐴", "🏠"}, {"🖋️", "📥"}, {"🧃", "🧥"}, {"🔑", "🪁"}, {"🍋", "🦁"},
            {"🐒", "🌙"}, {"🥅", "👃"}, {"🫒", "🍊"}, {"✈️", "🍕"}, {"👸", "❓"}, {"🐰", "🤖"}, {"🐍", "☀️"},
            {"🚕", "🍅"}, {"☂️", "⬆️"}, {"🚐", "🎻"}, {"⌚", "🪟"}, {"🩻", "🩻"}, {"🪀", "🪀"}, {"🦓", "🦓"}};
    /** Which letters each sounds stop (and each capitals stop) covers: from, to (not included). */
    private static final int[][] RANGES = {{0, 6}, {6, 12}, {12, 20}, {20, 26}, {0, 8}, {8, 16}, {16, 26}, {0, 26}};

    // The book's words to hear and find: colours (drawn), classroom, toys, animals, more colours, body, family, house.
    private static final String[][] VOCAB = {
            {"Blue", "Red", "Yellow", "Green", "Pink", "Orange"},
            {"Book", "Ruler", "Bag", "Pencil case", "Pen", "Pencil"},
            {"Bear", "Bike", "Doll", "Skates", "Balloon", "Present", "Cake", "Kite", "Robot", "Yo-yo", "Ball"},
            {"Camel", "Tiger", "Penguin", "Lion", "Monkey", "Elephant", "Zebra", "Snake", "Rabbit", "Duck", "Fish", "Goat"},
            {"Black", "White", "Brown", "Grey", "Blue", "Red", "Green", "Yellow"},
            {"Eyes", "Ears", "Nose", "Hands", "Arm", "Leg", "Feet"},
            {"Dad", "Mum", "Sister", "Brother", "Grandma", "Grandpa"},
            {"Bed", "Chair", "Door", "Window", "Bin", "House"},
            {},
    };
    private static final String[][] VOCAB_PICS = {
            {"", "", "", "", "", ""},
            {"📕", "📏", "🎒", "👝", "🖊️", "✏️"},
            {"🧸", "🚲", "🪆", "🛼", "🎈", "🎁", "🎂", "🪁", "🤖", "🪀", "⚽"},
            {"🐪", "🐯", "🐧", "🦁", "🐒", "🐘", "🦓", "🐍", "🐰", "🦆", "🐟", "🐐"},
            {"", "", "", "", "", "", "", ""},
            {"👀", "👂", "👃", "✋", "💪", "🦵", "🦶"},
            {"👨", "👩", "👧", "👦", "👵", "👴"},
            {"🛏️", "🪑", "🚪", "🪟", "🗑️", "🏠"},
            {},
    };
    /** The colours, drawn as round blobs. */
    private static final String[] COLOUR_NAMES = {"Blue", "Red", "Yellow", "Green", "Pink", "Orange", "Black", "White",
            "Brown", "Grey"};
    private static final int[] COLOUR_VALUES = {0xFF2F7DE1, 0xFFE53935, 0xFFFDD835, 0xFF43A047, 0xFFFF80AB, 0xFFFB8C00,
            0xFF222222, 0xFFFFFFFF, 0xFF8D5524, 0xFF9E9E9E};
    private static final String[] NUMBERS = {"One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten"};
    private static final String[] COUNTED = {"🍎", "🎈", "⚽", "🐟", "🌸", "🧸", "🚗", "⭐", "🐥", "🍊"};
    /** Big and little: the pictures shown at two sizes. */
    private static final String[] SIZED = {"🐘", "🦁", "🐯", "🐪", "🐒", "🐧", "🐰", "🐶"};

    // The book's sentences to hear and say along, with a picture each.
    private static final String[][] SAY = {
            {"Hello!", "Goodbye!", "What's your name?", "I'm Lili.", "Who's this?", "Here you are.", "Thank you."},
            {"Stand up.", "Sit down.", "Hands up.", "Hands down.", "Turn around.", "Open.", "Close.", "Draw.",
                    "What's this?", "Is it a pen?", "Yes.", "No."},
            {"How many toys?", "What is it?", "It's a kite.", "It's a doll.", "How old are you?", "I'm six.", "Happy birthday!"},
            {"Look!", "What can you see?", "Where's the tiger?", "In the bag.", "Listen and point."},
            {"The lion is big!", "The rabbits are little.", "The monkeys are funny!", "There are three cats.",
                    "How many red fish?"},
            {"My nose.", "I have two eyes.", "I wash my hands.", "I wash my face."},
            {"It's my friend.", "I have a green key.", "I'm Nada.", "I'm Ahmed."},
    };
    private static final String[][] SAY_PICS = {
            {"👋", "👋", "❓", "👧", "🤔", "🤲", "🙏"},
            {"🧍", "🪑", "🙋", "👇", "🔄", "📖", "📕", "✏️", "❓", "🖊️", "✅", "❌"},
            {"🧸", "❓", "🪁", "🪆", "🎂", "6️⃣", "🎉"},
            {"👀", "👀", "🐯", "🎒", "👉"},
            {"🦁", "🐰", "🐒", "🐱", "🐟"},
            {"👃", "👀", "🧼", "🧼"},
            {"🧑‍🤝‍🧑", "🔑", "👧", "👦"},
    };
    private static final String[] PRAISE = {"Good job!", "Well done!", "Great!", "Super!"};

    private static final int ASK = 0, LETTER = 1, LETTER_Q = 2, SOUNDS_Q = 3, WORD = 4, COUNT = 5, SAY_IT = 6, UPPER_Q = 7,
            ADJ = 8, ABC = 9, PAGE = 10, TEST = 11, DONE = 12;
    private static final int TEST_ROUNDS = 8;
    private static final int ROUNDS = 6;

    private SharedPreferences prefs;
    private Lessons book;
    private final Map<String, String> voice = new HashMap<>();
    private Stop stop;
    private int stage;
    private final Random rnd = new Random();
    private MediaPlayer player;
    private EngView view;
    private boolean girl, busy;

    private final List<Integer> steps = new ArrayList<>();
    private int step, round, rounds;

    // The question on screen: answer cards (letters, pictures or colours) and the right one.
    private final List<String> options = new ArrayList<>();
    /** Each card: 0 a word or letter, 1 a picture, 2 a colour (its value in {@link #swatch}), 3 a small picture. */
    private final List<Integer> optionKind = new ArrayList<>();
    private final List<Integer> swatch = new ArrayList<>();
    private int answer = -1, wrongPick = -1, rightPick = -1;
    private long wobbleAt;
    /** What is shown above the cards: a letter, a picture or a sentence; and how many to count. */
    private String subject = "", line = "";
    private int qa, qb, many;
    /** What Lili says after a right answer. */
    private final List<String> reply = new ArrayList<>();
    /** What Lili says when the speaker is pressed. */
    private final List<String> question = new ArrayList<>();
    /** Things done in a round of sentences, so they come in a new order without repeats. */
    private final List<Integer> deck = new ArrayList<>();
    /** A unit's opening page of the book. */
    private Bitmap page;

    /** A unit's test: the stops it asks from, the one asked now and its exercise, the first-try misses. */
    private final List<Stop> testPool = new ArrayList<>();
    private Stop testStop;
    private int testKind = -1, misses;
    private boolean missed;
    /** Played before the next question (the test's opening). */
    private final List<String> intro = new ArrayList<>();

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        String file = getIntent().getStringExtra(EXTRA_PREFS);
        if (file == null) file = "english";
        prefs = getSharedPreferences(file, MODE_PRIVATE);
        try {
            // The test's phrases and «watch and listen» are the science recordings; Lili's English ones are her own.
            book = new Lessons(readAsset("letters/lessons.json"));
            loadVoice("science");
            loadVoice("english");
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
        if (g.isEmpty() && file.endsWith("_english")) {
            g = getSharedPreferences(file.substring(0, file.length() - "_english".length()) + "_letters", MODE_PRIVATE)
                    .getString("gender", "");
            if (!g.isEmpty()) prefs.edit().putString("gender", g).apply();
        }
        girl = "g".equals(g);

        if (g.isEmpty()) steps.add(ASK);
        if (stop.type == T_LETTER) steps.add(LETTER);
        int[] byType = {LETTER_Q, SOUNDS_Q, WORD, COUNT, SAY_IT, UPPER_Q, ADJ, ABC, PAGE, TEST};
        steps.add(byType[stop.type]);
        if (stop.type == T_TEST) {
            // The unit's stops that ask a question (not its sentences to say along).
            for (int i = stage - 1; i >= 0 && STOPS[i].type != T_PAGE; i--) {
                if (STOPS[i].type != T_SAY && STOPS[i].type != T_TEST) testPool.add(STOPS[i]);
            }
        }
        if (stop.type == T_PAGE) {
            try (InputStream in = getAssets().open("english/u" + stop.mode + ".webp")) {
                page = BitmapFactory.decodeStream(in);
            } catch (IOException e) {
                page = null;
            }
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
        view = new EngView(this, face);
        root.addView(view, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
        startStep(Math.max(0, Math.min(getIntent().getIntExtra("step", 0), steps.size() - 1)));
    }

    private void loadVoice(String dir) throws Exception {
        JSONObject o = new JSONObject(readAsset(dir + "/sounds.json"));
        for (Iterator<String> it = o.keys(); it.hasNext(); ) {
            String k = it.next();
            voice.put(k, dir + "/" + o.getString(k));
        }
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

    /** Lili's recording of a word or sentence, as an asset path (null if not recorded yet). */
    private String m(String text) {
        return voice.get(text);
    }

    /** A phrase recorded for the letters game (hello, who plays, boy, girl). */
    private String l(String text) {
        String f = book.phrase(text);
        return f == null ? null : "letters/" + f;
    }

    private String g(String boy, String girlForm) {
        return m(girl ? girlForm : boy);
    }

    private String praise() {
        return m(PRAISE[rnd.nextInt(PRAISE.length)]);
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

    private void ask() {
        play(question, null);
    }

    // ---- Flow --------------------------------------------------------------------------------

    private int kind() {
        int k = steps.get(step);
        return k == TEST && testKind >= 0 ? testKind : k;
    }

    private boolean testing() {
        return steps.get(step) == TEST;
    }

    /** The exercise a stop gives in a test. */
    private int quizOf(Stop s) {
        int[] byType = {LETTER_Q, SOUNDS_Q, WORD, COUNT, SAY_IT, UPPER_Q, ADJ, ABC, PAGE, TEST};
        return byType[s.type];
    }

    /** The test's next question: one from a stop of the unit, not the same as the last one. */
    private void testRound() {
        Stop last = testStop;
        do testStop = testPool.get(rnd.nextInt(testPool.size())); while (testPool.size() > 1 && testStop == last);
        stop = testStop;
        testKind = quizOf(stop);
        missed = false;
        newQuestion(true);
        rounds = TEST_ROUNDS;
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
            case LETTER: {
                // Meet the letter: its name, its sound, and its words.
                int i = stop.mode;
                question.clear();
                question.add(m(NAMES[i]));
                question.add(m(SOUNDS[i]));
                question.add(m(NAMES[i]));
                question.add(m(SOUNDS[i]));
                question.add(m(WORDS[i][0]));
                if (!WORDS[i][1].equals(WORDS[i][0])) question.add(m(WORDS[i][1]));
                List<String> lead = new ArrayList<>();
                if (stage <= 2) lead.add(m("هَيَّا نَتَعَلَّمُ الْإِنْكِلِيزِيَّة"));
                lead.add(g("اِسْمَعْ وَكَرِّرْ مَعَ لِيلِي", "اِسْمَعِي وَكَرِّرِي مَعَ لِيلِي"));
                lead.addAll(question);
                play(lead, null);
                break;
            }
            case SAY_IT:
                deck.clear();
                for (int i = 0; i < SAY[stop.mode].length; i++) deck.add(i);
                Collections.shuffle(deck, rnd);
                rounds = Math.min(ROUNDS, deck.size());
                newQuestion(true);
                break;
            case PAGE: {
                // A unit begins: its page of the book.
                question.clear();
                if (stop.mode == 1) question.add(m("هَيَّا نَتَعَلَّمُ الْإِنْكِلِيزِيَّة"));
                question.add(m("Look!"));
                List<String> lead = new ArrayList<>();
                lead.add(g("شَاهِدْ وَاسْتَمِعْ", "شَاهِدِي وَاسْتَمِعِي"));
                lead.addAll(question);
                play(lead, null);
                break;
            }
            case TEST:
                misses = 0;
                testKind = -1;
                testStop = null;
                intro.clear();
                intro.add(m("هَيَّا نَخْتَبِرُ مَا تَعَلَّمْنَاه"));
                intro.add(m("اِخْتِبَارُ الْوَحْدَة"));
                testRound();
                break;
            case DONE:
                int reached = prefs.getInt("stage", 0);
                if (stage + 1 > reached) prefs.edit().putInt("stage", stage + 1).apply();
                if (testStop != null) {
                    // The test's result: Lili says how it went (the next unit opens either way).
                    stop = STOPS[stage];
                    int score = TEST_ROUNDS - misses;
                    List<String> res = new ArrayList<>();
                    if (score < TEST_ROUNDS - 3) res.add(m("نُرَاجِعُ الدَّرْسَ مَرَّةً أُخْرَى"));
                    else {
                        res.add(m(score >= TEST_ROUNDS - 1 ? "نَتِيجَةٌ رَائِعَة!" : "نَتِيجَةٌ جَيِّدَة"));
                        if (score >= TEST_ROUNDS - 1) res.add(g("أَجَبْتَ إِجَابَاتٍ صَحِيحَةً كَثِيرَة", "أَجَبْتِ إِجَابَاتٍ صَحِيحَةً كَثِيرَة"));
                        res.add(m("Well done!"));
                    }
                    play(res, null);
                    break;
                }
                play(m("Well done!"), null);
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

    private void addOption(String s, int how, int colour) {
        options.add(s);
        optionKind.add(how);
        swatch.add(colour);
    }

    /** A letter from the stop's range (or the whole alphabet). */
    private int someLetter() {
        int[] r = RANGES[Math.min(stop.mode, RANGES.length - 1)];
        if (stop.type == T_LETTER) return stop.mode;
        if (stop.type == T_ABC) return rnd.nextInt(26);
        return r[0] + rnd.nextInt(r[1] - r[0]);
    }

    /** Three different letters, one of them {@code keep}; c and k sound alike, so never both. */
    private List<Integer> threeLetters(int keep) {
        List<Integer> out = new ArrayList<>();
        out.add(keep);
        while (out.size() < 3) {
            int v = rnd.nextInt(26);
            boolean clash = out.contains(v) || ((v == 2 || v == 10) && (out.contains(2) || out.contains(10)));
            if (!clash) out.add(v);
        }
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
        optionKind.clear();
        swatch.clear();
        subject = "";
        line = "";
        many = 0;
        List<String> lead = new ArrayList<>();
        int k = kind();
        switch (k) {
            case LETTER_Q:
            case ABC: {
                // Which picture starts with this letter?
                qa = someLetter();
                subject = LOWER[qa];
                List<Integer> letters = threeLetters(qa);
                for (int v : letters) addOption(WORD_PICS[v][rnd.nextInt(2)], 1, 0);
                answer = letters.indexOf(qa);
                qb = WORD_PICS[qa][0].equals(options.get(answer)) ? 0 : 1;
                if (first) question.add(m("أَيُّ كَلِمَةٍ تَبْدَأُ بِهَذَا الْحَرْف؟"));
                question.add(m(NAMES[qa]));
                question.add(m(SOUNDS[qa]));
                reply.add(m(WORDS[qa][qb]));
                break;
            }
            case SOUNDS_Q: {
                qa = someLetter();
                List<Integer> letters = threeLetters(qa);
                if (round % 2 == 0) {
                    // Hear the sound, find the letter.
                    for (int v : letters) addOption(LOWER[v], 0, 0);
                    question.add(m("صَوْتُ الْحَرْف"));
                    question.add(m(SOUNDS[qa]));
                    question.add(m("أَيْنَ الْحَرْف؟"));
                    reply.add(m(NAMES[qa]));
                } else {
                    // Hear the sound, find the picture that starts with it.
                    qb = rnd.nextInt(2);
                    for (int v : letters) addOption(WORD_PICS[v][v == qa ? qb : rnd.nextInt(2)], 1, 0);
                    question.add(g("اِسْمَعْ صَوْتَ الْحَرْفِ وَاخْتَرِ الصُّورَة", "اِسْمَعِي صَوْتَ الْحَرْفِ وَاخْتَارِي الصُّورَة"));
                    question.add(m(SOUNDS[qa]));
                    reply.add(m(WORDS[qa][qb]));
                }
                answer = letters.indexOf(qa);
                break;
            }
            case UPPER_Q: {
                // A capital letter: find its small one.
                qa = someLetter();
                subject = UPPER[qa];
                List<Integer> letters = threeLetters(qa);
                for (int v : letters) addOption(LOWER[v], 0, 0);
                answer = letters.indexOf(qa);
                question.add(m("أَيْنَ الْحَرْف؟"));
                question.add(m(NAMES[qa]));
                reply.add(m(NAMES[qa]));
                break;
            }
            case WORD: {
                // Hear the word, find its picture (or colour).
                int md = stop.mode;
                String[] words = md < VOCAB.length - 1 ? VOCAB[md] : null;
                List<String> names = new ArrayList<>();
                List<String> pics = new ArrayList<>();
                if (words == null) {
                    for (int i = 0; i < WORDS.length; i++) {
                        names.add(WORDS[i][0]);
                        pics.add(WORD_PICS[i][0]);
                        if (!WORDS[i][1].equals(WORDS[i][0])) {
                            names.add(WORDS[i][1]);
                            pics.add(WORD_PICS[i][1]);
                        }
                    }
                } else {
                    for (int i = 0; i < words.length; i++) {
                        names.add(words[i]);
                        pics.add(VOCAB_PICS[md][i]);
                    }
                }
                qa = rnd.nextInt(names.size());
                List<Integer> some = pickSome(names.size(), 3, qa);
                for (int v : some) {
                    if (pics.get(v).isEmpty()) {
                        int c = 0;
                        for (int j = 0; j < COLOUR_NAMES.length; j++) if (COLOUR_NAMES[j].equals(names.get(v))) c = COLOUR_VALUES[j];
                        addOption(names.get(v), 2, c);
                    } else addOption(pics.get(v), 1, 0);
                }
                answer = some.indexOf(qa);
                if (first) question.add(g("اِسْمَعْ وَاخْتَرِ الصُّورَة", "اِسْمَعِي وَاخْتَارِي الصُّورَة"));
                question.add(m(names.get(qa)));
                reply.add(m(names.get(qa)));
                break;
            }
            case COUNT: {
                // How many? Count the pictures, then find the number.
                qa = rnd.nextInt(NUMBERS.length);
                many = qa + 1;
                subject = COUNTED[rnd.nextInt(COUNTED.length)];
                List<Integer> some = pickSome(NUMBERS.length, 3, qa);
                for (int v : some) addOption("" + (v + 1), 0, 0);
                answer = some.indexOf(qa);
                question.add(m("كَمْ عَدَدُهَا؟"));
                reply.add(m(NUMBERS[qa]));
                break;
            }
            case ADJ: {
                // Big or little: the same animal at two sizes.
                subject = SIZED[rnd.nextInt(SIZED.length)];
                qa = rnd.nextInt(2); // 0 big, 1 little
                qb = rnd.nextInt(2); // where the big one is
                for (int i = 0; i < 2; i++) addOption(subject, i == qb ? 1 : 3, 0);
                answer = qa == 0 ? qb : 1 - qb;
                question.add(m(qa == 0 ? "Big" : "Little"));
                reply.add(m(qa == 0 ? "Big" : "Little"));
                subject = "";
                break;
            }
            case SAY_IT: {
                // Lili says a sentence of the book; the child says it along, then goes on.
                qa = deck.get(round % deck.size());
                subject = SAY_PICS[stop.mode][qa];
                line = SAY[stop.mode][qa];
                if (first) lead.add(g("اِسْمَعْ وَكَرِّرْ مَعَ لِيلِي", "اِسْمَعِي وَكَرِّرِي مَعَ لِيلِي"));
                question.add(m(line));
                answer = -1;
                break;
            }
        }
        lead.addAll(question);
        lead.addAll(0, intro);
        intro.clear();
        view.invalidate();
        play(lead, null);
    }

    /** A sentence said along: Lili cheers and the next one comes. */
    private void said() {
        if (busy) return;
        busy = true;
        play(praise(), () -> {
            round++;
            if (round < rounds) newQuestion(false);
            else nextStep();
        });
    }

    private void pick(int i) {
        if (busy || i < 0 || i >= options.size()) return;
        if (i == answer) {
            busy = true;
            rightPick = i;
            view.invalidate();
            List<String> after = new ArrayList<>(reply);
            after.add(praise());
            play(after, () -> {
                round++;
                if (round < rounds && testing()) testRound();
                else if (round < rounds) newQuestion(false);
                else nextStep();
            });
        } else {
            if (testing() && !missed) {
                missed = true;
                misses++;
            }
            wrongPick = i;
            wobbleAt = SystemClock.uptimeMillis();
            view.invalidate();
            play(m("Try again."), this::ask);
        }
    }

    /** Tapping a picture of the letter's words says the word. */
    private void hear(int i) {
        if (kind() != LETTER) return;
        play(m(WORDS[stop.mode][i]), null);
    }

    // ---- Screen ------------------------------------------------------------------------------

    private class EngView extends View {
        private final float dp;
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint latin = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint emoji = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Bitmap lili;
        private final List<float[]> boxes = new ArrayList<>();
        private int down = -1;
        private static final int B_SPEAK = -10, B_NEXT = -11, B_BOY = -13, B_GIRL = -14, B_WORD = -20;

        EngView(Context ctx, Typeface face) {
            super(ctx);
            dp = getResources().getDisplayMetrics().density;
            text.setTypeface(Typeface.DEFAULT_BOLD);
            text.setTextAlign(Paint.Align.CENTER);
            latin.setTypeface(face);
            latin.setTextAlign(Paint.Align.CENTER);
            emoji.setTextAlign(Paint.Align.CENTER);
            lili = Draw.load(getResources(), R.drawable.home_jump_up);
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
                if (k == SAY_IT) said();
                else if (!busy || k == DONE) nextStep();
            } else if (w <= B_WORD) hear(B_WORD - w);
            else if (w >= 0) pick(w);
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

        /** English: a letter, a number or a sentence, in the rounded Latin font. */
        private void english(Canvas c, String s, float cx, float cy, float size, float maxW, int color) {
            latin.setTextSize(size);
            float tw = latin.measureText(s);
            if (tw > maxW && tw > 0) latin.setTextSize(size * maxW / tw);
            latin.setColor(color);
            c.drawText(s, cx, cy - (latin.ascent() + latin.descent()) / 2f, latin);
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

        /** The answer cards in a row: letters, numbers, pictures or colours. */
        private void cards(Canvas c, float l, float t, float r, float b) {
            int n = options.size();
            float gap = 12 * dp, cw = (r - l - gap * (n - 1)) / n, ch = b - t;
            for (int i = 0; i < n; i++) {
                float x = l + i * (cw + gap) + (i == wrongPick ? wobble() : 0);
                int fill = i == rightPick ? Draw.MINT : Draw.LILAC, deep = i == rightPick ? Draw.MINT_D : Draw.LILAC_D;
                if (optionKind.get(i) != 0) {
                    fill = i == rightPick ? Draw.MINT : 0xFFFFFFFF;
                    deep = i == rightPick ? Draw.MINT_D : 0xFFD9DCE8;
                }
                button(c, x, t, x + cw, t + ch, fill, deep, i);
                float cx = x + cw / 2, cy = t + (ch - 6 * dp) / 2, s = Math.min(cw, ch);
                int how = optionKind.get(i);
                if (how == 0) english(c, options.get(i), cx, cy - 2 * dp, s * 0.55f, cw * 0.8f, 0xFFFFFFFF);
                else if (how == 1) pic(c, options.get(i), cx, cy, s * 0.55f);
                else if (how == 3) pic(c, options.get(i), cx, cy, s * 0.25f);
                else {
                    c.drawCircle(cx, cy, s * 0.3f, Draw.fill(swatch.get(i)));
                    c.drawCircle(cx, cy, s * 0.3f, Draw.stroke(0x33000000, 2 * dp));
                }
            }
        }

        @Override
        protected void onDraw(Canvas c) {
            boxes.clear();
            float w = getWidth(), h = getHeight(), pad = 18 * dp;
            c.drawColor(Draw.SKY_TOP);
            int k = kind();
            float btnH = Math.max(64 * dp, Math.min(84 * dp, h * 0.11f));
            float sr = Math.max(36 * dp, Math.min(50 * dp, h * 0.065f));
            if (k == ASK) {
                label(c, "مَنْ يَلْعَبُ مَعَ لِيلِي؟", w / 2, pad + 30 * dp, 30 * dp, w - 2 * pad, Draw.PLUM);
                float cw = Math.min(w - 2 * pad, (h - 100 * dp) / 2 - pad);
                float ch = cw * 0.75f, t0 = pad + 70 * dp, x0 = (w - cw) / 2;
                button(c, x0, t0, x0 + cw, t0 + ch, Draw.BLUE, Draw.BLUE_D, B_BOY);
                label(c, "وَلَد", x0 + cw / 2, t0 + ch / 2 - 3 * dp, ch * 0.32f, cw * 0.8f, 0xFFFFFFFF);
                button(c, x0, t0 + ch + pad, x0 + cw, t0 + 2 * ch + pad, Draw.PINK, Draw.PINK_D, B_GIRL);
                label(c, "بِنْت", x0 + cw / 2, t0 + ch + pad + ch / 2 - 3 * dp, ch * 0.32f, cw * 0.8f, 0xFFFFFFFF);
                return;
            }
            if (k == PAGE) {
                float row = h - pad - btnH / 2;
                float y = row - btnH / 2 - pad;
                Draw.picture(c, page, pad, pad, w - pad, y - pad);
                speaker(c, w / 2 + btnH * 0.9f, row, btnH / 2);
                next(c, w / 2 - btnH * 1.1f, row, btnH * 2.2f, btnH, "هَيَّا");
                return;
            }
            if (k == DONE) {
                float ph = Math.min(h * 0.45f, w * 0.6f);
                Draw.picture(c, lili, w / 2 - ph / 2, pad, w / 2 + ph / 2, pad + ph);
                float y = pad + ph + pad;
                // After a test: stars for how it went, and how many were right the first time.
                int score = TEST_ROUNDS - misses, stars = testStop == null ? 3 : score >= TEST_ROUNDS - 1 ? 3
                        : score >= TEST_ROUNDS - 3 ? 2 : 1;
                for (int i = 0; i < 3; i++) Draw.star(c, w / 2 + (1 - i) * 54 * dp, y + 26 * dp, 22 * dp,
                        i < stars ? Draw.YELLOW_D : 0xFFD9DCE8);
                if (testStop == null) english(c, "Well done!", w / 2, y + 90 * dp, 40 * dp, w - 2 * pad, Draw.PLUM);
                else english(c, score + " / " + TEST_ROUNDS, w / 2, y + 90 * dp, 40 * dp, w - 2 * pad, Draw.PLUM);
                next(c, w / 2, y + 170 * dp, Math.min(w - 2 * pad, 240 * dp), btnH, "تَمّ");
                return;
            }
            if (testing() || round > 0 || rounds > 1) {
                String count = (round + 1) + " / " + (testing() ? TEST_ROUNDS : rounds);
                if (k != LETTER) english(c, count, w / 2, pad, 14 * dp, w, 0x995B3E8C);
            }
            float top = pad + 14 * dp;
            speaker(c, w / 2, top + sr, sr);
            float areaT = top + 2 * sr + pad, cardsH = Math.max(90 * dp, Math.min(130 * dp, h * 0.17f));
            float cardsT = h - pad - cardsH;
            if (k == LETTER) {
                // The letter big, small and capital, and its words to tap.
                int i = stop.mode;
                float bottom = h - pad - btnH - pad;
                float lh = (bottom - areaT) * 0.45f;
                english(c, UPPER[i] + " " + LOWER[i], w / 2, areaT + lh / 2, lh * 0.8f, w - 2 * pad, Draw.BLUE_D);
                boolean two = !WORDS[i][1].equals(WORDS[i][0]);
                int n = two ? 2 : 1;
                float gap = 14 * dp, cw = Math.min(180 * dp, (w - 2 * pad - gap * (n - 1)) / n);
                float x0 = (w - n * cw - (n - 1) * gap) / 2, t = areaT + lh + pad / 2, ch = bottom - t;
                for (int j = 0; j < n; j++) {
                    float x = x0 + j * (cw + gap);
                    button(c, x, t, x + cw, t + ch, 0xFFFFFFFF, 0xFFD9DCE8, B_WORD - j);
                    float s = Math.min(cw, ch);
                    pic(c, WORD_PICS[i][j], x + cw / 2, t + ch * 0.4f, s * 0.42f);
                    english(c, WORDS[i][j].toLowerCase(), x + cw / 2, t + ch * 0.78f, s * 0.16f, cw * 0.85f, Draw.PLUM);
                }
                next(c, w / 2, h - pad - btnH / 2, Math.min(w - 2 * pad, 240 * dp), btnH, "هَيَّا");
                return;
            }
            if (k == SAY_IT) {
                float bottom = h - pad - btnH - pad;
                float ph = (bottom - areaT) * 0.6f;
                pic(c, subject, w / 2, areaT + ph / 2, ph * 0.6f);
                english(c, line, w / 2, areaT + ph + (bottom - areaT - ph) / 2, 34 * dp, w - 2 * pad, Draw.PLUM);
                next(c, w / 2, h - pad - btnH / 2, Math.min(w - 2 * pad, 240 * dp), btnH, "Now you!");
                return;
            }
            float midT = areaT, midB = cardsT - pad, mh = midB - midT;
            if (k == COUNT) {
                // The things to count, in rows of up to five.
                int cols = Math.min(5, many), rows = (many + 4) / 5;
                float cell = Math.min((w - 2 * pad) / 5, mh / Math.max(2, rows));
                for (int i = 0; i < many; i++) {
                    int row = i / 5, col = i % 5, inRow = row < rows - 1 ? 5 : many - row * 5;
                    float cx = w / 2 + (col - (inRow - 1) / 2f) * cell;
                    float cy = midT + mh / 2 + (row - (rows - 1) / 2f) * cell;
                    pic(c, subject, cx, cy, cell * 0.75f);
                }
            } else if (!subject.isEmpty()) {
                if (k == LETTER_Q || k == ABC || k == UPPER_Q) english(c, subject, w / 2, midT + mh / 2, mh * 0.7f, w - 2 * pad, Draw.BLUE_D);
                else pic(c, subject, w / 2, midT + mh / 2, mh * 0.6f);
            } else {
                // Only listening: Lili listens along.
                float ph = Math.min(mh * 0.85f, w * 0.6f);
                Draw.picture(c, lili, w / 2 - ph / 2, midT + (mh - ph) / 2, w / 2 + ph / 2, midT + (mh + ph) / 2);
            }
            cards(c, pad, cardsT, w - pad, h - pad);
        }
    }
}
