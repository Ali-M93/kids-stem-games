#!/usr/bin/env bash
# Photographs every screen of the Lili & Friends app on the running emulator/device.
# Usage: tools/screenshots.sh <name> <en|ar>   (run from the repository root)
set -u
name=$1
lang=${2:-en}
out="shots/$name"
mkdir -p "$out"
pkg=com.lilifriends.games
apk=lili-app/app/build/outputs/apk/debug/app-debug.apk

wait_boot() {
    adb wait-for-device
    until [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do sleep 2; done
    sleep 8
    adb shell input keyevent KEYCODE_WAKEUP || true
    adb shell input keyevent 82 || true
}

if [ "$lang" = "ar" ]; then
    adb root || true
    sleep 4
    adb wait-for-device
    adb shell "setprop persist.sys.locale ar-IQ; setprop ctl.restart zygote" || true
    sleep 5
    wait_boot
fi

adb install -r "$apk"
adb shell getprop ro.build.version.release > "$out/android-version.txt"
adb shell wm size > "$out/screen.txt"
adb shell wm density >> "$out/screen.txt"

shot() {
    sleep 4
    adb exec-out screencap -p > "$out/$1.png"
    echo "shot $1"
}

# Screen size and density, to find the "+" badge on the first screen.
read -r W H < <(adb shell wm size | tr -d '\r' | sed -n 's/.*: *\([0-9]*\)x\([0-9]*\).*/\1 \2/p' | tail -1)
D=$(adb shell wm density | tr -d '\r' | sed -n 's/.*: *\([0-9]*\).*/\1/p' | tail -1)
ROT=$(adb shell dumpsys input | tr -d '\r' | sed -n 's/.*SurfaceOrientation: *\([0-9]\).*/\1/p' | head -1)
if [ "${ROT:-0}" = "1" ] || [ "${ROT:-0}" = "3" ]; then t=$W; W=$H; H=$t; fi
echo "screen ${W}x${H} @${D}dpi rot ${ROT:-0}" | tee -a "$out/screen.txt"

adb shell am start -W -n $pkg/.ProfilesActivity >/dev/null
shot 01-who-empty

# Tap "+": layout mirrors ProfilesActivity.layoutBadges with a single badge.
XY=$(awk -v w="$W" -v h="$H" -v d="$D" 'BEGIN{
    dp=d/160; sb=24*dp; nav=48*dp; ch=h-sb-nav; wide=(w>ch);
    top=ch*(wide?0.30:0.34); bottom=ch*0.88; cell=w*0.9; if(bottom-top<cell) cell=bottom-top;
    printf "%d %d", w/2, sb+top+cell/2 }')
adb shell input tap $XY
shot 02-home
adb shell input keyevent KEYCODE_BACK
shot 03-who-one-player

for g in path hops pattern scale; do
    adb shell am start -W -n $pkg/.MapActivity --ei player 1 --es game $g >/dev/null
    shot "1-map-$g"
    adb shell input keyevent KEYCODE_BACK
    sleep 1
done

for pair in path:robotpath hops:kangaroohops pattern:patternmachine scale:balancescale; do
    g=${pair%%:*}; p=${pair##*:}
    adb shell am start -W -n $pkg/io.github.foadsf.$p.GameActivity --es prefs p1_$g >/dev/null
    shot "2-game-$g"
    adb shell input keyevent KEYCODE_BACK
    sleep 1
done

# Landscape on phones too.
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1
sleep 3
adb shell am start -W -n $pkg/.HomeActivity --ei player 1 >/dev/null
shot "3-landscape-home"
adb shell input keyevent KEYCODE_BACK
adb shell am start -W -n $pkg/.MapActivity --ei player 1 --es game path >/dev/null
shot "3-landscape-map"
adb shell input keyevent KEYCODE_BACK
adb shell am start -W -n $pkg/io.github.foadsf.robotpath.GameActivity --es prefs p1_path >/dev/null
shot "3-landscape-game-path"
adb shell input keyevent KEYCODE_BACK
for pair in hops:kangaroohops pattern:patternmachine scale:balancescale; do
    g=${pair%%:*}; p=${pair##*:}
    adb shell am start -W -n $pkg/io.github.foadsf.$p.GameActivity --es prefs p1_$g >/dev/null
    shot "3-landscape-game-$g"
    adb shell input keyevent KEYCODE_BACK
    sleep 1
done
adb shell settings put system user_rotation 0
ls -la "$out"
