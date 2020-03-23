package com.tuyennm.decodevideo

import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.charset.Charset
import java.util.*
import kotlin.experimental.and

class MP4InputStream {
    private val `in`: InputStream?
    private val fin: RandomAccessFile?
    private val peeked: LinkedList<Byte> = LinkedList<Byte>()
    private var offset //only used with InputStream
            : Long = 0

    internal constructor(`in`: InputStream?) {
        this.`in` = `in`
        fin = null
        offset = 0
    }

    internal constructor(fin: RandomAccessFile?) {
        this.fin = fin
        `in` = null
    }

    @Throws(IOException::class)
    fun peek(): Int {
        var i = 0
        if (!peeked.isEmpty()) {
            i = peeked.remove().toInt() and MASK8
        } else if (`in` != null) {
            i = `in`.read()
        } else if (fin != null) {
            val currentFilePointer: Long = fin.filePointer
            i = try {
                fin.read()
            } finally {
                fin.seek(currentFilePointer)
            }
        }
        if (i == -1) {
            throw EOFException()
        }
        peeked.addFirst(i.toByte())
        return i
    }

    @Throws(IOException::class)
    fun read(): Int {
        var i = 0
        if (!peeked.isEmpty()) {
            i = peeked.remove().toInt() and MASK8
        } else if (`in` != null) {
            i = `in`.read()
        } else if (fin != null) {
            i = fin.read()
        }
        if (i == -1) {
            throw EOFException()
        } else if (`in` != null) {
            offset++
        }
        return i
    }

    @Throws(IOException::class)
    fun peek(b: ByteArray, off: Int, len: Int) {
        var read = 0
        var i = 0
        while (read < len && read < peeked.size) {
            b[off + read] = peeked.get(read)
            read++
        }
        var currentFilePointer: Long = -1
        if (fin != null) {
            currentFilePointer = fin.filePointer
        }
        try {
            while (read < len) {
                if (`in` != null) {
                    i = `in`.read(b, off + read, len - read)
                } else if (fin != null) {
                    i = fin.read(b, off + read, len - read)
                }
                read += if (i < 0) {
                    throw EOFException()
                } else {
                    for (j in 0 until i) {
                        peeked.add(b[off + j])
                    }
                    i
                }
            }
        } finally {
            if (fin != null) {
                fin.seek(currentFilePointer)
            }
        }
    }

    @Throws(IOException::class)
    fun read(b: ByteArray, off: Int, len: Int) {
        var read = 0
        var i = 0
        while (read < len && !peeked.isEmpty()) {
            b[off + read] = peeked.remove()
            read++
        }
        while (read < len) {
            if (`in` != null) i = `in`.read(b, off + read, len - read) else if (fin != null) i =
                fin.read(b, off + read, len - read)
            read += if (i < 0) throw EOFException() else i
        }
        offset += read.toLong()
    }

    @Throws(IOException::class)
    fun peekBytes(n: Int): Long {
        if (n < 1 || n > 8) throw IndexOutOfBoundsException("invalid number of bytes to read: $n")
        val b = ByteArray(n)
        peek(b, 0, n)
        var result: Long = 0
        for (i in 0 until n) {
            result = result shl 8 or (b[i].toLong() and 0xFF)
        }
        return result
    }

    @Throws(IOException::class)
    fun readBytes(n: Int): Long {
        if (n < 1 || n > 8) throw IndexOutOfBoundsException("invalid number of bytes to read: $n")
        val b = ByteArray(n)
        read(b, 0, n)
        var result: Long = 0
        for (i in 0 until n) {
            result = result shl 8 or (b[i].toLong() and 0xFF)
        }
        return result
    }

    @Throws(IOException::class)
    fun peekBytes(b: ByteArray) {
        peek(b, 0, b.size)
    }

    @Throws(IOException::class)
    fun readBytes(b: ByteArray) {
        read(b, 0, b.size)
    }

    @Throws(IOException::class)
    fun readString(n: Int): String {
        var i = -1
        var pos = 0
        val c = CharArray(n)
        while (pos < n) {
            i = read()
            c[pos] = i.toChar()
            pos++
        }
        return String(c, 0, pos)
    }

    @Throws(IOException::class)
    fun readUTFString(max: Int, encoding: String?): String {
        return String(readTerminated(max, 0), Charset.forName(encoding))
    }

    @Throws(IOException::class)
    fun readUTFString(max: Int): String {
        //read byte order mask
        val bom = ByteArray(2)
        read(bom, 0, 2)
        if (bom[0].toInt() == 0 || bom[1].toInt() == 0) return String()
        val i: Int = bom[0].toInt() shl 8 or bom[1].toInt()

        //read null-terminated
        val b = readTerminated(max - 2, 0)
        //copy bom
        val b2 = ByteArray(b.size + bom.size)
        System.arraycopy(bom, 0, b2, 0, bom.size)
        System.arraycopy(b, 0, b2, bom.size, b.size)
        return String(
            b2,
            Charset.forName(if (i == BYTE_ORDER_MASK) UTF16 else UTF8)
        )
    }

    @Throws(IOException::class)
    fun readTerminated(max: Int, terminator: Int): ByteArray {
        val b = ByteArray(max)
        var pos = 0
        var i = 0
        while (pos < max && i != -1) {
            i = read()
            if (i != -1) b[pos++] = i.toByte()
        }
        return b.copyOf(pos)
    }

    @Throws(IOException::class)
    fun readFixedPoint(m: Int, n: Int): Double {
        val bits = m + n
        require(bits % 8 == 0) { "number of bits is not a multiple of 8: " + (m + n) }
        val l = readBytes(bits / 8)
        val x = Math.pow(2.0, n.toDouble())
        return l.toDouble() / x
    }

    @Throws(IOException::class)
    fun skipBytes(n: Long) {
        var l: Long = 0
        while (l < n && !peeked.isEmpty()) {
            peeked.remove()
            l++
        }
        while (l < n) {
            if (`in` != null) l += `in`.skip(n - l) else if (fin != null) l += fin.skipBytes((n - l).toInt())
        }
        offset += l
    }

    @Throws(IOException::class)
    fun getOffset(): Long {
        var l: Long = -1
        if (`in` != null) l = offset else if (fin != null) l = fin.filePointer
        return l
    }

    @Throws(IOException::class)
    fun seek(pos: Long) {
        peeked.clear()
        if (fin != null) fin.seek(pos) else throw IOException("could not seek: no random access")
    }

    fun hasRandomAccess(): Boolean {
        return fin != null
    }

    @Throws(IOException::class)
    fun hasLeft(): Boolean {
        val b: Boolean
        if (!peeked.isEmpty()) {
            b = true
        } else if (fin != null) {
            b = fin.filePointer < fin.length() - 1
        } else {
            val i: Int = `in`!!.read()
            b = i != -1
            if (b) peeked.add(i.toByte())
        }
        return b
    }

    @Throws(IOException::class)
    fun close() {
        peeked.clear()
        if (`in` != null) `in`.close() else fin?.close()
    }

    companion object {
        const val MASK8 = 0xFF
        const val MASK16 = 0xFFFF
        const val UTF8 = "UTF-8"
        const val UTF16 = "UTF-16"
        private const val BYTE_ORDER_MASK = 0xFEFF
    }
}