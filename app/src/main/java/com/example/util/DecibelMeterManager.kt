package com.example.util

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Real-time Decibel & Ambient Noise Monitor for Library Silent Reading Halls.
 */
class DecibelMeterManager {

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _currentDb = MutableStateFlow(32f)
    val currentDb: StateFlow<Float> = _currentDb.asStateFlow()

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    @SuppressLint("MissingPermission")
    fun startMonitoring() {
        if (isRecording) return

        try {
            val sampleRate = 44100
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(2048)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                return
            }

            audioRecord?.startRecording()
            isRecording = true
            _isMonitoring.value = true

            recordingJob = scope.launch {
                val buffer = ShortArray(bufferSize)
                var smoothedDb = 35f

                while (isActive && isRecording) {
                    val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readSize > 0) {
                        var sum = 0.0
                        for (i in 0 until readSize) {
                            sum += buffer[i].toDouble() * buffer[i].toDouble()
                        }
                        val rms = sqrt(sum / readSize)

                        // Convert to approximated Decibel SPL (reference 1.0)
                        val db = if (rms > 1.0) {
                            (20.0 * log10(rms)).toFloat() + 15f
                        } else {
                            25f
                        }.coerceIn(25f, 95f)

                        // Low-pass filter for smooth gauge reading
                        smoothedDb = smoothedDb * 0.7f + db * 0.3f
                        _currentDb.value = smoothedDb
                    }
                    delay(120)
                }
            }
        } catch (_: Exception) {
            stopMonitoring()
        }
    }

    fun stopMonitoring() {
        isRecording = false
        _isMonitoring.value = false
        recordingJob?.cancel()
        recordingJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
    }

    enum class NoiseLevelCategory(val label: String, val tip: String, val colorHex: Long) {
        PIN_DROP("Pin-Drop Silence", "Ideal for deep study and focused reading.", 0xFF10B981),
        WHISPER("Quiet Whisper", "Acceptable reading hall volume.", 0xFF0284C7),
        MODERATE("Moderate Discussion", "Slight distraction in silent zone.", 0xFFF59E0B),
        NOISY("Noise Alert! High Level", "Exceeds library silent reading guidelines.", 0xFFEF4444)
    }

    companion object {
        fun getCategory(db: Float): NoiseLevelCategory {
            return when {
                db < 38f -> NoiseLevelCategory.PIN_DROP
                db < 48f -> NoiseLevelCategory.WHISPER
                db < 60f -> NoiseLevelCategory.MODERATE
                else -> NoiseLevelCategory.NOISY
            }
        }
    }
}
