package com.roubao.autopilot.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 语音识别状态
 */
sealed class SpeechRecognitionState {
    object Idle : SpeechRecognitionState()
    object Listening : SpeechRecognitionState()
    object Processing : SpeechRecognitionState()
    data class Success(val text: String) : SpeechRecognitionState()
    data class Error(val message: String) : SpeechRecognitionState()
}

/**
 * 语音识别帮助类
 */
class SpeechRecognizerHelper(private val context: Context) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var audioFocusGranted = false
    
    /**
     * 请求音频焦点
     */
    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { /* 不需要处理 */ }
                .build()
            
            val result = audioManager.requestAudioFocus(audioFocusRequest!!)
            audioFocusGranted = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            audioFocusGranted
        } else {
            @Suppress("DEPRECATION")
            val result = audioManager.requestAudioFocus(
                { /* 不需要处理 */ },
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
            audioFocusGranted = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            audioFocusGranted
        }
    }
    
    /**
     * 释放音频焦点
     */
    private fun abandonAudioFocus() {
        if (audioFocusGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus { /* 不需要处理 */ }
            }
            audioFocusGranted = false
        }
    }
    /**
     * 检查是否有录音权限
     */
    fun hasAudioPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 开始语音识别
     */
    suspend fun startListening(): String = suspendCancellableCoroutine { continuation ->
        if (!hasAudioPermission()) {
            continuation.resume("")
            return@suspendCancellableCoroutine
        }
        
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            continuation.resume("")
            return@suspendCancellableCoroutine
        }
        
        // 请求音频焦点
        if (!requestAudioFocus()) {
            Log.w("SpeechRecognizer", "无法获取音频焦点")
            // 即使无法获取音频焦点也继续尝试识别
        }
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d("SpeechRecognizer", "准备就绪")
                }
                
                override fun onBeginningOfSpeech() {
                    Log.d("SpeechRecognizer", "开始说话")
                }
                
                override fun onRmsChanged(rmsdB: Float) {
                    // 音量变化回调
                }
                
                override fun onBufferReceived(buffer: ByteArray?) {
                    // 缓冲数据接收
                }
                
                override fun onEndOfSpeech() {
                    Log.d("SpeechRecognizer", "说话结束")
                }
                
                override fun onError(error: Int) {
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "音频问题"
                        SpeechRecognizer.ERROR_CLIENT -> "客户端错误"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "权限不足"
                        SpeechRecognizer.ERROR_NETWORK -> "网络错误"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
                        SpeechRecognizer.ERROR_NO_MATCH -> "无法识别语音"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别器忙"
                        SpeechRecognizer.ERROR_SERVER -> "服务器错误"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "说话超时"
                        else -> "未知错误"
                    }
                    Log.e("SpeechRecognizer", "识别错误: $errorMessage")
                    isListening = false
                    abandonAudioFocus()
                    continuation.resume("")
                }
                
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val recognizedText = matches?.firstOrNull() ?: ""
                    Log.d("SpeechRecognizer", "识别结果: $recognizedText")
                    isListening = false
                    abandonAudioFocus()
                    continuation.resume(recognizedText)
                }
                
                override fun onPartialResults(partialResults: Bundle?) {
                    // 部分结果回调
                }
                
                override fun onEvent(eventType: Int, params: Bundle?) {
                    // 事件回调
                }
            })
        }
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "请说话...")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // 启用部分结果
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        
        isListening = true
        speechRecognizer?.startListening(intent)
        
        continuation.invokeOnCancellation {
            stopListening()
        }
    }
    
    /**
     * 停止语音识别
     */
    fun stopListening() {
        if (isListening) {
            speechRecognizer?.stopListening()
            isListening = false
        }
        abandonAudioFocus()
    }
    
    /**
     * 销毁语音识别器
     */
    fun destroy() {
        stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        abandonAudioFocus()
    }
}

/**
 * Composable 作用域内的语音识别状态管理
 */
@Composable
fun rememberSpeechRecognizer(): SpeechRecognizerHelper {
    val context = LocalContext.current
    return remember { SpeechRecognizerHelper(context) }
}