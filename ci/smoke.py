#!/usr/bin/env python3
"""Emulator smoke test for the Unggun APK: install, launch, screenshot, import two synthetic
entries through the system file picker, and check that they appear. Uses only public data."""
import json, os, re, subprocess, sys, time
import xml.etree.ElementTree as ET

PKG = "com.vanipy.unggun"
APK = sys.argv[1]
if "debug" in os.path.basename(APK):
    PKG += ".debug"  # debug builds carry an applicationIdSuffix
SHOTS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "shots")
os.makedirs(SHOTS, exist_ok=True)
FAILS = []


def adb(*a, check=True, timeout=120):
    r = subprocess.run(["adb", *a], capture_output=True, text=True, timeout=timeout)
    if check and r.returncode:
        raise RuntimeError("adb %s failed: %s" % (" ".join(a), r.stderr[-400:]))
    return r.stdout


def shot(name):
    with open(os.path.join(SHOTS, name + ".png"), "wb") as f:
        f.write(subprocess.run(["adb", "exec-out", "screencap", "-p"], capture_output=True).stdout)


def dump(name=None):
    for _ in range(3):
        adb("shell", "uiautomator", "dump", "/sdcard/ui.xml", check=False)
        x = adb("exec-out", "cat", "/sdcard/ui.xml", check=False)
        if "<hierarchy" in x:
            if name:
                open(os.path.join(SHOTS, name + ".xml"), "w").write(x)
            return ET.fromstring(x)
        time.sleep(1)
    return None


def find(root, pat):
    rx = re.compile(pat, re.I)
    for n in root.iter("node"):
        t = (n.get("text") or "") + "|" + (n.get("content-desc") or "")
        if rx.search(t) and n.get("bounds"):
            m = re.findall(r"\d+", n.get("bounds"))
            l, t_, r, b = map(int, m)
            if r > l and b > t_:
                return (l + r) // 2, (t_ + b) // 2, n
    return None


def labels(root):
    out = []
    for n in root.iter("node"):
        s = (n.get("text") or n.get("content-desc") or "").strip()
        if s:
            out.append(s.replace("\n", " "))
    return out


def tap(pat, name, wait=2.5, required=True):
    for _ in range(6):
        root = dump(name + "-ui")
        hit = find(root, pat) if root is not None else None
        if hit:
            adb("shell", "input", "tap", str(hit[0]), str(hit[1]))
            time.sleep(wait)
            shot(name)
            return True
        time.sleep(2)
    shot(name + "-missing")
    msg = "could not find %r at step %s; visible: %s" % (pat, name, labels(root)[:40] if root is not None else None)
    print("FAIL", msg)
    if required:
        FAILS.append(msg)
    return False


def main():
    print("install:", adb("install", "-r", "-g", APK).strip(), "package", PKG)
    info = adb("shell", "dumpsys", "package", PKG, check=False)
    print("installed:", re.findall(r"versionName=\S+", info)[:1], "pkg line present:", PKG in info)
    adb("shell", "pm", "grant", PKG, "android.permission.POST_NOTIFICATIONS", check=False)
    adb("shell", "monkey", "-p", PKG, "-c", "android.intent.category.LAUNCHER", "1")
    time.sleep(12)
    shot("01-first-launch")
    root = dump("01-first-launch-ui")
    print("first screen:", labels(root)[:30] if root is not None else "no hierarchy")
    pid = adb("shell", "pidof", PKG, check=False).strip()
    if not pid:
        FAILS.append("app process not running after launch")
    # first-run dialogs, if any
    for pat in (r"^(okay|ok|got it|allow|skip|close)$",):
        tap(pat, "02-dismiss", required=False)

    root = dump("02b-home-ui")
    print("home:", labels(root)[:40] if root is not None else None)
    shot("02b-home")
    # synthetic import file: two public repos, same schema as an export
    apps = json.load(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "synthetic-import.json")))
    adb("push", os.path.join(os.path.dirname(os.path.abspath(__file__)), "synthetic-import.json"),
        "/sdcard/Download/unggun-synthetic.json")

    # find the way to import/export
    if not tap(r"import/export", "03-menu", required=False):
        tap(r"settings", "03a-settings", required=False)
        tap(r"import/export", "03b-menu")
    tap(r"unggun import", "04-import")
    time.sleep(3)
    shot("05-picker")
    root = dump("05-picker-ui")
    print("picker:", labels(root)[:30] if root is not None else None)
    if not tap(r"unggun-synthetic", "06-pick-file", required=False):
        tap(r"(show roots|open drawer|more options)", "05a-roots", required=False)
        tap(r"^downloads", "05b-downloads", required=False)
        tap(r"unggun-synthetic", "06-pick-file")
    time.sleep(3)
    shot("07-after-pick")
    root = dump("07-after-pick-ui")
    print("after pick:", labels(root)[:30] if root is not None else None)
    tap(r"^continue$", "08-confirm", required=False)
    time.sleep(4)
    shot("09-imported")
    root = dump("09-imported-ui")
    print("after import:", labels(root)[:30] if root is not None else None)
    adb("shell", "input", "keyevent", "KEYCODE_BACK", check=False)
    adb("shell", "input", "keyevent", "KEYCODE_BACK", check=False)
    time.sleep(2)
    root = dump("10-apps-ui")
    shot("10-apps")
    names = " ".join(labels(root)) if root is not None else ""
    for want in ("tuju", "unggah"):
        if want not in names.lower():
            FAILS.append("imported app %r not visible on the apps page" % want)
    # crash check
    log = adb("logcat", "-d", "-b", "crash", check=False)
    open(os.path.join(SHOTS, "crash-log.txt"), "w").write(log)
    if PKG in log:
        FAILS.append("crash buffer mentions the app")
    if not adb("shell", "pidof", PKG, check=False).strip():
        FAILS.append("app process gone at end")
    print("RESULT:", "FAIL" if FAILS else "PASS", FAILS)
    sys.exit(1 if FAILS else 0)


main()
