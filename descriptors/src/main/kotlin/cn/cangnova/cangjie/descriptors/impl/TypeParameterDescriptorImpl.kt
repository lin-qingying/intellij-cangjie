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

package cn.cangnova.cangjie.descriptors.impl

import cn.cangnova.cangjie.descriptors.DeclarationDescriptor
import cn.cangnova.cangjie.descriptors.SourceElement
import cn.cangnova.cangjie.descriptors.SupertypeLoopChecker
import cn.cangnova.cangjie.descriptors.TypeParameterDescriptor
import cn.cangnova.cangjie.descriptors.annotations.Annotations
import cn.cangnova.cangjie.resolve.DescriptorUtils.getFqName
import cn.cangnova.cangjie.resolve.descriptorUtil.builtIns
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.Variance
import cn.cangnova.cangjie.types.isError

class TypeParameterDescriptorImpl private constructor(
    containingDeclaration: DeclarationDescriptor,
    annotations: Annotations,

    variance: Variance,
    name: Name,
    index: Int,
    source: SourceElement,
    private val reportCycleError:((CangJieType?)->Unit?)?,
    supertypeLoopsChecker: SupertypeLoopChecker,
    storageManager: StorageManager
) : AbstractTypeParameterDescriptor(
    storageManager,
    containingDeclaration,
    annotations,
    name,
    variance,  /*reified,*/
    index,
    source,
    supertypeLoopsChecker
) {
    var isInitialized: Boolean = false
        private set
    private val upperBounds: MutableList<CangJieType> = ArrayList(1)

    fun addUpperBound(bound: CangJieType) {
        checkUninitialized()
        doAddUpperBound(bound)
    }

    private fun doAddUpperBound(bound: CangJieType) {
        if (bound.isError) return
        upperBounds.add(bound) // TODO : Duplicates?
    }

    fun addDefaultUpperBound() {
        checkUninitialized()

        if (upperBounds.isEmpty()) {
            doAddUpperBound(containingDeclaration.builtIns.defaultBound)
        }
    }


    private fun nameForAssertions(): String {
        return name.toString() + " declared in " + getFqName(containingDeclaration)
    }

    private fun checkUninitialized() {
        check(!isInitialized) { "Type parameter descriptor is already initialized: " + nameForAssertions() }
    }

    fun setInitialized() {
        checkUninitialized()
        isInitialized = true
    }


    override fun reportSupertypeLoopError(type: CangJieType) {
        if (reportCycleError == null) return
        reportCycleError.invoke(type)
    }

    private fun checkInitialized() {
        check(isInitialized) { "Type parameter descriptor is not initialized: " + nameForAssertions() }
    }

    override fun resolveUpperBounds(): List<CangJieType> {
        checkInitialized()
        return upperBounds
    }


    companion object {
        @JvmStatic
        fun createWithDefaultBound(
            containingDeclaration: DeclarationDescriptor,
            annotations: Annotations,

            variance: Variance,
            name: Name,
            index: Int,
            storageManager: StorageManager
        ): TypeParameterDescriptor {
            val typeParameterDescriptor = createForFurtherModification(
                containingDeclaration,
                annotations,
                variance,
                name,
                index,
                SourceElement.NO_SOURCE,
                storageManager
            )
            typeParameterDescriptor.addUpperBound(containingDeclaration.builtIns.defaultBound)
            typeParameterDescriptor.setInitialized()
            return typeParameterDescriptor
        }

        @JvmStatic
        fun createForFurtherModification(
            containingDeclaration: DeclarationDescriptor,
            annotations: Annotations,

            variance: Variance,
            name: Name,
            index: Int,
            source: SourceElement,
            storageManager: StorageManager
        ): TypeParameterDescriptorImpl {
            return createForFurtherModification(
                containingDeclaration, annotations,  variance, name, index, source,
                null, SupertypeLoopChecker.EMPTY, storageManager
            )
        }
        @JvmStatic
        fun createForFurtherModification(
            containingDeclaration: DeclarationDescriptor,
            annotations: Annotations,
            variance: Variance,
            name: Name,
            index: Int,
            source: SourceElement,
            reportCycleError: ((CangJieType?)->Unit?)?,
            supertypeLoopsResolver: SupertypeLoopChecker,
            storageManager: StorageManager
        ): TypeParameterDescriptorImpl {
            return TypeParameterDescriptorImpl(
                containingDeclaration, annotations, variance, name,
                index, source, reportCycleError, supertypeLoopsResolver, storageManager
            )
        }
    }
}
