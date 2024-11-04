package com.linqingying.cangjie.name

import com.linqingying.cangjie.name.Name.Companion.identifier
import com.linqingying.cangjie.utils.join


class FqName {
    private val fqName: FqNameUnsafe

    @Transient
    private var parent: FqName? = null


    constructor(fqName: String) {
        this.fqName = FqNameUnsafe(fqName, this)
    }

    constructor(fqName: FqNameUnsafe) {
        this.fqName = fqName
    }

    private constructor(fqName: FqNameUnsafe, parent: FqName) {
        this.fqName = fqName
        this.parent = parent
    }

    fun asString(): String {
        return fqName.asString()
    }

    fun toUnsafe(): FqNameUnsafe {
        return fqName
    }

    val isRoot: Boolean
        get() = fqName.isRoot
    //        return parent == null;

    fun parent(): FqName {
        if (parent != null) {
            return parent!!
        }

        check(!isRoot) { "root" }

        parent = FqName(fqName.parent())

        return parent!!
    }

    val isModuleName: Boolean
        get() {
            if (parent == null) {
                return true
            }
            return parent!!.isRoot
        }

    val moduleName : Name get()   {
        var _this: FqName? = this
        while (true) {
            if (_this!!.parent().isRoot) return _this.shortName()
            _this = _this.parent
        }
    }

    fun child(name: Name): FqName {
        return FqName(fqName.child(name), this)
    }

    fun child(name: FqName): FqName {
        return FqName(fqName.child(name), this)
    }

    fun shortName(): Name {
        return fqName.shortName()
    }

    fun shortNameOrSpecial(): Name {
        return fqName.shortNameOrSpecial()
    }

    fun pathSegments(): List<Name> {
        return fqName.pathSegments()
    }

    fun startsWith(segment: Name): Boolean {
        return fqName.startsWith(segment)
    }

    fun startsWith(other: FqName): Boolean {
        return fqName.startsWith(other.fqName)
    }

    override fun toString(): String {
        return fqName.toString()
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o !is FqName) return false

        return fqName == o.fqName
    }

    override fun hashCode(): Int {
        return fqName.hashCode()
    }

    companion object {
        @JvmField
        val ROOT: FqName = FqName("")
        fun fromSegments(names: List<String >): FqName {
            return FqName(join(names, "."))
        }

        @JvmStatic
        fun topLevel(shortName: Name): FqName {
            return FqName(FqNameUnsafe.topLevel(shortName))
        }

        @JvmStatic
        fun fromString(fqName: String): FqName {
            val segments = fqName.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            var current = ROOT // 从根开始构建

            for (segment in segments) {
                current = current.child(identifier(segment)) // 创建子 FqName
            }

            return current // 返回最终的 FqName
        }
    }
}
