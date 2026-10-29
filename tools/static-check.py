#!/usr/bin/env python3
"""Dependency-free integrity checks, not a substitute for Kotlin/Android compilation."""
from pathlib import Path
import re, zipfile, xml.etree.ElementTree as ET, hashlib
root=Path(__file__).resolve().parent.parent
checks=0

def check(condition, message):
    global checks
    checks+=1
    if not condition: raise AssertionError(message)

resources={}
for file in (root/'app/src/main/res').rglob('*.xml'):
    ET.parse(file)
    checks+=1
for language in ['values','values-ar']:
    resources[language]={e.attrib['name'] for e in ET.parse(root/f'app/src/main/res/{language}/strings.xml').getroot() if e.tag=='string'}
check(resources['values']==resources['values-ar'],'Localizations differ')
for code in (root/'app/src').rglob('*.kt'):
    for name in re.findall(r'R\.string\.(\w+)',code.read_text()): check(name in resources['values'],f'Missing string: {name}')
for path in ['settings.gradle.kts','build.gradle.kts','app/build.gradle.kts','gradle.properties','gradlew','gradlew.bat','gradle/wrapper/gradle-wrapper.properties','gradle/wrapper/gradle-wrapper.jar','.github/workflows/android-build.yml','app/src/main/AndroidManifest.xml']:
    check((root/path).stat().st_size>0,f'Missing {path}')
with zipfile.ZipFile(root/'gradle/wrapper/gradle-wrapper.jar') as wrapper:
    check(wrapper.testzip() is None,'Corrupt wrapper')
    check('org/gradle/wrapper/GradleWrapperMain.class' in wrapper.namelist(),'Invalid wrapper')
properties=(root/'gradle/wrapper/gradle-wrapper.properties').read_text()
check(bool(re.search(r'distributionSha256Sum=[a-f0-9]{64}',properties)),'Missing Gradle checksum')
manifest=(root/'app/src/main/AndroidManifest.xml').read_text()
check('WRITE_EXTERNAL_STORAGE' not in manifest and 'MANAGE_EXTERNAL_STORAGE' not in manifest,'Broad storage permission')
check('android:allowBackup="false"' in manifest,'Backups expose history')
check('android:supportsRtl="true"' in manifest,'RTL disabled')
# Every resource reference in manifests/XML must resolve to a resource or framework item.
known=set()
for file in (root/'app/src/main/res').rglob('*.xml'):
    kind=file.parent.name.split('-')[0]
    if kind!='values': known.add((kind,file.stem))
    else:
        for item in ET.parse(file).getroot():
            if 'name' in item.attrib: known.add((item.tag,item.attrib['name']))
for file in [root/'app/src/main/AndroidManifest.xml',*(root/'app/src/main/res').rglob('*.xml')]:
    for kind,name in re.findall(r'@([a-z]+)/([\w.]+)',file.read_text()): check((kind,name) in known,f'Missing {kind}/{name}')
print(f'PASS: {checks} XML, localization, resource, wrapper and project integrity checks')
# v2 security configuration and pinned engine checks.
network=ET.parse(root/'app/src/main/res/xml/network_security_config.xml').getroot()
check(network.find('base-config').attrib.get('cleartextTrafficPermitted')=='false','Remote plaintext enabled')
paths=ET.parse(root/'app/src/main/res/xml/file_paths.xml').getroot()
check(len(paths)==1 and paths[0].tag=='files-path' and paths[0].attrib['path']=='media/','Provider scope is not private media only')
asset=root/'app/src/main/assets/yt-dlp-2026.10.26.zip'
check(hashlib.sha256(asset.read_bytes()).hexdigest()=='e51e69e89489f199530f13d32a4342da3ddb2cf34842f364603a066db05afbf3','Pinned engine corrupted')
with zipfile.ZipFile(asset) as engine:
    check(engine.testzip() is None,'Engine ZIP corrupt')
    check(any(n.startswith('yt_dlp_ejs/') for n in engine.namelist()),'EJS missing')
extractor=(root/'app/src/main/java/com/jameel/jvideodownloader/extractor/PlatformExtractor.kt').read_text()
check('"--ignore-config"' in extractor and '"--no-plugin-dirs"' in extractor,'External configuration enabled')
check('"--no-remote-components"' in extractor,'Implicit runtime component downloads allowed')
check('"--no-check-certificates"' not in extractor,'TLS verification disabled')
print(f'PASS: {checks} total project/resource/pinned-engine integrity checks')
