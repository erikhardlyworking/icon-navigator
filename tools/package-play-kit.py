#!/usr/bin/env python3
"""Export an explicit public-source allowlist and validate/package Play materials."""
import hashlib
import json
from pathlib import Path
import re
import zipfile
from xml.etree import ElementTree
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'publishing/google-play'
gradle=(ROOT/'app/build.gradle').read_text()
version=re.search(r"versionName '([^']+)'",gradle).group(1)
code=int(re.search(r'versionCode (\d+)',gradle).group(1))

def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def files(path):return sorted(p for p in path.rglob('*') if p.is_file())
def validate_public(path):
    relative=path.relative_to(ROOT)
    assert not path.is_symlink(),relative
    assert not any(p in {'.git','.private','.gradle','.codex','.agents','build','screenshots','__pycache__'} for p in relative.parts),relative
    assert path.suffix not in {'.jks','.p12','.keystore','.apk','.aab','.pyc'},relative
    assert path.name not in {'local.properties','password.txt','icon-navigator_key'},relative
    if path.suffix in {'.java','.py','.md','.json','.html','.xml','.gradle','.properties','.txt','.svg'} or path.name=='.gitignore':
        text=path.read_text()
        for pattern in [r'-----BEGIN (?:OPENSSH |RSA |EC )?PRIVATE KEY-----',r'gh[pousr]_[A-Za-z0-9]{30,}',r'github_pat_[A-Za-z0-9_]{30,}',r'AKIA[A-Z0-9]{16}',r'/home/[^/\s]+/']:
            # This script documents the rejection patterns themselves.
            if path.name!='package-play-kit.py':assert not re.search(pattern,text),relative

shots=json.loads((DEST/'manifest.json').read_text())
assert shots['appVersion']==version,shots['appVersion']
assert len(shots['screenshots'])==8
for record in shots['screenshots']:
    p=DEST/record['file']
    with Image.open(p) as image:assert image.size==(1080,1920) and image.mode=='RGB',p
    assert sha(p)==record['sha256'],p
for name,size,mode in [('app-icon-512.png',(512,512),'RGBA'),('feature-graphic-1024x500.png',(1024,500),'RGB')]:
    with Image.open(DEST/'graphics'/name) as image:assert image.size==size and image.mode==mode,name
assert (DEST/'graphics/app-icon-512.png').stat().st_size<=1024*1024
listing=json.loads((DEST/'store-listing.json').read_text())
app_name=ElementTree.parse(ROOT/'app/src/main/res/values/strings.xml').find("string[@name='app_name']").text
assert listing['title']==app_name,(listing['title'],app_name)
for key,limit in [('title',30),('shortDescription',80),('fullDescription',4000)]:assert len(listing[key])<=limit,key
release=json.loads((DEST/'release/release-manifest.json').read_text())
assert (release['versionName'],release['versionCode'],release['targetSdk'])==(version,code,36)
bundle=DEST/'release'/release['bundle']
assert sha(bundle)==release['sha256'] and release['signed'] and release['releaseExcludesDebugCaptureCode']
with zipfile.ZipFile(bundle) as archive:
    assert archive.testzip() is None
    assert any(n.startswith('META-INF/') and n.endswith('.RSA') for n in archive.namelist())
    assert not any(n.endswith('.so') for n in archive.namelist())
    for name in archive.namelist():
        if name.endswith('.dex'):
            for unwanted in [b'PlayScreenshotHomeActivity',b'PlayScreenshotInstrumentation',b'WidgetIntegrationTest']:
                assert unwanted not in archive.read(name),unwanted

# Only these roots are eligible. Never walk the workspace or private directory.
source=set(ROOT/name for name in ['.gitignore','AGENTS.md','README.md','PRIVACY.md','build.gradle','settings.gradle','gradle.properties','gradlew','gradlew.bat','app/build.gradle'])
for name in ['app/src','gradle/wrapper','tools','docs/simulations','publishing/google-play/graphics','publishing/google-play/listing','publishing/google-play/phone']:
    source.update(p for p in files(ROOT/name) if '__pycache__' not in p.parts)
source.update((ROOT/'docs').glob('*.md'))
source.update(p for p in DEST.iterdir() if p.is_file() and p.suffix in {'.md','.json','.html'} and p.name!='kit-manifest.json')
for path in source:validate_public(path)
source_zip=DEST/'icon-navigator-public-source.zip'
with zipfile.ZipFile(source_zip,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as archive:
    for path in sorted(source):archive.write(path,'icon-navigator/'+str(path.relative_to(ROOT)))

materials=set(p for folder in ['graphics','listing','phone'] for p in files(DEST/folder))
materials.update(p for p in DEST.iterdir() if p.is_file() and p.suffix in {'.md','.json','.html'} and p.name!='kit-manifest.json')
materials.update([bundle,DEST/'release/release-manifest.json',DEST/'release/upload-certificate.pem'])
# A self-contained copy of the public policy; no private source export is nested.
policy=ROOT/'PRIVACY.md'
records=[{'file':str(p.relative_to(DEST)),'sha256':sha(p),'bytes':p.stat().st_size} for p in sorted(materials)]
records.append({'file':'PRIVACY.md','sha256':sha(policy),'bytes':policy.stat().st_size})
manifest={'versionName':version,'versionCode':code,'sourceArchive':source_zip.name,'sourceSha256':sha(source_zip),'files':records}
(DEST/'kit-manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
kit=DEST/'icon-navigator-google-play-kit.zip'
with zipfile.ZipFile(kit,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as archive:
    for path in sorted(materials):archive.write(path,str(path.relative_to(DEST)))
    archive.write(policy,'PRIVACY.md');archive.write(DEST/'kit-manifest.json','kit-manifest.json')
for archive_path in [source_zip,kit]:
    with zipfile.ZipFile(archive_path) as archive:assert archive.testzip() is None
    print(archive_path.name,archive_path.stat().st_size,'bytes',sha(archive_path))
print('Public source files:',len(source),'; Play material files:',len(records))
