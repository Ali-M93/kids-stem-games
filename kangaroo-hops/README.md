# Kangaroo Hops

*Where will she land?* A kangaroo stands on a row of numbered stepping stones, a number line.
A card says **+3** or **−2**, drawn as hop arcs, and the child picks a number. Then the kangaroo
**hops one stone at a time, and each landing is counted aloud**, so the answer is checked by
counting, right in front of her. It trains counting, adding as hops forward, taking away as hops
back, and "more and less".

Like the other games in this collection, it runs on Android 4.4 (API 19) and up: phone or tablet, portrait or
landscape. It uses plain framework Views in Java, has no dependencies and no permissions, and the
APK is about 75 KB. The art and sound are made in code.

## How it plays

| Stage | What | Line |
|---|---|---|
| 0 | count from zero: start at 0, hop 1-5 | 0-10 |
| 1 | add 1 or 2 | 0-10 |
| 2 | add 3-5 | 0-10 |
| 3 | take away 1 or 2 | 0-10 |
| 4 | take away 3-5 | 0-10 |
| 5 | **which is more?** Two flags on the line; the bigger number is further along | 0-10 |
| 6 | add 2-5, not crossing ten | 0-20 |
| 7 | add 3-9 **crossing ten** (8 + 5) | 0-20 |
| 8 | take away 2-9 | 0-20 |
| 9+ | a mix of stages 5-8 | 0-20 |

- **Choices** are the answer and its neighbours (one off, two off), the classic slips. When the
  answer sits at the end of the line, the starting number ("forgot to hop") can be a choice too.
- **A wrong pick:** the kangaroo still hops and counts, so she sees where it really lands. Her
  button wobbles and greys out, the kangaroo slides back, and she tries again.
- **After two wrong picks**, dashed arcs mark every hop on the line.
- **Five puzzles make a round** and earn a sticker, then a clear More!/Done. A round with at most
  two wrong picks moves her up a stage.
- On a phone, 0-20 shows as a window of the line around the puzzle, with the stones still in
  order and the cut ends faded, so the stones stay big enough to read.

## Counting in Farsi, Dutch and English

The blue chip at the top right cycles **فا → NL → EN**. It sets the counting voice and the digits
on the stones, the card and the buttons; Farsi uses Persian digits (۰ ۱ ۲ … ۲۰). The rest of the
screen follows the device language.

**Voices, in order of preference:**

1. **Your recordings.** One short file per number in the app's folder on the phone:

   ```
   Android/data/io.github.foadsf.kangaroohops/files/voices/fa/0.m4a … 20.m4a
   Android/data/io.github.foadsf.kangaroohops/files/voices/nl/0.m4a … 20.m4a
   Android/data/io.github.foadsf.kangaroohops/files/voices/en/0.m4a … 20.m4a
   ```

   The app creates these folders, plus a `README.txt`, on first start. m4a, 3ga (from Samsung Voice
   Recorder), ogg, mp3, wav, aac and amr all work. Any number without a file falls back to the next
   option.
2. **The device's speech engine**, if it has one for that language.
3. **Nothing spoken.** The number pops up big above the kangaroo, and every stone plays its own
   note of a rising scale, so counting up sounds like climbing.

A debloated old phone may have no speech engine at all (the Galaxy S4 mini this was made for has
none), so on such a phone **recordings are what make it talk**. The chip reports which source it uses (TalkBack reads it, and `tools/verify.py`
prints it).

## Checked (2026-10-18)

- **The generator, on the laptop:** 3,000 puzzles per stage for stages 0-12. The answer is always
  among the choices; choices are distinct and on the line; the landing is right; stage 6 never
  crosses ten and stage 7 always does; "which is more" never compares equal numbers. Persian
  digits and the number words are also checked.
- **The app:** `tools/verify.py` plays it over adb on the API 19 emulator shaped like the S4 mini.
  It checks that:
  - two wrong picks each grey out, the kangaroo hops back to the start, and the second brings the
    hint;
  - each puzzle is solved from its own description;
  - five puzzles earn a sticker, and More! moves her up to stage 1;
  - the language chip cycles fa, nl, en;
  - a recording dropped into `voices/en/` is found and preferred;
  - a "which is more?" puzzle is solved.

  All checks pass. The emulator had an English speech engine but none for Farsi or Dutch.
- **By eye:** the hint arcs, the hop with its number popping up, Persian digits, the two flags, and
  landscape.
- **Not yet:** a tablet, and a real phone.

```bash
python3 tools/verify.py --serial SERIAL --shots /tmp/shots
```

## Build

```bash
./gradlew assembleDebug lintDebug
```

`lintDebug` fails the build on any call newer than API 19. If a `debug.keystore` sits in this
folder or the repository root (both gitignored), debug builds are signed with it, so builds from
several machines can update one installed copy.

`tools/check_logic.sh` checks the puzzle logic on any JDK, without Android or Gradle.

[Kid Launcher](https://github.com/Foadsf/kid-launcher), a home screen for young children, lists
this game on its Games page as **Hops** (from Kid Launcher 0.7.0).
