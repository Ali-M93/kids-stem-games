#!/usr/bin/env python3
"""Play Kangaroo Hops over adb and check what it does.

    python3 tools/verify.py --serial SERIAL [--adb PATH] [--shots DIR]

Resets progress, then:
  1. puzzle 1: two WRONG picks first. Each greys out, the kangaroo hops back to the start, and
     the second brings the hint. Then the right pick solves it.
  2. puzzles 2-5 are solved from the line's description ("0 plus 3" -> 3), so the app has to
     describe a puzzle that is actually solvable
  3. the round earns a sticker; "More!" starts stage 1 (two slips or fewer move her up)
  4. the counting-language chip cycles fa -> nl -> en and reports which voice it will use
  5. with progress set to stage 5 (debug build, run-as), a "which is more?" puzzle is solved
State is read from content descriptions in uiautomator dumps, never from adb exit codes.
"""
import argparse
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

PKG = "io.github.foadsf.kangaroohops"
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


def tap(n):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", n.get("bounds")))
    sh(f"input tap {(x1 + x2) // 2} {(y1 + y2) // 2}")


def shot(name):
    if args.shots:
        os.makedirs(args.shots, exist_ok=True)
        sh("screencap -p /data/local/tmp/shot.png")
        subprocess.run([args.adb, "-s", args.serial, "pull", "/data/local/tmp/shot.png",
                        os.path.join(args.shots, name + ".png")], capture_output=True)


def line():
    n = find("line")
    return n.get("content-desc") if n is not None else ""


def wait_line(pattern, secs=15):
    end = time.time() + secs
    while time.time() < end:
        d = line()
        if re.search(pattern, d):
            return d
        time.sleep(0.6)
    return line()


def answer(desc):
    m = re.search(r"(\d+) (plus|minus) (\d+)", desc)
    if m:
        a, op, b = int(m.group(1)), m.group(2), int(m.group(3))
        return a + b if op == "plus" else a - b
    m = re.search(r"more of (\d+) and (\d+)", desc)
    return max(int(m.group(1)), int(m.group(2))) if m else None


def start(stage_xml=None):
    sh(f"am force-stop {PKG}")
    sh(f"pm clear {PKG}")
    if stage_xml is not None:
        xml = ('<?xml version=\\"1.0\\" encoding=\\"utf-8\\" standalone=\\"yes\\" ?><map>'
               f'<int name=\\"stage\\" value=\\"{stage_xml}\\" /></map>')
        sh(f"run-as {PKG} sh -c 'mkdir -p shared_prefs; echo \"{xml}\" > shared_prefs/progress.xml'")
    sh(f"am start -W -n {PKG}/.GameActivity")
    time.sleep(2.5)


start()
shot("start")
for k in range(1, 6):
    d = wait_line(rf"puzzle {k} ")
    ok(f"puzzle {k} " in d, f"puzzle {k} shown ({d})")
    ans = answer(d)
    ok(ans is not None, f"puzzle {k} readable")
    if k == 1:
        ns = nodes()
        wrong = [n for n in ns if n.get("content-desc").startswith("choice ")
                 and n.get("content-desc") != f"choice {ans}"]
        for i, wbtn in enumerate(wrong[:2]):
            tap(wbtn)
            d = wait_line(r"slips %d" % (i + 1), 15)
            ok(f"slips {i + 1}" in d, f"wrong pick {i + 1} counted ({d})")
            st = re.search(r"(\d+) (?:plus|minus) \d+", d).group(1)
            ok(d.endswith(f"kangaroo at {st}"), f"the kangaroo hopped back to the start, {st} ({d})")
            g = find(wbtn.get("content-desc"))
            ok(g is not None and g.get("enabled") == "false", "the wrong pick greys out")
            if i == 1:
                ok(" hint " in d, f"hint after two wrong picks ({d})")
                shot("hint")
    tap(find(f"choice {ans}"))
    if k == 1:
        time.sleep(1.2)
        shot("hopping")
    end = time.time() + 20
    moved = False
    while time.time() < end and not moved:
        ns = nodes()
        ln, st = find("line", ns), find("sticker", ns)
        if k == 1 and ln is not None and "solved" in ln.get("content-desc"):
            shot("solved")
        moved = (k < 5 and ln is not None and f"puzzle {k + 1} " in ln.get("content-desc")) or \
                (k == 5 and st is not None and st.get("content-desc").startswith("sticker 1"))
        if not moved:
            time.sleep(0.6)
    ok(moved, f"puzzle {k} solved: the app moved on")

shot("sticker")
more = next((n for n in nodes() if n.get("content-desc") in ("More!", "Meer!", "باز هم!")), None)
ok(more is not None, "More! present")
tap(more)
d = wait_line(r"stage 1 puzzle 1 ")
ok("stage 1 puzzle 1 " in d, f"a round with two slips moves her up to stage 1 ({d})")

# ---- counting language ----------------------------------------------------------------------
chip = find("counting language")
first = chip.get("content-desc")
seen = [first]
for _ in range(3):
    tap(find("counting language"))
    time.sleep(1.0)
    seen.append(find("counting language").get("content-desc"))
langs = [re.search(r"language (\w+)", s).group(1) for s in seen]
ok(langs[0] == langs[3] and len(set(langs[:3])) == 3 and set(langs[:3]) == {"fa", "nl", "en"},
   f"language chip cycles through fa, nl, en ({langs})")
print("     voices:", [re.search(r"voice (\w+)", s).group(1) for s in seen[:3]])
while not find("counting language").get("content-desc").startswith("counting language fa"):
    tap(find("counting language"))
    time.sleep(0.8)
shot("farsi")

# ---- a family recording is found and preferred ----------------------------------------------
import struct, tempfile, wave, math
wav = os.path.join(tempfile.mkdtemp(), "1.wav")
with wave.open(wav, "wb") as w:
    w.setnchannels(1); w.setsampwidth(2); w.setframerate(16000)
    w.writeframes(b"".join(struct.pack("<h", int(8000 * math.sin(2 * math.pi * 440 * i / 16000)))
                           for i in range(4000)))
dest = f"/sdcard/Android/data/{PKG}/files/voices/en/1.wav"
subprocess.run([args.adb, "-s", args.serial, "push", wav, dest], capture_output=True)
while not find("counting language").get("content-desc").startswith("counting language en"):
    tap(find("counting language"))
    time.sleep(0.8)
d = find("counting language").get("content-desc")
ok(d.endswith("voice recorded"), f"a recording in voices/en/ is found and used ({d})")
sh(f"rm {dest}")

# ---- which is more? ----------------------------------------------------------------------
start(5)
d = wait_line(r"stage 5 puzzle 1 ")
ok("more of" in d, f"stage 5 asks which is more ({d})")
shot("more")
ans = answer(d)
tap(find(f"choice {ans}"))
d = wait_line(r"solved|puzzle 2 ", 8)
ok("solved" in d or "puzzle 2 " in d, f"picking the bigger number solves it ({d})")

print(f"\n{'ALL PASSED' if not failures else f'{len(failures)} FAILED'}")
sys.exit(1 if failures else 0)
