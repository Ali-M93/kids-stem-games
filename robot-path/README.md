# Robot Path

A first coding game for a five-year-old. She taps arrows to build a program, presses **Go**, and
a little robot follows it, one step at a time, to the star.

It trains **sequencing, planning and debugging**: the core of programming, with no reading
needed. When the robot bumps into a rock or a wall, the step that did it turns **red**. She fixes
that step instead of starting over: tap a step in the program to take it out, or use the undo key.

Like the other games in this collection, it runs on Android 4.4 (API 19) and up, from a 4.3" Galaxy S4 mini to a tablet,
in either orientation. It uses plain framework Views in Java, has no dependencies and no
permissions, and the APK is about 70 KB. All the art and the sounds are made in code.

## How it plays

- **Levels 1-3**: two moves on a 3×3 board: straight, straight up, then the first turn.
- **4-6**: three and four moves, then the first rock to go around.
- **7 and up**: generated from the level number, so level 9 is always the same board. Boards grow
  to 5×5 and 6×6, paths to nine moves, with more rocks. Past level 8 the shortest way always turns.
- She gets the shortest program's length **plus one or two spare slots**, so she can wander a
  bit but cannot brute-force it.
- **Misses**: on a bump, the bad step turns red. If the program ends short of the star, the next
  empty slot glows green ("add more"). The robot goes back to the start and **her program is kept**.
- **Hints**: after a second miss on the same board, dotted footprints show the first half of the
  way; after a third, all of it. Patience over frustration.
- **Rounds**: every star is saved; every **3 stars earn a sticker** (rocket, planet, flower, heart),
  then a clear choice: **More!** or **Done**. There is no endless play, no timer, no random reward.
- Progress is kept on the phone. Clearing the app's data (*Settings → Apps → Robot Path → Clear
  data*) starts again at level 1.

Every level is checked by breadth-first search when it is created. An unsolvable board cannot
load. Levels 1-80 were checked on 2026-10-18: each one is solvable, the same every time, and has
enough slots, and a deliberately walled-off board was rejected.

## Languages

English, Dutch and **Farsi**, following the device language. In Farsi the level number uses
Persian digits (مرحله ۱). The program strip always runs left to right, because the arrows are
real directions on the board, not reading order. The rounded font has no Persian letters, so
Farsi text uses the system font for now.

## Build and install

```bash
./gradlew assembleDebug lintDebug
```

`lintDebug` fails the build on any call newer than API 19. If a `debug.keystore` sits in this
folder or the repository root (both gitignored), debug builds are signed with it, so builds from
several machines can update one installed copy.

`tools/check_logic.sh` checks the puzzle logic on any JDK, without Android or Gradle.

[Kid Launcher](https://github.com/Foadsf/kid-launcher), a home screen for young children, lists
this game on its Games page as **Robot** (from Kid Launcher 0.7.0).

## Verify

`tools/verify.py` plays the game over adb. It resets progress, then checks each of these:
- a wrong program bumps at step 0;
- undo empties the program, and Go is then disabled;
- a too-short program stops short of the star;
- the right program wins, and level 2 loads by itself;
- levels 2 and 3 are solved;
- the third star shows the sticker screen, and **More!** carries on to level 4.

State is read from the board's content description, such as `board level 1 ready, last bump 0`.

```bash
python3 tools/verify.py --serial SERIAL --shots /tmp/shots
```

On 2026-10-18 it passed on two API 19 emulators: one shaped like the S4 mini (540×960, 240 dpi,
portrait) and one tablet (800×1280, 213 dpi, landscape). The Farsi screen was checked by
screenshot. **Real devices are still to do.**

## Ideas for later

- Voice prompts recorded by parents, as Kangaroo Hops already supports (no robot voices).
- **Repeat ×2** blocks: a first loop.
- A Persian rounded font (Vazirmatn, OFL) for the Farsi text.
- Optional extra levels made by a parent-run home server (not part of the app).
