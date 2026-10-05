#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
APP="$ROOT/uhc-android"
OUT="$APP/vercel-output"
TOOLS=/tmp/uhc-tools
WEB=/tmp/uhc-web
rm -rf "$OUT" "$TOOLS" "$WEB"
mkdir -p "$OUT" "$TOOLS"

# Local build toolchain, kept entirely under /tmp.
curl -fL --retry 2 "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse" -o "$TOOLS/jdk.tar.gz"
mkdir "$TOOLS/jdk"
tar -xzf "$TOOLS/jdk.tar.gz" -C "$TOOLS/jdk" --strip-components=1
export JAVA_HOME="$TOOLS/jdk"
export PATH="$JAVA_HOME/bin:$PATH"

export ANDROID_SDK_ROOT="$TOOLS/android-sdk"
export ANDROID_HOME="$ANDROID_SDK_ROOT"
mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"
curl -fL --retry 2 "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip" -o "$TOOLS/sdk.zip"
unzip -q "$TOOLS/sdk.zip" -d "$TOOLS/sdk-unzip"
mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools/latest"
mv "$TOOLS/sdk-unzip/cmdline-tools/"* "$ANDROID_SDK_ROOT/cmdline-tools/latest/"
export PATH="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$PATH"
yes | sdkmanager --licenses >/dev/null || true
sdkmanager "platforms;android-35" "build-tools;35.0.0"

curl -fL --retry 2 "https://services.gradle.org/distributions/gradle-8.9-bin.zip" -o "$TOOLS/gradle.zip"
unzip -q "$TOOLS/gradle.zip" -d "$TOOLS"
export PATH="$TOOLS/gradle-8.9/bin:$PATH"

git clone --depth=1 https://github.com/jennymaeleidig/unofficial-homestuck-collection-web.git "$WEB"
git -C "$WEB" rev-parse HEAD > "$APP/WEB_FRONTEND_COMMIT.txt"
cd "$WEB"
export ASSET_PACK_HREF="https://uhc-assets.local/"
export ASSET_DIR="/assets/"
export APP_VERSION="0.1.0-android"
export AUTH_SERVER_URL=""
export NODE_OPTIONS="--max_old_space_size=8192"
corepack enable || true
corepack prepare yarn@1.22.22 --activate || true
yarn install --frozen-lockfile --ignore-engines

python3 - <<'PY'
from pathlib import Path
p=Path("public/index.html")
p.write_text("\n".join(x for x in p.read_text().splitlines() if "goatcounter" not in x and "gc.zgo.at" not in x))
PY

mkdir -p build
(cd src && tar -cf - imods/ | gzip -9 - > imods.tar.gz)
yarn exec node src/js/validation.js src/imods/ src/js/crc_imods.json
node - <<'NODE' > build/webAppModTrees.json
const fs=require("fs"),path=require("path");
function w(d){let o={};for(const e of fs.readdirSync(d,{withFileTypes:true}))o[e.name]=e.isDirectory()?w(path.join(d,e.name)):true;return o}
console.log(JSON.stringify({imods:w("src/imods")}));
NODE
python3 -m venv "$TOOLS/py"
"$TOOLS/py/bin/pip" -q install jinja2-cli
"$TOOLS/py/bin/jinja2" webapp/browser.js.j2 -o webapp/browser.js
yarn run vue-cli-service build webapp/browser.js

rm -rf "$APP/app/src/main/assets/www"
mkdir -p "$APP/app/src/main/assets/www"
cp -a dist/. "$APP/app/src/main/assets/www/"

cd "$ROOT"
gradle -p "$APP" --no-daemon :app:assembleDebug
APK="$APP/app/build/outputs/apk/debug/app-debug.apk"
test -f "$APK"
"$ANDROID_SDK_ROOT/build-tools/35.0.0/apksigner" verify "$APK"
cp "$APK" "$OUT/UHC-Android-v0.1.0-debug.apk"
cp "$APP/README.md" "$OUT/README.txt"
cat > "$OUT/index.html" <<'HTML'
<!doctype html><meta charset="utf-8"><title>UHC Android</title>
<h1>UHC Android prototype</h1>
<p><a href="UHC-Android-v0.1.0-debug.apk">Download debug APK</a></p>
<p>The APK contains no Homestuck asset pack. Select your existing UHC Asset Pack at first launch.</p>
HTML
echo BUILD_OK
