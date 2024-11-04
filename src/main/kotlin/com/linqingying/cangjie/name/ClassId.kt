package com.linqingying.cangjie.name

import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.utils.runIf

interface IClassId {
    val packageFqName: FqName
    val relativeClassName: FqName
    val isLocal: Boolean
    val shortClassName: Name
    fun asSingleFqName(): FqName
    val outerClassId: ClassId?
}
//包含构造器的classid  用于枚举构造器
data class ClassIdByConstructor(val classId: ClassId, val types: List<CangJieType> = emptyList()) : IClassId {
    override val packageFqName: FqName
        get() = classId.packageFqName
    override val relativeClassName: FqName
        get() = classId.relativeClassName
    override val isLocal: Boolean
        get() = classId.isLocal
    override val shortClassName: Name
        get() = Name.identifier(classId.shortClassName.identifier + "(" + types.size + ")")

    override fun asSingleFqName(): FqName {
        return classId.asSingleFqName()
    }

    override val outerClassId: ClassId?
        get() = classId.outerClassId

    override fun toString(): String {
        return classId.toString() + "(" + types.joinToString(",") { it.toString() } + ")"
    }
}

data class ClassId(
    override val packageFqName: FqName,
    override val relativeClassName: FqName,
    override val isLocal: Boolean
) : IClassId {
    constructor(packageFqName: FqName, topLevelName: Name) : this(
        packageFqName,
        FqName.topLevel(topLevelName),
        isLocal = false
    )

    init {
        assert(!relativeClassName.isRoot) { "Class name must not be root: " + packageFqName + if (isLocal) " (local)" else "" }
    }

    val parentClassId: ClassId?
        get() = runIf(isNestedClass) {
            ClassId(packageFqName, relativeClassName.parent(), isLocal)
        }

    override val shortClassName: Name
        get() = relativeClassName.shortName()

    override val outerClassId: ClassId?
        get() {
            val parent = relativeClassName.parent()
            return runIf(!parent.isRoot) { ClassId(packageFqName, parent, isLocal) }
        }

    val outermostClassId: ClassId
        get() {
            var name = relativeClassName
            while (!name.parent().isRoot) {
                name = name.parent()
            }
            return ClassId(packageFqName, name, isLocal = false)
        }

    val isNestedClass: Boolean
        get() = !relativeClassName.parent().isRoot

    fun createNestedClassId(name: Name): ClassId {
        return ClassId(packageFqName, relativeClassName.child(name), isLocal)
    }

    override fun asSingleFqName(): FqName {
        return if (packageFqName.isRoot) relativeClassName else FqName(packageFqName.asString() + "." + relativeClassName.asString())
    }

    fun startsWith(segment: Name): Boolean {
        return packageFqName.startsWith(segment)
    }


    fun asString(): String {
        return if (packageFqName.isRoot) {
            relativeClassName.asString()
        } else {
            buildString {
                append(packageFqName.asString().replace('.', '/'))
                append("/")
                append(relativeClassName.asString())
            }
        }
    }

    override fun equals(other: Any?): Boolean {


        if (other !is ClassId) return false
        return other.packageFqName == packageFqName && other.relativeClassName == relativeClassName
    }

    fun asFqNameString(): String {
        return if (packageFqName.isRoot) {
            relativeClassName.asString()
        } else {
            buildString {
                append(packageFqName.asString())
                append(".")
                append(relativeClassName.asString())
            }
        }
    }

    override fun toString(): String {
        return if (packageFqName.isRoot) "/" + asString() else asString()
    }

    override fun hashCode(): Int {
        var result = packageFqName.hashCode()
        result = 31 * result + relativeClassName.hashCode()
        return result
    }

    companion object {
        @JvmStatic
        fun topLevel(topLevelFqName: FqName): ClassId {
            return ClassId(topLevelFqName.parent(), topLevelFqName.shortName())
        }


        @JvmOverloads
        @JvmStatic
        fun fromString(string: String, isLocal: Boolean = false): ClassId {
            val lastSlashIndex = string.lastIndexOf("/")
            val packageName: String
            val className: String
            if (lastSlashIndex == -1) {
                packageName = ""
                className = string
            } else {
                packageName = string.substring(0, lastSlashIndex).replace('/', '.')
                className = string.substring(lastSlashIndex + 1)
            }
            return ClassId(FqName(packageName), FqName(className), isLocal)
        }
    }
}
