# Pattern Machine

*What comes next?* A friendly machine shows a row of shapes, such as ● ▲ ● ▲ ● ?, and she taps the
one that fits. Spotting and continuing patterns is the root of algebra, and it trains careful
looking and working memory.

Every shape sings its own note on a pentatonic scale, so each pattern is also a little tune, and
any row she makes sounds pleasant. When she gets one right, the machine plays the row back and
its lights chase.

Like the other games in this collection, it runs on Android 4.4 (API 19) and up: phone or tablet, portrait or
landscape. It uses plain framework Views in Java, has no dependencies and no permissions, and the
APK is about 70 KB. The art and sound are made in code. English, Dutch and Farsi.

## How it plays

| Stage | Pattern | Example | Choices |
|---|---|---|---|
| 0 | AB; the two tokens differ in shape **and** colour | ● ▲ ● ▲ ● ? | 2 |
| 1 | AB | ★ ■ ★ ■ ★ ? | 3 |
| 2 | AAB | ● ● ▲ ● ● ▲ ● ? | 3 |
| 3 | ABB | | 3 |
| 4 | ABC | | 3 |
| 5 | AB **by colour only**: one shape, the colour alternates | | 3, including a decoy with the right colour and the wrong shape |
| 6 | AB or ABC with the gap **in the middle** | ● ▲ ? ▲ ● ▲ | 3 |
| 7 | AABB | | 4 |
| 8 | ABC with the **last two** missing, filled in order | ● ▲ ■ ● ▲ ■ ? ? | 4 |
| 9+ | a mix of stages 4-8 | | |

- **A wrong pick** wobbles, boops softly and greys out, so she narrows the choices down. There is
  no penalty.
- **After two wrong picks**, coloured bars underline each repeat of the rule. That is the hint.
- **Five puzzles make a round**, and each round earns a sticker, then a clear **More!** or
  **Done**. A round with at most two wrong picks moves her up a stage; otherwise she stays on that
  stage, with fresh puzzles.
- **Make mode** (the palette button): she builds her own row of up to 8 tokens and presses Play.
  The machine sings it. If her row repeats, for example ★ ● ★ ●, it **finds her rule and carries
  on with it** in paler tokens. If it doesn't repeat, it just plays her tune. This is "patterns
  she invents herself".

Progress (stage, stars, stickers) is kept on the device. *Clear data* in the app's settings starts
again at stage 0.

## Checked

- **The generator, on the laptop:** 2,000 puzzles per stage for stages 0-12, plus the rule
  finder. Every answer is among the choices, there are no duplicate choices, each full row has
  exactly the intended rule (its shortest period is the unit), at least two repeats show before
  the gap, colour-only rows keep one shape, and rows never exceed 9 tokens.
- **The app:** `tools/verify.py` plays it over adb. It solves each puzzle **independently**, from
  only what the machine shows, then checks:
  - a wrong pick greys out, and one slip gives no hint yet;
  - five puzzles earn a sticker, and More! moves her to stage 1;
  - in Make mode, ★●★● is continued by rule 2 and ★●♥ has no rule.

  All checks passed on 2026-10-18 on the API 19 emulator shaped like the S4 mini.
- **Checked by hand:** stage 8, where two wrong picks bring up the hint and both gaps fill in
  order, and the landscape layout. **A tablet and a real phone are still to do.**

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
this game on its Games page as **Patterns** (from Kid Launcher 0.7.0).
