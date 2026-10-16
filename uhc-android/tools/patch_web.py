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
      width: auto !important;
      flex: 1 1 auto !important;
      padding-left: 8px !important;
      padding-right: 4px !important;
    }
    html.uhc-android .homepage .cardContent.cardRows .rowItem .description h2 {
      font-size: 16px !important;
      line-height: 1.05 !important;
      overflow-wrap: anywhere !important;
    }
  }

  /* Never let the archive homepage inherit desktop intrinsic widths. */
  html.uhc-android .homepage {
    width: 100% !important;
    max-width: 100vw !important;
    min-width: 0 !important;
    overflow-x: hidden !important;
    box-sizing: border-box !important;
  }
  html.uhc-android .homepage .card {
    width: 100% !important;
    max-width: 100vw !important;
    min-width: 0 !important;
    padding-left: 20px !important;
    padding-right: 20px !important;
  }
  html.uhc-android .homepage .cardContent,
  html.uhc-android .homepage .cardEntry,
  html.uhc-android .homepage .cardRows {
    min-width: 0 !important;
    max-width: 100% !important;
    box-sizing: border-box !important;
  }
  html.uhc-android .homepage .cardEntry .description {
    min-width: 0 !important;
    flex: 1 1 auto !important;
    overflow-wrap: anywhere !important;
  }
  html.uhc-android .homepage .cardEntry .links {
    min-width: 0 !important;
    max-width: 100% !important;
    flex-wrap: wrap !important;
  }

  /* The TabFrame is normally horizontally scrollable because some desktop
     gimmick pages need it. Disable that only for layouts we explicitly fit. */
  html.uhc-android .tabFrame:has(.homepage) {
    overflow-x: hidden !important;
  }
  html.uhc-android .tabFrame:has([data-component="x2Combo"]) {
    overflow-x: auto !important;
  }

  /*
   * A6A5A1 x2 Combo pages are intentionally 1660px wide on desktop.
   * On Android/Fold, preserve the two simultaneous pages but reflow the
   * special component to exactly one viewport width instead of requiring
   * horizontal scrolling.
   */
  html.uhc-android [data-component="x2Combo"] {
    width: 100% !important;
    max-width: 100vw !important;
    overflow-x: hidden !important;
    align-items: stretch !important;
  }

  html.uhc-android [data-component="x2Combo"] .navBanners,
  html.uhc-android [data-component="x2Combo"] .pageFrame {
    width: 100% !important;
    max-width: 100vw !important;
    box-sizing: border-box !important;
  }

  html.uhc-android [data-component="x2Combo"] .navBanners {
    display: flex !important;
    gap: 6px !important;
  }

  html.uhc-android [data-component="x2Combo"] .navBanners .navBanner {
    flex: 1 1 0 !important;
    width: auto !important;
    min-width: 0 !important;
  }

  html.uhc-android [data-component="x2Combo"] .navBanners .navSpacer {
    display: none !important;
  }

  html.uhc-android [data-component="x2Combo"] .pageFrame {
    display: flex !important;
    align-items: flex-start !important;
    justify-content: stretch !important;
    gap: 8px !important;
    padding-left: 6px !important;
    padding-right: 6px !important;
  }

  html.uhc-android [data-component="x2Combo"] .pageContent {
    flex: 1 1 0 !important;
    width: calc(50% - 4px) !important;
    max-width: calc(50% - 4px) !important;
    min-width: 0 !important;
    margin: 0 !important;
    box-sizing: border-box !important;
    overflow: visible !important;
  }

  html.uhc-android [data-component="x2Combo"] .leftPage {
    margin-right: 0 !important;
  }

  html.uhc-android [data-component="x2Combo"] .mediaContent,
  html.uhc-android [data-component="x2Combo"] .media {
    width: 100% !important;
    max-width: 100% !important;
    min-width: 0 !important;
  }

  html.uhc-android [data-component="x2Combo"] .mediaContent .pageTitle {
    width: 100% !important;
    max-width: 100% !important;
    box-sizing: border-box !important;
    padding-left: 6px !important;
    padding-right: 6px !important;
    font-size: clamp(18px, 3.2vw, 30px) !important;
  }

  html.uhc-android [data-component="x2Combo"] .panel,
  html.uhc-android [data-component="x2Combo"] .panel img,
  html.uhc-android [data-component="x2Combo"] .panel video,
  html.uhc-android [data-component="x2Combo"] .panel canvas {
    width: auto !important;
    max-width: 100% !important;
    height: auto !important;
    object-fit: contain !important;
  }

  html.uhc-android [data-component="x2Combo"] iframe.mediaembed {
    max-width: 100% !important;
  }

  html.uhc-android [data-component="x2Combo"] .textContent {
    width: 100% !important;
    min-width: 0 !important;
    box-sizing: border-box !important;
    padding-left: 10px !important;
    padding-right: 10px !important;
  }

  html.uhc-android [data-component="x2Combo"] .textContent * {
    max-width: 100% !important;
    overflow-wrap: anywhere !important;
  }

  html.uhc-android [data-component="x2Combo"] .footer {
    width: 100% !important;
    max-width: 100vw !important;
  }
  /*
   * Final x2 Combo strategy: do NOT reflow or crop the two desktop pages.
   * Keep the native 1660px composition intact and let x2Combo.vue apply one
   * uniform CSS zoom so both halves, logs, arrows and banner fit the viewport.
   */
  html.uhc-android [data-component="x2Combo"] {
    width: 1660px !important;
    max-width: none !important;
    min-width: 1660px !important;
    overflow: visible !important;
    transform-origin: top left !important;
    margin: 0 !important;
  }
  html.uhc-android [data-component="x2Combo"] .navBanners,
  html.uhc-android [data-component="x2Combo"] .pageFrame {
    width: 1660px !important;
    max-width: none !important;
    min-width: 1660px !important;
    padding-left: 0 !important;
    padding-right: 0 !important;
    gap: 0 !important;
  }
  html.uhc-android [data-component="x2Combo"] .navBanners .navSpacer {
    display: block !important;
    padding: 0 50px !important;
  }
  html.uhc-android [data-component="x2Combo"] .pageContent {
    width: auto !important;
    max-width: 950px !important;
    min-width: 650px !important;
    margin: 0 !important;
    overflow: visible !important;
  }
  html.uhc-android [data-component="x2Combo"] .leftPage {
    margin-right: 33px !important;
  }
  html.uhc-android [data-component="x2Combo"] .mediaContent,
  html.uhc-android [data-component="x2Combo"] .media {
    width: auto !important;
    max-width: none !important;
  }
  html.uhc-android [data-component="x2Combo"] .panel,
  html.uhc-android [data-component="x2Combo"] .panel img,
  html.uhc-android [data-component="x2Combo"] .panel video,
  html.uhc-android [data-component="x2Combo"] .panel canvas,
  html.uhc-android [data-component="x2Combo"] iframe.mediaembed {
    width: auto !important;
    max-width: none !important;
  }
  html.uhc-android [data-component="x2Combo"] .textContent {
    width: 600px !important;
    min-width: 600px !important;
    max-width: 600px !important;
    padding-left: 0 !important;
    padding-right: 0 !important;
  }
