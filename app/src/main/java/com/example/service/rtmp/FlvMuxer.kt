package com.example.service.rtmp

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

object FlvMuxer {

    const val TAG_TYPE_AUDIO = 8
    const val TAG_TYPE_VIDEO = 9

    const val AVC_KEYFRAME = 1
    const val AVC_INTERFRAME = 2

    const val AVC_PACKET_TYPE_SEQUENCE_HEADER = 0
    const val AVC_PACKET_TYPE_NALU = 1

    const val AAC_PACKET_TYPE_SEQUENCE_HEADER = 0
    const val AAC_PACKET_TYPE_RAW = 1

    /**
     * Creates an FLV Video Tag for H.264/AVC data.
     */
    fun createVideoTag(
        isKeyframe: Boolean,
        isSequenceHeader: Boolean,
        timestampMs: Long,
        payload: ByteArray,
        offset: Int = 0,
        length: Int = payload.size
    ): ByteArray {
        val frameType = if (isKeyframe) AVC_KEYFRAME else AVC_INTERFRAME
        val packetType = if (isSequenceHeader) AVC_PACKET_TYPE_SEQUENCE_HEADER else AVC_PACKET_TYPE_NALU

        val headerSize = 5
        val tagPayloadSize = headerSize + length
        val tagBytes = ByteArray(11 + tagPayloadSize + 4)

        // FLV Tag Header (11 bytes)
        tagBytes[0] = TAG_TYPE_VIDEO.toByte()
        // Data Size (3 bytes)
        tagBytes[1] = ((tagPayloadSize shr 16) and 0xFF).toByte()
        tagBytes[2] = ((tagPayloadSize shr 8) and 0xFF).toByte()
        tagBytes[3] = (tagPayloadSize and 0xFF).toByte()
        // Timestamp (3 bytes) + TimestampExtended (1 byte)
        tagBytes[4] = ((timestampMs shr 16) and 0xFF).toByte()
        tagBytes[5] = ((timestampMs shr 8) and 0xFF).toByte()
        tagBytes[6] = (timestampMs and 0xFF).toByte()
        tagBytes[7] = ((timestampMs shr 24) and 0xFF).toByte()
        // StreamID (3 bytes, always 0)
        tagBytes[8] = 0
        tagBytes[9] = 0
        tagBytes[10] = 0

        // Video Payload Header (5 bytes)
        // Byte 0: FrameType (4 bits) | CodecID = 7 (AVC)
        tagBytes[11] = ((frameType shl 4) or 7).toByte()
        // Byte 1: AVCPacketType
        tagBytes[12] = packetType.toByte()
        // Bytes 2-4: CompositionTime (3 bytes, 0 for simple PTS)
        tagBytes[13] = 0
        tagBytes[14] = 0
        tagBytes[15] = 0

        // Copy NALU/Sequence payload
        System.arraycopy(payload, offset, tagBytes, 16, length)

        // PreviousTagSize (4 bytes at the end)
        val fullTagSize = 11 + tagPayloadSize
        val endIdx = 16 + length
        tagBytes[endIdx] = ((fullTagSize shr 24) and 0xFF).toByte()
        tagBytes[endIdx + 1] = ((fullTagSize shr 16) and 0xFF).toByte()
        tagBytes[endIdx + 2] = ((fullTagSize shr 8) and 0xFF).toByte()
        tagBytes[endIdx + 3] = (fullTagSize and 0xFF).toByte()

        return tagBytes
    }

