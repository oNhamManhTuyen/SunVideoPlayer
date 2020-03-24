package com.tuyennm.decodevideo

import java.io.DataInput
import java.io.IOException
import java.nio.charset.Charset

object ReadUtils {

    @Throws(IOException::class)
    fun readString(`in`: DataInput, length: Int): String? {
        val string = ByteArray(length)
        `in`.readFully(string)
        return String(string, Charset.defaultCharset())
    }

    @Throws(IOException::class)
    fun readStringCut(`in`: DataInput, length: Int): String? {
        val string = ByteArray(length)
        `in`.readFully(string)
        val out = String(string)
        return out.substring(0, out.indexOf(0x00.toChar()))
    }

    @Throws(IOException::class)
    fun readFloats(`in`: DataInput, floats: FloatArray) {
        for (i in floats.indices) {
            floats[i] = `in`.readFloat()
        }
    }

    @Throws(IOException::class)
    fun readInts(`in`: DataInput, ints: IntArray) {
        for (i in ints.indices) {
            ints[i] = `in`.readInt()
        }
    }

    @Throws(IOException::class)
    fun readShorts(`in`: DataInput, shorts: ShortArray): ShortArray? {
        for (i in shorts.indices) {
            shorts[i] = `in`.readShort()
        }
        return shorts
    }
}