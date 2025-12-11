/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.resolve

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.builtins.createFunctionType
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.DescriptorVisibilities.PUBLIC
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.impl.AbstractTypeParameterDescriptor
import org.cangnova.cangjie.descriptors.impl.SimpleFunctionDescriptorImpl
import org.cangnova.cangjie.descriptors.impl.ValueParameterDescriptorImpl
import org.cangnova.cangjie.diagnostics.Errors.UNSAFE_EXPRESSION_ERROR
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjUnsafeExpression
import org.cangnova.cangjie.resolve.calls.CallResolver
import org.cangnova.cangjie.resolve.calls.util.CallMaker
import org.cangnova.cangjie.resolve.descriptorUtil.builtIns
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeRefinement
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
import org.cangnova.cangjie.types.expressions.ExpressionTypingContext
import org.cangnova.cangjie.types.expressions.ExpressionTypingInternals
import org.cangnova.cangjie.types.expressions.ProcessingMode
import org.cangnova.cangjie.types.expressions.typeInfoFactory.createTypeInfo
import org.cangnova.cangjie.types.expressions.typeInfoFactory.noTypeInfo
import org.cangnova.cangjie.utils.exceptions.CangJieTypeInfo


class UnsafeExpressionResolver(
    val module: ModuleDescriptor,
    val callResolver: CallResolver,
    val facade: ExpressionTypingInternals,
    val languageVersionSettings: LanguageVersionSettings
) {


    fun resolveUnsafeExpression(expression: CjUnsafeExpression, context: ExpressionTypingContext): CangJieTypeInfo {

        val context = context.replaceProcessingMode(ProcessingMode.PARENT)

        val typeInfo = expression.block?.let { facade.getTypeInfo(it, context) } ?: noTypeInfo(context)
        return typeInfo

//        val callExpression = CjCallExpression(expression.node)
//
//        val call = CallMaker.makeCallForUnsafeExpression(expression)
//
//        val functionDescriptors = listOf(UnsafeFunctionDescriptor(module))
//
//        val resolutionResults =
//            callResolver.resolveCallExpressionWithGivenDescriptor(context, expression, call, functionDescriptors)
//        if (!resolutionResults.isSingleResult) {
//            return noTypeInfo(context)
//        }


//        return createTypeInfo(resolutionResults.resultingDescriptor.returnType, context)


    }

    // func spawn<T>(element:() -> T):Future<T>
    private class UnsafeFunctionDescriptor(module: ModuleDescriptor) : SimpleFunctionDescriptorImpl(
        module, null, Annotations.EMPTY, StandardNames.unsafeName,
        CallableMemberDescriptor.Kind.DECLARATION, SourceElement.NO_SOURCE
    ) {

        private class TypeParameterDescriptor(
            containingDeclaration: DeclarationDescriptor,
            annotations: Annotations,

            name: Name,
            index: Int,
            storageManager: StorageManager
        ) : AbstractTypeParameterDescriptor(
            storageManager,
            containingDeclaration,
            annotations,

            name,
            Variance.INVARIANT,
            index,
            SourceElement.NO_SOURCE,

            SupertypeLoopChecker.EMPTY,

            ) {
            private val upperBounds: List<CangJieType> = ArrayList<CangJieType>(1).apply {
                add(containingDeclaration.builtIns.defaultBound)
            }


            override fun reportSupertypeLoopError(type: CangJieType) {

            }

            override fun getTypeConstructor(): TypeConstructor {
                return object : TypeConstructor {
                    override fun getSupertypes(): List<CangJieType> {
                        return emptyList()
                    }

                    override fun equals(other: Any?): Boolean {
                        return this.hashCode() == other.hashCode()
                    }

                    override fun hashCode(): Int {
//                        为什么要固定hash值？ 因为在正常编码中的泛型是推断出来的，也就是说，类型的泛型参数与泛型约束的类型是同一个
//                        但是这里是自定义构建的方法，所以类型不是同一个，所以这里固定hash与原类型一致，让其正常工作
//                        也可以在使用原类型时直接替换泛型类型，这样就不需要固定hash值了
                        return 1764358125
                    }

                    override fun getBuiltIns(): CangJieBuiltIns {
                        return containingDeclaration.builtIns
                    }

                    override fun isDenotable(): Boolean {
                        return false
                    }

                    override fun toString(): String {
                        return "T"
                    }

                    override fun getDeclarationDescriptor(): ClassifierDescriptor {
                        return this@TypeParameterDescriptor

                    }


                    @TypeRefinement
                    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): TypeConstructor {
                        return this
                    }

                    override fun isFinal(): Boolean {
                        return true
                    }

                    override fun getParameters(): List<TypeParameterDescriptor> {
                        return emptyList()
                    }

                }
            }

            override fun getUpperBounds(): List<CangJieType> {
                return upperBounds
            }

            override fun resolveUpperBounds(): List<CangJieType> {
                return upperBounds
            }


            companion object {
                fun createWithDefaultBound(
                    containingDeclaration: DeclarationDescriptor,
                    annotations: Annotations,

                    name: Name,
                    index: Int,
                    storageManager: StorageManager
                ): TypeParameterDescriptor {
                    val typeParameterDescriptor = TypeParameterDescriptor(
                        containingDeclaration,
                        annotations,

                        name,
                        index,

                        storageManager
                    )

                    return typeParameterDescriptor
                }
            }
        }

        init {

            val T = TypeParameterDescriptor.createWithDefaultBound(
                this,
                Annotations.EMPTY,

                Name.identifier("T"),
                0,

                module.builtIns.storageManager
            )


            initialize(
                null, null, listOf(),

                listOf(T),
                listOf(
                    ValueParameterDescriptorImpl.createWithDestructuringDeclarations(
                        this,
                        null,
                        0,
                        Annotations.EMPTY,
                        Name.identifier("element"),
                        false,

                        createFunctionType(
                            module.builtIns, Annotations.EMPTY, null, emptyList(), emptyList(), null, T.defaultType
                        ),
                        false,
                        SourceElement.NO_SOURCE,
                        { emptyList() }
                    )), T.defaultType,
                Modality.FINAL,
                PUBLIC

            )
        }
    }
}

