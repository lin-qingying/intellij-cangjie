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

package org.cangnova.cangjie.protodebugger.memory.vfs

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.WritingAccessProvider

/**
 * 内存视图文件写入访问控制
 *
 * 控制哪些内存文件可以被写入。
 * 一般情况下，调试器运行时的内存是可写的，但某些情况下需要限制：
 * - 调试器未连接
 * - 目标进程已终止
 * - 某些受保护的内存区域（如代码段）
 */
class MemoryViewFileWritingAccessProvider : WritingAccessProvider() {

    @Deprecated("Deprecated in Java")
    override fun requestWriting(vararg files: VirtualFile): Collection<VirtualFile> {
        // 返回不允许写入的文件列表
        val deniedFiles = mutableListOf<VirtualFile>()

        files.forEach { file ->
            if (file is MemoryViewFile<*>) {
                if (!isWritable(file)) {
                    deniedFiles.add(file)
                }
            }
        }

        return deniedFiles
    }

    override fun isPotentiallyWritable(file: VirtualFile): Boolean {
        // 快速检查：内存文件理论上可写
        return file !is MemoryViewFile<*>
    }

    /**
     * 检查内存文件是否可写
     */
    private fun isWritable(file: MemoryViewFile<*>): Boolean {
        return false
        // TODO: 检查调试器状态

    }

    /**
     * 检查内存区域是否可写
     */
    private fun isMemoryRegionWritable(range: org.cangnova.cangjie.protodebugger.memory.AddressRange): Boolean {
        // TODO: 查询调试器，检查内存保护标志
        // 例如：只读、不可执行等
        return true
    }
}

/**
 * 内存区域权限
 */
enum class MemoryRegionPermission {
    READ,           // 可读
    WRITE,          // 可写
    EXECUTE,        // 可执行
    READ_WRITE,     // 读写
    READ_EXECUTE,   // 读执行
    ALL             // 全部权限
}

/**
 * 内存区域信息（从调试器获取）
 */
data class MemoryRegionInfo(
    val range: org.cangnova.cangjie.protodebugger.memory.AddressRange,
    val permissions: Set<MemoryRegionPermission>,
    val name: String? = null,    // 区域名称（如"[stack]", "[heap]"）
    val file: String? = null      // 映射的文件（如共享库）
) {
    /**
     * 是否可读
     */
    val isReadable: Boolean
        get() = permissions.contains(MemoryRegionPermission.READ) ||
                permissions.contains(MemoryRegionPermission.READ_WRITE) ||
                permissions.contains(MemoryRegionPermission.READ_EXECUTE) ||
                permissions.contains(MemoryRegionPermission.ALL)

    /**
     * 是否可写
     */
    val isWritable: Boolean
        get() = permissions.contains(MemoryRegionPermission.WRITE) ||
                permissions.contains(MemoryRegionPermission.READ_WRITE) ||
                permissions.contains(MemoryRegionPermission.ALL)

    /**
     * 是否可执行
     */
    val isExecutable: Boolean
        get() = permissions.contains(MemoryRegionPermission.EXECUTE) ||
                permissions.contains(MemoryRegionPermission.READ_EXECUTE) ||
                permissions.contains(MemoryRegionPermission.ALL)
}