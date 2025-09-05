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

package org.cangnova.cangjie.name

class CangJieClassName private constructor( // Internal name:  kotlin/Map$Entry
    // FqName:         kotlin.Map.Entry
    val internalName: String,
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
            return if (packageFqName.isRoot) {
                relativeClassName
            } else {
                packageFqName.asString().replace('.', '/') + "/" + relativeClassName
            }
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
