# Third-party material in this app

| What | File | Source | Licence |
|---|---|---|---|
| Fredoka font (static SemiBold instance, Latin subset) | `app/src/main/assets/fonts/fredoka_semibold.ttf` | [google/fonts `ofl/fredoka`](https://github.com/google/fonts/tree/main/ofl/fredoka), Copyright 2016 The Fredoka Project Authors | SIL Open Font License 1.1 - [`Fredoka-OFL.txt`](Fredoka-OFL.txt) |

The font file is the same one Kid Launcher bundles, made there by `tools/make_font.sh`
(instanced and subset; Fredoka declares no Reserved Font Name, so it keeps its name).

Everything else is original to this app and drawn in code: the fruit, the scale, the fact cards,
buttons and stickers in `Art.java`, `ScaleView.java` and `GameActivity.java`, the sky in
`Backdrop.java`. The sounds are synthesised at start-up in `Sounds.java`. There are no library
dependencies.
