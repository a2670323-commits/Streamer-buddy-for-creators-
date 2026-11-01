package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.BroadcastState
import com.example.data.model.NetworkHealth
import com.example.data.model.StreamPreset
import com.example.service.ScreenRecordingService
import com.example.service.recording.AudioMixer
import com.example.service.rtmp.FlvMuxer
import com.example.ui.home.HomeViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Streamer Buddy", appName)
  }

  @Test
  fun `verify stream presets are non-null and properly initialized`() {
    val presets = StreamPreset.ALL
    assertEquals(3, presets.size)
    for (preset in presets) {
      assertNotNull("Preset must not be null", preset)
      assertNotNull("Preset ID must not be null", preset.id)
      assertNotNull("Preset name must not be null", preset.name)
      assertTrue(preset.width > 0)
      assertTrue(preset.height > 0)
    }
  }

  @Test
  fun `verify HomeViewModel default state`() {
    val viewModel = HomeViewModel()
    val state = viewModel.uiState.value
    assertNotNull(state.selectedPreset)
    assertEquals("1080p_60", state.selectedPreset.id)
    assertFalse(state.isScreenRecording)
    assertEquals(0L, state.recordingDurationSeconds)
    assertEquals(BroadcastState.Offline, state.broadcastState)
    assertTrue(state.rtmpServerUrl.isNotEmpty())
    assertTrue(state.rtmpStreamKey.isNotEmpty())
  }

  @Test
  fun `verify HomeViewModel settings update`() {
    val viewModel = HomeViewModel()
    val newUrl = "rtmp://b.rtmp.youtube.com/live2?backup=1"
    val newKey = "live_custom_key_12345"
    viewModel.updateStreamSettings(newUrl, newKey)

    val updatedState = viewModel.uiState.value
    assertEquals(newUrl, updatedState.rtmpServerUrl)
    assertEquals(newKey, updatedState.rtmpStreamKey)
  }

  @Test
  fun `verify HomeViewModel preset selection updates checklist`() {
    val viewModel = HomeViewModel()
    viewModel.selectPreset(StreamPreset.Preset720p)

    val state = viewModel.uiState.value
    assertEquals("720p_60", state.selectedPreset.id)
    val presetChecklist = state.checklist.find { it.id == "preset" }
    assertNotNull(presetChecklist)
    assertTrue(presetChecklist!!.isReady)
  }

  @Test
  fun `verify ScreenRecordingService constants and flows`() {
    assertEquals("com.example.service.action.START", ScreenRecordingService.ACTION_START)
    assertEquals("com.example.service.action.STOP", ScreenRecordingService.ACTION_STOP)
    assertEquals("com.example.service.action.START_BROADCAST", ScreenRecordingService.ACTION_START_BROADCAST)
    assertEquals("com.example.service.action.STOP_BROADCAST", ScreenRecordingService.ACTION_STOP_BROADCAST)
    assertFalse(ScreenRecordingService.isRecording.value)
    assertEquals(0L, ScreenRecordingService.recordingDurationSeconds.value)
    assertEquals(BroadcastState.Offline, ScreenRecordingService.broadcastState.value)
  }

  @Test
  fun `verify AudioMixer mixes PCM samples cleanly`() {
    // 1 sample (16-bit = 2 bytes) with value 1000
    val micBuffer = byteArrayOf(0xE8.toByte(), 0x03.toByte()) // 1000 in little-endian
    // 1 sample with value 2000
    val internalBuffer = byteArrayOf(0xD0.toByte(), 0x07.toByte()) // 2000 in little-endian
    val outputBuffer = ByteArray(2)

    val mixedBytes = AudioMixer.mix16BitPcm(
      micBuffer = micBuffer,
      micBytes = 2,
      micGain = 1.0f,
      internalBuffer = internalBuffer,
      internalBytes = 2,
      internalGain = 1.0f,
      outputBuffer = outputBuffer
    )

    assertEquals(2, mixedBytes)
    // 1000 + 2000 = 3000 (0x0BB8 -> 0xB8, 0x0B)
    val resultSample = ((outputBuffer[0].toInt() and 0xFF) or (outputBuffer[1].toInt() shl 8)).toShort()
    assertEquals(3000.toShort(), resultSample)
  }

  @Test
  fun `verify AudioMixer handles null microphone buffer gracefully`() {
    val internalBuffer = byteArrayOf(0xD0.toByte(), 0x07.toByte())
    val outputBuffer = ByteArray(2)

    val mixedBytes = AudioMixer.mix16BitPcm(
      micBuffer = null,
      micBytes = 0,
      micGain = 1.0f,
      internalBuffer = internalBuffer,
      internalBytes = 2,
      internalGain = 1.0f,
      outputBuffer = outputBuffer
    )

    assertEquals(2, mixedBytes)
    val resultSample = ((outputBuffer[0].toInt() and 0xFF) or (outputBuffer[1].toInt() shl 8)).toShort()
    assertEquals(2000.toShort(), resultSample)
  }

  @Test
  fun `verify FlvMuxer video tag creation for AVC keyframe`() {
    val fakeNalu = byteArrayOf(0x65.toByte(), 0x88.toByte(), 0x84.toByte(), 0x00.toByte())
    val flvTag = FlvMuxer.createVideoTag(
      isKeyframe = true,
      isSequenceHeader = false,
      timestampMs = 100L,
      payload = fakeNalu
    )

    // FLV Tag Header checks:
    // Tag type: 9 = Video
    assertEquals(FlvMuxer.TAG_TYPE_VIDEO.toByte(), flvTag[0])
    // Video Header byte 11: FrameType = 1 (Keyframe) << 4 | CodecID = 7 (AVC) -> 0x17
    assertEquals(0x17.toByte(), flvTag[11])
    // Video Header byte 12: AVCPacketType = 1 (NALU)
    assertEquals(1.toByte(), flvTag[12])
  }

  @Test
  fun `verify FlvMuxer audio tag creation for AAC`() {
    val fakeAac = byteArrayOf(0x21.toByte(), 0x10.toByte(), 0x04.toByte(), 0x60.toByte())
    val flvTag = FlvMuxer.createAudioTag(
      isSequenceHeader = false,
      timestampMs = 50L,
      payload = fakeAac
    )

    // FLV Tag Header checks:
    // Tag type: 8 = Audio
    assertEquals(FlvMuxer.TAG_TYPE_AUDIO.toByte(), flvTag[0])
    // Audio Header byte 11: SoundFormat = 10 (AAC) << 4 | 44kHz (3<<2) | 16bit (1<<1) | Stereo (1) -> 0xAF
    assertEquals(0xAF.toByte(), flvTag[11])
    // Audio Header byte 12: AACPacketType = 1 (Raw)
    assertEquals(1.toByte(), flvTag[12])
  }

  @Test
  fun `verify BroadcastState Live health mapping`() {
    val liveState = BroadcastState.Live(
      viewerCount = 1420,
      durationSeconds = 3600,
      currentBitrateKbps = 6000,
      currentFps = 60,
      droppedFrames = 0,
      networkHealth = NetworkHealth.EXCELLENT
    )

    assertEquals("EXCELLENT", liveState.networkHealth.label)
    assertEquals(0xFF00E676, liveState.networkHealth.colorHex)
    assertEquals(1420, liveState.viewerCount)
  }
}
