import { useState, useEffect } from "react";

const questions = [
  // Tooth Types
  {
    category: "Tooth Types",
    question: "Which tooth type is responsible for BITING into food (like an apple)?",
    options: ["Canine", "Incisor", "Molar", "Premolar"],
    answer: "Incisor",
    explanation: "Incisors are the thin, flat front teeth — their straight cutting edge is perfect for biting into food. There are 8 total.",
    emoji: "🍎"
  },
  {
    category: "Tooth Types",
    question: "What are canines also called?",
    options: ["Bicuspids", "Cuspids", "Molars", "Laterals"],
    answer: "Cuspids",
    explanation: "Canines = Cuspids. Both terms mean the same thing! They're the pointy corner teeth used for gripping and tearing.",
    emoji: "🐕"
  },
  {
    category: "Tooth Types",
    question: "How many total teeth does a full adult set have (including wisdom teeth)?",
    options: ["28", "30", "32", "20"],
    answer: "32",
    explanation: "A full adult set = 32 teeth: 8 incisors, 4 canines, 8 premolars, 12 molars. Many people only have 28 after wisdom teeth removal.",
    emoji: "🔢"
  },
  {
    category: "Tooth Types",
    question: "Premolars are also called...?",
    options: ["Cuspids", "Bicuspids", "Tricuspids", "Laterals"],
    answer: "Bicuspids",
    explanation: "Premolar = Bicuspid. 'Bi' means two — these teeth typically have TWO cusps. You'll see both terms on prescriptions!",
    emoji: "✌️"
  },
  {
    category: "Tooth Types",
    question: "When a prescription says 'POSTERIOR', what should you be thinking?",
    options: ["Esthetics — make it look natural", "Strength — it needs to be durable", "Shade matching is #1 priority", "It's a front tooth case"],
    answer: "Strength — it needs to be durable",
    explanation: "Posterior = back teeth (premolars & molars). These take heavy chewing forces, so STRENGTH is the priority. Anterior = front teeth = esthetics.",
    emoji: "💪"
  },
  // Dental Arches
  {
    category: "Dental Arches",
    question: "Which arch is called the 'MAXILLARY' arch?",
    options: ["Lower arch", "Upper arch", "Both arches", "Neither — it's a bone"],
    answer: "Upper arch",
    explanation: "Maxillary = Upper. Mandibular = Lower. On prescriptions: 'U' = upper/maxillary, 'L' = lower/mandibular.",
    emoji: "⬆️"
  },
  {
    category: "Dental Arches",
    question: "Why are LOWER dentures harder to fit than upper dentures?",
    options: [
      "Lower teeth are bigger",
      "The lower arch has no palate for suction",
      "Lower bone is weaker",
      "Lower dentures use cheaper materials"
    ],
    answer: "The lower arch has no palate for suction",
    explanation: "Upper dentures use the palate to create suction — great retention! Lower dentures have only the narrow ridge to rest on, with the tongue in the way. Much trickier.",
    emoji: "😬"
  },
  {
    category: "Dental Arches",
    question: "What is the MANDIBLE?",
    options: [
      "The roof of the mouth",
      "The upper jaw bone",
      "The only movable bone in the skull",
      "The tissue connecting the tongue to the floor"
    ],
    answer: "The only movable bone in the skull",
    explanation: "The mandible is the lower jaw bone — and it's the ONLY bone in your entire skull that moves. That's what lets you chew, talk, and yawn!",
    emoji: "🦴"
  },
  // Tooth Numbering
  {
    category: "Tooth Numbering",
    question: "In the Universal Numbering System, where does tooth #1 sit?",
    options: [
      "Upper left wisdom tooth",
      "Upper right central incisor",
      "Upper right wisdom tooth",
      "Lower right wisdom tooth"
    ],
    answer: "Upper right wisdom tooth",
    explanation: "The Universal System starts at the upper RIGHT wisdom tooth (#1), sweeps across the top to #16, drops to lower left (#17), and ends at lower right (#32).",
    emoji: "1️⃣"
  },
  {
    category: "Tooth Numbering",
    question: "Teeth #8 and #9 are which teeth?",
    options: [
      "Upper first molars",
      "Upper central incisors (the two big front teeth)",
      "Upper canines",
      "Lower central incisors"
    ],
    answer: "Upper central incisors (the two big front teeth)",
    explanation: "#8 = upper RIGHT central incisor, #9 = upper LEFT central incisor. These are the most visible teeth — you'll see them on LOTS of esthetic cases!",
    emoji: "😁"
  },
  {
    category: "Tooth Numbering",
    question: "Teeth #19 and #30 are the lower first molars. Why should you memorize these?",
    options: [
      "They're the hardest to make",
      "They're among the most common teeth on crown prescriptions",
      "They're always shade A2",
      "They're the only teeth that need metal"
    ],
    answer: "They're among the most common teeth on crown prescriptions",
    explanation: "#19 (lower left) and #30 (lower right) first molars are the workhorses of the mouth. They take massive chewing forces and are super common crown cases!",
    emoji: "👑"
  },
  // Tooth Surfaces
  {
    category: "Tooth Surfaces",
    question: "The surface facing the TONGUE is called...?",
    options: ["Buccal", "Labial", "Lingual", "Occlusal"],
    answer: "Lingual",
    explanation: "Lingual = tongue side. This applies to ALL teeth. On upper teeth you might also hear 'palatal' since it faces the palate — same thing!",
    emoji: "👅"
  },
  {
    category: "Tooth Surfaces",
    question: "What does 'MESIAL' mean?",
    options: [
      "Toward the back of the mouth",
      "Toward the midline (center)",
      "Toward the tongue",
      "Toward the cheek"
    ],
    answer: "Toward the midline (center)",
    explanation: "Mesial = toward the MIDDLE. Distal = toward the DISTANCE (back of mouth). Easy memory trick: Mesial = Middle!",
    emoji: "🎯"
  },
  {
    category: "Tooth Surfaces",
    question: "An 'MOD' restoration covers which surfaces?",
    options: [
      "Mesial, Occlusal, Distal",
      "Molar, Occlusal, Dentin",
      "Mesial, Open, Distal",
      "Metal, Occlusal, Dentin"
    ],
    answer: "Mesial, Occlusal, Distal",
    explanation: "MOD = Mesial + Occlusal + Distal. That's 3 surfaces of a tooth. You'll see these surface abbreviations constantly on prescriptions!",
    emoji: "📋"
  },
  {
    category: "Tooth Surfaces",
    question: "The chewing surface of a MOLAR is called the...?",
    options: ["Incisal", "Occlusal", "Buccal", "Labial"],
    answer: "Occlusal",
    explanation: "Occlusal = chewing surface of POSTERIOR teeth (premolars & molars). Front teeth have an INCISAL edge instead — because they're thin and don't have a broad chewing surface.",
    emoji: "⚙️"
  },
  // Tooth Anatomy
  {
    category: "Tooth Anatomy",
    question: "What is the HARDEST substance in the human body?",
    options: ["Bone", "Dentin", "Enamel", "Zirconia"],
    answer: "Enamel",
    explanation: "Tooth enamel is the hardest substance in the human body — even harder than bone! When we make porcelain crowns, we're trying to replicate enamel's strength and translucency.",
    emoji: "💎"
  },
  {
    category: "Tooth Anatomy",
    question: "Why do teeth that have had ROOT CANALS often need crowns?",
    options: [
      "Root canals change the tooth's color",
      "The pulp is removed, making the tooth brittle",
      "Root canals weaken the enamel",
      "Insurance requires it"
    ],
    answer: "The pulp is removed, making the tooth brittle",
    explanation: "The pulp contains nerves and blood vessels that keep the tooth 'alive.' Without it, the tooth becomes dry and brittle — much more likely to crack. A crown protects it!",
    emoji: "🪥"
  },
  {
    category: "Tooth Anatomy",
    question: "What is a CUSP?",
    options: [
      "A valley between teeth",
      "A raised point or bump on the biting surface",
      "The edge of a front tooth",
      "The root tip"
    ],
    answer: "A raised point or bump on the biting surface",
    explanation: "Cusps are the raised bumps on the biting surface. Molars have 4-5 cusps, premolars have 2 (hence 'bicuspid'!). When upper and lower teeth meet, cusps fit into the opposing tooth's FOSSA.",
    emoji: "⛰️"
  },
  {
    category: "Directional Terms",
    question: "If a margin is described as 'CERVICAL', where is the problem?",
    options: [
      "At the biting surface",
      "Near the root tip",
      "Near the gumline",
      "On the tongue side"
    ],
    answer: "Near the gumline",
    explanation: "Cervical = toward the neck of the tooth, near the gumline. Also called 'gingival.' The opposite direction is OCCLUSAL (toward the biting surface).",
    emoji: "📍"
  },
  {
    category: "Directional Terms",
    question: "What does INTERPROXIMAL mean?",
    options: [
      "Inside the tooth",
      "The space between two adjacent teeth",
      "The area near the root tip",
      "The biting surface contact"
    ],
    answer: "The space between two adjacent teeth",
    explanation: "Interproximal = the contact/space between neighboring teeth. Getting interproximal contacts RIGHT is critical in crown work — too tight and the crown won't seat; too loose and food packs in!",
    emoji: "↔️"
  },
];

