#!/usr/bin/env python3
"""Update a signed install holding only synthetic public-repo entries."""
import sys,time,os
# Reuse UI-reading helpers, without running the initial import twice.
s=sys.argv[0];sys.argv=[s,'ci/apk/new.apk']
source=open('ci/smoke.py').read();exec(source.rsplit('\nmain()',1)[0])
print('upgrade install:',adb('install','-r','ci/apk/new.apk').strip())
adb('shell','am','force-stop',PKG)
adb('shell','monkey','-p',PKG,'-c','android.intent.category.LAUNCHER','1');time.sleep(12)
root=dump('upgrade-retained-ui');shot('upgrade-retained');names=' '.join(labels(root))if root is not None else ''
for want in ['app-one','app-two']:assert want in names.lower(),(want,names)
assert 'versionName=0.1.5' in adb('shell','dumpsys','package',PKG)
assert PKG not in adb('logcat','-d','-b','crash',check=False)
print('PASS: signed0.1.4 to0.1.5 retained both synthetic imported records without uninstall/data clear')
