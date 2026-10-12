// SPDX-License-Identifier: GPL-3.0-or-later
package com.apkcategorychecker.apk

import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Minimal reader for the DEX format: extracts the names of the classes defined in a
 * `classes*.dex` file without decompiling anything.
 *
 * See https://source.android.com/docs/core/runtime/dex-format
 */
object DexReader {

    /** Returns the fully qualified names (e.g. `org.apache.cordova.CordovaActivity`) of the defined classes. */
    fun classNames(dex: ByteArray): List<String> {
        if (dex.size < HEADER_SIZE || !dex.copyOfRange(0, 4).contentEquals(MAGIC)) {
            throw ApkFormatException("not a DEX file")
        }
        val buf = ByteBuffer.wrap(dex).order(ByteOrder.LITTLE_ENDIAN)
        try {
            val stringIdsOff = buf.getInt(0x3C)
            val typeIdsOff = buf.getInt(0x44)
            val classDefsSize = buf.getInt(0x60)
            val classDefsOff = buf.getInt(0x64)

            return List(classDefsSize) { i ->
                val typeIdx = buf.getInt(classDefsOff + i * CLASS_DEF_SIZE)
                val descriptorIdx = buf.getInt(typeIdsOff + typeIdx * 4)
                val stringDataOff = buf.getInt(stringIdsOff + descriptorIdx * 4)
                descriptorToClassName(readMutf8(dex, stringDataOff))
            }
        } catch (e: IndexOutOfBoundsException) {
            throw ApkFormatException("truncated or corrupted DEX file", e)
        } catch (e: BufferUnderflowException) {
            throw ApkFormatException("truncated or corrupted DEX file", e)
        }
    }

    /** `Lorg/apache/cordova/CordovaActivity;` -> `org.apache.cordova.CordovaActivity` */
    private fun descriptorToClassName(descriptor: String): String =
        descriptor.removePrefix("L").removeSuffix(";").replace('/', '.')

    /** Reads a string_data_item: a ULEB128 length in UTF-16 units followed by NUL-terminated MUTF-8. */
    private fun readMutf8(dex: ByteArray, offset: Int): String {
        var pos = offset
        while (dex[pos].toInt() and 0x80 != 0) pos++ // skip the ULEB128 utf16_size
        pos++

        val out = StringBuilder()
        while (true) {
            val a = dex[pos++].toInt() and 0xFF
            when {
                a == 0 -> return out.toString()
                a < 0x80 -> out.append(a.toChar())
                a and 0xE0 == 0xC0 -> {
                    val b = dex[pos++].toInt() and 0x3F
                    out.append((((a and 0x1F) shl 6) or b).toChar())
                }
                a and 0xF0 == 0xE0 -> {
                    val b = dex[pos++].toInt() and 0x3F
                    val c = dex[pos++].toInt() and 0x3F
                    out.append((((a and 0x0F) shl 12) or (b shl 6) or c).toChar())
                }
                else -> throw ApkFormatException("invalid MUTF-8 byte 0x%02x at offset %d".format(a, pos - 1))
            }
        }
    }

    private val MAGIC = byteArrayOf('d'.code.toByte(), 'e'.code.toByte(), 'x'.code.toByte(), '\n'.code.toByte())
    private const val HEADER_SIZE = 0x70
    private const val CLASS_DEF_SIZE = 0x20
}
