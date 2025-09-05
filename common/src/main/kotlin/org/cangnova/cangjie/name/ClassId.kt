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

import com.intellij.util.runIf

interface IClassId {
    val packageFqName: FqName
    val relativeClassName: FqName
    val isLocal: Boolean
    val shortClassName: Name
    fun asSingleFqName(): FqName
    val outerClassId: ClassId?
}

data class ClassId(
    override val packageFqName: FqName,
    override val relativeClassName: FqName,
    override val isLocal: Boolean,
) : IClassId {
    constructor(packageFqName: FqName, topLevelName: Name) : this(
        packageFqName,

        FqName.topLevel(topLevelName),
        isLocal = false,
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
