package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.builtins.StandardNames
import com.linqingying.cangjie.builtins.createFunctionType
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.DescriptorVisibilities.PUBLIC
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.descriptors.impl.AbstractTypeParameterDescriptor
import com.linqingying.cangjie.descriptors.impl.SimpleFunctionDescriptorImpl
import com.linqingying.cangjie.descriptors.impl.ValueParameterDescriptorImpl
import com.linqingying.cangjie.diagnostics.Errors.UNSAFE_EXPRESSION_ERROR
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.CjUnsafeExpression
import com.linqingying.cangjie.resolve.calls.CallResolver
import com.linqingying.cangjie.resolve.calls.util.CallMaker
import com.linqingying.cangjie.resolve.descriptorUtil.builtIns
import com.linqingying.cangjie.storage.StorageManager
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.TypeConstructor
import com.linqingying.cangjie.types.TypeRefinement
import com.linqingying.cangjie.types.Variance
import com.linqingying.cangjie.types.checker.CangJieTypeRefiner
import com.linqingying.cangjie.types.expressions.ExpressionTypingContext
import com.linqingying.cangjie.types.expressions.ProcessingMode
import com.linqingying.cangjie.types.expressions.typeInfoFactory.createTypeInfo
import com.linqingying.cangjie.types.expressions.typeInfoFactory.noTypeInfo
import com.linqingying.cangjie.utils.exceptions.CangJieTypeInfo


class UnsafeExpressionResolver(
    val module: ModuleDescriptor,
    val callResolver: CallResolver,

    val languageVersionSettings: LanguageVersionSettings
) {
    fun checkUnsafeExpression(expression: CjUnsafeExpression, context: ExpressionTypingContext) {
//        由于 unsafe并不是lambda 但是使用了lambda实现，所以这里检查lambda参数和箭头
        val lambda = expression.lambdaExpression ?: return
        lambda.parameterList ?: return

        context.trace.report(UNSAFE_EXPRESSION_ERROR.on(lambda.parameterList))


    }

    fun resolveUnsafeExpression(expression: CjUnsafeExpression, context: ExpressionTypingContext): CangJieTypeInfo {
        checkUnsafeExpression(expression, context)
        val context = context.replaceProcessingMode(ProcessingMode.PARENT)
//        val callExpression = CjCallExpression(expression.node)

        val call = CallMaker.makeCallForUnsafeExpression(expression)

        val functionDescriptors = listOf(UnsafeFunctionDescriptor(module))

        val resolutionResults =
            callResolver.resolveCallExpressionWithGivenDescriptor(context, expression, call, functionDescriptors)
        if (!resolutionResults.isSingleResult) {
            return noTypeInfo(context)
        }



        return createTypeInfo(resolutionResults.resultingDescriptor.returnType, context)


    }

    // func spawn<T>(element:() -> T):Future<T>
    private class UnsafeFunctionDescriptor(module: ModuleDescriptor) : SimpleFunctionDescriptorImpl(
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

