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

import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.types.*

class ExtendManagerImpl(
    private val storageManager: StorageManager,
    private val module: ModuleDescriptor,
) : ExtendManager {

    private val defsByCtor = mutableMapOf<TypeConstructor, MutableSet<ExtendManager.ExtensionDef>>()

    private data class Key(
        val ctor: TypeConstructor,
        val argsKey: List<CangJieType>,
        val excludeId: String?,
    )

    private val cache = mutableMapOf<Key, Collection<CangJieType>>()

    override fun rebuild(defs: Collection<ExtendManager.ExtensionDef>) {
        defsByCtor.clear()
        cache.clear()
        defs.forEach { d ->
            defsByCtor.getOrPut(d.extendedConstructor) { linkedSetOf() }.add(d)
        }
    }

    override fun invalidate() {
        defsByCtor.clear()
        cache.clear()
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
                    val applied = substitutor.substitute(iface)
                    if (applied != null) add(applied)
                }
            }
        }.distinct()

        cache[key] = result
        return result
    }

    private fun buildSubstitutor(
        typeParams: List<TypeParameterDescriptor>,
        typeArgs: List<CangJieType>,
    ): DefaultTypeSubstitutor {
        val pairs = typeParams.zip(typeArgs).associate { (p, a) -> p to TypeArgumentImpl(a) }
        val substitution = TypeConstructorSubstitution.createByParametersMap(pairs)
        return DefaultTypeSubstitutor.create(substitution)
    }
}

