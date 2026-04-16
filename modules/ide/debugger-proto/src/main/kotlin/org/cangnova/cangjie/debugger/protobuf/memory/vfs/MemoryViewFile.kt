/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.debugger.protobuf.memory.vfs

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.testFramework.LightVirtualFile
import org.cangnova.cangjie.debugger.protobuf.memory.MemoryCell
import org.cangnova.cangjie.debugger.protobuf.memory.state.MemoryStore
import org.cangnova.cangjie.debugger.protobuf.memory.vfs.MemoryViewVirtualFileSystem.Companion.PROTOCOL_PREFIX

/**
 * 内存视图虚拟文件
 *
 * 表示内存中的一个区域，可以在IDE中像普通文件一样打开和编辑。
 *
 * 核心特性：
 * - 内容动态生成，不存储在磁盘
 * - 支持读写（写入会修改调试器中的内存）
 * - 集成文件类型系统（语法高亮、代码折叠等）
 * - 支持刷新（重新从调试器读取）
 *
 * @param fileSystem 所属文件系统
 * @param viewMode 视图模式（HEX 或 DISASM）
 * @param centerAddress 中心地址（当前光标/焦点位置）
 * @param facade 内存视图门面（用于访问内存数据）
 */
class MemoryViewFile<T : MemoryCell>(


    val store: MemoryStore<T>,

    ) : LightVirtualFile("") {  // 初始名称为空，由 TabTitleProvider 提供

    companion object {
        private val LOG = Logger.getInstance(MemoryViewFile::class.java)
    }


    init {
        // 设置文件类型
        fileType = store.fileType


    }


    override fun getFileSystem(): VirtualFileSystem = MemoryViewVirtualFileSystem.getInstance()

    override fun getPath(): String {

        return fileType.name
    }

    override fun getUrl(): String = PROTOCOL_PREFIX + path


    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other !is MemoryViewFile<T>) return false
        if (fileType != other.fileType) return false
        if (path != other.path) return false
        if (url != other.url) return false
        return store != other.store

    }


    override fun toString(): String {

        return "MemoryViewFile[$fileType]"
    }
}

/**
 * 文件类型 - 十六进制
 */
object HexdumpFileType : FileType {
    val INSTANCE = this

    override fun getName(): String = "CangJieMemoryHexdump"

    override fun getDescription(): String = "CangJie Memory Hexdump View"

    override fun getDefaultExtension(): String = "cj-hex"

    override fun getIcon(): javax.swing.Icon? = null // TODO: 添加图标

    override fun isBinary(): Boolean = false

    override fun isReadOnly(): Boolean = false
}

/**
 * 文件类型 - 反汇编
 */
object DisasmFileType : FileType {
    val INSTANCE = this

    override fun getName(): String = "CangJieMemoryDisassembly"

    override fun getDescription(): String = "CangJie Memory Disassembly View"

    override fun getDefaultExtension(): String = "cj-disasm"

    override fun getIcon(): javax.swing.Icon? = null // TODO: 添加图标

    override fun isBinary(): Boolean = false

    override fun isReadOnly(): Boolean = false
}