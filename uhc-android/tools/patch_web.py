#!/usr/bin/env python3
from pathlib import Path
import re
import sys

root = Path(sys.argv[1] if len(sys.argv) > 1 else ".").resolve()

def read(rel):
    return (root / rel).read_text()

def write(rel, text):
    (root / rel).write_text(text)

# Responsive Android viewport + no analytics/network loader.
s = read("public/index.html")
s = s.replace(
    '<meta name="viewport" content="initial-scale=0.0, width=650">',
    '<meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover">'
)
s = "\n".join(
    line for line in s.splitlines()
    if "goatcounter" not in line and "gc.zgo.at" not in line
)

android_css = r"""
<style id="uhc-android-layout">
  html.uhc-android, html.uhc-android body {
    overscroll-behavior: none;
  }

  /* The upstream web build uses a 17px / 10px desktop nav. On a touch device
     that is visually tiny and a poor tap target. Keep the original look, but
     make the whole banner finger-friendly and let it wrap when necessary. */
  html.uhc-android .navBanner {
    min-height: 36px !important;
    height: auto !important;
    max-width: 100vw !important;
  }
  html.uhc-android .navBanner .navList {
    min-height: 36px !important;
    height: auto !important;
    flex-wrap: wrap !important;
    padding: 1px 6px !important;
    box-sizing: border-box !important;
  }
  html.uhc-android .navBanner .candyCorn {
    display: none !important;
  }
  html.uhc-android .navBanner ul {
    height: auto !important;
    max-height: none !important;
    font-size: 13px !important;
    line-height: 16px !important;
    flex-wrap: nowrap !important;
  }
  html.uhc-android .navBanner li a {
    display: inline-flex !important;
    min-height: 32px !important;
    align-items: center !important;
    padding: 0 3px !important;
  }

  /* Fold-open archive layout: preserve readable text while returning the
     desktop collection's side-by-side archive rows. The cover display stays
     single-column; the wider inner display becomes a two-column archive. */
  html.uhc-android .homepage .card {
    max-width: 100% !important;
    padding-left: 24px !important;
    padding-right: 24px !important;
  }

  @media (min-width: 420px) {
    html.uhc-android .homepage .cardContent.cardRows {
      gap: 0 8px !important;
      align-items: stretch !important;
    }
    html.uhc-android .homepage .cardContent.cardRows .rowItem {
      flex: 1 1 calc(50% - 8px) !important;
      width: calc(50% - 8px) !important;
      max-width: calc(50% - 4px) !important;
      min-width: 0 !important;
      box-sizing: border-box !important;
      align-items: flex-start !important;
    }
    html.uhc-android .homepage .cardContent.cardRows .rowItem .description {
      min-width: 0 !important;
      padding-left: 8px !important;
      padding-right: 4px !important;
    }
    html.uhc-android .homepage .cardContent.cardRows .rowItem .description h2 {
      font-size: 16px !important;
      line-height: 1.05 !important;
      overflow-wrap: anywhere !important;
    }
  }

  @media (min-width: 700px) {
    html.uhc-android .homepage .cardContent.cardRows .rowItem {
      flex-basis: calc(33.333% - 8px) !important;
      width: calc(33.333% - 8px) !important;
      max-width: calc(33.333% - 4px) !important;
    }
  }
</style>
<script>document.documentElement.classList.add("uhc-android");</script>
"""
s = s.replace("</head>", android_css + "\n</head>")
write("public/index.html", s)

# Remove the hosted fork's authentication gate. Android is a local single-user app.
s = read("src/App.vue")
auth_gate = """      // Only load the app if a user is authenticated (or in guest mode)
      if (!this.$localData.root.isAuthenticated && !this.$root.guestMode) {
        return false;
      }

"""
if auth_gate not in s:
    raise RuntimeError("App.vue authentication gate not found")
s = s.replace(
    auth_gate,
    "      // Android edition: local single-user app; no authentication gate.\n"
)
# This build targets Android on purpose, so the hosted site's generic
# "Mobile Experience Limited" banner is misleading noise.
s = s.replace("      <MobileNotification />\n", "")
write("src/App.vue", s)

# The hosted web fork downloads Ruffle from unpkg. The Android app has no
# INTERNET permission by design, so that produces a completely blank SWF.
# Bundle the fork's self-hosted Ruffle files into the APK and point every SWF
# iframe at that local runtime instead.
s = read("src/components/UIElements/MediaEmbed.vue")
old_ruffle = """        let scriptTag;
        if (this.$isWebApp) {
          scriptTag =
            '<script src="https://unpkg.com/@ruffle-rs/ruffle"><\\/script>';
        } else {
          scriptTag = \`<script src="\${this.$getResourceURL(
            "assets://js/ruffle/ruffle.js"
          )}"><\\/script>\`;
        }"""
