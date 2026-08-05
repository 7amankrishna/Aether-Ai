package com.example.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class TtsHelper(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    private val _speakingMessageId = MutableStateFlow<String?>(null)
    val speakingMessageId: StateFlow<String?> = _speakingMessageId

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isInitialized = true
            }
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _speakingMessageId.value = utteranceId
                }

                override fun onDone(utteranceId: String?) {
                    if (_speakingMessageId.value == utteranceId) {
                        _speakingMessageId.value = null
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (_speakingMessageId.value == utteranceId) {
                        _speakingMessageId.value = null
                    }
                }
            })
        }
    }

    fun speak(messageId: String, text: String) {
        if (!isInitialized || tts == null) return

        if (_speakingMessageId.value == messageId) {
            stop()
            return
        }

        stop()
        _speakingMessageId.value = messageId
        val cleanText = text.replace(Regex("[#*`_~\\[\\]()]"), " ")
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, messageId)
    }

    fun stop() {
        if (tts?.isSpeaking == true) {
            tts?.stop()
        }
        _speakingMessageId.value = null
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
    }
}
