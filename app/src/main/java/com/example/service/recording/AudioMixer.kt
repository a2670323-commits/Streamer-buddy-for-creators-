package com.example.service.recording

import kotlin.math.roundToInt

object AudioMixer {

    /**
     * Mixes two 16-bit PCM little-endian audio buffers into a target output buffer.
     * Applies gain scaling and clamps samples to prevent digital clipping/distortion.
     *
     * @param micBuffer Raw 16-bit PCM byte array from microphone AudioRecord
     * @param micBytes Number of valid bytes in micBuffer
     * @param micGain Gain multiplier for microphone (e.g. 1.0f for normal, 1.2f for boost)
     * @param internalBuffer Raw 16-bit PCM byte array from AudioPlaybackCapture AudioRecord
     * @param internalBytes Number of valid bytes in internalBuffer
     * @param internalGain Gain multiplier for game/internal audio (e.g. 0.8f)
     * @param outputBuffer Target buffer where mixed 16-bit PCM will be written
     * @return Number of valid mixed bytes written to outputBuffer
     */
    fun mix16BitPcm(
        micBuffer: ByteArray?,
        micBytes: Int,
        micGain: Float,
        internalBuffer: ByteArray?,
        internalBytes: Int,
        internalGain: Float,
        outputBuffer: ByteArray
    ): Int {
        val totalBytes = maxOf(
            if (micBuffer != null) micBytes else 0,
            if (internalBuffer != null) internalBytes else 0
        ).coerceAtMost(outputBuffer.size)

        // Process two bytes (1 short) at a time
        val samplePairs = totalBytes / 2

        for (i in 0 until samplePairs) {
            val byteIndex = i * 2

            val micSample: Short = if (micBuffer != null && byteIndex + 1 < micBytes) {
                ((micBuffer[byteIndex].toInt() and 0xFF) or (micBuffer[byteIndex + 1].toInt() shl 8)).toShort()
            } else {
                0
            }

            val internalSample: Short = if (internalBuffer != null && byteIndex + 1 < internalBytes) {
                ((internalBuffer[byteIndex].toInt() and 0xFF) or (internalBuffer[byteIndex + 1].toInt() shl 8)).toShort()
            } else {
                0
            }

            val mixedValue = (micSample * micGain + internalSample * internalGain).roundToInt()
            val clampedSample = mixedValue.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

            outputBuffer[byteIndex] = (clampedSample.toInt() and 0xFF).toByte()
            outputBuffer[byteIndex + 1] = ((clampedSample.toInt() shr 8) and 0xFF).toByte()
        }

        return samplePairs * 2
    }
}
