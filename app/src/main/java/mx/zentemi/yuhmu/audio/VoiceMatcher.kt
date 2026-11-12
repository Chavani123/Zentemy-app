package mx.zentemi.yuhmu.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Compara lo que dice la persona con el audio nativo de la palabra, TODO en el dispositivo
 * (el micrófono no se guarda ni se envía). Método: MFCC + DTW. La grabación debe parecerse
 * al audio de la palabra objetivo MÁS que a las palabras "rivales" de la misma categoría.
 *
 * Para endurecer o relajar la prueba ajusta TOLERANCE (1.0 = solo gana la más parecida).
 */
class VoiceMatcher(private val context: Context, private val audioPlayer: AudioPlayer? = null) {

    enum class Result { PASS, FAIL, NO_SPEECH, NO_MIC, NO_REFERENCE }

    private val refCache = HashMap<String, Array<FloatArray>?>()

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    suspend fun listenAndJudge(targetId: String, pool: List<String>, onLevel: (Float) -> Unit): Result =
        withContext(Dispatchers.IO) {
            val target = reference(targetId) ?: return@withContext Result.NO_REFERENCE
            if (!hasPermission()) return@withContext Result.NO_MIC
            val job = coroutineContext[Job]
            val pcm = record(onLevel) { job?.isActive != false } ?: return@withContext Result.NO_MIC
            if (pcm.isEmpty()) return@withContext Result.NO_SPEECH
            val spoken = Mfcc.compute(pcm) ?: return@withContext Result.NO_SPEECH

            val dTarget = dtw(spoken, target)
            val rivals = pool.filter { it != targetId }.mapNotNull { reference(it) }.map { dtw(spoken, it) }
            if (rivals.isEmpty()) return@withContext if (dTarget < ABS_MAX) Result.PASS else Result.FAIL

            val better = rivals.count { it < dTarget }
            val best = min(dTarget, rivals.min())
            val pass = better == 0 || (better == 1 && dTarget <= best * TOLERANCE)
            if (pass) Result.PASS else Result.FAIL
        }

    // ------------------------------------------------------------ grabación

