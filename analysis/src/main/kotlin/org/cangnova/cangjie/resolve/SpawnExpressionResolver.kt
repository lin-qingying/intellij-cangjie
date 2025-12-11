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
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.DescriptorVisibilities.PUBLIC
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.impl.AbstractTypeParameterDescriptor
import org.cangnova.cangjie.descriptors.impl.SimpleFunctionDescriptorImpl
import org.cangnova.cangjie.descriptors.impl.ValueParameterDescriptorImpl
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjSpawnExpression
import org.cangnova.cangjie.resolve.calls.CallResolver
import org.cangnova.cangjie.resolve.calls.util.CallMaker
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeConstructor
import org.cangnova.cangjie.types.Variance
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
import org.cangnova.cangjie.types.createFunctionType
import org.cangnova.cangjie.types.expressions.CangJieTypeInfo
import org.cangnova.cangjie.types.expressions.ExpressionTypingContext
import org.cangnova.cangjie.types.expressions.ProcessingMode
import org.cangnova.cangjie.types.expressions.typeInfoFactory.createTypeInfo
import org.cangnova.cangjie.types.expressions.typeInfoFactory.noTypeInfo

class SpawnExpressionResolver(
    val module: ModuleDescriptor,
    val callResolver: CallResolver,

    val languageVersionSettings: LanguageVersionSettings
) {
    fun resolveSpawnExpression(expression: CjSpawnExpression, context: ExpressionTypingContext): CangJieTypeInfo {

        val context = context.replaceProcessingMode(ProcessingMode.PARENT)
//        val callExpression = CjCallExpression(expression.node)

        val call = CallMaker.makeCallForSpawnExpression(expression)

        val functionDescriptors = listOf(SapwnFunctionDescriptor(module))

        val resolutionResults =
            callResolver.resolveCallExpressionWithGivenDescriptor(context, expression, call, functionDescriptors)
        if (!resolutionResults.isSingleResult) {
            return noTypeInfo(context)
        }



        return createTypeInfo(resolutionResults.resultingDescriptor.returnType, context)


    }

    // func spawn<T>(element:() -> T):Future<T>
    private class SapwnFunctionDescriptor(module: ModuleDescriptor) : SimpleFunctionDescriptorImpl(
        module, null, Annotations.EMPTY, StandardNames.spawnName,
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
            override val upperBounds: List<CangJieType> = ArrayList<CangJieType>(1).apply {
                add(containingDeclaration.builtIns.defaultBound)
            }


            override fun reportSupertypeLoopError(type: CangJieType) {

            }

            override val typeConstructor: TypeConstructor
                get() = object : TypeConstructor {

                    override val supertypes: Collection<CangJieType>
                        get() = emptyList()

                    override fun equals(other: Any?): Boolean {
                        return this.hashCode() == other.hashCode()
                    }


                    override val builtIns: CangJieBuiltIns
                        get() = containingDeclaration.builtIns


                    override val isDenotable: Boolean
                        get() = false

                    override fun toString(): String {
                        return "T"
                    }

                    override val declarationDescriptor: ClassifierDescriptor?
                        get() = this@TypeParameterDescriptor


//                override fun isSameClassifier(classifier: ClassifierDescriptor): Boolean {
//                    return false
//                }


                    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): TypeConstructor {
                        return this
                    }

                    override val isFinal: Boolean
                        get() = true

                    override val parameters: List<org.cangnova.cangjie.descriptors.TypeParameterDescriptor>
                        get() = emptyList()


                }


            override fun resolveUpperBounds(): List<CangJieType> {
                return upperBounds
            }


            companion object {
                fun createWithDefaultBound(
                    containingDeclaration: DeclarationDescriptor,
                    annotations: Annotations,  //            boolean reified,
                    variance: Variance,
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
//                typeParameterDescriptor.addUpperBound(containingDeclaration.builtIns.defaultBound)
//                typeParameterDescriptor.setInitialized()
                    return typeParameterDescriptor
                }
            }
        }

        init {

            val T = TypeParameterDescriptor.createWithDefaultBound(
                this,
                Annotations.EMPTY,
                Variance.INVARIANT,
                Name.identifier("T"),
                0,

                module.builtIns.storageManager
            )

            val futureType = module.builtIns.futureType
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
                )), futureType,
                Modality.FINAL,
                PUBLIC

            )
        }
    }
}

