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

package cn.cangnova.cangjie.types.expressions

import cn.cangnova.cangjie.builtins.CangJieBuiltIns
import cn.cangnova.cangjie.descriptors.BindingTrace
import cn.cangnova.cangjie.descriptors.ClassifierDescriptor
import cn.cangnova.cangjie.diagnostics.Errors
import cn.cangnova.cangjie.psi.CjTypeReference
import cn.cangnova.cangjie.resolve.BindingContext
import cn.cangnova.cangjie.resolve.PossiblyBareType
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.ErrorUtils.createErrorType
import cn.cangnova.cangjie.types.TypeConstructor
import cn.cangnova.cangjie.types.TypeReconstructionResult
import cn.cangnova.cangjie.types.error.ErrorTypeKind

object TypeReconstructionUtil {

    private fun allProjectionsString(constructor: TypeConstructor): String {
        val size: Int = constructor.parameters.size
        assert(size != 0) { "No projections possible for a nilary type constructor$constructor" }
        val declarationDescriptor: ClassifierDescriptor =
            checkNotNull(constructor.declarationDescriptor) { "No declaration descriptor for type constructor $constructor" }
        val name: String = declarationDescriptor.name.asString()

        return getTypeNameAndProjectionsString(
            name,
            size
        )
    }

    fun getTypeNameAndProjectionsString(name: String, size: Int): String {
        val builder = StringBuilder(name)
        builder.append("<")
        for (i in 0 until size) {
            builder.append("*")
            if (i == size - 1) break
            builder.append(", ")
        }
        builder.append(">")

        return builder.toString()
    }

    fun reconstructBareType(
        right: CjTypeReference,
        possiblyBareTarget: PossiblyBareType,
        subjectType: CangJieType?,
        trace: BindingTrace,
        builtIns: CangJieBuiltIns
    ): CangJieType {
        var subjectType: CangJieType? = subjectType
        if (subjectType == null) {
            // Recovery: let's reconstruct as if we were casting from Any, to get some type there
            subjectType = builtIns.anyType
        }
        val reconstructionResult: TypeReconstructionResult =
            possiblyBareTarget.reconstruct(subjectType)
        if (!reconstructionResult.isAllArgumentsInferred) {
            val typeConstructor: TypeConstructor =
                possiblyBareTarget.bareTypeConstructor
            trace.report(
                Errors.NO_TYPE_ARGUMENTS_ON_RHS.on(
                    right,
                    typeConstructor.parameters.size,
                    allProjectionsString(
                        typeConstructor
                    )
                )
            )
        }

        val targetType = reconstructionResult.resultingType
        if (targetType != null) {
            if (possiblyBareTarget.isBare) {
                trace.record<CjTypeReference, CangJieType>(
                    BindingContext.TYPE,
                    right,
                    targetType
                )
            }
            return targetType
        }

        return createErrorType(
            ErrorTypeKind.ERROR_WHILE_RECONSTRUCTING_BARE_TYPE,
            right.text
        )
    }

}
