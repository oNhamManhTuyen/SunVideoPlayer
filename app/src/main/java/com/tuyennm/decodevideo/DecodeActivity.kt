package com.tuyennm.decodevideo

import android.app.Activity
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView


//from https://github.com/vecio/MediaCodecDemo


class DecodeActivity : Activity(), SurfaceHolder.Callback {
    private var mPlayer: PlayerThread? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sv = SurfaceView(this)
        sv.holder.addCallback(this)
        setContentView(sv)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {}
    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int
    ) {
        if (mPlayer == null) {
            mPlayer = PlayerThread(holder.surface)
            mPlayer!!.start()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        if (mPlayer != null) {
            mPlayer!!.interrupt()
        }
    }

    private inner class PlayerThread(private val surface: Surface) :
        Thread() {
        private var extractor: MediaExtractor? = null
        private var decoder: MediaCodec? = null
        override fun run() {
            extractor = MediaExtractor()
            extractor!!.setDataSource(assets.openFd("samplevideo_1280x720_5mb.mp4"))
            for (i in 0 until extractor!!.trackCount) {
                val format = extractor!!.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime.startsWith("video/")) {
                    extractor!!.selectTrack(i)
                    decoder = MediaCodec.createDecoderByType(mime)
                    decoder!!.configure(format, surface, null, 0)
                    break
                }
            }
            if (decoder == null) {
                Log.e("DecodeActivity", "Can't find video info!")
                return
            }
            decoder!!.start()
            val inputBuffers = decoder!!.inputBuffers
            var outputBuffers = decoder!!.outputBuffers
            val info = MediaCodec.BufferInfo()
            var isEOS = false
            val startMs = System.currentTimeMillis()
            while (!interrupted()) {
                if (!isEOS) {
                    val inIndex = decoder!!.dequeueInputBuffer(10000)
                    if (inIndex >= 0) {
                        val buffer = inputBuffers[inIndex]
                        val sampleSize = extractor!!.readSampleData(buffer, 0)
                        if (sampleSize < 0) {
                            // We shouldn't stop the playback at this point, just pass the EOS
                            // flag to decoder, we will get it again from the
                            // dequeueOutputBuffer
                            Log.d(
                                "DecodeActivity",
                                "InputBuffer BUFFER_FLAG_END_OF_STREAM"
                            )
                            decoder!!.queueInputBuffer(
                                inIndex,
                                0,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            isEOS = true
                        } else {
                            decoder!!.queueInputBuffer(
                                inIndex,
                                0,
                                sampleSize,
                                extractor!!.sampleTime,
                                0
                            )
                            extractor!!.advance()
                        }
                    }
                }
                val outIndex = decoder!!.dequeueOutputBuffer(info, 10000)
                when (outIndex) {
                    MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                        Log.d("DecodeActivity", "INFO_OUTPUT_BUFFERS_CHANGED")
                        outputBuffers = decoder!!.outputBuffers
                    }
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Log.d(
                        "DecodeActivity",
                        "New format " + decoder!!.outputFormat
                    )
                    MediaCodec.INFO_TRY_AGAIN_LATER -> Log.d(
                        "DecodeActivity",
                        "dequeueOutputBuffer timed out!"
                    )
                    else -> {
                        val buffer = outputBuffers[outIndex]
                        Log.v(
                            "DecodeActivity",
                            "We can't use this buffer but render it due to the API limit, $buffer"
                        )

                        // We use a very simple clock to keep the video FPS, or the video
                        // playback will be too fast
                        while (info.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                            try {
                                sleep(10)
                            } catch (e: InterruptedException) {
                                e.printStackTrace()
                                break
                            }
                        }
                        decoder!!.releaseOutputBuffer(outIndex, true)
                    }
                }

                // All decoded frames have been rendered, we can stop playing now
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    Log.d("DecodeActivity", "OutputBuffer BUFFER_FLAG_END_OF_STREAM")
                    break
                }
            }
            decoder!!.stop()
            decoder!!.release()
            extractor!!.release()
        }

    }

    companion object {
        private val SAMPLE =
            Environment.getExternalStorageDirectory().toString() + "/video.mp4"
    }
}