</style>
<script>document.documentElement.classList.add("uhc-android");</script>
<style id="uhc-android-shell-overlays">
  #uhcSburbController {
    position:fixed; left:10px; right:10px; bottom:8px; height:138px;
    z-index:2147483000; display:none; pointer-events:none;
    font-family:monospace; -webkit-user-select:none; user-select:none;
  }
  #uhcSburbController.visible { display:block; }
  #uhcSburbController * { box-sizing:border-box; }
  #uhcSburbController .plate {
    position:absolute; inset:0; border:2px solid rgba(88,255,244,.60);
    border-radius:20px; background:linear-gradient(180deg,rgba(11,19,30,.34),rgba(4,9,17,.72));
    box-shadow:inset 0 0 0 2px rgba(255,0,170,.10),0 4px 18px rgba(0,0,0,.35);
    backdrop-filter:blur(2px);
  }
  #uhcSburbController .brand {
    position:absolute; left:50%; top:7px; transform:translateX(-50%);
    color:rgba(210,255,250,.88); font:bold 10px monospace; letter-spacing:2px;
    text-shadow:0 1px #000; pointer-events:none;
  }
  #uhcSburbController button {
    pointer-events:auto; position:absolute; border:2px solid rgba(255,255,255,.78);
    background:rgba(9,14,22,.78); color:#fff; font:bold 18px monospace;
    touch-action:none; -webkit-tap-highlight-color:transparent;
    box-shadow:inset 0 -3px rgba(0,0,0,.35);
  }
  #uhcSburbController .joy {
    pointer-events:auto; position:absolute; left:12px; bottom:10px; width:108px; height:108px;
    border-radius:54px; border:3px solid rgba(77,235,255,.82);
    background:radial-gradient(circle,rgba(27,52,68,.72),rgba(4,10,18,.84));
    touch-action:none; box-shadow:inset 0 0 16px rgba(0,220,255,.28);
  }
  #uhcSburbController .joy:after {
    content:''; position:absolute; left:36px; top:36px; width:32px; height:32px;
    border-radius:50%; background:rgba(230,255,255,.88); border:2px solid rgba(0,0,0,.55);
  }
  #uhcSburbController .joyLabel {
    position:absolute; left:41px; bottom:53px; color:#061016; font:bold 8px monospace;
    z-index:1; pointer-events:none;
  }
  #uhcSburbController .u{left:154px;bottom:82px;width:42px;height:42px;border-radius:9px}
  #uhcSburbController .d{left:154px;bottom:10px;width:42px;height:42px;border-radius:9px}
  #uhcSburbController .l{left:118px;bottom:46px;width:42px;height:42px;border-radius:9px}
  #uhcSburbController .r{left:190px;bottom:46px;width:42px;height:42px;border-radius:9px}
  #uhcSburbController .abxy{position:absolute;right:10px;bottom:10px;width:145px;height:112px;pointer-events:none}
  #uhcSburbController .a,#uhcSburbController .b,#uhcSburbController .x,#uhcSburbController .y{
    width:46px;height:46px;border-radius:50%;font-size:17px
  }
  #uhcSburbController .a{right:0;bottom:33px;border-color:#80ff9a;color:#9affaa}
  #uhcSburbController .b{right:40px;bottom:0;border-color:#ff7272;color:#ff9d9d}
  #uhcSburbController .x{right:40px;bottom:66px;border-color:#72b9ff;color:#a7d4ff}
  #uhcSburbController .y{right:80px;bottom:33px;border-color:#ffe36b;color:#fff1a5}
  #uhcSburbController .shoulder{width:56px;height:28px;border-radius:9px;font-size:9px;bottom:101px}
  #uhcSburbController .shift{left:254px;border-color:#cf8cff;color:#e2b9ff}
  #uhcSburbController .enter{right:170px;border-color:#70ffe8;color:#a5fff2}

  #uhcSpecialScrollRail {
    position:fixed; right:8px; top:50%; transform:translateY(-50%);
    z-index:2147483100; display:none; width:52px; padding:5px;
    border-radius:16px; border:2px solid rgba(255,255,255,.72);
    background:rgba(15,15,15,.72); box-shadow:0 3px 14px rgba(0,0,0,.35);
  }
  #uhcSpecialScrollRail.visible { display:block; }
  #uhcSpecialScrollRail button {
    width:38px; height:48px; margin:2px; border-radius:11px; border:1px solid #ddd;
    background:rgba(245,245,245,.92); color:#111; font:bold 22px sans-serif;
    touch-action:none; -webkit-tap-highlight-color:transparent;
  }
  #uhcSpecialScrollRail .label {
    color:white; font:8px sans-serif; text-align:center; writing-mode:vertical-rl;
    margin:4px auto; height:44px; letter-spacing:1px;
  }

  html.uhc-android [data-component="FULLSCREENFLASH"] .pageFrame {
    width:100% !important; max-width:100vw !important; justify-content:center !important;
  }
  html.uhc-android [data-component="FULLSCREENFLASH"] .pageContent {
    transform-origin:top center !important;
    max-width:none !important;
  }
