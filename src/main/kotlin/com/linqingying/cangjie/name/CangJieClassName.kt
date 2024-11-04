package com.linqingying.cangjie.name


class CangJieClassName private constructor(// Internal name:  kotlin/Map$Entry
    // FqName:         kotlin.Map.Entry
    val internalName: String
) {
    private var fqName: FqName? = null

    val fqNameForClassNameWithoutDollars: FqName
        /**
         * WARNING: internal name cannot be reliably converted to FQ name.
         *
         * This method treats all dollar characters ('$') in the internal name as inner class separators.
         * So it _will work incorrectly_ for classes where dollar characters are a part of the identifier.
         *
         * E.g. CangJieClassName("org/foo/bar/Baz$quux").getFqNameForClassNameWithoutDollars() -> FqName("org.foo.bar.Baz.quux")
         */
        get() {
            if (fqName == null) {
                this.fqName = FqName(internalName.replace('$', '.').replace('/', '.'))
            }
            return fqName!!
        }

    val fqNameForTopLevelClassMaybeWithDollars: FqName
        /**
         * WARNING: internal name cannot be reliably converted to FQ name.
         *
         * This method treats all dollar characters ('$') in the internal name as a part of the identifier.
         * So it _will work incorrectly_ for inner classes.
         *
         * E.g. CangJieClassName("org/foo/bar/Baz$quux").getFqNameForTopLevelClassMaybeWithDollars() -> FqName("org.foo.bar.Baz$quux")
         */
        get() = FqName(internalName.replace('/', '.'))

    val packageFqName: FqName
        get() {
            val lastSlash = internalName.lastIndexOf("/")
            if (lastSlash == -1) return FqName.ROOT
            return FqName(internalName.substring(0, lastSlash).replace('/', '.'))
        }

    override fun toString(): String {
        return internalName
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        return internalName == (o as CangJieClassName).internalName
    }

    override fun hashCode(): Int {
        return internalName.hashCode()
    }

    companion object {
        fun byInternalName(internalName: String): CangJieClassName {
            return CangJieClassName(internalName)
        }

        fun byClassId(classId: ClassId): CangJieClassName {
            return CangJieClassName(internalNameByClassId(classId))
        }

        fun internalNameByClassId(classId: ClassId): String {
            val packageFqName: FqName = classId.packageFqName
            val relativeClassName: String = classId.relativeClassName.asString().replace('.', '$')
            return if (packageFqName.isRoot)
                relativeClassName
            else
                packageFqName.asString().replace('.', '/') + "/" + relativeClassName
        }

        /**
         * WARNING: fq name cannot be uniquely mapped to JVM class name.
         */
        fun byFqNameWithoutInnerClasses(fqName: FqName): CangJieClassName {
            val r = CangJieClassName(fqName.asString().replace('.', '/'))
            r.fqName = fqName
            return r
        }

        fun byFqNameWithoutInnerClasses(fqName: String): CangJieClassName {
            return byFqNameWithoutInnerClasses(FqName(fqName))
        }
    }
}
