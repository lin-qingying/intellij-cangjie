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
 */

package org.cangnova.cangjie.project.workspace.compat

import com.intellij.platform.workspace.jps.entities.ExcludeUrlEntity
import com.intellij.platform.workspace.jps.entities.ExcludeUrlEntityBuilder
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntityBuilder
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.url.VirtualFileUrl

/**
 * Workspace Model 兼容层 - Builder 版本 (253+)
 *
 * 此文件用于 IntelliJ Platform 2025.3 及以后版本。
 * 在这些版本中，ExcludeUrlEntity 和 SourceRootEntity 是工厂函数，返回对应的 Builder。
 */

/**
 * ExcludeUrl 兼容类型别名
 * 在 253+ 版本中，这是 ExcludeUrlEntityBuilder
 */
typealias ExcludeUrlCompat = ExcludeUrlEntityBuilder

/**
 * SourceRoot 兼容类型别名
 * 在 253+ 版本中，这是 SourceRootEntityBuilder
 */
typealias SourceRootCompat = SourceRootEntityBuilder

/**
 * 创建 ExcludeUrl 实体
 *
 * @param url 要排除的虚拟文件 URL
 * @param entitySource 实体来源
 * @return ExcludeUrlEntityBuilder 实例
 */
fun createExcludeUrl(
    url: VirtualFileUrl,
    entitySource: EntitySource
): ExcludeUrlCompat {
    return ExcludeUrlEntity(
        url = url,
        entitySource = entitySource
    )
}

/**
 * 创建 SourceRoot 实体
 *
 * @param url 源码根目录的虚拟文件 URL
 * @param rootTypeId 源码根类型 ID
 * @param entitySource 实体来源
 * @return SourceRootEntityBuilder 实例
 */
fun createSourceRoot(
    url: VirtualFileUrl,
    rootTypeId: SourceRootTypeId,
    entitySource: EntitySource
): SourceRootCompat {
    return SourceRootEntity(
        url = url,
        rootTypeId = rootTypeId,
        entitySource = entitySource
    )
}