</style>
<script>
(function(){
  let activeSburbWindow=null, controller=null, scrollRail=null, activeSpecialTab=null;

  function send(code,key,down){
    if(!activeSburbWindow) return;
    try{activeSburbWindow.postMessage({type:'uhcSburbInput',code:code,key:key,down:down},'*');}catch(e){}
  }
  function bindButton(btn,code,key){
    let down=false;
    const press=e=>{e.preventDefault();if(down)return;down=true;send(code,key,true);};
    const release=e=>{if(e)e.preventDefault();if(!down)return;down=false;send(code,key,false);};
    btn.addEventListener('pointerdown',press); btn.addEventListener('pointerup',release);
    btn.addEventListener('pointercancel',release); btn.addEventListener('pointerleave',release);
    btn.addEventListener('contextmenu',e=>e.preventDefault());
  }
  function bindJoystick(el){
    const held={87:false,65:false,83:false,68:false};
    const set=(code,on,key)=>{if(held[code]===on)return;held[code]=on;send(code,key,on);};
    const update=e=>{
      const r=el.getBoundingClientRect(), x=e.clientX-(r.left+r.width/2), y=e.clientY-(r.top+r.height/2), dead=13;
      set(65,x<-dead,'a'); set(68,x>dead,'d'); set(87,y<-dead,'w'); set(83,y>dead,'s');
    };
    const clear=()=>{set(65,false,'a');set(68,false,'d');set(87,false,'w');set(83,false,'s');};
    el.addEventListener('pointerdown',e=>{e.preventDefault();try{el.setPointerCapture(e.pointerId)}catch(_){}update(e);});
    el.addEventListener('pointermove',e=>{if(e.buttons||e.pressure>0)update(e);});
    el.addEventListener('pointerup',e=>{e.preventDefault();clear();}); el.addEventListener('pointercancel',clear);
  }
  function ensureController(){
    if(controller)return controller;
    controller=document.createElement('div'); controller.id='uhcSburbController';
    controller.innerHTML='<div class="plate"></div><div class="brand">SBURB CONTROLLER</div>'+
      '<div class="joy"><span class="joyLabel">MOVE</span></div>'+
      '<button class="u">▲</button><button class="d">▼</button><button class="l">◀</button><button class="r">▶</button>'+
      '<div class="abxy"><button class="a">A</button><button class="b">B</button><button class="x">X</button><button class="y">Y</button></div>'+
      '<button class="shoulder shift">SHIFT</button><button class="shoulder enter">ENTER</button>';
    document.body.appendChild(controller);
    bindJoystick(controller.querySelector('.joy'));
    bindButton(controller.querySelector('.u'),38,'ArrowUp'); bindButton(controller.querySelector('.d'),40,'ArrowDown');
    bindButton(controller.querySelector('.l'),37,'ArrowLeft'); bindButton(controller.querySelector('.r'),39,'ArrowRight');
    bindButton(controller.querySelector('.a'),32,' '); bindButton(controller.querySelector('.b'),17,'Control');
    bindButton(controller.querySelector('.x'),88,'x'); bindButton(controller.querySelector('.y'),90,'z');
    bindButton(controller.querySelector('.shift'),16,'Shift'); bindButton(controller.querySelector('.enter'),13,'Enter');
    return controller;
  }

  function ensureScrollRail(){
    if(scrollRail)return scrollRail;
    scrollRail=document.createElement('div'); scrollRail.id='uhcSpecialScrollRail';
    scrollRail.innerHTML='<button class="up">▲</button><div class="label">SCROLL</div><button class="down">▼</button>';
    document.body.appendChild(scrollRail);
    function hold(btn,dir){
      let timer=null;
      const step=()=>{if(activeSpecialTab)activeSpecialTab.scrollBy({top:dir*28,left:0,behavior:'auto'});};
      const start=e=>{e.preventDefault();step();timer=setInterval(step,24);};
      const stop=e=>{if(e)e.preventDefault();if(timer){clearInterval(timer);timer=null;}};
      btn.addEventListener('pointerdown',start);btn.addEventListener('pointerup',stop);
      btn.addEventListener('pointercancel',stop);btn.addEventListener('pointerleave',stop);
    }
    hold(scrollRail.querySelector('.up'),-1); hold(scrollRail.querySelector('.down'),1);
    return scrollRail;
  }

  function fitSpecialPages(){
    const root=document.querySelector('[data-component="FULLSCREENFLASH"]');
    let crowbar=false;
    activeSpecialTab=null;
    if(root){
      const tab=root.closest('.tabFrame');
      const content=root.querySelector('.pageBody .pageContent');
      const frame=root.querySelector('.pageBody .pageFrame');
      const flash=root.querySelector('iframe.mediaembed[data-flash-id]');
      if(tab&&content&&flash){
        const natural=Math.max(650,Number(flash.getAttribute('width'))||950);
        const available=Math.max(1,tab.clientWidth);
        const fit=Math.min(1,available/natural);
        content.style.zoom=String(fit);
        content.style.width=natural+'px';
        content.style.maxWidth='none';
        content.style.marginLeft='auto';
        content.style.marginRight='auto';
        if(frame){frame.style.width='100%';frame.style.maxWidth='100%';frame.style.justifyContent='center';}
        const id=flash.getAttribute('data-flash-id')||'';
        crowbar=(id==='05492'||id==='05777');
        if(crowbar) activeSpecialTab=tab;
      }
    }
    ensureScrollRail().classList.toggle('visible',crowbar);
  }

  window.addEventListener('message',e=>{
    const d=e.data||{};
    if(d.type==='uhcSburbReady'){
      activeSburbWindow=e.source;
      ensureController().classList.add('visible');
    }else if(d.type==='uhcSburbGone'&&e.source===activeSburbWindow){
      activeSburbWindow=null;if(controller)controller.classList.remove('visible');
    }
  });

  setInterval(()=>{
    if(activeSburbWindow){
      let alive=false;
      document.querySelectorAll('iframe').forEach(f=>{try{if(f.contentWindow===activeSburbWindow&&document.contains(f))alive=true;}catch(e){}});
      if(!alive){activeSburbWindow=null;if(controller)controller.classList.remove('visible');}
    }
    fitSpecialPages();
  },350);

  window.addEventListener('resize',fitSpecialPages);
})();
</script>
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
ruffle_start = s.index("        let scriptTag;")
ruffle_end = s.index("        // Inject Ruffle config", ruffle_start)
s = (
    s[:ruffle_start]
    + r'''        const scriptTag =
          `<script src="${window.location.origin}/ruffle/ruffle.js"><\/script>`;
'''
    + s[ruffle_end:]
)

