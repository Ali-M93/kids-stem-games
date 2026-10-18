# Balance Scale

*Make both sides weigh the same.* Fruit sits on the left pan. The child drags fruit from the
basket onto the right pan (or just taps it), and the beam tips and wobbles toward the heavier
side. When both sides weigh the same, the beam levels out and a big **=** appears. There is
nothing to read: the tilt says everything.

It trains equality, comparison and early algebra. Heavier fruit come with a **fact card**, a small
balanced scale showing that one watermelon weighs three apples, so she can reason "the watermelon
is like three apples", which is substitution, with fruit.

Like the other games in this collection, it runs on Android 4.4 (API 19) and up: phone or tablet, portrait or
landscape. It uses plain framework Views in Java, has no dependencies and no permissions, and the
APK is under 100 KB. The art and sounds are made in code.

## How it plays

Weights are counted in apples: apple 1, pear 1, banana 2, watermelon 3.

| Stage | Left pan | Basket | Idea |
|---|---|---|---|
| 0 | 1-3 apples | apple | the beam tips **live**, so she learns what tipping means |
| 1 | 3-6 apples | apple | how many? Decide first, then **Weigh** |
| 2 | 4-7 apples, right pan already has some | apple | 5 = 2 + ? |
| 3 | apples and pears | apple, pear | different things, same weight |
| 4 | one watermelon | apple | fact card: 🍉 = 🍎🍎🍎 |
| 5 | watermelon + 1-3 apples | apple | |
| 6 | 1-2 bananas + 0-2 apples | banana, apple | fact card: 🍌 = 🍎🍎; any mix that weighs the same balances |
| 7 | two heavy fruits; the right pan starts with a lighter one | banana, apple | both pans start full |
| 8+ | a mix of stages 4-7 | | |

**The peg.** From stage 1 on, a red peg holds the beam level while she loads the pan. She presses
**Weigh** to pull it, and only then does the beam show the truth. Without the peg, adding apples
one at a time always passes through "balanced", so she could just tap until it levels without
ever thinking about *how many*; the scripted play-through exposed exactly that. A weigh that does
not balance counts as a slip, and the peg goes back in as soon as she changes anything.

- **Taking fruit off:** tap a fruit she added, or use the undo key. Fruit that came with the puzzle
  has a small white dot and stays put.
- **Rounds:** five puzzles make a round and earn a sticker, then a clear More!/Done. A round with
  at most three slips moves her up a stage.
- The right pan holds at most 10 fruit.

## Checked (2026-10-18)

- **The generator, on the laptop:** 3,000 puzzles per stage for stages 0-11. The right pan always
  starts lighter; an apple is always in the basket, so every puzzle can be balanced; every fruit
  heavier than an apple in play has its fact card; no pan needs more than 7 fruit.
- **The app:** `tools/verify.py` plays it over adb on the API 19 emulator shaped like the S4 mini:
  - stage 0: a **real drag gesture** (swipe from the basket to the pan) adds an apple, and five
    puzzles are solved from the scale's own description;
  - the sticker, then More! moves her to stage 1;
  - stage 1: the peg holds the beam while she loads, a weigh one short tips left and counts a
    slip, adding fruit puts the peg back, and weighing again solves it;
  - stage 6: bananas count as two apples.

  All checks pass.
- **By eye:** the tilt, the peg, the fact cards and the banana pile.
- **Not yet:** a tablet, landscape, and a real phone.

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
this game on its Games page as **Balance** (from Kid Launcher 0.7.0).
