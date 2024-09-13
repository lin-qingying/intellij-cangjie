package com.huawei.cangjie.name

import com.huawei.cangjie.name.Name.Companion.guessByFirstCharacter
import com.huawei.cangjie.name.Name.Companion.special
import java.util.*
import java.util.regex.Pattern

import kotlin.math.max

class FqNameUnsafe {
    private val fqName: String


    @Transient
    private var safe: FqName? = null

    @Transient
    private var parent: FqNameUnsafe? = null

    @Transient
    private var shortName: Name? = null

    internal constructor(fqName: String, safe: FqName) {
        this.fqName = fqName
        this.safe = safe
    }

    constructor(fqName: String) {
        this.fqName = fqName
    }

    private constructor(fqName: String, parent: FqNameUnsafe, shortName: Name) {
        this.fqName = fqName
        this.parent = parent
        this.shortName = shortName
    }

    /**
     * 返回 shortName 的集合，按指定顺序排列。
     *
     * @param ascending 如果为 true，则返回正序，否则返回倒序。
     * @return shortName 的集合。
     */
    fun getShortNames(ascending: Boolean): List<Name?> {
        val shortNames: MutableList<Name?> = ArrayList()

        // 获取路径段
        val segments = pathSegments()
        for (segment in segments) {
            shortNames.add(segment)
        }

        // 根据 ascending 参数决定排序顺序
        if (!ascending) {
            Collections.reverse(shortNames)
        }

        return shortNames
    }

    private fun compute() {
        val lastDot = fqName.lastIndexOf('.')
        if (lastDot >= 0) {
            shortName = guessByFirstCharacter(fqName.substring(lastDot + 1))
            parent = FqNameUnsafe(fqName.substring(0, lastDot))
        } else {
            shortName = guessByFirstCharacter(fqName)
            parent = FqName.ROOT.toUnsafe()
        }
    }

    fun asString(): String {
        return fqName
    }


val isSafe: Boolean
    get() {return safe != null || asString().indexOf('<') < 0}

    fun toSafe(): FqName {
        if (safe != null) {
            return safe!!
        }
        safe = FqName(this)
        return safe!!
    }

    val isRoot: Boolean
        get() = fqName.isEmpty()
    //        return parent == null;

    fun parent(): FqNameUnsafe {
        if (parent != null) {
            return parent!!
        }

        check(!isRoot) { "root" }

        compute()

        return parent!!
    }

    fun child(name: Name): FqNameUnsafe {
        val childFqName = if (isRoot) {
            name.asString()
        } else {
            fqName + "." + name.asString()
        }
        return FqNameUnsafe(childFqName, this, name)
    }

    fun child(fqname: FqName): FqNameUnsafe {
        var _this = this
        for (name in fqname.pathSegments()) {
            _this = _this.child(name)
        }
        return _this
    }

    fun shortName(): Name {
        if (shortName != null) {
            return shortName!!
        }

        check(!isRoot) { "root" }

        compute()

        return shortName!!
    }

    fun shortNameOrSpecial(): Name {
        return if (isRoot) {
            ROOT_NAME
        } else {
            shortName()
        }
    }

    fun pathSegments(): List<Name> {
        return if (isRoot) emptyList() else SPLIT_BY_DOTS.split(fqName).map(STRING_TO_NAME)
    }

    fun startsWith(segment: Name): Boolean {
        if (isRoot) return false

        val firstDot = fqName.indexOf('.')
        val segmentAsString = segment.asString()
        return fqName.regionMatches(
            0,
            segmentAsString,
            0,
            if (firstDot == -1)
                max(fqName.length.toDouble(), segmentAsString.length.toDouble()).toInt()
            else
                firstDot
        )
    }

    fun startsWith(other: FqNameUnsafe): Boolean {
        if (isRoot) return false

        val thisLength = fqName.length
        val otherLength = other.fqName.length
        if (thisLength < otherLength) return false

        return (thisLength == otherLength || fqName[otherLength] == '.') &&
                fqName.regionMatches(0, other.fqName, 0, otherLength)
    }

    override fun toString(): String {
        return if (isRoot) ROOT_NAME.asString() else fqName
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o !is FqNameUnsafe) return false

        return fqName == o.fqName
    }

    override fun hashCode(): Int {
        return fqName.hashCode()
    }

    companion object {
        private val ROOT_NAME = special("<root>")
        private val SPLIT_BY_DOTS: Pattern = Pattern.compile("\\.")

        private val STRING_TO_NAME: (String ) -> Name =
            {   name: String -> guessByFirstCharacter(name) }

        fun isValid(qualifiedName: String?): Boolean {
            // TODO: ���ڴ���ת���ַ�����Ч����''
            return qualifiedName != null && qualifiedName.indexOf('/') < 0 && qualifiedName.indexOf('*') < 0
        }

        fun topLevel(shortName: Name): FqNameUnsafe {
            return FqNameUnsafe(shortName.asString(), FqName.ROOT.toUnsafe(), shortName)
        }
    }
}
