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

import com.intellij.openapi.diagnostic.Logger
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.types.*

private val LOG = Logger.getInstance(ExtendManagerImpl::class.java)

//TODO 现在有一个问题，当文件a中调用了文件b声明的扩展方法，这时候文件b中的扩展可能还没注册，同时扩展的可见性检查可能没有生效

/**
 * 扩展管理器实现
 *
 * 内存中的 ExtendManager 实现，维护模块内所有 extend 声明的映射关系。
 *
 */
class ExtendManagerImpl(

) : ExtendManager {

    /**
     * 按类型构造器索引的扩展定义映射
     *
     * key: 被扩展类型的类型构造器
     * value: 该类型的所有扩展定义集合
     */
    private val defsByCtor = mutableMapOf<TypeConstructor, MutableSet<ExtendManager.ExtensionDef>>()

    /**
     * 按类型名称索引的扩展定义映射（备用索引）
     *
     * 用于解决 PrimitiveTypeConstructor 与 ClassifierBasedTypeConstructor 的 equals() 不兼容问题。
     * 当 defsByCtor 查找失败时，通过类型名称进行备用查找。
     *
     * key: 被扩展类型的短名称（如 "Int64"）
     * value: 该类型名称的所有扩展定义集合
     */
    private val defsByTypeName = mutableMapOf<String, MutableSet<ExtendManager.ExtensionDef>>()

    /**
     * 按扩展 ID 索引的扩展定义映射
     *
     * 用于快速查找和替换已存在的扩展定义
     */
    private val defsById = mutableMapOf<String, ExtendManager.ExtensionDef>()

    /**
     * 扩展发现器
     *
     * 当查询某个类型的扩展但缓存为空时，会调用发现器来查找并解析扩展。
     * 这解决了缓存失效后扩展未被重新解析的问题。
     */
    private var discoverer: ExtensionDiscoverer? = null

    /**
     * 已发现的类型名称集合
     *
     * 用于避免重复调用发现器。一旦为某个类型名称调用过发现器，就不再重复调用。
     */
    private val discoveredTypeNames = mutableSetOf<String>()

    /**
     * 待解析的 PSI 元素映射
     *
     * key: 被扩展类型的短名称（如 "Int64"）
     * value: 该类型的所有待解析 PSI 元素集合（实际类型为 CjExtend）
     *
     * 这是延迟解析策略的核心：预加载时只注册 PSI，查询时才触发解析。
     */
    private val pendingPsiByTypeName = mutableMapOf<String, MutableSet<Any>>()

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

        val typeName = def.extendedConstructor.declarationDescriptor?.name?.asString()

        // 如果已存在相同 ID 的扩展，先移除旧的
        defsById[def.id]?.let { oldDef ->
            defsByCtor[oldDef.extendedConstructor]?.remove(oldDef)
            // 同时从 defsByTypeName 移除
            val oldTypeName = oldDef.extendedConstructor.declarationDescriptor?.name?.asString()
            if (oldTypeName != null) {
                defsByTypeName[oldTypeName]?.remove(oldDef)
            }
        }

        // 注册新的扩展定义
        defsById[def.id] = def
        defsByCtor.getOrPut(def.extendedConstructor) { linkedSetOf() }.add(def)

        // 同时注册到 defsByTypeName（备用索引）
        if (typeName != null) {
            defsByTypeName.getOrPut(typeName) { linkedSetOf() }.add(def)
        }

        if (LOG.isDebugEnabled) {
            val ctor = def.extendedConstructor
            LOG.debug(
                "Registered extend '${def.id}': " +
                    "constructor=${ctor::class.simpleName}@${System.identityHashCode(ctor)}, " +
                    "declarationDescriptor=${ctor.declarationDescriptor?.name}, " +
                    "hashCode=${ctor.hashCode()}, " +
                    "total keys in defsByCtor=${defsByCtor.keys.size}, " +
                    "total keys in defsByTypeName=${defsByTypeName.keys.size}"
            )
        }
    }

    override fun rebuild(defs: Collection<ExtendManager.ExtensionDef>) {
        defsByCtor.clear()
        defsByTypeName.clear()
        defsById.clear()
        cache.clear()
        defs.forEach { d ->
            defsById[d.id] = d
            defsByCtor.getOrPut(d.extendedConstructor) { linkedSetOf() }.add(d)
            // 同时注册到 defsByTypeName
            val typeName = d.extendedConstructor.declarationDescriptor?.name?.asString()
            if (typeName != null) {
                defsByTypeName.getOrPut(typeName) { linkedSetOf() }.add(d)
            }
        }
    }

    override fun invalidate() {
        defsByCtor.clear()
        defsByTypeName.clear()
        defsById.clear()
        cache.clear()
        discoveredTypeNames.clear()
        pendingPsiByTypeName.clear()
    }

    override fun setDiscoverer(discoverer: ExtensionDiscoverer?) {
        this.discoverer = discoverer
    }

    override fun registerPendingPsi(typeName: String, psi: Any) {
        pendingPsiByTypeName.getOrPut(typeName) { mutableSetOf() }.add(psi)
        if (LOG.isDebugEnabled) {
            LOG.debug("Registered pending PSI for type '$typeName', total pending for this type: ${pendingPsiByTypeName[typeName]?.size}")
        }
    }

    /**
     * 解析指定类型的待解析 PSI 元素
     *
     * 从 pendingPsiByTypeName 中取出并移除该类型的所有待解析 PSI，
     * 然后调用发现器来触发解析。
     *
     * @param typeName 类型名称
     */
    private fun resolvePendingPsi(typeName: String) {
        val pendingPsis = pendingPsiByTypeName.remove(typeName)
        if (pendingPsis.isNullOrEmpty()) return

        if (LOG.isDebugEnabled) {
            LOG.debug("Resolving ${pendingPsis.size} pending PSI(s) for type '$typeName'")
        }

        // 调用发现器来解析这些 PSI
        // 发现器会通过 stub 索引找到相同类型的扩展并解析它们
        discoverer?.discoverExtensions(typeName)
    }

    override fun getExtensionsForType(forConstructor: TypeConstructor): Collection<ExtendManager.ExtensionDef> {
        val typeName = forConstructor.declarationDescriptor?.name?.asString()

        // 如果有待解析的 PSI，先解析它们
        if (typeName != null && pendingPsiByTypeName.containsKey(typeName)) {
            resolvePendingPsi(typeName)
        }

        if (LOG.isDebugEnabled) {
            LOG.debug(
                "getExtensionsForType query: " +
                    "constructor=${forConstructor::class.simpleName}@${System.identityHashCode(forConstructor)}, " +
                    "declarationDescriptor=${forConstructor.declarationDescriptor?.name}, " +
                    "hashCode=${forConstructor.hashCode()}, " +
                    "defsByCtor keys=${defsByCtor.keys.map { "${it::class.simpleName}@${System.identityHashCode(it)}(hash=${it.hashCode()})" }}"
            )
        }

        // 首先尝试通过 TypeConstructor 直接查找
        val result = defsByCtor[forConstructor]
        if (result != null && result.isNotEmpty()) {
            if (LOG.isDebugEnabled) {
                LOG.debug("Found ${result.size} extend(s) via TypeConstructor lookup for ${forConstructor.declarationDescriptor?.name}")
            }
            return result
        }

        // TypeConstructor 查找失败，尝试通过类型名称备用查找
        // 这解决了 PrimitiveTypeConstructor 与 ClassifierBasedTypeConstructor 的 equals() 不兼容问题
        if (typeName != null) {
            val fallbackResult = defsByTypeName[typeName]
            if (fallbackResult != null && fallbackResult.isNotEmpty()) {
                if (LOG.isDebugEnabled) {
                    LOG.debug(
                        "TypeConstructor lookup failed, but found ${fallbackResult.size} extend(s) via typeName fallback for '$typeName'. " +
                            "This indicates TypeConstructor equals() mismatch between registration and query."
                    )
                }
                return fallbackResult
            }
        }

        // 如果没有找到扩展，尝试通过发现器查找并解析
        if (typeName != null && discoverer != null && typeName !in discoveredTypeNames) {
            if (LOG.isDebugEnabled) {
                LOG.debug("Triggering discoverer for type '$typeName'")
            }
            // 标记为已发现，避免重复调用
            discoveredTypeNames.add(typeName)
            // 调用发现器，这会触发扩展的解析并注册到本管理器
            discoverer?.discoverExtensions(typeName)

            // 发现后再次尝试查找（优先使用 TypeConstructor，失败后使用类型名称备用查找）
            val discoveredResult = defsByCtor[forConstructor]
            if (discoveredResult != null && discoveredResult.isNotEmpty()) {
                if (LOG.isDebugEnabled) {
                    LOG.debug("After discovery: found ${discoveredResult.size} extend(s) via TypeConstructor lookup")
                }
                return discoveredResult
            }

            // 备用查找
            val fallbackResult = defsByTypeName[typeName]
            if (fallbackResult != null && fallbackResult.isNotEmpty()) {
                if (LOG.isDebugEnabled) {
                    LOG.debug(
                        "After discovery: TypeConstructor lookup failed, but found ${fallbackResult.size} extend(s) via typeName fallback. " +
                            "defsByCtor keys=${defsByCtor.keys.map { "${it::class.simpleName}(hash=${it.hashCode()})" }}"
                    )
                }
                return fallbackResult
            }

            if (LOG.isDebugEnabled) {
                LOG.debug("After discovery: no extends found for type '$typeName'")
            }
            return emptyList()
        }

        if (LOG.isDebugEnabled && typeName != null && typeName in discoveredTypeNames) {
            LOG.debug("Type '$typeName' already discovered, but no extend found")
        }

        return emptyList()
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

        val typeName = forConstructor.declarationDescriptor?.name?.asString()

        // 先尝试通过 TypeConstructor 获取已注册的扩展
        var defs = defsByCtor[forConstructor].orEmpty()

        // TypeConstructor 查找失败，尝试通过类型名称备用查找
        if (defs.isEmpty() && typeName != null) {
            defs = defsByTypeName[typeName].orEmpty()
        }

        // 如果还是没有找到扩展，尝试通过发现器查找并解析
        if (defs.isEmpty()) {
            if (typeName != null && discoverer != null && typeName !in discoveredTypeNames) {
                discoveredTypeNames.add(typeName)
                discoverer?.discoverExtensions(typeName)
                // 发现后再次查找
                defs = defsByCtor[forConstructor].orEmpty()
                if (defs.isEmpty()) {
                    defs = defsByTypeName[typeName].orEmpty()
                }
            }
        }

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

