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
        if (index == 0) error("Index must be non-zero")
        return decls[index - 1]
    }


    /**
     * 根据Kind分组
     */
    val byTypeKind = decls.groupBy { it.kind }
}
