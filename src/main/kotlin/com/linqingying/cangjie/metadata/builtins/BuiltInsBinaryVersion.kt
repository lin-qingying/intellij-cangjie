/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.linqingying.cangjie.metadata.builtins

import com.linqingying.cangjie.metadata.deserialization.BinaryVersion
import java.io.DataInputStream
import java.io.InputStream
/**
 * 存储 `.cangjie_builtins` (`builtins.proto`) 文件的格式版本。此版本还包括核心 protobuf 消息 (`metadata.proto`) 的版本。
 *
 * 当以下情况发生时，必须提升此版本：
 * - 在 `builtins.proto` 中进行了不兼容的更改
 * - 在 `metadata.proto` 中进行了不兼容的更改
 * - 在内置序列化/反序列化逻辑中进行了不兼容的更改
 *
 * 版本提升必须遵守 [com.linqingying.cangjie.metadata.deserialization.BinaryVersion] 规则（参见 `BinaryVersion` KDoc）。
 */
class BuiltInsBinaryVersion(vararg numbers: Int) : BinaryVersion(*numbers) {
    override fun isCompatibleWithCurrentCompilerVersion(): Boolean =
        this.isCompatibleTo(INSTANCE)

    companion object {
        @JvmField
        val INSTANCE = BuiltInsBinaryVersion(0, 0, 1)

        @JvmField
        val INVALID_VERSION = BuiltInsBinaryVersion()

        fun readFrom(stream: InputStream): BuiltInsBinaryVersion {
            val dataInput = DataInputStream(stream)
            return BuiltInsBinaryVersion(*(1..dataInput.readInt()).map { dataInput.readInt() }.toIntArray())
        }
    }
}
