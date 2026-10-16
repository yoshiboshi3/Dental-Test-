/* Android compatibility bridge for legacy Sburb/Openbound pages. */
(function(){
  "use strict";

  function parentPost(data){
    try { window.parent.postMessage(data, "*"); } catch (_) {}
  }

  function report(msg){
    try{
      let p=document.getElementById("uhcOpenboundError");
      if(!p){
        p=document.createElement("pre");
        p.id="uhcOpenboundError";
        p.style.cssText="position:fixed;left:6px;right:6px;top:6px;z-index:2147483647;max-height:42vh;overflow:auto;background:rgba(0,0,0,.86);color:#ffb;padding:8px;font:11px monospace;white-space:pre-wrap;pointer-events:none";
        document.body.appendChild(p);
      }
      p.textContent += msg + "\n";
    }catch(_){}
  }

  window.addEventListener("error", e => report("JS "+(e.message||"error")+" @ "+(e.filename||"")+":"+(e.lineno||"")));
  window.addEventListener("unhandledrejection", e => report("PROMISE "+String(e.reason)));

  function eventFor(type,code,key){
    let e;
    try {
      e=new KeyboardEvent(type,{key:key||"",code:key||"",keyCode:code,which:code,bubbles:true,cancelable:true});
      Object.defineProperty(e,"keyCode",{value:code});
      Object.defineProperty(e,"which",{value:code});
    } catch(_) {
      e=new Event(type,{bubbles:true,cancelable:true});
      try {
        Object.defineProperty(e,"keyCode",{value:code});
        Object.defineProperty(e,"which",{value:code});
        Object.defineProperty(e,"key",{value:key||""});
      } catch(__) {}
    }
    return e;
  }

  function keyboardTarget(){
    return document.getElementById("SBURBgameDiv") ||
           document.getElementById("SBURBStage") ||
           document.body;
  }

  function sendKey(type,code,key){
    const t=keyboardTarget();
    try {
      const st=document.getElementById("SBURBStage");
      if(st) st.focus();
    } catch(_) {}
    const e=eventFor(type,code,key);
    const h=(type==="keydown" ? t.onkeydown : t.onkeyup);
    if(typeof h==="function"){
      try { h.call(t,e); return; } catch(err){ report("INPUT "+String(err)); }
    }
    try { t.dispatchEvent(e); } catch(_) {}
    try { document.dispatchEvent(eventFor(type,code,key)); } catch(_) {}
    try { window.dispatchEvent(eventFor(type,code,key)); } catch(_) {}
  }

  let mouseMode=false;
  let translateDamara=false;

  window.addEventListener("message", e => {
    const d=e.data||{};
    if(d.type==="uhcSburbInput"){
      sendKey(d.down?"keydown":"keyup",Number(d.code),String(d.key||""));
    } else if(d.type==="uhcSburbMouseMode"){
      mouseMode=!!d.enabled;
    } else if(d.type==="uhcDamaraTranslate"){
      translateDamara=!!d.enabled;
      try {
        if(window.Sburb && Sburb.dialoger && Sburb.dialoger.dialog && Sburb.dialoger.dialog.__uhcOriginalText){
          const eng=Sburb.dialoger.dialog;
          const start=eng.start, end=eng.end;
          eng.setText(eng.__uhcOriginalText);
          eng.showSubText(start,end);
        }
      } catch(_) {}
    }
  });

  function installTouchMouse(){
    const style=document.createElement("style");
    style.textContent="#SBURBgameDiv,#SBURBStage,#SBURBgameDiv canvas{touch-action:none!important;-webkit-user-select:none!important;user-select:none!important;-webkit-touch-callout:none!important}";
    document.head.appendChild(style);

    const mouse=(type,touch)=>{
      const target=document.elementFromPoint(touch.clientX,touch.clientY) ||
                   document.getElementById("SBURBStage") || keyboardTarget();
      try {
        target.dispatchEvent(new MouseEvent(type,{
          bubbles:true,cancelable:true,view:window,
          clientX:touch.clientX,clientY:touch.clientY,
          screenX:touch.screenX,screenY:touch.screenY,
          button:0,buttons:(type==="mouseup"||type==="click")?0:1
        }));
      } catch(_) {}
    };

    document.addEventListener("touchstart",e=>{
      if(!e.touches||!e.touches.length)return;
      e.preventDefault();
      mouse("mousemove",e.touches[0]);
      mouse("mousedown",e.touches[0]);
    },{passive:false,capture:true});

    document.addEventListener("touchend",e=>{
      const t=(e.changedTouches&&e.changedTouches[0]);
      if(!t)return;
      e.preventDefault();
      mouse("mouseup",t);
      mouse("click",t);
    },{passive:false,capture:true});

    document.addEventListener("touchmove",e=>{
      if(!mouseMode||!e.touches||!e.touches.length)return;
      e.preventDefault();
      mouse("mousemove",e.touches[0]);
    },{passive:false,capture:true});
  }

  const CJK=/[\u3040-\u30ff\u3400-\u9fff]/;
  const DAMARA_TRANSLATIONS = {
    "あなたは時間の領主に挑戦します。":"You challenge the Lord of Time.",
    "あなたの哀れな軍隊は失敗しなければならない。":"Your pathetic army must fail.",
    "彼はあなたの幽霊を食べるようになる。彼は現実そのものを消費するからである。":"He will come to eat your ghosts, because he consumes reality itself.",
    "ない。あなたはそうしなかった。":"No. You did not.",
    "私は何も後悔はありません。":"I regret nothing.",
    "なぜですか？":"Why?",
    "はい、もちろん。私はあなたの友達です。":"Yes, of course. I am your friend.",
    "何を壊す？彼のホーン？":"Break what? His horn?",
    "あなたのセックスライフはどうですか？":"How is your sex life?",
    "RUFIOH。私はあなたにこのことを告げた。毎日。永遠に。":"RUFIOH. I told you this. Every day. Forever.",
    "私は怒って嫉妬した。":"I was angry and jealous.",
    "はい。愛が私には死んでいる。ので、誰かがずっと前に、心臓を介して私を刺した。":"Yes. Love is dead to me, because someone stabbed me through the heart a long time ago.",
    "あなたは私が彼を殺したいですか？再び？":"Do you want me to kill him? Again?",
    "私は彼を誘惑したいですか？":"Do you want me to seduce him?",
    "私は彼の魂を消費するために悪魔をもたらすでしょう。":"I will bring a demon to consume his soul.",
    "あなたは必然だけを遅らせる。":"You only delay the inevitable.",
    "私たちの終了時間が近いです。":"Our ending time is near.",
    "あなたのデュアルフォークを取る。二回自分自身をファック。":"Take your dual fork. Fuck yourself twice.",
    "あなたに性的快感を与えるために十分な厚さではない。":"It is not thick enough to give you sexual pleasure.",
    "あなたはあなたの言葉で私を退屈続けるのだろうか？または。あなたは私の服を脱ぐのだろうか？":"Will you keep boring me with your words? Or will you take off my clothes?",
    "私はあなたの歯の間に私の乳首を感じるようにしたい。":"I want to feel my nipple between your teeth.",
    "十分に公平。":"Fair enough.",
    "私が覚えている。":"I remember.",
    "時々私は、そのメモリに自慰行為。":"Sometimes I masturbate to that memory.",
    "性交あなたは何を話している？白痴雌犬。":"Fuck, what are you talking about? Idiot bitch.",
    "ハマグリは、真珠を生成しません。あなたは海の何も知らない。":"Clams do not make pearls. You know nothing about the sea.",
    "あなたは非常に怒っているように見える。性交をチルアウト。のはお互いに触れてみましょう。私と一緒に投石入手してください。":"You look very angry. Fucking chill out. Let us touch each other. Get stoned with me.",
    "正確にどのくらいの。あなたは知りたいですか？水のビッチ。":"Exactly how much do you want to know, water bitch?",
    "あなたは私になりたいですか？":"Do you want to be me?",
    "あなたは私にはできません。あなたは私を理解できない場合。":"You cannot be me if you cannot understand me.",
    "さらに。あなたは私にはできません。あなたは私を性交することができない場合。":"Furthermore, you cannot be me if you cannot fuck me.",
    "もちろんそんなことはない。あなたの告発は言語道断。":"Of course not. Your accusation is outrageous.",
    "黙れ。そう。あなたはまだ彼と別れたのですか？":"Shut up. So, have you broken up with him yet?",
    "あなたは、どういう意味ですか。それは恋人があなたを裏切ったときに感じる？はい、私は知っている。":"What do you mean? How does it feel when a lover betrays you? Yes, I know.",
    "右。我々が表示されます。":"Right. We will see.",
    "いいえクソ方法。":"No fucking way.",
    "罰金。しかし、あなたは私に借りがある。性的な接待。":"Fine. But you owe me. Sexual entertainment.",
    "あなたは私の胸をいつでも表示することができます。":"You can look at my breasts anytime.",
    "私のおっぱいの上にミルクを注いでください。":"Pour milk on my tits.",
    "ない。手袋を着用しないでください。私はそれがラフ好きです。私の体を押しつぶす。馬男。":"No. Do not wear gloves. I like it rough. Crush my body, horse man.",
    "あなたは、幽霊のような私の性器が性的クライマックスを持っていることを確認してください。あなたの幻の馬のペニスでそれを行う必要があります。":"Make sure my ghostly genitals reach sexual climax. Do it with your phantom horse penis.",
    "私の裸の底にあなたの汗まみれの顔をこする。":"Rub your sweaty face on my bare bottom.",
    "私はあなたのアジアの女子高生です。ズタズタに私の服を減らすことができます。あなたは私を喜ばせる必要があります。あなたのホーンを使用してください。":"I am your Asian schoolgirl. Tear my clothes to shreds. You need to please me. Use your horn.",
    "静かにしています。黙って私をファック。":"Be quiet. Shut up and fuck me.",
    "私の体内に入る。または。私はあなたを破壊するだろう。":"Enter my body. Or I will destroy you.",
    "これ以上のゲームはありません。馬男。今私をファック。":"No more games, horse man. Fuck me now.",
    "HORSEAPONIまたがっ私を曲げる。あなたは私がハードファック。HORSEYスタイル。":"Bend me over the HORSEAPONI. Fuck me hard. HORSEY style.",
    "私にリンゴを養う。その後、私の髪をつかむ。あなたはチャンピオンのように私に乗るだろう。":"Feed me an apple. Then grab my hair. Ride me like a champion.",
    "のバケツを埋めることができます。その後、内容がスプラッシュ。私の体の上にすべての。次に干し草の山に私を投げる。":"Fill a bucket. Splash the contents all over my body. Then throw me into a haystack.",
    "私はあなたのホーンを吸うことができます。同時に、あなたは私のお尻を平手打ち。リズミカルにそれを行う。また。いななく。":"I can suck your horn. At the same time, slap my ass. Do it rhythmically. Also, neigh.",
    "私のすべての反復をファック。性的エクスタシーを体験する私たちのすべてを引き起こす。私たちは一斉にオーガズムましょう。":"Fuck every iteration of me. Make all of us experience sexual ecstasy. Let us orgasm in unison.",
    "清潔なタオルで私を包んでください。私は農場の動物のようにうめき声をしなければならない。":"Wrap me in a clean towel. I must moan like a farm animal.",
    "私の足の間に腹立たしそうに鼻を鳴らす。私は殺された子羊のように悲鳴を上げるでしょう。":"Snort angrily between my legs. I will scream like a slaughtered lamb.",
    "私たちは一緒に私たちのお尻を持ってみましょう。共通のオーガズムに参加。すべての終わりでは、私たちを取り囲んでいる。":"Let us put our asses together and join in a shared orgasm. The end of everything surrounds us.",
    "もっと私を低下させる。私はほとんどそこにいる。":"Degrade me more. I am almost there.",
    "フリーク。":"Freak."
  };

  function translateDamaraText(text){
    let out=text;
    for(const jp in DAMARA_TRANSLATIONS){
      out=out.split(jp).join(DAMARA_TRANSLATIONS[jp]);
    }
    return out;
  }

  function addCjkBreaks(text){
    const firstSpace=text.indexOf(" ");
    if(firstSpace<0)return text;
    const prefix=text.slice(0,firstSpace+1);
    let body=text.slice(firstSpace+1), run=0, out="";
    for(const ch of body){
      out+=ch;
      if(CJK.test(ch)){
        run++;
        if(run>=10){ out+=" "; run=0; }
      } else if(/\s/.test(ch)||/[。！？、]/.test(ch)){
        run=0;
      }
    }
    return prefix+out;
  }

  function installSburbPatches(){
    if(!window.Sburb || !Sburb.FontEngine || Sburb.FontEngine.prototype.__uhcMobilePatched)return false;
    const proto=Sburb.FontEngine.prototype;
    const originalSetText=proto.setText;
    proto.setText=function(text){
      const raw=String(text==null?"":text);
      this.__uhcOriginalText=raw;
      if(/DAMARA:\s*.*[\u3040-\u30ff\u3400-\u9fff]/.test(raw)){
        parentPost({type:"uhcDamaraSeen"});
      }
      let next=translateDamara ? translateDamaraText(raw) : raw;
      next=addCjkBreaks(next);
      return originalSetText.call(this,next);
    };
    proto.__uhcMobilePatched=true;
    return true;
  }

  function ready(){
    installTouchMouse();
    if(!installSburbPatches()){
      const timer=setInterval(()=>{ if(installSburbPatches()) clearInterval(timer); },100);
      setTimeout(()=>clearInterval(timer),15000);
    }
    parentPost({type:"uhcSburbReady"});
  }

  if(document.readyState==="loading") document.addEventListener("DOMContentLoaded",ready);
  else ready();

  window.addEventListener("pagehide",()=>parentPost({type:"uhcSburbGone"}));
})();