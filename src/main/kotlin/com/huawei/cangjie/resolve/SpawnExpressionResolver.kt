package com.huawei.cangjie.resolve

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.builtins.StandardNames
import com.huawei.cangjie.builtins.createFunctionType
import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.descriptors.DescriptorVisibilities.PUBLIC
import com.huawei.cangjie.descriptors.annotations.Annotations
import com.huawei.cangjie.descriptors.impl.AbstractTypeParameterDescriptor
import com.huawei.cangjie.descriptors.impl.SimpleFunctionDescriptorImpl
import com.huawei.cangjie.descriptors.impl.ValueParameterDescriptorImpl
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.CjSpawnExpression
import com.huawei.cangjie.resolve.calls.CallResolver
import com.huawei.cangjie.resolve.calls.util.CallMaker
import com.huawei.cangjie.resolve.descriptorUtil.builtIns
import com.huawei.cangjie.storage.StorageManager
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.TypeConstructor
import com.huawei.cangjie.types.TypeRefinement
import com.huawei.cangjie.types.Variance
import com.huawei.cangjie.types.checker.CangJieTypeRefiner
import com.huawei.cangjie.types.expressions.ExpressionTypingContext
import com.huawei.cangjie.types.expressions.ProcessingMode
import com.huawei.cangjie.types.expressions.typeInfoFactory.createTypeInfo
import com.huawei.cangjie.types.expressions.typeInfoFactory.noTypeInfo
import com.huawei.cangjie.utils.exceptions.CangJieTypeInfo

class SpawnExpressionResolver(
    val module: ModuleDescriptor,
    val callResolver: CallResolver,

    val languageVersionSettings: LanguageVersionSettings
) {
    fun resolveSpawnExpression(expression: CjSpawnExpression, context: ExpressionTypingContext): CangJieTypeInfo {

      val  context =  context.replaceProcessingMode(ProcessingMode.PARENT)
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

//                override fun isSameClassifier(classifier: ClassifierDescriptor): Boolean {
//                    return false
//                }

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
                listOf(ValueParameterDescriptorImpl.createWithDestructuringDeclarations(
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

