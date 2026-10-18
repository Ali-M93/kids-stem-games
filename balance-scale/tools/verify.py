#!/usr/bin/env python3
"""Play Balance Scale over adb and check what it does.

    python3 tools/verify.py --serial SERIAL [--adb PATH] [--shots DIR]

Resets progress, then:
  1. stage 0 (the beam tips live): DRAGS an apple onto the right pan with a real swipe gesture,
     then solves all five puzzles from the scale's description (left weight minus right weight
     = apples to add); the round earns a sticker and "More!" starts stage 1
  2. stage 1 (a peg holds the beam until Weigh): one apple short + Weigh -> the beam shows
     "left heavier" and a slip is counted; adding the missing apple puts the peg back in; Weigh
     again -> solved
  3. stage 6 (debug build, run-as): bananas count as two apples; a mix of bananas and apples
     balances when weighed
State is read from content descriptions in uiautomator dumps, never from adb exit codes.
"""
import argparse
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

PKG = "io.github.foadsf.balancescale"
ap = argparse.ArgumentParser()
ap.add_argument("--serial", required=True)
ap.add_argument("--adb", default=os.environ.get("ADB", "adb"))
ap.add_argument("--shots")
args = ap.parse_args()
failures = []


def sh(cmd):
    for _ in range(6):
        p = subprocess.run([args.adb, "-s", args.serial, "shell", cmd], capture_output=True, text=True)
        if p.returncode == 0:
            return "\n".join(l for l in p.stdout.splitlines() if "WARNING: linker" not in l)
        time.sleep(2)
    return ""


def ok(cond, what):
    print(("PASS " if cond else "FAIL ") + what)
    if not cond:
        failures.append(what)


def nodes():
    sh("uiautomator dump /data/local/tmp/ui.xml")
    xml = sh("cat /data/local/tmp/ui.xml")
    root = ET.fromstring(xml[xml.find("<?xml"): xml.rfind(">") + 1])
    return [n for n in root.iter("node") if n.get("content-desc")]


def find(prefix, ns=None):
    for n in (nodes() if ns is None else ns):
        if n.get("content-desc").startswith(prefix):
            return n
    return None


def box(n):
    return list(map(int, re.findall(r"\d+", n.get("bounds"))))


def tap(n):
    x1, y1, x2, y2 = box(n)
    sh(f"input tap {(x1 + x2) // 2} {(y1 + y2) // 2}")
    time.sleep(0.35)


def shot(name):
    if args.shots:
        os.makedirs(args.shots, exist_ok=True)
        sh("screencap -p /data/local/tmp/shot.png")
        subprocess.run([args.adb, "-s", args.serial, "pull", "/data/local/tmp/shot.png",
                        os.path.join(args.shots, name + ".png")], capture_output=True)


def scale():
    n = find("scale")
    return n.get("content-desc") if n is not None else ""


def weights(d):
    m = re.search(r"left.* = (\d+); right.* = (\d+);", d)
    return (int(m.group(1)), int(m.group(2))) if m else (None, None)


def wait_scale(pattern, secs=12):
    end = time.time() + secs
    while time.time() < end:
        d = scale()
        if re.search(pattern, d):
            return d
        time.sleep(0.5)
    return scale()


def start(stage=None):
    sh(f"am force-stop {PKG}")
    sh(f"pm clear {PKG}")
    if stage is not None:
        xml = ('<?xml version=\\"1.0\\" encoding=\\"utf-8\\" standalone=\\"yes\\" ?><map>'
               f'<int name=\\"stage\\" value=\\"{stage}\\" /></map>')
        sh(f"run-as {PKG} sh -c 'mkdir -p shared_prefs; echo \"{xml}\" > shared_prefs/progress.xml'")
    sh(f"am start -W -n {PKG}/.GameActivity")
    time.sleep(2.5)


def moved_on(k):
    """Solved = the app moves on: next puzzle, or the sticker after the fifth."""
    end = time.time() + 12
    while time.time() < end:
        ns = nodes()
        s, st = find("scale", ns), find("sticker", ns)
        if k < 5 and s is not None and f"puzzle {k + 1}:" in s.get("content-desc"):
            return True
        if k == 5 and st is not None and st.get("content-desc").startswith("sticker 1"):
            return True
        time.sleep(0.6)
    return False


start()
shot("start")
# ---- stage 0: live tilt, a drag, a full round ------------------------------------------
for k in range(1, 6):
    d = wait_scale(rf"puzzle {k}:")
    L, R = weights(d)
    ok(L is not None and L > R and "latched" not in d, f"stage 0 puzzle {k}: live, left {L} > right {R}")
    apple = find("add apple")
    need = L - R
    if k == 1:
        x1, y1, x2, y2 = box(apple)
        sc = box(find("scale"))
        # the right pan hangs at about 79% across and 52% down the scale view
        tx, ty = sc[0] + int((sc[2] - sc[0]) * 0.79), sc[1] + int((sc[3] - sc[1]) * 0.52)
        sh(f"input swipe {(x1 + x2) // 2} {(y1 + y2) // 2} {tx} {ty} 900")
        time.sleep(0.8)
        d = scale()
        L2, R2 = weights(d)
        ok(R2 == R + 1 or "solved" in d or "puzzle 2:" in d,
           f"dragging an apple onto the pan adds it ({d})")
        shot("dragged")
        need = need - 1
    for _ in range(need):
        tap(apple)
    ok(moved_on(k), f"stage 0 puzzle {k} balanced: the app moved on")
shot("sticker")
more = next((n for n in nodes() if n.get("content-desc") in ("More!", "Meer!", "باز هم!")), None)
ok(more is not None, "a round of five earns a sticker with More!")
tap(more)

# ---- stage 1: the peg, Weigh, a slip, then right ----------------------------------------
d = wait_scale(r"stage 1 puzzle 1:")
ok("latched" in d, f"stage 1 starts with the peg in ({d})")
L, R = weights(d)
apple = find("add apple")
for _ in range(L - R - 1):
    tap(apple)
d = scale()
ok("latched" in d, "the beam stays level while she loads it")
tap(find("Weigh"))
time.sleep(0.8)
d = scale()
ok("left heavier" in d and "latched" not in d and "slips 1" in d,
   f"weighing one short shows left heavier and counts a slip ({d})")
shot("weighed-short")
tap(apple)
d = scale()
ok("latched" in d, f"adding fruit puts the peg back in ({d})")
tap(find("Weigh"))
time.sleep(0.6)
d = scale()
ok("solved" in d or "puzzle 2:" in d, f"weighing a balanced scale solves it ({d})")
shot("weighed-right")

# ---- bananas weigh two apples ----------------------------------------------------------
start(6)
d = wait_scale(r"stage 6 puzzle 1:")
f = find("facts")
ok(f is not None and "banana=2" in f.get("content-desc"), "the fact card says a banana weighs two apples")
L, R = weights(d)
for _ in range(L // 2):
    tap(find("add banana"))
if L % 2:
    tap(find("add apple"))
tap(find("Weigh"))
time.sleep(0.6)
d = scale()
ok("solved" in d or "puzzle 2:" in d, f"{L // 2} banana(s) + {L % 2} apple balance {L} when weighed ({d})")
shot("bananas")

print(f"\n{'ALL PASSED' if not failures else f'{len(failures)} FAILED'}")
sys.exit(1 if failures else 0)
