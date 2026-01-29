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
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

/*
 * Default in-memory ExtendManager implementation (skeleton).
 */
package org.cangnova.cangjie.resolve.extend

import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.types.*

/**
 * 扩展管理器实现
 *
 * 内存中的 ExtendManager 实现，维护模块内所有 extend 声明的映射关系。
 *
 * @param storageManager 存储管理器（可选，当前实现不使用）
 */
class ExtendManagerImpl(
    private val storageManager: StorageManager? = null,
) : ExtendManager {

    /**
     * 按类型构造器索引的扩展定义映射
     *
     * key: 被扩展类型的类型构造器
     * value: 该类型的所有扩展定义集合
     */
    private val defsByCtor = mutableMapOf<TypeConstructor, MutableSet<ExtendManager.ExtensionDef>>()

    /**
     * 按扩展 ID 索引的扩展定义映射
     *
     * 用于快速查找和替换已存在的扩展定义
     */
    private val defsById = mutableMapOf<String, ExtendManager.ExtensionDef>()

    /**
     * 缓存键
     */
    private data class Key(
        val ctor: TypeConstructor,
        val argsKey: List<CangJieType>,
        val excludeId: String?,
    )

    /**
     * 超类型查询结果缓存
     */
    private val cache = mutableMapOf<Key, Collection<CangJieType>>()

    override fun register(def: ExtendManager.ExtensionDef) {
        // 清除缓存，因为新的扩展可能影响查询结果
        cache.clear()

        // 如果已存在相同 ID 的扩展，先移除旧的
        defsById[def.id]?.let { oldDef ->
            defsByCtor[oldDef.extendedConstructor]?.remove(oldDef)
        }

        // 注册新的扩展定义
        defsById[def.id] = def
        defsByCtor.getOrPut(def.extendedConstructor) { linkedSetOf() }.add(def)
    }

    override fun rebuild(defs: Collection<ExtendManager.ExtensionDef>) {
        defsByCtor.clear()
        defsById.clear()
        cache.clear()
        defs.forEach { d ->
            defsById[d.id] = d
            defsByCtor.getOrPut(d.extendedConstructor) { linkedSetOf() }.add(d)
        }
    }

    override fun invalidate() {
        defsByCtor.clear()
        defsById.clear()
        cache.clear()
    }

    override fun getExtensionsForType(forConstructor: TypeConstructor): Collection<ExtendManager.ExtensionDef> {
        return defsByCtor[forConstructor].orEmpty()
    }

    override fun getAllExtensions(): Collection<ExtendManager.ExtensionDef> {
        return defsById.values
    }

    override fun getExtendSupertypes(
        forConstructor: TypeConstructor,
        forTypeArgs: List<CangJieType>,
        excludeExtendId: String?,
    ): Collection<CangJieType> {
        val key = Key(forConstructor, forTypeArgs, excludeExtendId)
        cache[key]?.let { return it }

        val defs = defsByCtor[forConstructor].orEmpty()
        if (defs.isEmpty()) return emptyList<CangJieType>().also { cache[key] = it }

        val result = buildList {
            defs.forEach { def ->
                if (excludeExtendId != null && def.id == excludeExtendId) return@forEach
                val substitutor = buildSubstitutor(def.typeParameters, forTypeArgs)
                def.interfaces.forEach { iface ->
                    // 仓颉语言中所有类型参数都是不变的，直接替换即可
                    val applied = substitutor.safeSubstitute(iface.unwrap())
                    add(applied)
                }
            }
        }.distinct()

        cache[key] = result
        return result
    }

    private fun buildSubstitutor(
        typeParams: List<TypeParameterDescriptor>,
        typeArgs: List<CangJieType>,
    ): ComposableTypeSubstitutor {
        val map = typeParams.zip(typeArgs).associate { (p, a) ->
            p.typeConstructor to a.unwrap()
        }
        return ComposableTypeSubstitutor.create(map)
    }
}

