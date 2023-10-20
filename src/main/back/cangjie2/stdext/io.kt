
package com.huawei.cangjie.stdext

import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.DataInputOutputUtil
import java.io.*

@Throws(IOException::class)
fun DataInput.readVarInt(): Int =
    DataInputOutputUtil.readINT(this)

@Throws(IOException::class)
fun DataOutput.writeVarInt(value: Int): Unit =
    DataInputOutputUtil.writeINT(this, value)

@Throws(IOException::class)
fun OutputStream.writeStream(input: InputStream): Unit =
    FileUtil.copy(input, this)

@Throws(IOException::class)
fun <E : Enum<E>> DataOutput.writeEnum(e: E) = writeByte(e.ordinal)

@Throws(IOException::class)
inline fun <reified E : Enum<E>> DataInput.readEnum(): E = enumValues<E>()[readUnsignedByte()]

@Throws(IOException::class)
fun <T, E> DataInput.readCjResult(
    okReader: DataInput.() -> T,
    errReader: DataInput.() -> E
): CjResult<T, E> = when (readBoolean()) {
    true -> CjResult.Ok(okReader())
    false -> CjResult.Err(errReader())
}

@Throws(IOException::class)
fun <T, E> DataOutput.writeCjResult(
    value: CjResult<T, E>,
    okWriter: DataOutput.(T) -> Unit,
    errWriter: DataOutput.(E) -> Unit
): Unit = when (value) {
    is CjResult.Ok -> {
        writeBoolean(true)
        okWriter(value.ok)
    }
    is CjResult.Err -> {
        writeBoolean(false)
        errWriter(value.err)
    }
}
