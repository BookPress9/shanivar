const links = [...document.querySelectorAll('.screen-link')];
const screens = [...document.querySelectorAll('.screen')];
const screenIds = screens.map((screen) => screen.dataset.screen);
const dots = document.getElementById('flowDots');
const languageToggle = document.getElementById('languageToggle');
const originalText = new WeakMap();
const query = new URLSearchParams(location.search);
let currentIndex = 0;
let currentLanguage = query.get('lang') === 'hi' ? 'hi' : 'en';

const hindi = new Map(Object.entries({
  "SHANIVAR / शनिवार": "शनिवार / SHANIVAR",
  "Choose the voice": "वह भाषा चुनें",
  "that feels natural.": "जो सहज लगे।",
  "You can change this later. The practice remains the same.": "आप इसे बाद में बदल सकते हैं। साधना वही रहेगी।",
  "honours the spirit of time, responsibility, and patient discipline associated with Shani. Faith is welcomed; belief is never required.": "शनि से जुड़े समय, कर्म और धैर्यपूर्ण अनुशासन के भाव का सम्मान करता है। आस्था आपकी है; चुनाव भी आपका।",
  "SETUP · 1 OF 3": "आरंभ · 1 / 3",
  "ACCOUNT": "खाता",
  "Your place": "शनिवार में",
  "in Shanivar.": "आपका स्थान।",
  "An account keeps your rhythm, reflections, and practice history with you.": "खाता आपकी दिनचर्या, चिंतन और साधना का इतिहास सुरक्षित रखता है।",
  "Create my account": "अपना खाता बनाएँ",
  "I already have an account": "मेरा खाता पहले से है",
  "OR": "या",
  "Explore first": "पहले देखकर समझें",
  "You can begin without an account. Cloud history and personalized daily notes require sign-in.": "आप बिना खाते के शुरू कर सकते हैं। ऑनलाइन इतिहास और व्यक्तिगत दैनिक संदेश के लिए साइन इन करें।",
  "SETUP · 2 OF 3": "आरंभ · 2 / 3",
  "OPTIONAL": "वैकल्पिक",
  "A MORE PERSONAL MIRROR": "आपके लिए अधिक प्रासंगिक दर्पण",
  "Let the guidance": "मार्गदर्शन को अपना",
  "know your context.": "संदर्भ जानने दें।",
  "Birth details help Shanivar shape chart-based reflections around your time and place. They do not define you.": "जन्म विवरण से शनिवार समय और स्थान के आधार पर अधिक प्रासंगिक चिंतन दे सकता है। ये विवरण आपको परिभाषित नहीं करते।",
  "Date of birth": "जन्म तिथि",
  "14 August 1991": "14 अगस्त 1991",
  "Time of birth": "जन्म समय",
  "Choose “I don’t know” if uncertain.": "निश्चित न हों तो ‘मुझे नहीं पता’ चुनें।",
  "Birthplace": "जन्म स्थान",
  "New Delhi, India": "नई दिल्ली, भारत",
  "WHY WE ASK": "हम क्यों पूछते हैं",
  "Used for your chart context and stored with your account. Never used to target ads.": "आपके चार्ट संदर्भ के लिए, आपके खाते में सुरक्षित। विज्ञापन लक्ष्य बनाने के लिए कभी नहीं।",
  "Continue with these details": "इन विवरणों के साथ आगे बढ़ें",
  "Skip for now": "अभी छोड़ें",
  "SETUP · 3 OF 3": "आरंभ · 3 / 3",
  "CONTROL": "नियंत्रण",
  "Strong structure.": "मज़बूत संरचना।",
  "Your choice.": "चुनाव आपका।",
  "Shanivar can hold a boundary when intention alone is tired.": "जब केवल संकल्प थक जाए, शनिवार आपकी चुनी सीमा को थाम सकता है।",
  "Protect a focus window": "ध्यान की अवधि सुरक्षित करें",
  "Block only the apps and times you choose.": "सिर्फ़ वही ऐप और समय रोकें जिन्हें आप चुनें।",
  "Wake you into movement": "उठते ही चलने में साथ",
  "Use steps or an optional private scene check.": "कदम गिनें या निजी दृश्य-जाँच चुनें।",
  "Offer daily direction": "हर दिन एक दिशा",
  "One reflection, one action, no endless feed.": "एक चिंतन, एक कर्म—अंतहीन फ़ीड नहीं।",
  "LATER": "बाद में",
  "READY": "तैयार",
  "In Indian tradition, Shani represents time, consequence, and patient discipline. Shanivar carries that spirit with respect—your faith and interpretation remain yours.": "भारतीय परंपरा में शनि समय, कर्मफल और धैर्यपूर्ण अनुशासन के प्रतीक हैं। शनिवार उस भाव को सम्मान से रखता है—आस्था और अर्थ आपके अपने हैं।",
  "I understand": "मैं समझ गया/गई",
  "No Android permission is requested on this screen.": "इस स्क्रीन पर कोई Android अनुमति नहीं माँगी जाती।",
  "SHANIVAR / THE HYBRID MONK": "शनिवार / द हाइब्रिड मोंक",
  "01 · ATTENTION": "01 · ध्यान",
  "Your attention.": "आपका ध्यान।",
  "Back in your hands.": "फिर आपके हाथों में।",
  "A daily rhythm for less scrolling, a morning that gets you moving, and space to know yourself.": "कम स्क्रोलिंग की दिनचर्या, उठकर चलने वाली सुबह, और स्वयं को जानने की जगह।",
  "Wake with a physical action": "शरीर को चलाकर जागें",
  "Protect a window of attention": "ध्यान की एक अवधि सुरक्षित रखें",
  "Practice, reflect, return": "साधना, चिंतन, वापसी",
  "Enter today": "आज में प्रवेश करें",
  "You choose the structure. Practice is always available.": "संरचना आप चुनते हैं। साधना हमेशा उपलब्ध है।",
  "SATURDAY · 12 SEP": "शनिवार · 12 सितम्बर",
  "Good morning,": "सुप्रभात,",
  "Arjun.": "अर्जुन।",
  "Before the day gathers speed": "दिन की रफ़्तार बढ़ने से पहले",
  "Your morning boundary is available until 08:00": "सुबह की सीमा 08:00 तक उपलब्ध है",
  "YOUR DIRECTION TODAY": "आज की आपकी दिशा",
  "PERSONAL": "व्यक्तिगत",
  "Urgency is not": "जल्दी हर बार",
  "the same as importance.": "ज़रूरी नहीं होती।",
  "Before you answer, notice whose clock you are obeying.": "जवाब देने से पहले देखें—आप किसकी घड़ी के अनुसार चल रहे हैं?",
  "Sit with this": "इसके साथ ठहरें",
  "Why this today? · interpretive, not predictive": "आज यह क्यों? · संकेत, भविष्यवाणी नहीं",
  "Morning focus suggested": "सुबह का ध्यान समय सुझाया गया",
  "30 min · Instagram · adjustable": "30 मिनट · Instagram · बदला जा सकता है",
  "Review": "देखें",
  "4-MINUTE PRACTICE": "4 मिनट की साधना",
  "The two pauses of breath · VBT verse 24": "श्वास के दो विराम · VBT श्लोक 24",
  "Today": "आज",
  "Focus": "ध्यान",
  "Rhythm": "दिनचर्या",
  "Practice": "साधना",
  "FOCUS WINDOW": "ध्यान अवधि",
  "1 OF 2": "1 / 2",
  "Choose the": "अपनी सीमा",
  "boundary.": "चुनें।",
  "A focus window is a commitment you can inspect before it closes.": "ध्यान अवधि एक स्पष्ट संकल्प है—शुरू होने से पहले सब देख लें।",
  "Morning": "सुबह",
  "30 min after waking": "जागने के बाद 30 मिनट",
  "Night": "रात",
  "45 min before sleep": "सोने से 45 मिनट पहले",
  "Custom": "अपना समय",
  "Choose a transition": "कोई सहज बदलाव चुनें",
  "MINUTES": "मिनट",
  "Ends at 07:04": "07:04 पर समाप्त",
  "BLOCKED APPS": "रोकने वाले ऐप",
  "Edit": "बदलें",
  "Unavailable until 07:04": "07:04 तक उपलब्ध नहीं",
  "Android access needed": "Android अनुमति चाहिए",
  "Usage access · display over apps": "उपयोग अनुमति · ऐप के ऊपर दिखना",
  "Review & start": "देखकर शुरू करें",
  "Night suggestions follow a 30–60 minute screen-free range. Morning timing adapts from your choices.": "रात का सुझाव सोने से 30–60 मिनट पहले स्क्रीन से दूरी पर आधारित है। सुबह का समय आपकी पसंद से बदलेगा।",
  "PROTECTION CONFIRMED": "सुरक्षा की पुष्टि",
  "ANDROID": "ANDROID",
  "FOCUS HELD": "ध्यान सुरक्षित",
  "ENDS 07:04": "07:04 पर समाप्त",
  "The boundary": "सीमा बनी",
  "is holding.": "हुई है।",
  "Instagram remains unavailable. Calls, messages, and the rest of your phone are open.": "Instagram अभी बंद है। कॉल, संदेश और फ़ोन की बाकी सुविधाएँ खुली हैं।",
  "Blocked · native status confirmed": "रुका हुआ · फ़ोन से पुष्टि",
  "PRACTICE": "साधना",
  "Enter four quiet minutes": "चार शांत मिनटों में जाएँ",
  "MESSAGES": "संदेश",
  "Reflect with Shanivar": "शनिवार के साथ चिंतन",
  "End focus early": "ध्यान अवधि पहले समाप्त करें",
  "YOUR RHYTHM": "आपकी दिनचर्या",
  "Built around": "आपके दिन के",
  "your day.": "अनुसार।",
  "Change the structure when life changes. Night schedules are welcome here.": "जीवन बदले तो दिनचर्या बदलें। रात की पाली के समय भी यहाँ स्वीकार हैं।",
  "WAKE": "जागना",
  "Alarm confirmed · 18 steps daily": "अलार्म की पुष्टि · रोज़ 18 कदम",
  "30 minutes": "30 मिनट",
  "Instagram · ready to start": "Instagram · शुरू करने को तैयार",
  "DAILY NOTE": "दैनिक संदेश",
  "One clear direction": "एक स्पष्ट दिशा",
  "App notification · chosen time": "ऐप सूचना · आपका चुना समय",
  "QUIET": "शांति",
  "Nothing asks for you": "आपसे कुछ नहीं माँगा जाएगा",
  "Until 06:30 tomorrow": "कल 06:30 तक",
  "Edit rhythm": "दिनचर्या बदलें",
  "✓ Saved on this device · 06:27": "✓ इस फ़ोन पर सुरक्षित · 06:27",
  "× EXIT": "× बाहर",
  "PASSAGE 02 · THE PAUSE": "अंश 02 · विराम",
  "At the end of the inhale, notice the brief pause before the breath turns.": "श्वास भीतर आने के अंत में उस छोटे विराम को देखें, जहाँ से श्वास लौटती है।",
  "Do not hold it. Let the pause reveal itself.": "इसे रोकें नहीं। विराम को स्वयं प्रकट होने दें।",
  "Source · VBT verse 24": "स्रोत · VBT श्लोक 24",
  "Pause practice": "साधना रोकें",
  "MESSAGES FROM SHANIVAR": "शनिवार के संदेश",
  "General reflection": "सामान्य चिंतन",
  "Focus protected · chart context unavailable": "ध्यान सुरक्षित · चार्ट संदर्भ उपलब्ध नहीं",
  "SHANIVAR": "शनिवार",
  "The first reach of the morning is rarely about the object in your hand.": "सुबह पहली बार फ़ोन की ओर बढ़ता हाथ अक्सर फ़ोन के बारे में नहीं होता।",
  "What feeling arrives just before you open the feed?": "फ़ीड खोलने से ठीक पहले कौन-सा भाव आता है?",
  "Why this reflection?": "यह चिंतन क्यों?",
  "Why this reflection? ⌄": "यह चिंतन क्यों? ⌄",
  "CHOOSE WHAT FITS": "जो सही लगे चुनें",
  "This fits": "यह सही लगता है",
  "Not today": "आज नहीं",
  "I'm unsure": "निश्चित नहीं",
  "Prefer not to share": "साझा नहीं करना",
  "Write in your own words": "अपने शब्दों में लिखें",
  "Continue after responding": "उत्तर के बाद आगे बढ़ें",
  "Your response is private and optional.": "आपका उत्तर निजी और वैकल्पिक है।",
  "WAKE ALARM": "जगाने का अलार्म",
  "MOVEMENT": "चलना",
  "CHOOSE A MORNING CUE": "सुबह का संकेत चुनें",
  "Where will you": "आप कहाँ तक",
  "walk to?": "चलकर जाएँगे?",
  "Pick somewhere outside arm's reach. Shanivar counts movement after the alarm begins.": "ऐसी जगह चुनें जो हाथ की पहुँच से बाहर हो। अलार्म शुरू होने के बाद शनिवार आपके कदम गिनेगा।",
  "Morning light": "सुबह की रोशनी",
  "Walk 18 steps toward a window or balcony": "खिड़की या बालकनी की ओर 18 कदम",
  "Kitchen": "रसोई",
  "Walk 16 steps · optional scene check": "16 कदम · दृश्य-जाँच वैकल्पिक",
  "Washroom": "वॉशरूम",
  "Walk 12 steps · movement only": "12 कदम · केवल चलना",
  "Choose my own": "अपना संकेत चुनें",
  "Set steps and an accessible alternative": "कदम और सुलभ विकल्प तय करें",
  "PRIVATE BY DEFAULT": "निजता पहले",
  "If you add a scene check, recognition happens on this phone. The image is discarded after verification.": "दृश्य-जाँच चुनने पर पहचान इसी फ़ोन पर होती है। पुष्टि के बाद चित्र मिटा दिया जाता है।",
  "Use this movement cue": "यह चलने का संकेत चुनें",
  "I need a non-walking option": "मुझे बिना चलने वाला विकल्प चाहिए",
  "SHANIVAR ALARM": "शनिवार अलार्म",
  "SAT · 12 SEP": "शनि · 12 सितम्बर",
  "YOUR CHOSEN ACTION": "आपका चुना हुआ कर्म",
  "Stand up.": "उठिए।",
  "Find the light.": "रोशनी की ओर चलिए।",
  "Walk toward your morning window. Keep the phone with you.": "सुबह की खिड़की की ओर चलें। फ़ोन साथ रखें।",
  "/ 18 STEPS": "/ 18 कदम",
  "Movement detected · keep walking": "चलना पहचाना गया · आगे बढ़ते रहें",
  "Continue moving": "चलते रहें",
  "Optional: confirm morning light with camera": "वैकल्पिक: कैमरे से सुबह की रोशनी की पुष्टि",
  "The image is processed on-device and is not saved.": "चित्र इसी फ़ोन पर जाँचा जाता है और सुरक्षित नहीं होता।",
  "PRACTICE COMPLETE": "साधना पूर्ण",
  "Carry less": "हल्के होकर",
  "into the day.": "दिन में लौटें।",
  "You stayed for four minutes. Notice what does not need to follow you from this room.": "आप चार मिनट ठहरे। देखें—इस कमरे से क्या साथ ले जाना आवश्यक नहीं है।",
  "DISCIPLINE": "अनुशासन",
  "Maintained": "बना रहा",
  "Focus held for the full practice": "पूरी साधना में ध्यान सुरक्षित रहा",
  "ONE LINE FOR YOURSELF": "अपने लिए एक पंक्ति",
  "What became quieter?": "क्या शांत हुआ?",
  "Return to my day": "अपने दिन में लौटें",
  "Streak update waiting for account confirmation.": "निरंतरता का अपडेट खाते की पुष्टि की प्रतीक्षा में है।"
}));

