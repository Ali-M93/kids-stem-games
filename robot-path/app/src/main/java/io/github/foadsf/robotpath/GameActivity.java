package io.github.foadsf.robotpath;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Robot Path: tap arrows to build a program, press Go, and the robot follows it to the star.
 *
 * What it trains is sequencing and debugging. When the robot bumps into a rock, the step that
 * did it turns red and she fixes that step instead of starting over. After two misses on the
 * same board, footprints show part of the way; after three, all of it. Three stars earn a
 * sticker and a clear "More, or done?" choice.
 */
public class GameActivity extends Activity implements BoardView.Listener {
    private static final String PREFS = "progress", KEY_LEVEL = "level", KEY_STARS = "stars",
            KEY_STICKERS = "stickers";
    private static final int STARS_PER_STICKER = 3;
    /** Intent extras: the progress file to use, and the level to start on (at most the furthest). */
    public static final String EXTRA_PREFS = "prefs", EXTRA_LEVEL = "level";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private Sounds sounds;
    private Typeface face;
    private GameLayout layout;
    private BoardView board;
    private ProgramView program;
    private TextView title;
    private CandyButton go, undo;
    private CandyButton[] arrows;
    private StickerView sticker;
    private LinearLayout stickerButtons;
    private Level level;
    private int misses;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        // In the Lili & Friends app each child has their own progress file, and the journey map
        // passes the level to play; on its own the game keeps one file and resumes.
        String file = getIntent().getStringExtra(EXTRA_PREFS);
        prefs = getSharedPreferences(file != null ? file : PREFS, MODE_PRIVATE);
        sounds = new Sounds();
        Art.load(getResources());
        face = Typeface.createFromAsset(getAssets(), "fonts/fredoka_semibold.ttf");

        layout = new GameLayout(this);
        layout.backdrop = new Backdrop(this);
        board = new BoardView(this);
        program = new ProgramView(this);
        program.setListener(this::refreshButtons);

