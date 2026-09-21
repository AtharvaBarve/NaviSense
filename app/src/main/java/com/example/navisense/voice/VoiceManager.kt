package com.example.navisense.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class VoiceManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var isTtsReady = false

    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _recognizedText = MutableStateFlow("")

    private var onDestinationRecognizedListener: ((String) -> Unit)? = null

    var currentLocale: Locale = Locale.US
        private set

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(currentLocale)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isTtsReady = true
            }
        }
    }

    fun setLanguage(languageCode: String): Boolean {
        val locale = Locale(languageCode)
        val result = tts?.isLanguageAvailable(locale)

        return if (result == TextToSpeech.LANG_AVAILABLE ||
            result == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
            result == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
        ) {
            tts?.language = locale
            currentLocale = locale
            true
        } else {
            false
        }
    }

    fun speak(instruction: String) {
        if (isTtsReady) {
            tts?.speak(instruction, TextToSpeech.QUEUE_FLUSH, null, "NaviSenseTTS")
        }
    }

    fun startListening(onDestinationRecognized: (String) -> Unit) {
        this.onDestinationRecognizedListener = onDestinationRecognized

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _isListening.value = false
            return
        }

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isListening.value = true
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                _isListening.value = false
            }

            override fun onError(error: Int) {
                _isListening.value = false
            }

            override fun onResults(results: Bundle?) {
                _isListening.value = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val rawSpeech = matches[0]
                    _recognizedText.value = rawSpeech
                    val destination = DestinationParser.parse(rawSpeech)
                    onDestinationRecognizedListener?.invoke(destination)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLocale.language)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Where do you want to go?")
        }

        speechRecognizer?.startListening(intent)
    }

    fun processManualInput(inputText: String, onDestinationRecognized: (String) -> Unit) {
        val destination = DestinationParser.parse(inputText)
        _recognizedText.value = inputText
        onDestinationRecognized(destination)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        speechRecognizer?.destroy()
    }
}
