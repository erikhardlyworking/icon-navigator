#!/usr/bin/env python3
"""Build/sign a Play App Bundle. Private signing material never enters the kit."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import secrets
import shutil
import subprocess
import zipfile

ROOT=Path(__file__).resolve().parents[1]
PRIVATE=ROOT/'.private/play-signing'
DEST=ROOT/'publishing/google-play/release'

def run(*args):
    return subprocess.check_output(args,cwd=ROOT,text=True,stderr=subprocess.STDOUT)

parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--create-upload-key',action='store_true')
args=parser.parse_args()
java=Path(os.environ['JAVA_HOME'])/'bin'
PRIVATE.mkdir(parents=True,exist_ok=True);os.chmod(PRIVATE,0o700)
keystore=PRIVATE/'upload-key.p12';password=PRIVATE/'password.txt'
if not keystore.exists():
    if password.exists():raise SystemExit('Key missing but password exists. Restore the original key; do not replace it.')
    if not args.create_upload_key:raise SystemExit('No upload key. Use --create-upload-key only for the first release.')
    descriptor=os.open(password,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
    with os.fdopen(descriptor,'w') as stream:stream.write(secrets.token_urlsafe(40)+'\n')
    old_mask=os.umask(0o077)
    try:
        run(str(java/'keytool'),'-genkeypair','-storetype','PKCS12','-keystore',str(keystore),
            '-storepass:file',str(password),'-keypass:file',str(password),'-alias','upload',
            '-keyalg','RSA','-keysize','4096','-sigalg','SHA256withRSA','-validity','10000',
            '-dname','CN=Hardly Working, O=Hardly Working')
    finally:os.umask(old_mask)
if not password.exists():raise SystemExit('Restore the original signing password file before building.')
os.chmod(keystore,0o600);os.chmod(password,0o600)
run('./gradlew',':app:bundleRelease','--console=plain')
gradle=(ROOT/'app/build.gradle').read_text()
version=re.search(r"versionName '([^']+)'",gradle).group(1)
version_code=int(re.search(r'versionCode (\d+)',gradle).group(1))
DEST.mkdir(parents=True,exist_ok=True)
bundle=DEST/f'icon-navigator-{version}.aab'
run(str(java/'jarsigner'),'-keystore',str(keystore),'-storepass:file',str(password),'-keypass:file',str(password),
    '-sigalg','SHA256withRSA','-digestalg','SHA-256','-signedjar',str(bundle),
    str(ROOT/'app/build/outputs/bundle/release/app-release.aab'),'upload')
verification=run(str(java/'jarsigner'),'-verify',str(bundle))
if 'jar verified' not in verification:raise RuntimeError(verification)
certificate=run(str(java/'keytool'),'-exportcert','-rfc','-keystore',str(keystore),'-storepass:file',str(password),'-alias','upload')
(DEST/'upload-certificate.pem').write_text(certificate)
with zipfile.ZipFile(bundle) as archive:
    assert archive.testzip() is None
    for name in archive.namelist():
        if name.endswith('.dex'):
            data=archive.read(name)
            for excluded in [b'PlayScreenshotHomeActivity',b'PlayScreenshotInstrumentation',b'WidgetIntegrationTest']:
                assert excluded not in data,excluded
    assert any(name.startswith('META-INF/') and name.endswith('.RSA') for name in archive.namelist())
manifest={'applicationId':'dev.iconnavigator','versionName':version,'versionCode':version_code,
          'minSdk':31,'targetSdk':36,'signed':True,'uploadKey':'RSA 4096-bit; kept privately outside public source and kit',
          'bundle':bundle.name,'sha256':hashlib.sha256(bundle.read_bytes()).hexdigest(),
          'releaseExcludesDebugCaptureCode':True}
(DEST/'release-manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
print('Signed and verified:',bundle)
print('Upload key/password are private in .private/play-signing. Back up that directory securely.')