# Hide Ruffle's branded splash/preloader so Flash pages feel like native UHC.
# The iframe already uses the SWF's configured background colour while loading.
ruffle_config_marker = """              function isIOS() {"""
ruffle_config_insert = """              window.RufflePlayer = window.RufflePlayer || {};
              window.RufflePlayer.config = {
                ...(window.RufflePlayer.config || {}),
                splashScreen: false,
                preloader: false,
                contextMenu: false
              };

              function isIOS() {"""
if ruffle_config_marker not in s:
    raise RuntimeError("Ruffle config insertion point not found")
s = s.replace(ruffle_config_marker, ruffle_config_insert, 1)
# Expose the resolved Flash id to the Android shell for page-specific UI.
swf_tag = """    @load="initIframe"
    seamless
    class="mediaembed"
  />"""
swf_tag_new = """    @load="initIframe"
    seamless
    :data-flash-id="flashProps.id"
    class="mediaembed"
  />"""
if swf_tag not in s:
    raise RuntimeError("SWF iframe tag marker not found")
s = s.replace(swf_tag, swf_tag_new, 1)
write("src/components/UIElements/MediaEmbed.vue", s)

# The first Openbound/Meenah cutscene is overlaid separately from the HTML5
# Sburb game. Its soundtrack is a separate BGM asset, so play it during the
# overlay and stop it before handing control back to the game.
s = read("src/components/StoryPage/Page.vue")
intro_marker = """              <div
                ref="introOverlay"
                class="panel intro-overlay"
                style="position:absolute;z-index:99;pointer-events:auto;"
              >"""
