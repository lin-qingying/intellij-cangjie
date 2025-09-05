package org.cangnova.cangjie.metadata.deserialization

import org.cangnova.cangjie.metadata.model.Decl
import org.cangnova.cangjie.metadata.model.SemaTy


class DeclTable(val decls: List<Decl>) {

    operator fun get(indexs: List<Int>): List<Decl> {
        if(indexs.isEmpty()) return emptyList()
        assert(indexs.any { it != 0 })
        return indexs.map { this[it] }
    }

    operator fun get(index: Int): Decl {
        if (index == 0) error("Index must be non-zero")
        return decls[index - 1]
    }


    /**
     * 根据Kind分组
     */
    val byTypeKind = decls.groupBy { it.kind }
}