const categoryColors = {
  "Tooth Types": { bg: "#FFF3E0", accent: "#E65100", light: "#FFE0B2" },
  "Dental Arches": { bg: "#E8F5E9", accent: "#2E7D32", light: "#C8E6C9" },
  "Tooth Numbering": { bg: "#E3F2FD", accent: "#1565C0", light: "#BBDEFB" },
  "Tooth Surfaces": { bg: "#F3E5F5", accent: "#6A1B9A", light: "#E1BEE7" },
  "Tooth Anatomy": { bg: "#FCE4EC", accent: "#880E4F", light: "#F8BBD0" },
  "Directional Terms": { bg: "#E0F7FA", accent: "#006064", light: "#B2EBF2" },
};

export default function DentalQuiz() {
  const [shuffled, setShuffled] = useState([]);
  const [current, setCurrent] = useState(0);
  const [selected, setSelected] = useState(null);
  const [score, setScore] = useState(0);
  const [streak, setStreak] = useState(0);
  const [bestStreak, setBestStreak] = useState(0);
  const [showExplanation, setShowExplanation] = useState(false);
  const [done, setDone] = useState(false);
  const [categoryScores, setCategoryScores] = useState({});
  const [animate, setAnimate] = useState(false);
  const [wrongShake, setWrongShake] = useState(false);

  useEffect(() => {
    const s = [...questions].sort(() => Math.random() - 0.5);
    setShuffled(s);
    const init = {};
    questions.forEach(q => { init[q.category] = { correct: 0, total: 0 }; });
    setCategoryScores(init);
  }, []);

  const q = shuffled[current];
  const colors = q ? categoryColors[q.category] : {};
  const totalQ = shuffled.length;

  function handleAnswer(opt) {
    if (selected) return;
    setSelected(opt);
    const correct = opt === q.answer;
    if (correct) {
      setScore(s => s + 1);
      const newStreak = streak + 1;
      setStreak(newStreak);
      if (newStreak > bestStreak) setBestStreak(newStreak);
      setAnimate(true);
      setTimeout(() => setAnimate(false), 600);
    } else {
      setStreak(0);
      setWrongShake(true);
      setTimeout(() => setWrongShake(false), 500);
    }
    setCategoryScores(prev => ({
      ...prev,
      [q.category]: {
        correct: prev[q.category].correct + (correct ? 1 : 0),
        total: prev[q.category].total + 1
      }
    }));
    setShowExplanation(true);
  }

  function next() {
    if (current + 1 >= totalQ) {
      setDone(true);
    } else {
      setCurrent(c => c + 1);
      setSelected(null);
      setShowExplanation(false);
    }
  }

  function restart() {
    setShuffled([...questions].sort(() => Math.random() - 0.5));
    setCurrent(0);
    setSelected(null);
    setScore(0);
    setStreak(0);
    setShowExplanation(false);
    setDone(false);
    const init = {};
    questions.forEach(q => { init[q.category] = { correct: 0, total: 0 }; });
    setCategoryScores(init);
  }

  const pct = totalQ > 0 ? Math.round((score / totalQ) * 100) : 0;

  const styles = {
    app: {
      minHeight: "100vh",
      background: "#F7F3EE",
      fontFamily: "'Georgia', 'Times New Roman', serif",
      padding: "0",
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
    },
    header: {
      width: "100%",
      background: "#1A1A2E",
      color: "#F7F3EE",
      padding: "20px 24px",
      display: "flex",
      alignItems: "center",
      justifyContent: "space-between",
      boxSizing: "border-box",
    },
    headerTitle: {
      fontSize: "18px",
      fontWeight: "700",
      letterSpacing: "2px",
      textTransform: "uppercase",
      margin: 0,
    },
    headerSub: {
      fontSize: "11px",
      letterSpacing: "3px",
      opacity: 0.6,
      textTransform: "uppercase",
      margin: "2px 0 0 0",
    },
    statsRow: {
      display: "flex",
      gap: "16px",
      alignItems: "center",
    },
    statPill: {
      background: "rgba(255,255,255,0.1)",
      borderRadius: "20px",
      padding: "4px 12px",
      fontSize: "12px",
      letterSpacing: "1px",
    },
    container: {
      width: "100%",
      maxWidth: "640px",
      padding: "24px 20px",
      boxSizing: "border-box",
    },
    progressBar: {
      width: "100%",
      height: "6px",
      background: "#E0D8D0",
      borderRadius: "3px",
      marginBottom: "24px",
      overflow: "hidden",
    },
    progressFill: {
      height: "100%",
      background: "#1A1A2E",
      borderRadius: "3px",
      transition: "width 0.4s ease",
      width: `${totalQ > 0 ? ((current) / totalQ) * 100 : 0}%`,
    },
    card: {
      background: colors.bg || "#fff",
      borderRadius: "16px",
      padding: "28px 28px 24px",
      boxShadow: "0 4px 24px rgba(0,0,0,0.08)",
      border: `2px solid ${colors.light || "#eee"}`,
      transition: "all 0.3s ease",
    },
    categoryTag: {
      display: "inline-block",
      background: colors.accent || "#333",
      color: "#fff",
      fontSize: "10px",
      letterSpacing: "2px",
      textTransform: "uppercase",
      padding: "4px 10px",
      borderRadius: "20px",
      marginBottom: "16px",
      fontFamily: "system-ui, sans-serif",
    },
    emoji: {
      fontSize: "36px",
      marginBottom: "12px",
      display: "block",
    },
    question: {
      fontSize: "20px",
      fontWeight: "700",
      color: "#1A1A2E",
      lineHeight: "1.4",
      marginBottom: "24px",
    },
    options: {
      display: "flex",
      flexDirection: "column",
      gap: "10px",
    },
    option: (opt) => {
      let bg = "#fff";
      let border = "2px solid #E0D8D0";
      let color = "#1A1A2E";
      let transform = "none";
      if (selected) {
        if (opt === q.answer) {
          bg = "#E8F5E9";
          border = "2px solid #2E7D32";
          color = "#1B5E20";
        } else if (opt === selected && opt !== q.answer) {
          bg = "#FFEBEE";
          border = "2px solid #C62828";
          color = "#B71C1C";
        } else {
          bg = "#F5F5F5";
          border = "2px solid #E0D8D0";
          color = "#999";
        }
      }
      return {
        background: bg,
        border,
        color,
        borderRadius: "10px",
        padding: "14px 18px",
        fontSize: "15px",
        cursor: selected ? "default" : "pointer",
        textAlign: "left",
        fontFamily: "system-ui, -apple-system, sans-serif",
        fontWeight: opt === q.answer && selected ? "700" : "400",
        transition: "all 0.2s ease",
        transform,
      };
    },
    explanation: {
      marginTop: "20px",
      background: "#fff",
      border: `2px solid ${colors.light || "#eee"}`,
      borderRadius: "10px",
      padding: "16px",
      fontSize: "14px",
      lineHeight: "1.6",
      color: "#333",
      fontFamily: "system-ui, sans-serif",
    },
    explanationLabel: {
      fontWeight: "700",
      color: colors.accent || "#333",
      display: "block",
      marginBottom: "6px",
      fontSize: "11px",
      letterSpacing: "2px",
      textTransform: "uppercase",
    },
    nextBtn: {
      marginTop: "20px",
      width: "100%",
      background: "#1A1A2E",
      color: "#F7F3EE",
      border: "none",
      borderRadius: "10px",
      padding: "16px",
      fontSize: "15px",
      fontWeight: "700",
      letterSpacing: "1px",
      cursor: "pointer",
      fontFamily: "system-ui, sans-serif",
      textTransform: "uppercase",
    },
    streakBanner: {
      background: "#1A1A2E",
      color: "#FFD700",
      textAlign: "center",
      padding: "8px",
      borderRadius: "8px",
      marginBottom: "16px",
      fontSize: "13px",
      letterSpacing: "2px",
      fontFamily: "system-ui, sans-serif",
      animation: animate ? "pulse 0.3s ease" : "none",
    },
    // Done screen
    doneCard: {
      background: "#1A1A2E",
      borderRadius: "16px",
      padding: "40px 32px",
      textAlign: "center",
      color: "#F7F3EE",
    },
    bigScore: {
      fontSize: "72px",
      fontWeight: "900",
      lineHeight: "1",
      marginBottom: "8px",
    },
    doneSubtitle: {
      fontSize: "14px",
      letterSpacing: "3px",
      textTransform: "uppercase",
      opacity: 0.7,
      marginBottom: "32px",
      fontFamily: "system-ui, sans-serif",
    },
    catGrid: {
      display: "grid",
      gridTemplateColumns: "1fr 1fr",
      gap: "10px",
      marginBottom: "28px",
      textAlign: "left",
    },
    catItem: (cat) => ({
      background: categoryColors[cat].bg,
      border: `2px solid ${categoryColors[cat].light}`,
      borderRadius: "10px",
      padding: "12px",
    }),
    catName: (cat) => ({
      fontSize: "10px",
      letterSpacing: "1px",
      textTransform: "uppercase",
      color: categoryColors[cat].accent,
      fontFamily: "system-ui, sans-serif",
      fontWeight: "700",
      marginBottom: "4px",
    }),
    catFrac: {
      fontSize: "20px",
      fontWeight: "700",
      color: "#1A1A2E",
    },
    restartBtn: {
      background: "#F7F3EE",
      color: "#1A1A2E",
      border: "none",
      borderRadius: "10px",
      padding: "16px 32px",
      fontSize: "14px",
      fontWeight: "700",
      letterSpacing: "2px",
      cursor: "pointer",
      textTransform: "uppercase",
      fontFamily: "system-ui, sans-serif",
    },
  };

  if (!q && !done) return (
    <div style={styles.app}>
      <div style={{ padding: 40, color: "#999" }}>Loading quiz...</div>
    </div>
  );

  if (done) {
    const grade = pct >= 90 ? "🏆 Expert!" : pct >= 70 ? "⭐ Great Work!" : pct >= 50 ? "📚 Keep Studying!" : "🦷 Back to the Books!";
    return (
      <div style={styles.app}>
        <style>{`@import url('https://fonts.googleapis.com/css2?family=DM+Serif+Display&display=swap');`}</style>
        <div style={{ ...styles.header }}>
          <div>
            <p style={styles.headerTitle}>🦷 Dental Module 1</p>
            <p style={styles.headerSub}>Quiz Complete</p>
          </div>
        </div>
        <div style={styles.container}>
          <div style={styles.doneCard}>
            <div style={styles.bigScore}>{pct}%</div>
            <div style={styles.doneSubtitle}>{grade}</div>
            <div style={{ fontSize: "16px", marginBottom: "8px", opacity: 0.8, fontFamily: "system-ui, sans-serif" }}>
              {score} / {totalQ} correct · Best streak: {bestStreak} 🔥
            </div>
            <div style={{ fontSize: "13px", opacity: 0.6, marginBottom: "28px", fontFamily: "system-ui, sans-serif" }}>
              {pct >= 90 ? "You've mastered Module 1! Ready for Module 2?" :
               pct >= 70 ? "Solid foundation! Review the missed ones and try again." :
               "Keep at it — re-read the tricky sections and retry!"}
            </div>
            <div style={styles.catGrid}>
              {Object.entries(categoryScores).filter(([, v]) => v.total > 0).map(([cat, v]) => (
                <div key={cat} style={styles.catItem(cat)}>
                  <div style={styles.catName(cat)}>{cat}</div>
                  <div style={styles.catFrac}>{v.correct}/{v.total}</div>
                </div>
              ))}
            </div>
            <button style={styles.restartBtn} onClick={restart}>Try Again</button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div style={styles.app}>
      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=DM+Serif+Display&display=swap');
        button:hover { opacity: 0.88; transform: translateY(-1px); }
        @keyframes pulse { 0%,100%{transform:scale(1)} 50%{transform:scale(1.03)} }
        @keyframes shake { 0%,100%{transform:translateX(0)} 20%,60%{transform:translateX(-6px)} 40%,80%{transform:translateX(6px)} }
      `}</style>
      <div style={styles.header}>
        <div>
          <p style={styles.headerTitle}>🦷 Dental Module 1</p>
          <p style={styles.headerSub}>Anatomy & Terminology</p>
        </div>
        <div style={styles.statsRow}>
          <span style={styles.statPill}>✅ {score}</span>
          <span style={styles.statPill}>{current + 1}/{totalQ}</span>
          {streak >= 2 && <span style={{ ...styles.statPill, background: "#FFD700", color: "#1A1A2E" }}>🔥 {streak}</span>}
        </div>
      </div>

      <div style={styles.container}>
        <div style={styles.progressBar}>
          <div style={styles.progressFill} />
        </div>

        {streak >= 3 && (
          <div style={styles.streakBanner}>
            🔥 {streak} IN A ROW — YOU'RE ON FIRE!
          </div>
        )}

        <div style={{
          ...styles.card,
          animation: wrongShake ? "shake 0.4s ease" : "none",
        }}>
          <span style={styles.categoryTag}>{q.category}</span>
          <span style={styles.emoji}>{q.emoji}</span>
          <div style={styles.question}>{q.question}</div>

          <div style={styles.options}>
            {q.options.map(opt => (
              <button
                key={opt}
                style={styles.option(opt)}
                onClick={() => handleAnswer(opt)}
              >
                {selected && opt === q.answer && "✓ "}
                {selected && opt === selected && opt !== q.answer && "✗ "}
                {opt}
              </button>
            ))}
          </div>

          {showExplanation && (
            <div style={styles.explanation}>
              <span style={styles.explanationLabel}>
                {selected === q.answer ? "✅ Correct!" : "❌ Not quite —"}
              </span>
              {q.explanation}
            </div>
          )}

          {showExplanation && (
            <button style={styles.nextBtn} onClick={next}>
              {current + 1 >= totalQ ? "See Results →" : "Next Question →"}
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
