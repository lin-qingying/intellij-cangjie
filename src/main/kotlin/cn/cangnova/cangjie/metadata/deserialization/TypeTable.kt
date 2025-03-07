/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.metadata.deserialization

import cn.cangnova.cangjie.metadata.ProtoBuf

/**
 * 表示类型表的类。
 *
 * @param typeTable 包含类型信息的 ProtoBuf 类型表
 */
class TypeTable(typeTable: ProtoBuf.TypeTable) {
    /**
     * 包含类型列表的属性。
     * 如果 [typeTable] 中存在第一个可空索引，则从该索引开始的所有类型都将被标记为可空。
     */
    val types: List<ProtoBuf.Type> = run {
        val originalTypes = typeTable.typeList
        if (typeTable.hasFirstNullable()) {
            val firstNullable = typeTable.firstNullable
            typeTable.typeList.mapIndexed { i, type ->
                if (i >= firstNullable) {
                    type.toBuilder().setNullable(true).build()
                } else type
            }
        } else originalTypes
    }

    /**
     * 通过索引获取类型。
     *
     * @param index 索引
     * @return 对应索引的类型
     */
    operator fun get(index: Int) = types[index]
}