    /**
     * Creates an FLV Audio Tag for AAC stereo audio data.
     */
    fun createAudioTag(
        isSequenceHeader: Boolean,
        timestampMs: Long,
        payload: ByteArray,
        offset: Int = 0,
        length: Int = payload.size
    ): ByteArray {
        val packetType = if (isSequenceHeader) AAC_PACKET_TYPE_SEQUENCE_HEADER else AAC_PACKET_TYPE_RAW

        val headerSize = 2
        val tagPayloadSize = headerSize + length
        val tagBytes = ByteArray(11 + tagPayloadSize + 4)

        // FLV Tag Header (11 bytes)
        tagBytes[0] = TAG_TYPE_AUDIO.toByte()
        tagBytes[1] = ((tagPayloadSize shr 16) and 0xFF).toByte()
        tagBytes[2] = ((tagPayloadSize shr 8) and 0xFF).toByte()
        tagBytes[3] = (tagPayloadSize and 0xFF).toByte()
        tagBytes[4] = ((timestampMs shr 16) and 0xFF).toByte()
        tagBytes[5] = ((timestampMs shr 8) and 0xFF).toByte()
        tagBytes[6] = (timestampMs and 0xFF).toByte()
        tagBytes[7] = ((timestampMs shr 24) and 0xFF).toByte()
        tagBytes[8] = 0
        tagBytes[9] = 0
        tagBytes[10] = 0

        // Audio Payload Header (2 bytes)
        // 0xAF = AAC (10 = 0xA0) | 44kHz (3 = 0x0C) | 16-bit (1 = 0x02) | Stereo (1 = 0x01)
        tagBytes[11] = 0xAF.toByte()
        tagBytes[12] = packetType.toByte()

        System.arraycopy(payload, offset, tagBytes, 13, length)

        // PreviousTagSize (4 bytes)
        val fullTagSize = 11 + tagPayloadSize
        val endIdx = 13 + length
        tagBytes[endIdx] = ((fullTagSize shr 24) and 0xFF).toByte()
        tagBytes[endIdx + 1] = ((fullTagSize shr 16) and 0xFF).toByte()
        tagBytes[endIdx + 2] = ((fullTagSize shr 8) and 0xFF).toByte()
        tagBytes[endIdx + 3] = (fullTagSize and 0xFF).toByte()

        return tagBytes
    }

    /**
     * Builds AVCDecoderConfigurationRecord for H.264 sequence header.
     */
    fun buildAvcSequenceHeader(sps: ByteArray, pps: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(1) // configurationVersion = 1
        out.write(sps[1].toInt()) // AVCProfileIndication
        out.write(sps[2].toInt()) // profile_compatibility
        out.write(sps[3].toInt()) // AVCLevelIndication
        out.write(0xFF) // 6 bits reserved (111111) + lengthSizeMinusOne (3 = 4 bytes length)

        out.write(0xE1) // 3 bits reserved (111) + numOfSequenceParameterSets (1)
        out.write((sps.size shr 8) and 0xFF)
        out.write(sps.size and 0xFF)
        out.write(sps)

        out.write(1) // numOfPictureParameterSets = 1
        out.write((pps.size shr 8) and 0xFF)
        out.write(pps.size and 0xFF)
        out.write(pps)

        return out.toByteArray()
    }

    /**
     * Builds standard 2-byte AudioSpecificConfig for AAC-LC 44.1kHz stereo.
     * Profile: 2 (AAC-LC)
     * Sampling freq index: 4 (44100Hz)
     * Channel config: 2 (Stereo)
     */
    fun buildAacAudioSpecificConfig(sampleRate: Int = 44100, channelCount: Int = 2): ByteArray {
        val profile = 2 // AAC LC
        val sampleRateIdx = when (sampleRate) {
            96000 -> 0
            88200 -> 1
            64000 -> 2
            48000 -> 3
            44100 -> 4
            32000 -> 5
            24000 -> 6
            22050 -> 7
            16000 -> 8
            12000 -> 9
            11025 -> 10
            8000 -> 11
            else -> 4
        }
        val config = (profile shl 11) or (sampleRateIdx shl 7) or (channelCount shl 3)
        return byteArrayOf(
            ((config shr 8) and 0xFF).toByte(),
            (config and 0xFF).toByte()
        )
    }
}