        title = new TextView(this);
        title.setTypeface(face);
        title.setTextColor(Art.PLUM);
        title.setGravity(Gravity.CENTER);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);

        Resources res = getResources();
        CandyButton home = new CandyButton(this, CandyButton.ICON_HOME, 0, Art.PINK, Art.PINK_D, null, face);
        home.setContentDescription(res.getString(R.string.cd_home));
        home.setOnClickListener(v -> finish());

        int[] dirs = {Level.UP, Level.RIGHT, Level.DOWN, Level.LEFT};
        int[] fills = {Art.PINK, Art.YELLOW, Art.BLUE, Art.LILAC};
        int[] deeps = {Art.PINK_D, Art.YELLOW_D, Art.BLUE_D, Art.LILAC_D};
        int[] names = {R.string.cd_up, R.string.cd_right, R.string.cd_down, R.string.cd_left};
        arrows = new CandyButton[4];
        for (int i = 0; i < 4; i++) {
            final int d = dirs[i];
            arrows[i] = new CandyButton(this, CandyButton.ICON_ARROW, d, fills[i], deeps[i], null, face);
            arrows[i].setContentDescription(res.getString(names[i]));
            arrows[i].setOnClickListener(v -> {
                if (program.add(d)) sounds.tap();
            });
        }
        undo = new CandyButton(this, CandyButton.ICON_UNDO, 0, 0xFFC9CDE0, 0xFF9AA0BC, null, face);
        undo.setContentDescription(res.getString(R.string.cd_undo));
        undo.setOnClickListener(v -> {
            if (program.removeLast()) sounds.tap();
        });
        go = new CandyButton(this, CandyButton.ICON_PLAY, 0, Art.MINT, Art.MINT_D,
                res.getString(R.string.go), face);
        go.setContentDescription(res.getString(R.string.go));
        go.setOnClickListener(v -> run());

        layout.home = home;
        layout.title = title;
        layout.board = board;
        layout.program = program;
        layout.up = arrows[0];
        layout.right = arrows[1];
        layout.down = arrows[2];
        layout.left = arrows[3];
        layout.undo = undo;
        layout.go = go;
        for (View v : new View[]{layout.backdrop, home, title, board, program, arrows[0], arrows[1],
                arrows[2], arrows[3], undo, go}) {
            layout.addView(v);
        }
        int sb = res.getIdentifier("status_bar_height", "dimen", "android");
        layout.setTopInset(sb > 0 ? res.getDimensionPixelSize(sb) : 0);

        FrameLayout root = new FrameLayout(this);
        root.addView(layout);
        sticker = new StickerView(this, face);
        sticker.setVisibility(View.GONE);
        root.addView(sticker);
        stickerButtons = new LinearLayout(this);
        stickerButtons.setGravity(Gravity.CENTER);
        int btnH = Math.round(72 * res.getDisplayMetrics().density);
        int btnW = Math.round(150 * res.getDisplayMetrics().density);
        int m = Math.round(10 * res.getDisplayMetrics().density);
        CandyButton more = new CandyButton(this, CandyButton.ICON_NONE, 0, Art.MINT, Art.MINT_D,
                res.getString(R.string.more), face);
        more.setContentDescription(res.getString(R.string.more));
        more.setOnClickListener(v -> {
            hideSticker();
            loadLevel(level.number + 1);
        });
        CandyButton done = new CandyButton(this, CandyButton.ICON_NONE, 0, Art.PEACH, Art.PEACH_D,
                res.getString(R.string.done), face);
        done.setContentDescription(res.getString(R.string.done));
        done.setOnClickListener(v -> {
            hideSticker();
            finish();
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(btnW, btnH);
        lp.setMargins(m, 0, m, 0);
        stickerButtons.addView(more, lp);
        stickerButtons.addView(done, new LinearLayout.LayoutParams(lp));
        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM);
        flp.bottomMargin = Math.round(40 * res.getDisplayMetrics().density);
        stickerButtons.setVisibility(View.GONE);
        root.addView(stickerButtons, flp);
        setContentView(root);

        int furthest = prefs.getInt(KEY_LEVEL, 1);
        int start = getIntent().getIntExtra(EXTRA_LEVEL, furthest);
        loadLevel(Math.max(1, Math.min(start, furthest)));
    }

    private void loadLevel(int n) {
        level = Level.get(n);
        misses = 0;
        board.setLevel(level);
        program.setSlots(level.slots());
        program.setLocked(false);
        title.setText(getString(R.string.level, n));
        refreshButtons();
    }

    private void refreshButtons() {
        boolean idle = !board.isRunning();
        for (CandyButton a : arrows) a.setEnabled(idle && program.size() < program.slots());
        undo.setEnabled(idle && program.size() > 0);
        go.setEnabled(idle && program.size() > 0);
    }

    private void run() {
        if (board.isRunning() || program.size() == 0) return;
        program.setLocked(true);
        board.run(program.program(), this);
        refreshButtons();
    }

    @Override
    public void onStep(int index) {
        program.mark(index, ProgramView.MARK_NOW);
        sounds.step();
    }

    @Override
    public void onBump(int index) {
        sounds.bump();
        program.mark(index, ProgramView.MARK_ERROR);
        miss();
    }

    @Override
    public void onShort() {
        program.mark(program.size() < program.slots() ? program.size() : -1, ProgramView.MARK_NEXT);
        miss();
    }

    /** Back to the start with the program kept, so she fixes it rather than starting over. */
    private void miss() {
        misses++;
        handler.postDelayed(() -> {
            board.reset();
            program.setLocked(false);
            if (misses >= 2) {
                int[] path = level.solution();
                board.showHint(path, misses == 2 ? (path.length + 1) / 2 : path.length);
            }
            refreshButtons();
        }, 1100);
    }

    @Override
    public void onWin(int stepsUsed) {
        sounds.win();
        program.clearMark();
        int next = level.number + 1;
        int stars = prefs.getInt(KEY_STARS, 0) + 1;
        final boolean earned = stars % STARS_PER_STICKER == 0;
        final int stickers = prefs.getInt(KEY_STICKERS, 0) + (earned ? 1 : 0);
        // Replaying an old level never moves the furthest level back.
        prefs.edit().putInt(KEY_LEVEL, Math.max(next, prefs.getInt(KEY_LEVEL, 1))).putInt(KEY_STARS, stars)
                .putInt(KEY_STICKERS, stickers).apply();
        refreshButtons();
        handler.postDelayed(() -> {
            if (earned) {
                sticker.show(stickers);
                stickerButtons.setVisibility(View.VISIBLE);
            } else {
                loadLevel(next);
            }
        }, 1900);
    }

    private void hideSticker() {
        sticker.setVisibility(View.GONE);
        stickerButtons.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        sounds.release();
        super.onDestroy();
    }
}
