# Third-party material in this app

| What | File | Source | Licence |
|---|---|---|---|
| Fredoka font (static SemiBold instance, Latin subset) | `app/src/main/assets/fonts/fredoka_semibold.ttf` | [google/fonts `ofl/fredoka`](https://github.com/google/fonts/tree/main/ofl/fredoka), Copyright 2016 The Fredoka Project Authors | SIL Open Font License 1.1 - [`Fredoka-OFL.txt`](Fredoka-OFL.txt) |

The font file is the same one Kid Launcher bundles, made there by `tools/make_font.sh`
(instanced and subset; Fredoka declares no Reserved Font Name, so it keeps its name).

Everything else is original to this app and drawn in code: the kangaroo, stones, flags, buttons
and stickers in `Art.java` and `LineView.java`, the sky in `Backdrop.java`. The notes are
synthesised at start-up in `Sounds.java`. No voices are bundled: counting aloud uses recordings
the family adds (see the README) or the device's own speech engine. There are no library
dependencies.
