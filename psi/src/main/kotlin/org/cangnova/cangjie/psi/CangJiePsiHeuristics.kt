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

package org.cangnova.cangjie.psi

import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorConventions
import com.google.common.collect.HashMultimap
import kotlin.collections.orEmpty

object CangJiePsiHeuristics {
    @JvmStatic
    fun isPossibleOperator(declaration: CjNamedFunction): Boolean {
        if (declaration.hasModifier(CjTokens.OPERATOR_KEYWORD)) {
            return true
        } else if (!declaration.hasModifier(CjTokens.OVERRIDE_KEYWORD)) {
            // Operator modifier could be omitted only for overridden function
            return false
        }

        val name = declaration.name ?: return false
        return OperatorConventions.isConventionName(Name.identifier(name))
    }
    @JvmStatic
    fun isProbablyNothing(type: CjBasicType): Boolean {
        val referencedName = type.text

        if (referencedName == "Nothing") {
            return true
        }

        // TODO: why don't use PSI-less stub for calculating aliases?
        val file = type.getContainingCjFile()

        // TODO: support type aliases
        if (!file.hasImportAlias()) return false
        return file.aliasImportMap[referencedName].contains("Nothing")
    }
    @JvmStatic
    fun isProbablyNothing(typeReference: CjTypeReference): Boolean {
        return false
        val userType = typeReference.typeElement as? CjBasicType ?: return false
        return isProbablyNothing(userType)
    }

    private val CjFile.aliasImportMap by userDataCached("ALIAS_IMPORT_MAP_KEY") { file ->
        HashMultimap.create<String, String>().apply {
            for (directive in file.importDirectives) {
                for (item in directive.importItems) {
                    val aliasName = item.aliasName ?: continue
                    val name = item.importPath?.fqName?.shortName()?.asString() ?: continue
                    put(aliasName, name)
                }
            }
        }
    }
}