new_ruffle = """        const scriptTag =
          \`<script src="\${window.location.origin}/ruffle/ruffle.js"><\\/script>\`;"""
if old_ruffle not in s:
    raise RuntimeError("Ruffle CDN block not found")
s = s.replace(old_ruffle, new_ruffle)
write("src/components/UIElements/MediaEmbed.vue", s)

# Restore local-only persistence. The hosted fork moved save/settings data to an
# HTTP auth server, which makes no sense in a fully offline APK.
s = read("src/store/localData.js")
s = s.replace(
    "isAuthenticated: false, // Add isAuthenticated property",
    "isAuthenticated: true, // Android edition has no account/login layer"
)

save_re = re.compile(
    r"        async _saveLocalStorage\(\) \{.*?\n        \},\n        applySaveIfPending\(\) \{",
    re.S,
)
save_repl = """        async _saveLocalStorage() {
          if (this.saveDebounce) {
            clearTimeout(this.saveDebounce);
            this.saveDebounce = undefined;
          }
          try {
            localStorage.setItem("uhc.android.saveData", JSON.stringify(this.saveData));
            localStorage.setItem("uhc.android.settings", JSON.stringify(this.settings));
            localStorage.setItem("uhc.android.tabData", JSON.stringify(this.tabData));
            this.$logger.debug("Android local data saved.");
          } catch (e) {
            this.$logger.error("Failed to save Android local data:", e);
          }
        },
        applySaveIfPending() {"""
s, count = save_re.subn(save_repl, s, count=1)
if count != 1:
    raise RuntimeError("_saveLocalStorage patch failed")

reload_re = re.compile(
    r"        async reloadLocalStorage\(\) \{.*?\n        \},\n        clearLocalStorage\(\) \{",
    re.S,
)
reload_repl = """        async reloadLocalStorage() {
          this.applySaveIfPending();
          try {
            const savedData = JSON.parse(localStorage.getItem("uhc.android.saveData") || "null");
            const savedSettings = JSON.parse(localStorage.getItem("uhc.android.settings") || "null");
            const savedTabs = JSON.parse(localStorage.getItem("uhc.android.tabData") || "null");

            this.assetDir = "web";
            this.saveData = savedData || DEFAULT_SAVEDATA;
            this.settings = {
              ...DEFAULT_SETTINGS,
              ...(savedSettings || {}),
              newReader: {
                ...DEFAULT_SETTINGS.newReader,
                ...((savedSettings && savedSettings.newReader) || {})
              },
              textOverride: {
                ...DEFAULT_SETTINGS.textOverride,
                ...((savedSettings && savedSettings.textOverride) || {})
              }
            };
            if (savedTabs) {
              this.tabData = savedTabs;
              Object.values(this.tabData.tabs || {}).forEach(tab => {
                if (tab.url === "/index.html" || tab.url === "index.html") tab.url = "/";
                if (Array.isArray(tab.history)) {
                  tab.history = tab.history.map(u => (u === "/index.html" || u === "index.html") ? "/" : u);
                }
                if (Array.isArray(tab.future)) {
                  tab.future = tab.future.map(u => (u === "/index.html" || u === "index.html") ? "/" : u);
                }
              });
            }
            this.isAuthenticated = true;
            this.$logger.debug("Android local data loaded.");
          } catch (e) {
            this.$logger.error("Failed to load Android local data; using defaults:", e);
            this.assetDir = "web";
            this.saveData = DEFAULT_SAVEDATA;
            this.settings = DEFAULT_SETTINGS;
            this.tabData = DEFAULT_TABDATA;
            this.isAuthenticated = true;
          }
        },
        clearLocalStorage() {"""
s, count = reload_re.subn(reload_repl, s, count=1)
if count != 1:
    raise RuntimeError("reloadLocalStorage patch failed")

s = s.replace(
    """        clearLocalStorage() {
          // This method will now clear local client-side state, not server data
          this.applySaveIfPending();""",
    """        clearLocalStorage() {
          localStorage.removeItem("uhc.android.saveData");
          localStorage.removeItem("uhc.android.settings");
          localStorage.removeItem("uhc.android.tabData");
          this.applySaveIfPending();""",
)

install_re = re.compile(
    r"    // Check for existing session on app start.*?\n    checkSession\(\);\n",
    re.S,
)
install_repl = """    // Android is local-only: no auth server, account, or network session.
    the_store.VM.isAuthenticated = true;
    the_store.VM.reloadLocalStorage();
"""
s, count = install_re.subn(install_repl, s, count=1)
if count != 1:
    raise RuntimeError("session/auth patch failed")

write("src/store/localData.js", s)
print("Applied UHC Android offline/auth/viewport patches.")
