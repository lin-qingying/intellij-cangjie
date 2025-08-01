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

package cn.cangnova.cangjie.types

import cn.cangnova.cangjie.builtins.CangJieBuiltIns
import cn.cangnova.cangjie.descriptors.ClassifierDescriptor
import cn.cangnova.cangjie.descriptors.TypeParameterDescriptor
import cn.cangnova.cangjie.types.checker.CangJieTypeRefiner
import cn.cangnova.cangjie.types.error.ErrorTypeKind

val CangJieType?.isFunctionPlaceholder: Boolean
    get() {
        return this != null && constructor is FunctionPlaceholderTypeConstructor
    }
class FunctionPlaceholderTypeConstructor(
    val argumentTypes: List<CangJieType>,
    val hasDeclaredArguments: Boolean,
    private val cangjieBuiltIns: CangJieBuiltIns
) : TypeConstructor {
    private val errorTypeConstructor: TypeConstructor =
        ErrorUtils.createErrorTypeConstructor(ErrorTypeKind.FUNCTION_PLACEHOLDER_TYPE, argumentTypes.toString())

    override fun getParameters(): List<TypeParameterDescriptor> {
        return errorTypeConstructor.parameters
    }
    override fun isFinal(): Boolean {
        return errorTypeConstructor.isFinal
    }

    override fun getSupertypes(): Collection<CangJieType> {
        return errorTypeConstructor.supertypes
    }


    override fun isDenotable(): Boolean {
        return errorTypeConstructor.isDenotable
    }

    override fun getDeclarationDescriptor(): ClassifierDescriptor? {
        return errorTypeConstructor.declarationDescriptor
    }

    override fun toString(): String {
        return errorTypeConstructor.toString()
    }

    override fun getBuiltIns(): CangJieBuiltIns {
        return cangjieBuiltIns
    }

    @TypeRefinement
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): TypeConstructor = this
}
