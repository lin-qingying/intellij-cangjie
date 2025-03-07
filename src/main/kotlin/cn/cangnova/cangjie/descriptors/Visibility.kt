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

package cn.cangnova.cangjie.descriptors

import cn.cangnova.cangjie.name.FqName

abstract class Visibility protected constructor(
    val name: String,
    val isPublicAPI: Boolean
) {

    open val internalDisplayName: String
        get() = name

    open val externalDisplayName: String
        get() = internalDisplayName

    abstract fun mustCheckInImports(): Boolean

    open fun compareTo(visibility: Visibility): Int? {
        return Visibilities.compareLocal(this, visibility)
    }

    final override fun toString() = internalDisplayName

    open fun normalize(): Visibility = this

    // Should be overloaded in Java visibilities
    open fun customEffectiveVisibility(): EffectiveVisibility? = null

    open fun visibleFromPackage(fromPackage: FqName, myPackage: FqName): Boolean = true
}
