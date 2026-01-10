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

package org.cangnova.cangjie.types.functions

import org.cangnova.cangjie.name.FqName
import kotlin.text.iterator



class FunctionTypeKindExtractor(private val kinds: List<FunctionTypeKind>){
    fun getFunctionalClassKind(packageFqName: FqName, className: String): FunctionTypeKind? {
        return getFunctionalClassKindWithArity(packageFqName, className)?.kind
    }
    private val knownKindsByPackageFqName = kinds.groupBy { it.packageFqName }

    fun getFunctionalClassKindWithArity(packageFqName: FqName, className: String): KindWithArity? {
        val kinds = knownKindsByPackageFqName[packageFqName] ?: return null
        for (kind in kinds) {
            if (!className.startsWith(kind.classNamePrefix)) continue
            val arity = toInt(className.substring(kind.classNamePrefix.length)) ?: continue
            return KindWithArity(kind, arity)
        }
        return null
    }
    private fun toInt(s: String): Int? {
        if (s.isEmpty()) return null

        var result = 0
        for (c in s) {
            val d = c - '0'
            if (d !in 0..9) return null
            result = result * 10 + d
        }
        return result
    }
    data class KindWithArity(val kind: FunctionTypeKind, val arity: Int)

    companion object {
        /**
         * This instance should be used only in:
         *  - FE 1.0, since it does not support custom functional kinds from plugins
         *  - places in FIR where session is not accessible by design (like in renderer of FIR elements)
         */
        @JvmStatic

        val Default = FunctionTypeKindExtractor(
            listOf(
                FunctionTypeKind.Function,

            )
        )
    }
}
