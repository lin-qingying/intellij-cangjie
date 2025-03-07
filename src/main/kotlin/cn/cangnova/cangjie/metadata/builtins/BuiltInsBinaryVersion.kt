/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package cn.cangnova.cangjie.metadata.builtins

import cn.cangnova.cangjie.metadata.deserialization.BinaryVersion
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
 * 版本提升必须遵守 [cn.cangnova.cangjie.metadata.deserialization.BinaryVersion] 规则（参见 `BinaryVersion` CDoc）。
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
