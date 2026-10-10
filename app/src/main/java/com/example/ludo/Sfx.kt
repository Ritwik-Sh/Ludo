package com.example.ludo

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.VibrationEffect
import android.os.Vibrator
import kotlin.math.*
import kotlin.random.Random

/** Synthesised sound effects (no audio files) + light haptics. */
class Fx(ctx: Context, private val sound: Boolean, private val vib: Boolean) {
    private val SR = 22050
    private val vibrator: Vibrator? = ctx.getSystemService(Vibrator::class.java)

    private fun synth(sec: Float, f: (Double) -> Double) =
        ShortArray((SR * sec).toInt()) { (f(it.toDouble() / SR).coerceIn(-1.0, 1.0) * 32767).toInt().toShort() }

    private fun mk(d: ShortArray): AudioTrack? = try {
        AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SR).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(d.size * 2).setTransferMode(AudioTrack.MODE_STATIC).build()
            .also { it.write(d, 0, d.size) }
    } catch (e: Exception) { null }

    private val rnd = Random(7)
    private val click = if (sound) mk(synth(0.03f) { t -> sin(2 * PI * 1500 * t) * exp(-t * 200) * 0.7 + (rnd.nextDouble() - .5) * exp(-t * 500) * .4 }) else null
    private val hiss = if (sound) mk(synth(0.8f) { t -> (rnd.nextDouble() * 2 - 1) * min(1.0, t * 40) * (1 - t / 0.8).pow(1.6) * 0.5 }) else null
    private val hits = listOf(0.0 to 1.0, .07 to .9, .15 to .8, .24 to .85, .34 to .6, .46 to .55, .6 to .4)
    private val rollT = if (sound) mk(synth(0.8f) { t ->
        hits.sumOf { (tc, a) -> if (t >= tc) { val u = t - tc; a * sin(2 * PI * (900 + 500 * ((tc * 37) % 1.0)) * u) * exp(-u * 140) } else 0.0 } * 0.5
    }) else null

    private val cheerT = if (sound) mk(synth(0.7f) { t ->
        val pop = if (t < 0.12) (rnd.nextDouble() * 2 - 1) * exp(-t * 55) * 0.8 else 0.0
        val notes = listOf(784.0, 988.0, 1175.0, 1568.0)
        var sum = 0.0
        for (k in notes.indices) { val u = t - 0.06 - 0.08 * k; if (u >= 0) sum += sin(2 * PI * notes[k] * u) * exp(-u * 8) * 0.3 }
        pop + sum
    }) else null

    private val safeT = if (sound) mk(synth(0.3f) { t ->
        sin(2 * PI * 1319 * t) * exp(-t * 16) * 0.35 + (if (t > 0.07) sin(2 * PI * 1760 * (t - 0.07)) * exp(-(t - 0.07) * 14) * 0.3 else 0.0)
    }) else null

    private fun play(t: AudioTrack?) {
        if (t == null) return
        try { t.stop(); t.reloadStaticData(); t.play() } catch (e: Exception) { }
    }
    fun step() {
        play(click)
        if (vib) vibrator?.vibrate(VibrationEffect.createOneShot(12, 80))
    }
    fun hiss() = play(hiss)
    fun roll() = play(rollT)
    fun cheer() = play(cheerT)
    fun safe() = play(safeT)
    fun release() { listOf(click, hiss, rollT, cheerT, safeT).forEach { try { it?.release() } catch (e: Exception) { } } }
}