intro_replacement = """              <div
                ref="introOverlay"
                class="panel intro-overlay"
                style="position:absolute;z-index:99;pointer-events:auto;"
              >
                <audio
                  ref="introAudio"
                  autoplay
                  preload="auto"
                  :src="$getResourceURL('/storyfiles/hs2/05260/resources/openbound/audio/music/FuschiaRulerQuiet.mp3')"
                  style="display:none;"
                ></audio>"""
if intro_marker not in s:
    raise RuntimeError("Openbound intro overlay marker not found")
s = s.replace(intro_marker, intro_replacement, 1)
finish_marker = """      if (event.data && event.data.type === "swfFinished") {
        // Hide the intro overlay
        this.showIntroOverlay = false;
      }"""
finish_replacement = """      if (event.data && event.data.type === "swfFinished") {
        if (this.$refs.introAudio) {
          try {
            this.$refs.introAudio.pause()
            this.$refs.introAudio.currentTime = 0
          } catch (e) {}
        }
        // Hide the intro overlay
        this.showIntroOverlay = false;
      }"""
if finish_marker not in s:
    raise RuntimeError("Openbound intro finish handler not found")
s = s.replace(finish_marker, finish_replacement, 1)
write("src/components/StoryPage/Page.vue", s)

# Give the special A6A5 x2 Combo component a stable Android-only selector.
s = read("src/components/SpecialPages/x2Combo.vue")
old_x2_root = '<div class="pageBody customStyles">'
new_x2_root = '<div class="pageBody customStyles" data-component="x2Combo">'
if old_x2_root not in s:
    raise RuntimeError("x2Combo root element not found")