    @SuppressLint("MissingPermission")
    private fun record(onLevel: (Float) -> Unit, active: () -> Boolean): FloatArray? {
        val minBuf = AudioRecord.getMinBufferSize(SR, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minBuf <= 0) return null
        val rec = try {
            AudioRecord(MediaRecorder.AudioSource.MIC, SR, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, max(minBuf, SR))
        } catch (e: Exception) {
            return null
        }
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            rec.release()
            return null
        }
        val chunk = ShortArray(SR / 10)             // 100 ms
        val out = FloatArray(SR * MAX_SECONDS)
        var n = 0
        var floor = 0f
        var started = false
        var silentMs = 0
        var chunks = 0
        try {
            rec.startRecording()
            while (n + chunk.size <= out.size && active()) {
                val read = rec.read(chunk, 0, chunk.size)
                if (read <= 0) break
                var sum = 0.0
                for (i in 0 until read) {
                    val s = chunk[i] / 32768f
                    out[n + i] = s
                    sum += (s * s).toDouble()
                }
                n += read
                val rms = sqrt(sum / read).toFloat()
                onLevel((rms * 10f).coerceIn(0f, 1f))
                chunks++
                if (chunks <= 3) {
                    floor = max(floor, rms)
                } else if (rms > max(floor * 2.5f, 0.015f)) {
                    started = true
                    silentMs = 0
                } else if (started) {
                    silentMs += 100
                }
                if (started && silentMs >= 700) break
            }
        } finally {
            try { rec.stop() } catch (_: Exception) {}
            rec.release()
        }
        return if (started) out.copyOf(n) else FloatArray(0)
    }

    // ------------------------------------------------------------ referencia (m4a de assets)

    private fun reference(id: String): Array<FloatArray>? = synchronized(refCache) {
        refCache.getOrPut(id) { decode(id)?.let { Mfcc.compute(it) } }
    }

    private fun decode(id: String): FloatArray? {
        val assetPath = audioPlayer?.getAssetPath(id) ?: return null
        return try {
            val fd = context.assets.openFd(assetPath)
            val ex = MediaExtractor()
            ex.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            fd.close()
            var track = -1
            for (i in 0 until ex.trackCount) {
                if (ex.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    track = i
                    break
                }
            }
            if (track < 0) {
                ex.release()
                null
            } else {
                ex.selectTrack(track)
                val format = ex.getTrackFormat(track)
                var rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
                codec.configure(format, null, null, 0)
                codec.start()
                val info = MediaCodec.BufferInfo()
                var pcm = ShortArray(rate * 4)
                var count = 0
                var inputDone = false
                var outputDone = false
                while (!outputDone) {
                    if (!inputDone) {
                        val ib = codec.dequeueInputBuffer(10_000)
                        if (ib >= 0) {
                            val buf = codec.getInputBuffer(ib)!!
                            val size = ex.readSampleData(buf, 0)
                            if (size < 0) {
                                codec.queueInputBuffer(ib, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                codec.queueInputBuffer(ib, 0, size, ex.sampleTime, 0)
                                ex.advance()
                            }
                        }
                    }
                    val ob = codec.dequeueOutputBuffer(info, 10_000)
                    if (ob >= 0) {
                        val sb = codec.getOutputBuffer(ob)!!.order(ByteOrder.nativeOrder()).asShortBuffer()
                        val tmp = ShortArray(sb.remaining())
                        sb.get(tmp)
                        if (count + tmp.size > pcm.size) {
                            pcm = pcm.copyOf(max(pcm.size * 2, count + tmp.size))
                        }
                        System.arraycopy(tmp, 0, pcm, count, tmp.size)
                        count += tmp.size
                        codec.releaseOutputBuffer(ob, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                    } else if (ob == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        val newFormat = codec.outputFormat
                        rate = newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        channels = newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                }
                codec.stop()
                codec.release()
                ex.release()

                // Mono + remuestreo lineal a 16 kHz.
                val frames = count / channels
                val outLen = (frames.toLong() * SR / rate).toInt()
                val mono = FloatArray(outLen)
                for (i in 0 until outLen) {
                    val src = (i.toLong() * rate / SR).toInt().coerceAtMost(frames - 1)
                    var acc = 0f
                    for (c in 0 until channels) acc += pcm[src * channels + c] / 32768f
                    mono[i] = acc / channels
                }
                mono
            }
        } catch (_: Exception) {
            null
        }
    }

    // ------------------------------------------------------------ DTW

    private fun dtw(a: Array<FloatArray>, b: Array<FloatArray>): Float {
        val n = a.size
        val m = b.size
        val inf = 1e9f
        var prev = FloatArray(m + 1) { inf }
        prev[0] = 0f
        var cur = FloatArray(m + 1)
        for (i in 1..n) {
            cur.fill(inf)
            for (j in 1..m) {
                var d = 0f
                val x = a[i - 1]
                val y = b[j - 1]
                for (k in x.indices) { val t = x[k] - y[k]; d += t * t }
                cur[j] = sqrt(d) + min(prev[j - 1], min(prev[j], cur[j - 1]))
            }
            val t = prev; prev = cur; cur = t
        }
        return prev[m] / (n + m)
    }

    companion object {
        const val SR = 16_000
        const val MAX_SECONDS = 4
        /** 1.0 = solo pasa si es la más parecida; 1.08 = también pasa si es 2.ª y casi empata. */
        const val TOLERANCE = 1.08f
        const val ABS_MAX = 1.9f
    }
}

/** MFCC (12 coeficientes) con recorte de silencio y normalización CMVN. */
internal object Mfcc {
    private const val FRAME = 400
    private const val HOP = 160
    private const val NFFT = 512
    private const val NMEL = 24
    private const val NCEP = 12
    private const val SR = 16_000

    private val window = FloatArray(FRAME) { (0.54 - 0.46 * cos(2 * PI * it / (FRAME - 1))).toFloat() }
    private val bank: Array<FloatArray> = melBank()
    private val dct = Array(NCEP) { k -> FloatArray(NMEL) { n -> cos(PI * (k + 1) * (n + 0.5) / NMEL).toFloat() } }

    fun compute(x: FloatArray): Array<FloatArray>? {
        if (x.size < FRAME * 8) return null
        val n = (x.size - FRAME) / HOP + 1
        val logE = FloatArray(n)
        val feats = Array(n) { FloatArray(NCEP) }
        val re = FloatArray(NFFT)
        val im = FloatArray(NFFT)
        val logMel = FloatArray(NMEL)
        for (f in 0 until n) {
            val off = f * HOP
            for (i in 0 until FRAME) {
                val prev = if (off + i > 0) x[off + i - 1] else 0f
                re[i] = (x[off + i] - 0.97f * prev) * window[i]
            }
            for (i in FRAME until NFFT) re[i] = 0f
            im.fill(0f)
            fft(re, im)
            var total = 0f
            val bins = NFFT / 2 + 1
            val power = FloatArray(bins)
            for (k in 0 until bins) { power[k] = re[k] * re[k] + im[k] * im[k]; total += power[k] }
            logE[f] = ln(total + 1e-10f)
            for (m in 0 until NMEL) {
                var s = 0f
                val w = bank[m]
                for (k in 0 until bins) s += w[k] * power[k]
                logMel[m] = ln(s + 1e-10f)
            }
            for (c in 0 until NCEP) {
                var s = 0f
                for (m in 0 until NMEL) s += dct[c][m] * logMel[m]
                feats[f][c] = s
            }
        }
        val maxE = logE.max()
        var first = 0
        var last = n - 1
        while (first < n && logE[first] < maxE - 5.5f) first++
        while (last > first && logE[last] < maxE - 5.5f) last--
        if (last - first + 1 < 8) return null
        val seg = Array(last - first + 1) { feats[first + it] }
        // CMVN
        for (c in 0 until NCEP) {
            var mean = 0f
            for (r in seg) mean += r[c]
            mean /= seg.size
            var v = 0f
            for (r in seg) { val d = r[c] - mean; v += d * d }
            val std = max(sqrt(v / seg.size), 1e-3f)
            for (r in seg) r[c] = (r[c] - mean) / std
        }
        return seg
    }

    private fun melBank(): Array<FloatArray> {
        fun mel(hz: Double) = 2595.0 * log10(1.0 + hz / 700.0)
        fun hz(m: Double) = 700.0 * (Math.pow(10.0, m / 2595.0) - 1.0)
        val lo = mel(80.0)
        val hi = mel(7600.0)
        val pts = IntArray(NMEL + 2) { ((NFFT + 1) * hz(lo + (hi - lo) * it / (NMEL + 1)) / SR).toInt() }
        return Array(NMEL) { m ->
            FloatArray(NFFT / 2 + 1).also { w ->
                for (k in pts[m] until pts[m + 1]) w[k] = (k - pts[m]).toFloat() / max(1, pts[m + 1] - pts[m])
                for (k in pts[m + 1] until pts[m + 2]) w[k] = (pts[m + 2] - k).toFloat() / max(1, pts[m + 2] - pts[m + 1])
            }
        }
    }

    private fun fft(re: FloatArray, im: FloatArray) {
        val n = re.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while ((j and bit) != 0) { j = j xor bit; bit = bit shr 1 }
            j = j xor bit
            if (i < j) {
                val tr = re[i]; re[i] = re[j]; re[j] = tr
                val ti = im[i]; im[i] = im[j]; im[j] = ti
            }
        }
        var len = 2
        while (len <= n) {
            val ang = -2.0 * PI / len
            val wr = cos(ang).toFloat()
            val wi = sin(ang).toFloat()
            val half = len / 2
            var i = 0
            while (i < n) {
                var cr = 1f
                var ci = 0f
                for (k in 0 until half) {
                    val a = i + k
                    val b = a + half
                    val vr = re[b] * cr - im[b] * ci
                    val vi = re[b] * ci + im[b] * cr
                    re[b] = re[a] - vr; im[b] = im[a] - vi
                    re[a] += vr; im[a] += vi
                    val nr = cr * wr - ci * wi
                    ci = cr * wi + ci * wr
                    cr = nr
                }
                i += len
            }
            len = len shl 1
        }
    }
}
