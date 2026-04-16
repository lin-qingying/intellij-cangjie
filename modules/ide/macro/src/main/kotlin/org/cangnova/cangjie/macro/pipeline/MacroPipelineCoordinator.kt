/*
 * Copyright 2026 LinQingYing. and contributors.
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
 */

package org.cangnova.cangjie.macro.pipeline

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * 宏编译/展开管线协调器
 *
 * 统一管理宏编译和展开的时序关系，解决以下问题：
 * 1. **时序竞争**：确保展开任务在编译完成后才启动
 * 2. **增量感知**：区分宏源文件和普通文件的变更，按需触发重编译
 * 3. **产物检查**：展开前检查动态库是否存在，缺失时自动触发编译
 *
 * ## 使用场景
 *
 * - **项目同步**：`projectSynced` → [scheduleFullPipeline]
 * - **文件变更**：`.cj` 文件保存 → [scheduleIncrementalPipeline]
 * - **展开前检查**：[ensureMacroLibsReady] 确保产物可用
 */
interface MacroPipelineCoordinator {

    /**
     * 全量管线：编译所有宏包 → 清除缓存 → 全项目展开
     *
     * 用于项目同步完成后的完整刷新。
     * 快速连续调用时，新调用会取消旧的管线。
     */
    fun scheduleFullPipeline()

    /**
     * 增量管线：根据变更文件类型按需编译后展开
     *
     * - 宏源文件变更：重编译宏包 → 全项目展开
     * - 普通文件变更：检查产物 → 仅展开变更文件
     *
     * @param changedFiles 发生变更的文件列表
     */
    fun scheduleIncrementalPipeline(changedFiles: Collection<VirtualFile>)

    /**
     * 确保宏动态库可用
     *
     * 检查编译输出目录中是否存在 `lib-macro_*` 文件。
     * 如果缺失，触发一次编译并等待完成。
     *
     * @return true 表示产物就绪（或成功编译），false 表示编译失败或服务不可用
     */
    suspend fun ensureMacroLibsReady(): Boolean

    companion object {
        @JvmStatic
        fun getInstance(project: Project): MacroPipelineCoordinator {
            return project.getService(MacroPipelineCoordinator::class.java)
        }
    }
}
