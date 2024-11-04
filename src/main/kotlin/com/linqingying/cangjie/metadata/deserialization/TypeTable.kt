package com.linqingying.cangjie.metadata.deserialization

import com.linqingying.cangjie.metadata.ProtoBuf

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
