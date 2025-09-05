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

import org.cangnova.cangjie.name.Name.Companion.identifier
import com.intellij.openapi.util.text.StringUtil.join

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

        check(!isRoot) { "this is root:${this.asString()}" }

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

    val moduleName: Name get() {
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
        fun fromSegments(names: List<String>): FqName {
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

