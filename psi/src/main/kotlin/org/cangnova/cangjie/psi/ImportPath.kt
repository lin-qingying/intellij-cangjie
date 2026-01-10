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

import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.renderer.render

data class ImportPath @JvmOverloads constructor(val fqName: FqName, val isAllUnder: Boolean, val alias: Name? = null) {

    val pathStr: String
        get() = fqName.toUnsafe().render() + if (isAllUnder) ".*" else ""

    override fun toString(): String {
        return pathStr + if (alias != null) " as " + alias.asString() else ""
    }

    fun hasAlias(): Boolean {
        return alias != null
    }

    val importedName: Name?
        get() {
            if (!isAllUnder) {
                return alias ?: fqName.shortName()
            }

            return null
        }

    companion object {
        @JvmStatic
        fun fromString(pathStr: String): ImportPath {
            return if (pathStr.endsWith(".*")) {
                ImportPath(FqName(pathStr.substring(0, pathStr.length - 2)), isAllUnder = true)
            } else {
                ImportPath(FqName(pathStr), isAllUnder = false)
            }
        }
    }
}