const walker = document.createTreeWalker(document.querySelector('.phone-screen'), NodeFilter.SHOW_TEXT);
const translatableNodes = [];
while (walker.nextNode()) {
  const node = walker.currentNode;
  if (node.nodeValue.trim()) {
    originalText.set(node, node.nodeValue);
    translatableNodes.push(node);
  }
}

screenIds.forEach((_, index) => {
  const dot = document.createElement('i');
  dot.setAttribute('aria-hidden', 'true');
  if (index === 0) dot.classList.add('active');
  dots.appendChild(dot);
});

function updateUrl() {
  history.replaceState(null, '', `?screen=${screenIds[currentIndex]}&lang=${currentLanguage}`);
}

function setLanguage(language, update = true) {
  currentLanguage = language === 'hi' ? 'hi' : 'en';
  document.body.classList.toggle('lang-hi', currentLanguage === 'hi');
  document.documentElement.lang = currentLanguage;
  translatableNodes.forEach((node) => {
    const source = originalText.get(node);
    const match = source.match(/^(\s*)([\s\S]*?)(\s*)$/);
    const core = match[2];
    node.nodeValue = `${match[1]}${currentLanguage === 'hi' ? (hindi.get(core) ?? core) : core}${match[3]}`;
  });
  languageToggle.textContent = currentLanguage === 'hi' ? 'English' : 'हिंदी';
  if (update) updateUrl();
}

