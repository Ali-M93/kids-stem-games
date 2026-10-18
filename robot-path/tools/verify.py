#!/usr/bin/env python3
"""Play Robot Path over adb and check what it does.

    python3 -I tools/verify.py --serial SERIAL [--adb PATH] [--shots DIR]

Resets progress (`pm clear`), then on level 1:
  1. a wrong program (left, left) bumps: the board reports the bump at step 0
  2. undo empties the program; Go is then disabled
  3. a too-short program (right) stops short of the star
  4. after that second miss a hint appears (checked by screenshot only)
  5. the right program (right, right) wins, and level 2 loads by itself
Then levels 2 and 3 are solved from their known answers, and the third star must show the
sticker screen; "More!" must carry on to level 4.

State is read from content descriptions ("board level 1 won", "program right right") in
uiautomator dumps, never from adb exit codes. Works on API 19, where adb has no exec-out.
"""
import argparse
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

PKG = "io.github.foadsf.robotpath"
ap = argparse.ArgumentParser()
ap.add_argument("--serial", required=True)
ap.add_argument("--adb", default=os.environ.get("ADB", "adb"))
ap.add_argument("--shots")
args = ap.parse_args()
failures = []


def sh(cmd):
    for _ in range(6):
        p = subprocess.run([args.adb, "-s", args.serial, "shell", cmd], capture_output=True, text=True)
        out = "\n".join(l for l in p.stdout.splitlines() if "WARNING: linker" not in l)
        if p.returncode == 0:
            return out
        time.sleep(2)
    return out


def ok(cond, what):
    print(("PASS " if cond else "FAIL ") + what)
    if not cond:
        failures.append(what)


def dump():
    sh("uiautomator dump /data/local/tmp/ui.xml")
    xml = sh("cat /data/local/tmp/ui.xml")
    return ET.fromstring(xml[xml.find("<?xml"): xml.rfind(">") + 1])


def nodes():
    return {n.get("content-desc"): n for n in dump().iter("node") if n.get("content-desc")}


def find(prefix):
    for desc, n in nodes().items():
        if desc.startswith(prefix):
            return desc, n
    return None, None


def tap(desc):
    d, n = find(desc)
    if n is None:
        ok(False, f"button '{desc}' present")
        return
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", n.get("bounds")))
    sh(f"input tap {(x1 + x2) // 2} {(y1 + y2) // 2}")
    time.sleep(0.4)


def board():
    return find("board")[0] or ""


def wait_board(pattern, secs=10):
    end = time.time() + secs
    while time.time() < end:
        b = board()
        if re.search(pattern, b):
            return b
        time.sleep(0.5)
    return board()


def shot(name):
    if args.shots:
        os.makedirs(args.shots, exist_ok=True)
        sh("screencap -p /data/local/tmp/shot.png")
        subprocess.run([args.adb, "-s", args.serial, "pull", "/data/local/tmp/shot.png",
                        os.path.join(args.shots, name + ".png")], capture_output=True)


def play(*moves):
    for m in moves:
        tap(m)
    tap("Go") if find("Go")[1] is not None else tap("go")


sh(f"pm clear {PKG}")
sh(f"am start -W -n {PKG}/.GameActivity")
time.sleep(2)
ok(board().startswith("board level 1 ready"), f"starts on level 1 ({board()})")
shot("level1")

# 1. bump
play("left", "left")
b = wait_board(r"bump|won|short")
ok(b.endswith("bump 0"), f"left,left bumps at step 0 ({b})")
shot("bump")
time.sleep(1.5)

# 2. undo
tap("undo")
tap("undo")
ok(find("program")[0] == "program", f"undo empties the program ({find('program')[0]})")
go = find("Go")[1]
ok(go is not None and go.get("enabled") == "false", "Go is disabled with an empty program")

# 3. short
play("right")
b = wait_board(r"last short|level 1 short")
ok(b.endswith("short"), f"right alone stops short ({b})")
time.sleep(1.6)
shot("hint")
tap("undo")

# 5. win, then level 2 loads by itself
play("right", "right")
b = wait_board(r"won")
ok(b.startswith("board level 1 won"), f"right,right wins ({b})")
shot("won")
b = wait_board(r"level 2 ready", 6)
ok(b.startswith("board level 2 ready"), f"level 2 loads by itself ({b})")

play("up", "up")
ok("won" in wait_board(r"won"), "level 2: up,up wins")
wait_board(r"level 3 ready", 6)
play("right", "up")
ok("won" in wait_board(r"won"), "level 3: right,up wins")
time.sleep(2.5)
d, _ = find("sticker")
ok(d is not None and d.startswith("sticker 1"), f"third star shows the first sticker ({d})")
shot("sticker")
tap("More!") if find("More!")[1] is not None else tap("Meer!")
b = wait_board(r"level 4 ready", 6)
ok(b.startswith("board level 4 ready"), f"More! carries on to level 4 ({b})")

print(f"\n{'ALL PASSED' if not failures else f'{len(failures)} FAILED'}")
sys.exit(1 if failures else 0)
