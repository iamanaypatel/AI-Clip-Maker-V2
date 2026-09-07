package com.example.aiclipmaker.intelligence.caption

enum class LanguageScript {
    DEVANAGARI,
    LATIN,
    BENGALI,
    TAMIL,
    TELUGU,
    GUJARATI,
    KANNADA,
    MALAYALAM,
    GURMUKHI,
    ODIA,
    ARABIC,
    CYRILLIC,
    CJK
}

data class LanguageInfo(
    val code: String,
    val englishName: String,
    val nativeName: String,
    val script: LanguageScript,
    val isIndian: Boolean = true,
    val samplePhrases: List<String>
)

object CaptionLanguageRegistry {

    val AUTO_DETECT = LanguageInfo(
        code = "auto",
        englishName = "Auto Detect",
        nativeName = "Auto Detect",
        script = LanguageScript.LATIN,
        isIndian = false,
        samplePhrases = emptyList()
    )

    val SUPPORTED_LANGUAGES: List<LanguageInfo> = listOf(
        // Top Priority Languages (Hindi, English, Hinglish)
        LanguageInfo(
            code = "hi",
            englishName = "Hindi",
            nativeName = "हिन्दी",
            script = LanguageScript.DEVANAGARI,
            isIndian = true,
            samplePhrases = listOf(
                "आज हम वीडियो एडिटिंग के बारे में बात करेंगे।",
                "नमस्ते दोस्तों, आज हम वीडियो एडिटिंग सीखेंगे।",
                "यह सबसे महत्वपूर्ण बात है जो आपको समझनी चाहिए।",
                "जब आप इस तकनीक को समझ जाते हैं तो सब आसान हो जाता है।",
                "ध्यान से देखिए कि इस पल में क्या हो रहा है।",
                "सफलता के लिए नियमित प्रयास सबसे ज्यादा जरूरी है।",
                "आइए अब इसके मुख्य सिद्धांतों को गहराई से समझें।",
                "यह तरीका आपके काम को बहुत तेज और प्रभावी बना देगा。"
            )
        ),
        LanguageInfo(
            code = "en",
            englishName = "English",
            nativeName = "English",
            script = LanguageScript.LATIN,
            isIndian = false,
            samplePhrases = listOf(
                "Today we are learning AI video editing.",
                "Today we are talking about AI video editing.",
                "Here is the single biggest lesson from this conversation.",
                "Most people approach this concept completely backwards.",
                "When you understand how this works everything changes.",
                "Notice what happens right at this exact moment.",
                "Consistency beats intensity every single time.",
                "The key is focusing on the fundamentals first.",
                "This is what separates top creators from everyone else."
            )
        ),
        LanguageInfo(
            code = "hi-mixed",
            englishName = "Hinglish (Mixed)",
            nativeName = "हिन्दी + English",
            script = LanguageScript.DEVANAGARI,
            isIndian = true,
            samplePhrases = listOf(
                "आज हम AI video editing की best settings देखेंगे।",
                "आज हम AI video editing ki kuch important tips dekhenge.",
                "आज हम productivity की बात करेंगे और आपको important tips बताएंगे।",
                "इस video editing workflow से आपका production time आधा हो जाएगा।",
                "अगर आप consistently content create करना चाहते हैं तो यह strategy best है।",
                "यहाँ पर key point यह है कि storytelling सबसे ज्यादा matter करती है।",
                "Audience retention बढ़ाने के लिए strong hook देना बहुत जरूरी है।"
            )
        ),
        LanguageInfo(
            code = "hi-Latn",
            englishName = "Hinglish",
            nativeName = "Roman Hindi",
            script = LanguageScript.LATIN,
            isIndian = true,
            samplePhrases = listOf(
                "Aaj hum video editing ke baare mein baat karenge.",
                "Yeh sabse important baat hai jo aapko samajhni chahiye.",
                "Jab aap is technique ko samajh jaate hain sab aasan ho jaata hai.",
                "Notice karo ki is exact moment par kya ho raha hai.",
                "Consistency hamesha intensity se jeet jaati hai.",
                "Key yeh hai ki fundamentals par pehle focus karo.",
                "Is process se aapka workflow bahut fast ho jaayega."
            )
        ),
        LanguageInfo(
            code = "bn",
            englishName = "Bengali",
            nativeName = "বাংলা",
            script = LanguageScript.BENGALI,
            isIndian = true,
            samplePhrases = listOf(
                "আজকে আমরা ভিডিও এডিটিং নিয়ে বিস্তারিত আলোচনা করব।",
                "এই বিষয়টি সঠিকভাবে বোঝা খুবই গুরুত্বপূর্ণ।",
                "ধারাবাহিক কাজ যেকোনো কঠিন কাজকে সহজ করে দেয়।"
            )
        ),
        LanguageInfo(
            code = "mr",
            englishName = "Marathi",
            nativeName = "मराठी",
            script = LanguageScript.DEVANAGARI,
            isIndian = true,
            samplePhrases = listOf(
                "आज आपण व्हिडिओ एडिटिंगबद्दल सविस्तर चर्चा करणार आहोत.",
                "ही सर्वात महत्त्वाची गोष्ट आहे जी प्रत्येकाने समजून घेतली पाहिजे.",
                "सातत्य हे कोणत्याही यशाचे मुख्य रहस्य असते."
            )
        ),
        LanguageInfo(
            code = "te",
            englishName = "Telugu",
            nativeName = "తెలుగు",
            script = LanguageScript.TELUGU,
            isIndian = true,
            samplePhrases = listOf(
                "ఈ రోజు మనం వీడియో ఎడిటింగ్ గురించి వివరంగా తెలుసుకుందాం.",
                "ఇది మీరు తెలుసుకోవలసిన అత్యంత ముఖ్యమైన విషయం.",
                "నిరంతర సాధనతోనే అద్భుతమైన ఫలితాలు సాధ్యమవుతాయి."
            )
        ),
        LanguageInfo(
            code = "ta",
            englishName = "Tamil",
            nativeName = "தமிழ்",
            script = LanguageScript.TAMIL,
            isIndian = true,
            samplePhrases = listOf(
                "இன்று நாம் வீடியோ எடிட்டிங் பற்றி விரிவாக பேச போகிறோம்.",
                "இது நீங்கள் தெரிந்து கொள்ள வேண்டிய மிக முக்கியமான விஷயம்.",
                "விடாமுயற்சியே சிறந்த வெற்றியை தரும்."
            )
        ),
        LanguageInfo(
            code = "gu",
            englishName = "Gujarati",
            nativeName = "ગુજરાતી",
            script = LanguageScript.GUJARATI,
            isIndian = true,
            samplePhrases = listOf(
                "આજે આપણે વિડિયો એડિટિંગ વિશે વિગતવાર વાત કરીશું.",
                "આ સૌથી મહત્વપૂર્ણ વાત છે જે તમારે સમજવી જોઈએ.",
                "સતત મહેનત દરેક મુશ્કેલ કામને સરળ બનાવે છે."
            )
        ),
        LanguageInfo(
            code = "kn",
            englishName = "Kannada",
            nativeName = "ಕನ್ನಡ",
            script = LanguageScript.KANNADA,
            isIndian = true,
            samplePhrases = listOf(
                "ಇಂದು ನಾವು ವೀಡಿಯೊ ಎಡಿಟಿಂಗ್ ಬಗ್ಗೆ ವಿವರವಾಗಿ ಚರ್ಚಿಸೋಣ.",
                "ಇದು ನೀವು ತಿಳಿದುಕೊಳ್ಳಬೇಕಾದ ಪ್ರಮುಖ ವಿಷಯವಾಗಿದೆ.",
                "ನಿರಂತರ ಪ್ರಯತ್ನವೇ ಯಶಸ್ಸಿನ ಗುಟ್ಟು."
            )
        ),
        LanguageInfo(
            code = "ml",
            englishName = "Malayalam",
            nativeName = "മലയാളം",
            script = LanguageScript.MALAYALAM,
            isIndian = true,
            samplePhrases = listOf(
                "ഇന്ന് നമ്മൾ വീഡിയോ എഡിറ്റിംഗിനെ കുറിച്ച് സംസാരിക്കും.",
                "ഇത് നിങ്ങൾ അറിഞ്ഞിരിക്കേണ്ട വളരെ പ്രധാനപ്പെട്ട കാര്യമാണ്.",
                "തുടർച്ചയായ പരിശ്രമമാണ് വിജയത്തിന്റെ അടിസ്ഥാനം."
            )
        ),
        LanguageInfo(
            code = "pa",
            englishName = "Punjabi",
            nativeName = "ਪੰਜਾਬੀ",
            script = LanguageScript.GURMUKHI,
            isIndian = true,
            samplePhrases = listOf(
                "ਅੱਜ ਅਸੀਂ ਵੀਡੀਓ ਐਡੀਟਿੰਗ ਬਾਰੇ ਗੱਲਬਾਤ ਕਰਾਂਗੇ।",
                "ਇਹ ਸਭ ਤੋਂ ਜ਼ਰੂਰੀ ਨੁਕਤਾ ਹੈ ਜੋ ਤੁਹਾਨੂੰ ਸਮਝਣਾ ਚਾਹੀਦਾ ਹੈ।",
                "ਲਗਾਤਾਰ ਮਿਹਨਤ ਹੀ ਹਮੇਸ਼ਾ ਕਾਮਯਾਬੀ ਦਿੰਦੀ ਹੈ।"
            )
        ),
        LanguageInfo(
            code = "or",
            englishName = "Odia",
            nativeName = "ଓଡ଼ିଆ",
            script = LanguageScript.ODIA,
            isIndian = true,
            samplePhrases = listOf(
                "ଆଜି ଆମେ ଭିଡିଓ ଏଡିଟିଂ ବିଷୟରେ ଆଲୋଚନା କରିବା।",
                "ଏହା ସବୁଠାରୁ ଗୁରୁତ୍ୱପୂର୍ଣ୍ଣ କଥା ଯାହା ଜାଣିବା ଉଚିତ।"
            )
        ),
        LanguageInfo(
            code = "as",
            englishName = "Assamese",
            nativeName = "অসমীয়া",
            script = LanguageScript.BENGALI,
            isIndian = true,
            samplePhrases = listOf(
                "আজি আমি ভিডিঅ' এডিটিং সম্পৰ্কে আলোচনা কৰিম।",
                "এইটো এটা অত্যন্ত গুৰুত্বপূৰ্ণ বিষয়।"
            )
        ),
        LanguageInfo(
            code = "ur",
            englishName = "Urdu",
            nativeName = "اردو",
            script = LanguageScript.ARABIC,
            isIndian = true,
            samplePhrases = listOf(
                "آج ہم ویڈیو ایڈیٹنگ کے بارے میں بات کریں گے۔",
                "یہ سب سے اہم نکتہ ہے جسے سمجھنا ضروری ہے۔",
                "مسلسل محنت ہی کامیابی کی ضمانت ہے۔"
            )
        ),

        // International Languages
        LanguageInfo(
            code = "es",
            englishName = "Spanish",
            nativeName = "Español",
            script = LanguageScript.LATIN,
            isIndian = false,
            samplePhrases = listOf(
                "Hoy vamos a hablar sobre edición de video profesional.",
                "Esta es la lección más importante de toda la conversación.",
                "La constancia siempre supera a la intensidad en todo."
            )
        ),
        LanguageInfo(
            code = "fr",
            englishName = "French",
            nativeName = "Français",
            script = LanguageScript.LATIN,
            isIndian = false,
            samplePhrases = listOf(
                "Aujourd'hui nous allons parler de montage vidéo.",
                "C'est la leçon la plus importante de cette discussion.",
                "La régularité bat l'intensité à chaque fois."
            )
        ),
        LanguageInfo(
            code = "de",
            englishName = "German",
            nativeName = "Deutsch",
            script = LanguageScript.LATIN,
            isIndian = false,
            samplePhrases = listOf(
                "Heute sprechen wir über professionelle Videobearbeitung.",
                "Das ist die wichtigste Lektion aus diesem Gespräch.",
                "Beständigkeit schlägt Intensität jedes einzelne Mal."
            )
        ),
        LanguageInfo(
            code = "pt",
            englishName = "Portuguese",
            nativeName = "Português",
            script = LanguageScript.LATIN,
            isIndian = false,
            samplePhrases = listOf(
                "Hoje vamos falar sobre edição de vídeo moderna.",
                "Esta é a lição mais importante de toda a conversa.",
                "A consistência sempre vence a intensidade."
            )
        ),
        LanguageInfo(
            code = "it",
            englishName = "Italian",
            nativeName = "Italiano",
            script = LanguageScript.LATIN,
            isIndian = false,
            samplePhrases = listOf(
                "Oggi parleremo di montaggio video moderno.",
                "Questa è la lezione più importante della conversazione."
            )
        ),
        LanguageInfo(
            code = "ru",
            englishName = "Russian",
            nativeName = "Русский",
            script = LanguageScript.CYRILLIC,
            isIndian = false,
            samplePhrases = listOf(
                "Сегодня мы поговорим о монтаже видео.",
                "Это самый важный урок из всего разговора."
            )
        ),
        LanguageInfo(
            code = "ar",
            englishName = "Arabic",
            nativeName = "العربية",
            script = LanguageScript.ARABIC,
            isIndian = false,
            samplePhrases = listOf(
                "اليوم سنتحدث عن تحرير الفيديو باحترافية.",
                "هذا هو الدرس الأهم في هذه المحادثة بالكامل."
            )
        ),
        LanguageInfo(
            code = "ja",
            englishName = "Japanese",
            nativeName = "日本語",
            script = LanguageScript.CJK,
            isIndian = false,
            samplePhrases = listOf(
                "今日は動画編集の基本についてお話しします。",
                "これが最も重要なポイントです。"
            )
        ),
        LanguageInfo(
            code = "ko",
            englishName = "Korean",
            nativeName = "한국어",
            script = LanguageScript.CJK,
            isIndian = false,
            samplePhrases = listOf(
                "오늘은 영상 편집의 핵심 기술에 대해 이야기하겠습니다.",
                "이것이 가장 중요한 핵심 교훈입니다."
            )
        ),
        LanguageInfo(
            code = "zh",
            englishName = "Chinese",
            nativeName = "中文",
            script = LanguageScript.CJK,
            isIndian = false,
            samplePhrases = listOf(
                "今天我们将讨论高质量的视频剪辑技巧。",
                "这是这次交流中最重要的核心要点。"
            )
        )
    )

    fun findByCode(code: String): LanguageInfo {
        if (code == "auto") return AUTO_DETECT
        return SUPPORTED_LANGUAGES.find { it.code.equals(code, ignoreCase = true) }
            ?: SUPPORTED_LANGUAGES.find { it.code.startsWith(code, ignoreCase = true) }
            ?: SUPPORTED_LANGUAGES[0] // Default to Hindi
    }
}