s = s.replace(old_x2_root, new_x2_root, 1)

# Preserve the exact desktop x2 composition and scale the WHOLE thing to one
# Android viewport. CSS zoom participates in layout, unlike transform:scale,
# so the page remains vertically scrollable and all links keep their hitboxes.
x2_methods_marker = """  methods: {
    keyNavEvent(dir) {"""
if x2_methods_marker not in s:
    raise RuntimeError("x2Combo methods marker not found")

x2_lifecycle = """  mounted() {
    this.__androidPinchScale = 1
    this.__androidPinchStartDistance = 0
    this.__androidPinchStartScale = 1

    this.__androidFitX2 = () => {
      const host = this.$el.parentElement
      const available = (host && host.clientWidth) || document.documentElement.clientWidth || window.innerWidth
      const fit = Math.min(1, available / 1660)
      const saved = Math.max(0.60, Math.min(1.80, Number(this.$localData.settings.x2ComboZoomPercent || 100) / 100))
      const pinch = this.$localData.settings.x2ComboAllowUserZoom === false
        ? 1
        : Math.max(0.60, Math.min(2.50, Number(this.__androidPinchScale || 1)))
      const scale = fit * saved * pinch
      this.$el.style.zoom = String(scale)
      this.$el.style.width = "1660px"
      this.$el.style.minWidth = "1660px"
      this.$el.style.maxWidth = "none"
    }

    const distance = touches => {
      const dx = touches[0].clientX - touches[1].clientX
      const dy = touches[0].clientY - touches[1].clientY
      return Math.sqrt(dx * dx + dy * dy)
    }

    this.__androidTouchStart = e => {
      if (this.$localData.settings.x2ComboAllowUserZoom === false) return
      if (e.touches && e.touches.length === 2) {
        this.__androidPinchStartDistance = distance(e.touches)
        this.__androidPinchStartScale = this.__androidPinchScale || 1
      }
    }
    this.__androidTouchMove = e => {
      if (this.$localData.settings.x2ComboAllowUserZoom === false) return
      if (e.touches && e.touches.length === 2 && this.__androidPinchStartDistance > 0) {
        e.preventDefault()
        const ratio = distance(e.touches) / this.__androidPinchStartDistance
        this.__androidPinchScale = Math.max(0.60, Math.min(2.50, this.__androidPinchStartScale * ratio))
        this.__androidFitX2()
      }
    }
    this.__androidTouchEnd = e => {
      if (!e.touches || e.touches.length < 2) this.__androidPinchStartDistance = 0
    }

    this.$nextTick(this.__androidFitX2)
    window.addEventListener("resize", this.__androidFitX2)
    this.$el.addEventListener("touchstart", this.__androidTouchStart, { passive: true })
    this.$el.addEventListener("touchmove", this.__androidTouchMove, { passive: false })
    this.$el.addEventListener("touchend", this.__androidTouchEnd, { passive: true })
    this.$el.addEventListener("touchcancel", this.__androidTouchEnd, { passive: true })
  },
  beforeDestroy() {
    if (this.__androidFitX2) window.removeEventListener("resize", this.__androidFitX2)
    if (this.__androidTouchStart) this.$el.removeEventListener("touchstart", this.__androidTouchStart)
    if (this.__androidTouchMove) this.$el.removeEventListener("touchmove", this.__androidTouchMove)
    if (this.__androidTouchEnd) {
      this.$el.removeEventListener("touchend", this.__androidTouchEnd)
      this.$el.removeEventListener("touchcancel", this.__androidTouchEnd)
    }
  },
"""
s = s.replace(x2_methods_marker, x2_lifecycle + x2_methods_marker)
write("src/components/SpecialPages/x2Combo.vue", s)

# Add the Android-specific global x2 Combo zoom preference.
s = read("src/store/localData.js")
zoom_marker = """  ruffleFallback: true,

  // Dynamically enable mods based on what's available in the asset pack"""
zoom_replacement = """  ruffleFallback: true,
  x2ComboZoomPercent: 100,
  x2ComboAllowUserZoom: true,

  // Dynamically enable mods based on what's available in the asset pack"""
