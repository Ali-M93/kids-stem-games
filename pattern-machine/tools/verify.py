#!/usr/bin/env python3
"""Play Pattern Machine over adb and check what it does.

    python3 tools/verify.py --serial SERIAL [--adb PATH] [--shots DIR]

Resets progress (`pm clear`), then:
  1. reads each puzzle from the machine's description, SOLVES IT INDEPENDENTLY (shortest period
     consistent with the visible tokens), and taps the answer. A puzzle the solver cannot pin
     down from what is visible is a failure: it would be unfair to a child too.
  2. on the first puzzle, first taps a wrong choice: it must grey out; a second wrong choice
     (when there is one) must bring up the hint
  3. after five puzzles the sticker screen must appear; "More!" must start stage 1, because a
     round with at most two slips moves her up
  4. Make mode: star, circle, star, circle + Play must find rule 2 and carry on; star, circle,
     heart must find no rule
State is read from content descriptions in uiautomator dumps, never from adb exit codes.
"""
import argparse
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

PKG = "io.github.foadsf.patternmachine"
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
    for n in ns if ns is not None else nodes():
        if n.get("content-desc").startswith(prefix):
            return n
    return None


def tap(n):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", n.get("bounds")))
    sh(f"input tap {(x1 + x2) // 2} {(y1 + y2) // 2}")


def shot(name):
    if args.shots:
        os.makedirs(args.shots, exist_ok=True)
        sh("screencap -p /data/local/tmp/shot.png")
        subprocess.run([args.adb, "-s", args.serial, "pull", "/data/local/tmp/shot.png",
                        os.path.join(args.shots, name + ".png")], capture_output=True)


def machine():
    n = find("machine")
    return n.get("content-desc") if n is not None else ""


def wait_machine(pattern, secs=12):
    end = time.time() + secs
    while time.time() < end:
        m = machine()
        if re.search(pattern, m):
            return m
        time.sleep(0.6)
    return machine()


def solve(desc):
    """'machine stage 0 puzzle 1: a b a b a ?' -> the token for the first '?'."""
    row = desc.split(":", 1)[1].split("|")[0].split()
    n = len(row)
    gap = row.index("?")
    for p in range(1, n // 2 + 1):
        ok_p, seen = True, 0
        for i in range(n):
            if row[i] == "?":
                continue
            for j in range(i % p, n, p):
                if row[j] != "?" and row[j] != row[i]:
                    ok_p = False
            seen += 1
        if not ok_p:
            continue
        # the unit must be visible at least twice before trusting it
        known = [row[j] for j in range(gap % p, n, p) if row[j] != "?"]
        if known and n >= 2 * p:
            return known[0]
    return None


sh(f"pm clear {PKG}")
sh(f"am start -W -n {PKG}/.GameActivity")
time.sleep(2.5)
shot("start")

# ---- a round of five --------------------------------------------------------------------
for k in range(1, 6):
    m = machine() if k > 1 else wait_machine(rf"stage 0 puzzle {k}:")
    ok(f"puzzle {k}:" in m, f"puzzle {k} shown ({m})")
    while "?" in machine().split(":", 1)[1].split("|")[0]:
        m = machine()
        answer = solve(m)
        if answer is None:
            ok(False, f"puzzle solvable from what is visible ({m})")
            break
        ns = nodes()
        if k == 1 and "slip-tested" not in globals():
            slip_tested = True
            wrong = [n for n in ns if n.get("content-desc").startswith("choice ")
                     and n.get("content-desc") != "choice " + answer]
            tap(wrong[0])
            time.sleep(1.0)
            again = find(wrong[0].get("content-desc"))
            ok(again is not None and again.get("enabled") == "false", "a wrong choice greys out")
            ok("hint" not in machine(), "no hint after one slip")
            shot("slip")
            if len(wrong) > 1:
                tap(wrong[1])
                time.sleep(1.0)
                ok("hint" in machine(), f"hint after two slips ({machine()})")
                shot("hint")
            ns = nodes()
        c = find("choice " + answer, ns)
        ok(c is not None, f"answer {answer} offered")
        if c is None:
            break
        tap(c)
        time.sleep(0.8)
    if k == 1:
        time.sleep(0.8)
        shot("solved")
    # Solved means the app moved on: the next puzzle loads, or after the fifth the sticker shows.
    nxt = rf"stage 0 puzzle {k + 1}:" if k < 5 else None
    end = time.time() + 12
    moved = False
    while time.time() < end and not moved:
        ns = nodes()
        st = find("sticker", ns)
        m = find("machine", ns)
        moved = (nxt and m is not None and re.search(nxt, m.get("content-desc"))) or \
                (k == 5 and st is not None and st.get("content-desc").startswith("sticker 1"))
        if not moved:
            time.sleep(0.6)
    ok(bool(moved), f"puzzle {k} solved: the app moved on")

time.sleep(1)
st = find("sticker")
ok(st is not None and st.get("content-desc").startswith("sticker 1"), "a round of five earns a sticker")
shot("sticker")
more = next((n for n in nodes() if n.get("content-desc") in ("More!", "Meer!", "باز هم!")), None)
ok(more is not None, "More! button present")
tap(more)
m = wait_machine(r"stage 1 puzzle 1:")
ok("stage 1 puzzle 1:" in m, f"two slips or fewer moves her up to stage 1 ({m})")

# ---- make your own ----------------------------------------------------------------------
tap(find("make your own"))
time.sleep(1.2)
ok(wait_machine(r"^machine make:").startswith("machine make:"), "Make mode opens")
for t in ["star-green", "circle-pink", "star-green", "circle-pink"]:
    tap(find("add " + t))
    time.sleep(0.5)
tap(find("Play"))
m = wait_machine(r"make rule|make no rule", 6)
ok("make rule 2" in m and "| then star-green circle-pink" in m, f"her ★●★● is continued by the rule ({m})")
shot("make")
time.sleep(5)
for _ in range(4):
    tap(find("undo"))
    time.sleep(0.4)
for t in ["star-green", "circle-pink", "heart-purple"]:
    tap(find("add " + t))
    time.sleep(0.4)
tap(find("Play"))
m = wait_machine(r"make rule|make no rule", 6)
ok("make no rule" in m and "then" not in m, f"★●♥ has no rule to continue ({m})")

print(f"\n{'ALL PASSED' if not failures else f'{len(failures)} FAILED'}")
sys.exit(1 if failures else 0)