function selectScreen(id, update = true) {
  const index = screenIds.indexOf(id);
  if (index < 0) return;
  currentIndex = index;
  screens.forEach((screen) => screen.classList.toggle('active', screen.dataset.screen === id));
  links.forEach((link) => link.classList.toggle('active', link.dataset.target === id));
  [...dots.children].forEach((dot, dotIndex) => dot.classList.toggle('active', dotIndex === index));

  const screen = screens[index];
  document.getElementById('annotationIndex').textContent = `SCREEN ${String(index + 1).padStart(2, '0')} / ${String(screens.length).padStart(2, '0')}`;
  document.getElementById('annotationPhase').textContent = screen.dataset.phase;
  document.getElementById('annotationTitle').textContent = screen.dataset.title;
  document.getElementById('annotationNote').textContent = screen.dataset.note;
  document.getElementById('annotationContract').textContent = screen.dataset.contract;
  if (update) updateUrl();
}

links.forEach((link) => link.addEventListener('click', () => selectScreen(link.dataset.target)));
document.querySelectorAll('[data-go]').forEach((button) => button.addEventListener('click', () => selectScreen(button.dataset.go)));
document.querySelectorAll('[data-lang]').forEach((button) => button.addEventListener('click', () => setLanguage(button.dataset.lang)));
document.getElementById('previous').addEventListener('click', () => selectScreen(screenIds[(currentIndex - 1 + screenIds.length) % screenIds.length]));
document.getElementById('next').addEventListener('click', () => selectScreen(screenIds[(currentIndex + 1) % screenIds.length]));
languageToggle.addEventListener('click', () => setLanguage(currentLanguage === 'hi' ? 'en' : 'hi'));

window.addEventListener('keydown', (event) => {
  if (event.key === 'ArrowLeft') selectScreen(screenIds[(currentIndex - 1 + screenIds.length) % screenIds.length]);
  if (event.key === 'ArrowRight') selectScreen(screenIds[(currentIndex + 1) % screenIds.length]);
});

const requested = query.get('screen');
selectScreen(screenIds.includes(requested) ? requested : screenIds[0], false);
setLanguage(currentLanguage, false);