if zoom_marker not in s:
    raise RuntimeError("x2 zoom DEFAULT_SETTINGS marker not found")
s = s.replace(zoom_marker, zoom_replacement, 1)
write("src/store/localData.js", s)

# Add an Android-specific settings card linking to a dedicated preview page.
s = read("src/components/SystemPages/Settings.vue")
settings_anchor = """    <div class="card">
      <div class="settings controversial">"""
settings_card = """    <div class="card">
      <div class="settings">
        <h2>2x Combo Layout</h2>
        <p class="settingDesc">
          Set the default size of Act 6 Act 5's dual-page x2 Combo layout.
          The preview page also includes an "Allow user to zoom" toggle, enabled by default.
        </p>
        <p><a href="/x2-combo-preview">Open 2x Combo Preview</a></p>
      </div>
    </div>

"""
if settings_anchor not in s:
    raise RuntimeError("Settings x2 card insertion point not found")
s = s.replace(settings_anchor, settings_card + settings_anchor, 1)
write("src/components/SystemPages/Settings.vue", s)

# Dedicated x2 preview/settings page.
x2_preview = r'''<template>
  <GenericCardPage>
    <div class="x2SettingsBar">
      <div class="title">2x Combo Preview</div>
      <label class="zoomToggle">
        <input
          type="checkbox"
          v-model="$localData.settings.x2ComboAllowUserZoom"
          @change="save"
        />
        Allow user to zoom
      </label>
      <label class="slider">
        Default zoom
        <input
          type="range"
          min="60"
          max="180"
          step="5"
          v-model.number="$localData.settings.x2ComboZoomPercent"
          @input="save"
          @change="save"
        />
        <strong>{{ $localData.settings.x2ComboZoomPercent || 100 }}%</strong>
      </label>
      <button @click="reset">Reset</button>
      <a href="/settings">Back to Settings</a>
    </div>

    <div class="previewHost" ref="viewport">
      <div class="previewStage" :style="{ zoom: previewZoom }">
        <div class="previewHeader">ACT 6 ACT 5 ACT 1 x2 COMBO!!!</div>
        <div class="previewNav">
          <span>HOMESTUCK COLLECTION · HELP · MAP · LOG · SEARCH</span>
          <span>HOMESTUCK COLLECTION · HELP · MAP · LOG · SEARCH</span>
        </div>
        <div class="previewPages">
          <section>
            <div class="arrow">&gt; ==&gt;</div>
            <div class="fakeArt leftArt">
              <div class="moon moonLeft"></div>
              <div class="chain"></div>
            </div>
            <div class="log">
              <button>Hide Pesterlog</button><br /><br />
              <span class="tt">TT: Is it still going on?</span><br />
              <span class="gg">GG: No, it stopped.</span><br />
              <span class="tt">TT: What do you think it was?</span><br />
              <span class="gg">GG: I don't know. Maybe an earthquake?</span>
            </div>
            <div class="bottomArrow">&gt; ==&gt; &nbsp;&nbsp; ↖ back to top</div>
          </section>
          <section>
            <div class="arrow">&gt; ==&gt;</div>
            <div class="fakeArt rightArt">
              <div class="moon moonRight"></div>
              <div class="chain"></div>
            </div>
            <div class="log">
              <button>Hide Pesterlog</button><br /><br />
              <span class="gg">GG: Doesn't matter.</span><br />
              <span class="tt">TT: Maybe it was like, tidal forces.</span><br />
              <span class="gg">GG: Um. Yeah let's say that.</span>
            </div>
            <div class="bottomArrow">&gt; ==&gt; ==&gt; &nbsp;&nbsp; next page</div>
          </section>
        </div>
      </div>
    </div>
  </GenericCardPage>
</template>

<script>
import GenericCardPage from "@/components/Template/GenericCardPage.vue";

export default {
  name: "X2ComboPreview",
  components: { GenericCardPage },
  data() {
    return { viewportWidth: 1000 };
  },
  computed: {
    previewZoom() {
      const fit = Math.min(1, (this.viewportWidth || 1000) / 1660);
      const chosen = Math.max(0.6, Math.min(1.8, Number(this.$localData.settings.x2ComboZoomPercent || 100) / 100));
      return fit * chosen;
    }
  },
  mounted() {
    this.updateWidth();
    this.__resizePreview = () => this.updateWidth();
    window.addEventListener("resize", this.__resizePreview);
  },
  beforeDestroy() {
    if (this.__resizePreview) window.removeEventListener("resize", this.__resizePreview);
  },
  methods: {
    updateWidth() {
      this.$nextTick(() => {
        if (this.$refs.viewport) this.viewportWidth = this.$refs.viewport.clientWidth;
      });
    },
    save() {
      this.$localData.root.saveLocalStorage();
      this.updateWidth();
    },
    reset() {
      this.$localData.settings.x2ComboZoomPercent = 100;
      this.$localData.settings.x2ComboAllowUserZoom = true;
      this.save();
    }
  }
};
</script>

<style scoped>
.x2SettingsBar {
  position: sticky;
  top: 0;
  z-index: 50;
  width: 100%;
  min-height: 62px;
  padding: 8px 14px;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  flex-wrap: wrap;
  background: rgba(238,238,238,.96);
  border-top: 1px solid #aaa;
  border-bottom: 1px solid #888;
  color: #111;
  font: 14px Verdana, Arial, sans-serif;
}
.x2SettingsBar .title { font-weight: bold; font-size: 18px; }
.x2SettingsBar .slider { display:flex; align-items:center; gap:8px; }
.x2SettingsBar .slider input { width:min(42vw,420px); }
.x2SettingsBar a { color:#03c; }
.previewHost {
  width:100%;
  max-width:100%;
  overflow:auto;
  background:#535353;
  align-self:stretch;
  min-height:100vh;
}
.previewStage {
  width:1660px;
  min-width:1660px;
  transform-origin:top left;
  background:#535353;
  font-family:"Courier New", monospace;
}
.previewHeader {
  width:1660px; box-sizing:border-box; text-align:center; padding:16px;
  font-size:42px; font-weight:bold; background:#222; color:#ffd84a;
}
.previewNav {
  display:flex; width:1660px; box-sizing:border-box; justify-content:space-around;
  padding:8px; background:#666; color:white; font:bold 13px Arial;
}
.previewPages { display:flex; gap:33px; justify-content:center; align-items:flex-start; padding:12px 0 40px; }
.previewPages section { width:650px; background:#eee; }
.arrow { height:70px; box-sizing:border-box; padding:18px; font-size:30px; text-align:center; color:#111; }
.fakeArt {
  height:450px; position:relative; overflow:hidden;
  border-top:4px solid currentColor; border-bottom:4px solid currentColor;
}
.leftArt { background:#050505; color:#cf00ff; }
.rightArt { background:#050505; color:#ffc000; }
.moon { position:absolute; width:260px; height:260px; border-radius:50%; top:90px; box-shadow:0 0 28px currentColor; background:currentColor; }
.moonLeft { left:210px; color:#a000ff; }
.moonRight { left:180px; color:#ffbf00; }
.chain {
  position:absolute; left:0; right:0; top:220px; height:28px;
  background:repeating-linear-gradient(90deg,#6b4300 0 28px,#111 28px 38px);
  transform:rotate(-8deg);
}
.log { margin:30px 22px; min-height:150px; border:1px dashed #888; padding:14px; font-size:17px; line-height:1.35; }
.tt { color:#e5a800; } .gg { color:#00b8d4; }
.bottomArrow { padding:18px 24px 30px; font-size:23px; text-decoration:underline; color:#03c; }
</style>
''''''
write("src/components/SystemPages/X2ComboPreview.vue", x2_preview)

# Register the new preview route/component.
s = read("src/components/TabFrame.vue")
settings_import = "const SETTINGS = () => import('@/components/SystemPages/Settings.vue')"
if settings_import not in s:
    raise RuntimeError("TabFrame settings import not found")
s = s.replace(settings_import, settings_import + "\nconst X2COMBOPREVIEW = () => import('@/components/SystemPages/X2ComboPreview.vue')", 1)
settings_component = "        SETTINGS,\n        CREDITS,"
if settings_component not in s:
    raise RuntimeError("TabFrame settings component registration not found")
s = s.replace(settings_component, "        SETTINGS,\n        X2COMBOPREVIEW,\n        CREDITS,", 1)
route_marker = "                'mspfa': 'MSPFADisambig'\n"
if route_marker not in s:
    raise RuntimeError("TabFrame mapRoute marker not found")
s = s.replace(route_marker, "                'mspfa': 'MSPFADisambig',\n                'x2-combo-preview': 'X2ComboPreview'\n", 1)
write("src/components/TabFrame.vue", s)

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
