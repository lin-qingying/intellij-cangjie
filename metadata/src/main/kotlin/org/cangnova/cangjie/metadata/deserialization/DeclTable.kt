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

package org.cangnova.cangjie.metadata.deserialization

import org.cangnova.cangjie.metadata.model.fb.FbDecl


class DeclTable(val decls: List<FbDecl>) {

    operator fun get(indexs: List<Int>): List<FbDecl> {
        if(indexs.isEmpty()) return emptyList()
        assert(indexs.any { it != 0 })
        return indexs.map { this[it] }
    }

    operator fun get(index: UInt): FbDecl {

        return get(index.toInt())
    }
    operator fun get(index: Int): FbDecl {
        if (index == 0) {
            error("Index must be non-zero")
        }
        return decls[index - 1]
    }


    /**
     * 根据Kind分组
     */
    val byTypeKind = decls.groupBy { it.kind }
}
