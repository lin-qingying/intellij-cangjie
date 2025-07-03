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


import cn.cangnova.cangjie.mpp.TypeParameterSymbolMarker
import cn.cangnova.cangjie.storage.StorageManager
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.TypeConstructor
import cn.cangnova.cangjie.types.Variance
import cn.cangnova.cangjie.types.model.TypeParameterMarker

interface TypeParameterDescriptor : ClassifierDescriptor, TypeParameterMarker, TypeParameterSymbolMarker {
    //    boolean isReified();

    val variance: Variance


    val upperBounds: MutableList<CangJieType>


    override val typeConstructor: TypeConstructor

    override val original: TypeParameterDescriptor


    val index: Int

    /**
     * Is current parameter just a copy of another type parameter (getOriginal) from outer declaration
     * to be used for type constructor of inner declaration (i.e. inner class).
     *
     * If this method returns true:
     * 1. Containing declaration for current parameter is the inner one
     * 2. 'getOriginal' returns original type parameter from outer declaration
     * 3. 'getTypeConstructor' is the same as for original declaration (at least in means of 'equals')
     */
    val isCapturedFromOuterDeclaration: Boolean


    val storageManager: StorageManager
}
