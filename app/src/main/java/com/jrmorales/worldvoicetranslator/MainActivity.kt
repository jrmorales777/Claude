package com.jrmorales.worldvoicetranslator

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(242, 248, 255)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(18), px(18), px(18), px(32))
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(241,248,255), Color.WHITE, Color.rgb(239,247,255)))
        }
        val scroll = ScrollView(this)
        scroll.fillViewport = true
        scroll.addView(root)
        setContentView(scroll)
        root.addView(label("🌐 World Voice Translator", 27f).apply {
            typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.rgb(14,48,96))
        })
        root.addView(label("Speak naturally. Connect across languages.", 14f))
        root.addView(label("LANGUAGES", 13f).apply { typeface = Typeface.DEFAULT_BOLD })
        val langRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        sourceSelect = Spinner(this)
        targetSelect = Spinner(this)
        val names = supported.map { "${display(it)} ($it)" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
        sourceSelect.adapter = adapter; targetSelect.adapter = adapter
        sourceSelect.background = bg(Color.WHITE, 14, Color.rgb(198, 222, 250))
        targetSelect.background = bg(Color.WHITE, 14, Color.rgb(198, 222, 250))
        langRow.addView(sourceSelect, LinearLayout.LayoutParams(0, px(58), 1f))
        langRow.addView(button("⇄") { swap() })
        langRow.addView(targetSelect, LinearLayout.LayoutParams(0, px(58), 1f))
        root.addView(langRow)
        sourceSelect.setSelection(supported.indexOf("en").coerceAtLeast(0))
        targetSelect.setSelection(supported.indexOf("es").coerceAtLeast(0))
        root.addView(label("YOUR MESSAGE", 13f).apply { typeface = Typeface.DEFAULT_BOLD })
        input = EditText(this).apply {
            minLines = 3; maxLines = 7; gravity = Gravity.TOP; textSize = 19f
            hint = "Speak or type your message..."
            background = bg(Color.WHITE, 20, Color.rgb(202, 225, 249))
            setPadding(px(16), px(17), px(16), px(17))
        }
        root.addView(input, LinearLayout.LayoutParams(-1, -2))
        val speakRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        speakRow.addView(button("🎤 Speak", true) { listen() }, LinearLayout.LayoutParams(0, -2, 1f))
        speakRow.addView(button("Translate", true) { translate() }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(speakRow)
        root.addView(label("TRANSLATION", 13f).apply { typeface = Typeface.DEFAULT_BOLD })
        output = TextView(this).apply {
            text = "Your translation appears here"; textSize = 21f; minHeight = px(115)
            setTextColor(Color.rgb(16, 57, 104))
            background = bg(Color.WHITE, 20, Color.rgb(202, 225, 249))
            setPadding(px(17), px(18), px(17), px(18))
        }
        root.addView(output)
        val playRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        playRow.addView(button("🔊 Play translation") { play() }, LinearLayout.LayoutParams(0, -2, 1f))
        playRow.addView(button("Stop") { reader?.stop() }, LinearLayout.LayoutParams(0, -2, .45f))
        root.addView(playRow)
        root.addView(button("↔ Reply in the other language") {
            swap(); input.setText(""); output.text = "Your translation appears here"; listen()
        })
        root.addView(CheckBox(this).apply {
            text = "Speak translations automatically"; isChecked = true
            setOnCheckedChangeListener { _, checked -> autoPlay = checked }
        })
        message = label("Ready. First use may download language models.", 14f)
        root.addView(message)
        root.addView(label("RECENT TRANSLATIONS", 13f).apply { typeface = Typeface.DEFAULT_BOLD })
        history = label("No translations yet", 14f).apply {
            background = bg(Color.WHITE); setPadding(px(15), px(14), px(15), px(14))
        }
        root.addView(history)
        history.text = getSharedPreferences("history", MODE_PRIVATE).getString("items", "No translations yet")
        reader = TextToSpeech(this, this)
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
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            message.text = "Speech recognition is unavailable on this phone. You can still type."
            return
        }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { message.text = "Listening in ${display(from())}…" }
            override fun onBeginningOfSpeech() { message.text = "Listening…" }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { message.text = "Processing speech…" }
            override fun onError(error: Int) { message.text = "Speech recognition error $error. Check speech service and selected language." }
            override fun onResults(results: Bundle?) {
                val heard = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (heard.isNullOrBlank()) { message.text = "No speech was recognized."; return }
                input.setText(heard); translate()
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val language = Locale.forLanguageTag(from()).toLanguageTag()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try { recognizer?.startListening(intent) }
        catch (e: Exception) { message.text = "Microphone unavailable: ${e.localizedMessage}" }
    }
    private fun translate() {
        val phrase = input.text.toString().trim()
        if (phrase.isEmpty()) { message.text = "Speak or type a message first."; return }
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
        message.text = "Preparing ${display(source)} → ${display(destination)}..."
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
                    if (token == revision) message.text = "Translation error: ${error.localizedMessage}"
                }
            }.addOnFailureListener { error ->
                if (token == revision) message.text = "Model download failed: ${error.localizedMessage}"
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
        if (phrase.isBlank() || phrase == "Your translation appears here") return
        if (!readerReady) { message.text = "Speech playback is not ready."; return }
        val engine = reader ?: return
        val locale = Locale.forLanguageTag(to())
        val support = engine.isLanguageAvailable(locale)
        if (support == TextToSpeech.LANG_NOT_SUPPORTED || support == TextToSpeech.LANG_MISSING_DATA) {
            message.text = "This phone does not have a spoken voice for ${display(to())}. The text translation is available."
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
