# Contributing

Thank you for helping. These games are for small children on old phones; they stay useful by
staying small, calm and private.

- **Calm by design.** No timers, countdowns, random rewards, streaks, ads or endless play. Rounds
  end with a clear choice. A wrong answer is a gentle wobble, never a penalty.
- **No permissions, ever.** No network, storage, analytics or crash reporting. CI fails if an APK
  requests any permission.
- **API 19 stays supported.** No AndroidX, no Compose, no library dependencies. `lintDebug` fails
  on any call newer than API 19; keep it that way instead of suppressing it.
- **Pure puzzle logic.** Keep each game's generator in plain Java (no Android types) so
  `tools/check_logic.sh` can check it on any JDK, and extend the checker when you change the
  rules.
- **Art and sound in code.** Do not add bitmaps or sound files unless they are CC0 or similar and
  recorded in that game's `licenses/README.md`.
- **Languages.** Add every new string to `values/`, `values-nl/` and `values-fa/`.
- **Touch targets of 56 dp and up**, in portrait and landscape, on a 4.3" phone and a tablet.
- **Pin versions exactly**, and change one thing per pull request.

## Pull requests

1. Run `tools/check_logic.sh` and `./gradlew assembleDebug lintDebug` in the game you changed.
2. If you can, run `tools/verify.py` on an API 19 emulator or an old phone, and check both
   orientations for layout changes. Say what you tested on.

By contributing, you agree that your work is licensed under the GPL-3.0.
