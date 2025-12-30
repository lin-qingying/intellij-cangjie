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

package org.cangnova.cangjie.resolve.caches


import org.cangnova.cangjie.moduleinfo.ModuleInfo

interface BuiltInsCacheKey {
    object DefaultBuiltInsKey : BuiltInsCacheKey
}

/**
 * 仓颉模块的内置类型缓存键
 *
 * 基于 [AnalysisContext] 创建的缓存键，用于在不同的分析上下文之间共享或隔离内置类型。
 *
 * ## 缓存策略
 *
 * - 每个 [AnalysisContext] 对应一个唯一的缓存键
 * - 通过 [equals] 和 [hashCode] 确保缓存的正确性
 * - 用于 `CachedValuesManager` 等缓存系统
 *
 * ## 使用场景
 *
 * ```kotlin
 * val builtIns = cache.get(CangJieModuleBuiltInsKey(context)) {
 *     // 计算内置类型
 *     createBuiltIns(context)
 * }
 * ```
 *
 * @property context 关联的分析上下文
 * @see BuiltInsCacheKey
 * @see AnalysisContext
 */
class CangJieModuleBuiltInsKey(private val context: ModuleInfo) : BuiltInsCacheKey {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CangJieModuleBuiltInsKey) return false
        return context == other.context
    }

    override fun hashCode(): Int {
        return context.hashCode()
    }

    override fun toString(): String {
        return "CangJieModuleBuiltInsKey(context=$context)"
    }
}

private var _builtinsKey: CangJieModuleBuiltInsKey? = null
fun ModuleInfo.getKeyForBuiltIns(): BuiltInsCacheKey {
//    if (_builtinsKey == null) {
//        _builtinsKey = CangJieModuleBuiltInsKey(this)
//    }
//    return _builtinsKey!!

    return CangJieModuleBuiltInsKey(this)
}
