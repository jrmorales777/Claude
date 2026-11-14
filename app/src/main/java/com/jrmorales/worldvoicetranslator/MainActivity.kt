package com.jrmorales.worldvoicetranslator

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.widget.*
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var sourceSelect: Spinner
    private lateinit var targetSelect: Spinner
    private lateinit var input: EditText
    private lateinit var output: TextView
    private lateinit var message: TextView
    private lateinit var history: TextView
    private var reader: TextToSpeech? = null
    private var recognizer: SpeechRecognizer? = null
    private var translator: Translator? = null
    private var readerReady = false
    private var pendingMicrophone = false
    private val speechRequest = 1001
    private var revision = 0
    private var autoPlay = true
    private val supported = TranslateLanguage.getAllLanguages().sortedBy { display(it) }
    private fun display(code: String) = Locale.forLanguageTag(code).getDisplayLanguage(Locale.ENGLISH)
    private fun from() = supported[sourceSelect.selectedItemPosition]
    private fun to() = supported[targetSelect.selectedItemPosition]
    private fun px(n: Int) = (resources.displayMetrics.density * n).toInt()
    private fun bg(color: Int, radius: Int = 18, line: Int? = null): GradientDrawable = GradientDrawable().apply {
        setColor(color); cornerRadius = px(radius).toFloat()
        if (line != null) setStroke(px(1), line)
    }
    private fun label(text: String, size: Float = 15f): TextView = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(Color.rgb(27, 66, 117))
        setPadding(0, px(13), 0, px(7))
    }
    private fun button(text: String, primary: Boolean = false, action: () -> Unit): Button = Button(this).apply {
        this.text = text; isAllCaps = false; textSize = 15f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(if (primary) Color.WHITE else Color.rgb(25, 78, 156))
        background = bg(if (primary) Color.rgb(35, 111, 225) else Color.rgb(224, 239, 255))
        setPadding(px(12), px(12), px(12), px(12))
        setOnClickListener { action() }
    }

    private val ink = Color.rgb(20, 46, 85)
    private val blue = Color.rgb(39, 102, 225)
    private val pale = Color.rgb(243, 248, 255)
    private val muted = Color.rgb(95, 115, 147)
    private val orange = Color.rgb(255, 158, 72)
    private fun surface(color: Int = Color.WHITE, corner: Int = 24, border: Int = Color.rgb(225, 236, 252)): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = px(corner).toFloat()
            setStroke(px(1), border)
        }
    private fun fadeIn(view: View) {
        view.startAnimation(AlphaAnimation(0.35f, 1f).apply { duration = 380 })
    }
    private fun titleText(text: String, size: Float, color: Int = ink, bold: Boolean = true): TextView =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
            includeFontPadding = true
        }
    private fun clickable(text: String, filled: Boolean = false, action: () -> Unit): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 15f
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(if (filled) Color.WHITE else blue)
            val shape = surface(
                if (filled) blue else Color.rgb(235, 243, 255),
                17, if (filled) blue else Color.rgb(221, 235, 255)
            )
            background = RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), shape, null)
            setPadding(px(15), px(16), px(15), px(16))
            isClickable = true
            setOnClickListener { action() }
        }
    private fun sectionHeading(value: String): TextView =
        titleText(value, 12.5f, muted).apply {
            letterSpacing = .115f
            setPadding(0, px(12), 0, px(10))
        }
    private fun gap(height: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, px(height))
    }
    private fun mainPanel(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = surface()
        elevation = px(3).toFloat()
        setPadding(px(17), px(16), px(17), px(16))
    }
    private fun rowButtons(parent: LinearLayout, first: View, second: View) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(first, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(gapHorizontal(10))
        row.addView(second, LinearLayout.LayoutParams(0, -2, 1f))
        parent.addView(row)
    }
    private fun gapHorizontal(width: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(px(width), 1)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(247, 250, 255)
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.rgb(245, 250, 255), Color.WHITE, Color.rgb(239, 247, 255))
        )
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(19), px(22), px(19), px(34))
            setBackground(background)
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            addView(root)
        }
        setContentView(scroll)

        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val emblem = ImageView(this).apply {
            setImageResource(R.drawable.ic_worldvoice)
            background = surface(Color.WHITE, 20)
            setPadding(px(6), px(6), px(6), px(6))
            elevation = px(3).toFloat()
            contentDescription = "World Voice Translator globe and sound logo"
        }
        brand.addView(emblem, LinearLayout.LayoutParams(px(72), px(72)))
        brand.addView(gapHorizontal(13))
        val brandText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        brandText.addView(titleText("WORLD VOICE", 22f))
        brandText.addView(titleText("T R A N S L A T O R", 10.5f, blue))
        brandText.addView(titleText("Speak beyond borders.", 12f, muted, false).apply { setPadding(0, px(4), 0, 0) })
        brand.addView(brandText, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(brand)
        root.addView(gap(20))

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(19), px(19), px(19), px(18))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(25, 82, 173), Color.rgb(46, 112, 229), Color.rgb(73, 157, 240))).apply {
                cornerRadius = px(25).toFloat()
            }
        }
        hero.addView(titleText("The world, in your voice.", 20f, Color.WHITE))
        hero.addView(gap(6))
        hero.addView(titleText("Translate speech in 50+ languages.", 14f, Color.WHITE, false))
        hero.addView(gap(10))
        val accentLine = View(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(orange, Color.rgb(255, 228, 185), Color.TRANSPARENT)).apply { cornerRadius = px(4).toFloat() }
        }
        hero.addView(accentLine, LinearLayout.LayoutParams(-1, px(3)))
        root.addView(hero)
        root.addView(gap(20))

        val languagePanel = mainPanel()
        languagePanel.addView(sectionHeading("CHOOSE YOUR LANGUAGES"))
        val langRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val languages = supported.map { display(it) + " (" + it + ")" }
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        sourceSelect = Spinner(this).apply {
            adapter = spinnerAdapter
            background = surface(pale, 14, Color.rgb(225, 235, 250))
            setPadding(px(8), px(5), px(8), px(5))
            contentDescription = "Language you speak"
        }
        targetSelect = Spinner(this).apply {
            adapter = spinnerAdapter
            background = surface(pale, 14, Color.rgb(225, 235, 250))
            setPadding(px(8), px(5), px(8), px(5))
            contentDescription = "Language to translate into"
        }
        langRow.addView(sourceSelect, LinearLayout.LayoutParams(0, px(58), 1f))
        langRow.addView(gapHorizontal(7))
        langRow.addView(clickable("⇄") { swap() }, LinearLayout.LayoutParams(px(49), px(54)))
        langRow.addView(gapHorizontal(7))
        langRow.addView(targetSelect, LinearLayout.LayoutParams(0, px(58), 1f))
        languagePanel.addView(langRow)
        languagePanel.addView(titleText("Choose from the available translation languages above.", 12f, muted, false).apply {
            setPadding(0, px(11), 0, 0)
        })
        root.addView(languagePanel)
        sourceSelect.setSelection(supported.indexOf("en").coerceAtLeast(0))
        targetSelect.setSelection(supported.indexOf("es").coerceAtLeast(0))
        root.addView(gap(17))

        val inputPanel = mainPanel()
        inputPanel.addView(sectionHeading("01   YOUR MESSAGE"))
        input = EditText(this).apply {
            minLines = 3
            maxLines = 7
            gravity = Gravity.TOP
            textSize = 19f
            setTextColor(ink)
            hint = "Tap the microphone or type a message..."
            setHintTextColor(Color.rgb(139, 156, 177))
            background = surface(pale, 17, Color.rgb(227, 236, 248))
            setPadding(px(15), px(17), px(15), px(17))
        }
        inputPanel.addView(input, LinearLayout.LayoutParams(-1, -2))
        inputPanel.addView(gap(15))
        val mic = TextView(this).apply {
            text = "🎙"
            textSize = 42f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(23, 86, 193), Color.rgb(68, 150, 255))).apply {
                shape = GradientDrawable.OVAL
                setStroke(px(4), Color.rgb(222, 236, 255))
            }
            elevation = px(6).toFloat()
            contentDescription = "Speak into your phone"
            setOnClickListener {
                animate().scaleX(0.90f).scaleY(0.90f).setDuration(100).withEndAction {
                    animate().scaleX(1f).scaleY(1f).setDuration(170).start()
                }.start()
                listen()
            }
        }
        val micWrap = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.VERTICAL }
        micWrap.addView(mic, LinearLayout.LayoutParams(px(106), px(106)))
        micWrap.addView(titleText("TAP TO SPEAK", 13f, blue).apply {
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, px(12), 0, px(13))
        })
        inputPanel.addView(micWrap)
        inputPanel.addView(clickable("Translate my message  →", true) { translate() }, LinearLayout.LayoutParams(-1, -2))
        root.addView(inputPanel)
        root.addView(gap(18))

        val outputPanel = mainPanel()
        outputPanel.addView(sectionHeading("02   YOUR TRANSLATION"))
        output = TextView(this).apply {
            text = "Your translation will appear here."
            textSize = 20f
            setTextColor(ink)
            minHeight = px(100)
            background = surface(pale, 17, Color.rgb(225, 235, 250))
            setPadding(px(17), px(19), px(17), px(19))
        }
        outputPanel.addView(output)
        outputPanel.addView(gap(14))
        rowButtons(outputPanel,
            clickable("▶  Play voice") { play() },
            clickable("■  Stop voice") { reader?.stop() })
        outputPanel.addView(gap(10))
        outputPanel.addView(clickable("⇄  Switch speakers & reply", true) {
            swap()
            input.setText("")
            output.text = "Your translation will appear here."
            listen()
        })
        root.addView(outputPanel)
        root.addView(gap(15))

        val preferences = mainPanel()
        preferences.addView(CheckBox(this).apply {
            text = "Speak each translation automatically"
            textSize = 14f
            setTextColor(ink)
            buttonTintList = ColorStateList.valueOf(blue)
            isChecked = true
            setOnCheckedChangeListener { _, value -> autoPlay = value }
        })
        message = titleText("Ready to translate • Internet needed for new language models", 13f, muted, false).apply {
            setPadding(px(9), px(9), px(9), px(9))
            background = surface(pale, 12)
        }
        preferences.addView(message)
        root.addView(preferences)
        root.addView(gap(18))

        val historyPanel = mainPanel()
        historyPanel.addView(sectionHeading("RECENT CONVERSATIONS"))
        history = titleText("No translations yet.", 13f, muted, false).apply {
            background = surface(pale, 13)
            setPadding(px(13), px(14), px(13), px(14))
        }
        historyPanel.addView(history)
        historyPanel.addView(gap(10))
        historyPanel.addView(clickable("Clear saved history") {
            getSharedPreferences("history", MODE_PRIVATE).edit().remove("items").apply()
            history.text = "No translations yet."
            message.text = "Saved translation history cleared."
        })
        root.addView(historyPanel)
        root.addView(gap(15))
        root.addView(titleText("WORLD VOICE  •  v1.2 PREMIUM", 11f, muted, false).apply { gravity = Gravity.CENTER })
        history.text = getSharedPreferences("history", MODE_PRIVATE).getString("items", "No translations yet.")
        reader = TextToSpeech(this, this)
        fadeIn(root)
    }
    private fun swap() {
        val index = sourceSelect.selectedItemPosition
        sourceSelect.setSelection(targetSelect.selectedItemPosition)
        targetSelect.setSelection(index)
        revision++
        translator?.close(); translator = null
        message.text = "Languages swapped."
    }

    private fun listen() {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            pendingMicrophone = true
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 42)
            return
        }
        val language = Locale.forLanguageTag(from()).toLanguageTag()
        val voiceIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak in ${display(from())}")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try {
            message.text = "Opening voice input for ${display(from())}..."
            startActivityForResult(voiceIntent, speechRequest)
            return
        } catch (e: Exception) {
            message.text = "Trying another voice input method..."
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            message.text = "Voice recognition is not installed. Enable Google Speech Services in your phone settings, or type instead."
            Toast.makeText(this, message.text, Toast.LENGTH_LONG).show()
            return
        }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { message.text = "Listening in ${display(from())}..." }
            override fun onBeginningOfSpeech() { message.text = "Listening..." }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { message.text = "Processing speech..." }
            override fun onError(error: Int) { message.text = "Speech error $error. Verify microphone access and the selected speech language." }
            override fun onResults(results: Bundle?) {
                val heard = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (heard.isNullOrBlank()) { message.text = "No speech recognized. Try again."; return }
                input.setText(heard)
                translate()
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        try { recognizer?.startListening(voiceIntent) }
        catch (e: Exception) { message.text = "Microphone unavailable: ${e.localizedMessage}" }
    }
    @Deprecated("Android speech activity callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != speechRequest) return
        if (resultCode != RESULT_OK) {
            message.text = "Voice input cancelled or speech not recognized. Try again or type."
            return
        }
        val heard = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (heard.isNullOrBlank()) {
            message.text = "No words recognized. Try again in a quiet place."
        } else {
            input.setText(heard)
            message.text = "Speech recognized. Translating..."
            translate()
        }
    }
    private fun translate() {
        val phrase = input.text.toString().trim()
        if (phrase.isEmpty()) { message.text = "Speak or type words first, then press Translate."; Toast.makeText(this, "Speak or type a message first", Toast.LENGTH_LONG).show(); return }
        val source = from(); val destination = to()
        val token = ++revision
        translator?.close(); translator = null
        if (source == destination) {
            output.text = phrase; message.text = "Languages are the same."
            if (autoPlay) play()
            return
        }
        val client = Translation.getClient(TranslatorOptions.Builder()
            .setSourceLanguage(source).setTargetLanguage(destination).build())
        translator = client
        message.text = "Preparing ${display(source)} → ${display(destination)}. First use can take time to download the language model; internet must be on."
        output.text = "Preparing translation..."
        client.downloadModelIfNeeded(DownloadConditions.Builder().build())
            .addOnSuccessListener {
                if (token != revision || translator !== client) return@addOnSuccessListener
                message.text = "Translating..."
                client.translate(phrase).addOnSuccessListener { result ->
                    if (token != revision || translator !== client) return@addOnSuccessListener
                    output.text = result
                    message.text = "Translated ${display(source)} → ${display(destination)}."
                    addHistory("${display(source)} → ${display(destination)}\n$phrase\n$result")
                    if (autoPlay) play()
                }.addOnFailureListener { error ->
                    if (token == revision) { output.text = "Translation failed"; message.text = "Translation failed: ${error.localizedMessage}. Check your internet connection and try again."; Toast.makeText(this, message.text, Toast.LENGTH_LONG).show() }
                }
            }.addOnFailureListener { error ->
                if (token == revision) { output.text = "Language model unavailable"; message.text = "Language download failed: ${error.localizedMessage}. Check Wi-Fi or mobile data and retry."; Toast.makeText(this, message.text, Toast.LENGTH_LONG).show() }
            }
    }
    private fun addHistory(entry: String) {
        val prefs = getSharedPreferences("history", MODE_PRIVATE)
        val old = prefs.getString("items", "") ?: ""
        val combined = (listOf(entry) + old.split("\n---\n").filter { it.isNotBlank() })
            .take(15).joinToString("\n---\n")
        prefs.edit().putString("items", combined).apply()
        history.text = combined
    }
    private fun play() {
        val phrase = output.text.toString()
        if (phrase.isBlank() || phrase == "Your translation will appear here." || phrase == "Preparing translation...") return
        if (!readerReady) { message.text = "Speech playback is not ready."; return }
        val engine = reader ?: return
        val locale = Locale.forLanguageTag(to())
        val support = engine.isLanguageAvailable(locale)
        if (support == TextToSpeech.LANG_NOT_SUPPORTED || support == TextToSpeech.LANG_MISSING_DATA) {
            message.text = "Spoken voice for ${display(to())} is not installed. Add it in Android text-to-speech settings. The translation text is still available."
            return
        }
        engine.language = locale; engine.setSpeechRate(.95f)
        if (engine.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "voice-${System.currentTimeMillis()}") == TextToSpeech.ERROR) {
            message.text = "Could not play this translation."
        }
    }
    override fun onInit(status: Int) {
        readerReady = status == TextToSpeech.SUCCESS
        if (!readerReady) message.text = "Android voice playback is unavailable."
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 42) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED && pendingMicrophone) {
                pendingMicrophone = false; listen()
            } else {
                pendingMicrophone = false; message.text = "Microphone permission denied. Text translation is still available."
            }
        }
    }
    override fun onDestroy() {
        revision++
        recognizer?.destroy(); translator?.close(); reader?.stop(); reader?.shutdown()
        super.onDestroy()
    }
}
