package com.linqingying.cangjie.descriptors.impl.basic

import com.linqingying.cangjie.builtins.StandardNames
import com.linqingying.cangjie.builtins.StandardNames.FqNames.core
import com.linqingying.cangjie.builtins.StandardNames.FqNames.ctypeFqName
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.DescriptorVisibilities.PUBLIC
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.descriptors.impl.*
import com.linqingying.cangjie.incremental.components.LookupLocation
import com.linqingying.cangjie.incremental.components.NoLookupLocation
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.psi.CjPsiFactory
import com.linqingying.cangjie.psi.CjSuperTypeListEntry
import com.linqingying.cangjie.resolve.lazy.declarations.impl.PackageFragmentDescriptorBasicImpl
import com.linqingying.cangjie.resolve.lazy.descriptors.LazyExtendClassDescriptor
import com.linqingying.cangjie.resolve.scopes.DescriptorKindFilter
import com.linqingying.cangjie.resolve.scopes.LexicalScope
import com.linqingying.cangjie.resolve.scopes.MemberScope
import com.linqingying.cangjie.storage.StorageManager
import com.linqingying.cangjie.types.*
import com.linqingying.cangjie.types.checker.CangJieTypeRefiner
import com.linqingying.cangjie.utils.OperatorNameConventions.AND
import com.linqingying.cangjie.utils.OperatorNameConventions.ANDAND
import com.linqingying.cangjie.utils.OperatorNameConventions.DEC
import com.linqingying.cangjie.utils.OperatorNameConventions.DIV
import com.linqingying.cangjie.utils.OperatorNameConventions.EXPONENTIATION
import com.linqingying.cangjie.utils.OperatorNameConventions.INC
import com.linqingying.cangjie.utils.OperatorNameConventions.LEFT_SHIFT
import com.linqingying.cangjie.utils.OperatorNameConventions.MINUS
import com.linqingying.cangjie.utils.OperatorNameConventions.NOT
import com.linqingying.cangjie.utils.OperatorNameConventions.OR
import com.linqingying.cangjie.utils.OperatorNameConventions.OROR
import com.linqingying.cangjie.utils.OperatorNameConventions.PLUS
import com.linqingying.cangjie.utils.OperatorNameConventions.REM
import com.linqingying.cangjie.utils.OperatorNameConventions.RIGHT_SHIFT
import com.linqingying.cangjie.utils.OperatorNameConventions.TIMES
import com.linqingying.cangjie.utils.OperatorNameConventions.UNARY_MINUS
import com.linqingying.cangjie.utils.OperatorNameConventions.XOR
import com.linqingying.cangjie.utils.Printer

class BuiltInTypeDescriptor(
    basicMemberScope: PackageFragmentDescriptorBasicImpl.BasicMemberScope,
    storageManager: StorageManager,
    name: Name,
    private val parameters: List<TypeParameterDescriptor> = emptyList(),
//    如果后期有其他内置类型有泛型，可以在这里添加回调函数，目前只有CPointer有泛型，所以不做更改
) : BasicTypeDescriptor(basicMemberScope, storageManager, name) {
    override fun getEndConstructors(): Collection<ClassConstructorDescriptor> = emptySet()


    override val typeConstructor = BuiltInTypeConstructor(this, storageManager, parameters.toMutableList())


    override fun getDeclaredTypeParameters(): MutableList<TypeParameterDescriptor> {

        return typeConstructor.parameters.map {
            it.apply {
                this as TypeParameterDescriptorImpl
                if (!isInitialized) {
                    findCangJieTypeByFqName(storageManager.project, ctypeFqName)?.let { addUpperBound(it) }
                    setInitialized()
                }

            }
        }.toMutableList()
    }

    fun addParameter(parameters: TypeParameterDescriptor) {

        typeConstructor.addParameter(parameters)
    }

//    override val visibility: DescriptorVisibility
//        get() = DescriptorVisibilities.LOCAL

    val packageView = EmptyDeclarationDescriptor {


        basicMemberScope.getBuiltIns().builtInsModule.getPackage(core)
//        getPackageView(storageManager.project, core)
    }


    override val containingDeclaration: DeclarationDescriptor
        get() = packageView

    companion object {

        fun create(
            memberScope: PackageFragmentDescriptorBasicImpl.BasicMemberScope,
            storageManager: StorageManager,
            name: Name,
            parameters: List<TypeParameterDescriptor> = emptyList()
        ): BuiltInTypeDescriptor {
            return BuiltInTypeDescriptor(
                memberScope,
                storageManager,
                name,
                parameters.toMutableList()
            )
        }
    }
}

class EmptyDeclarationDescriptor(val getContainingDeclaration: (() -> DeclarationDescriptor?)?) :
    DeclarationDescriptor {
    override val original: DeclarationDescriptor
        get() = this
    override val containingDeclaration: DeclarationDescriptor?
        get() = getContainingDeclaration?.let { it() }


    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R? {
        return null
    }

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {

    }

    override val name: Name = Name.identifier("EmptyDeclarationDescriptor")

}

open class BasicTypeDescriptor(
    val basicMemberScope: PackageFragmentDescriptorBasicImpl.BasicMemberScope,
    val storageManager: StorageManager,
    override val name: Name
) : AbstractClassDescriptor(
    storageManager, name
), ClassDescriptorWithResolutionScopes {
    override fun getEndConstructors(): Collection<ClassConstructorDescriptor> = emptySet()

    private inner class OperatorFunctionDescriptor(
        functionName: Name,
        rightType: CangJieType?,
        returnType: CangJieType?
    ) : SimpleFunctionDescriptorImpl(
        this, null, Annotations.EMPTY, functionName,
        CallableMemberDescriptor.Kind.DECLARATION, SourceElement.NO_SOURCE
    ) {
        init {

            initialize(
                null, null, listOf(), listOf(

                ), listOfNotNull(
                    rightType?.let {
                        ValueParameterDescriptorImpl.createWithDestructuringDeclarations(
                            this,
                            null,
                            0,
                            Annotations.EMPTY,
                            Name.identifier("right"),
                            false,
                            rightType,
                            false,
                            SourceElement.NO_SOURCE,
                            { emptyList() }
                        )
                    }
                ), returnType,
                Modality.FINAL,
                PUBLIC

            )
        }

        override fun isOperator(): Boolean {
            return true
        }
    }

    private val operatorFunctions = mutableMapOf<Name, Collection<OperatorFunctionDescriptor>>()

    inner class BasicTypeMemberScope : MemberScope {
        override fun getContributedVariables(name: Name, location: LookupLocation): Collection<VariableDescriptor> {
            return emptyList()
        }

        override fun getContributedPropertys(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
            return emptyList()

        }

        override fun getFunctionNames(): Set<Name> {
            return emptySet()

        }

        override fun getVariableNames(): Set<Name> {
            return emptySet()

        }

        override fun getClassifierNames(): Set<Name>? {
            return null

        }

        override fun getPropertyNames(): Set<Name> {
            return emptySet()
        }

        private fun fillOperatorFunctions() {
            fun createOperatorFunction(
                name: Name,
                rightType: Name? = null,
                returnType: Name? = null
            ): OperatorFunctionDescriptor {
                return OperatorFunctionDescriptor(
                    name, rightType?.let {

                        basicMemberScope.getContributedClassifier(
                            rightType, NoLookupLocation.FROM_BUILTINS
                        )!!.getDefaultType()
                    }, returnType?.let {
                        basicMemberScope.getContributedClassifier(
                            it, NoLookupLocation.FROM_BUILTINS
                        )!!.getDefaultType()
                    }
                )
            }

            when (name.asString()) {
                "Int64" -> {
                    operatorFunctions[TIMES] =
                        listOf(createOperatorFunction(TIMES, StandardNames.INT64, StandardNames.INT64))


                    operatorFunctions[PLUS] =
                        listOf(createOperatorFunction(PLUS, StandardNames.INT64, StandardNames.INT64))
                    operatorFunctions[DIV] =
                        listOf(createOperatorFunction(DIV, StandardNames.INT64, StandardNames.INT64))
                    operatorFunctions[MINUS] =
                        listOf(createOperatorFunction(MINUS, StandardNames.INT64, StandardNames.INT64))
                    operatorFunctions[MINUS] =
                        listOf(createOperatorFunction(MINUS, StandardNames.INT64, StandardNames.INT64))
                    operatorFunctions[REM] =
                        listOf(createOperatorFunction(REM, StandardNames.INT64, StandardNames.INT64))

                    operatorFunctions[EXPONENTIATION] =
                        listOf(createOperatorFunction(EXPONENTIATION, StandardNames.INT64, StandardNames.INT64))

                    operatorFunctions[AND] =
                        listOf(createOperatorFunction(AND, StandardNames.INT64, StandardNames.INT64))
                    operatorFunctions[XOR] =
                        listOf(createOperatorFunction(XOR, StandardNames.INT64, StandardNames.INT64))
                    operatorFunctions[OR] =
                        listOf(createOperatorFunction(OR, StandardNames.INT64, StandardNames.INT64))

                    operatorFunctions[LEFT_SHIFT] =
                        listOf(createOperatorFunction(LEFT_SHIFT, StandardNames.INT64, StandardNames.INT64))
                    operatorFunctions[RIGHT_SHIFT] =
                        listOf(createOperatorFunction(RIGHT_SHIFT, StandardNames.INT64, StandardNames.INT64))


//                    自增自减
                    operatorFunctions[INC] =
                        listOf(createOperatorFunction(INC, null, StandardNames.INT64))

                    operatorFunctions[DEC] =
                        listOf(createOperatorFunction(DEC, null, StandardNames.INT64))
//                    负号
                    operatorFunctions[UNARY_MINUS] =
                        listOf(createOperatorFunction(UNARY_MINUS, null, StandardNames.INT64))

//                    operatorFunctions[COMPARE_LTEQ] =
//                        listOf(createOperatorFunction(COMPARE_LTEQ, StandardNames.INT64, StandardNames.BOOL))
//                    operatorFunctions[COMPARE_LT] =
//                        listOf(createOperatorFunction(COMPARE_LT, StandardNames.INT64, StandardNames.BOOL))
////                    operatorFunctions[COMPARE_GT] =
////                        listOf(createOperatorFunction(COMPARE_GT, StandardNames.INT64, StandardNames.BOOL))
//                    operatorFunctions[COMPARE_LTEQ] =
//                        listOf(createOperatorFunction(COMPARE_LTEQ, StandardNames.INT64, StandardNames.BOOL))
//
//                    operatorFunctions[EQUALS] =
//                        listOf(createOperatorFunction(EQUALS, StandardNames.INT64, StandardNames.BOOL))
//                    operatorFunctions[NOT_EQUALS] =
//                        listOf(createOperatorFunction(NOT_EQUALS, StandardNames.INT64, StandardNames.BOOL))
//
//
                    operatorFunctions[NOT] =
                        listOf(createOperatorFunction(NOT))


                }

                "Int32" -> {
                    operatorFunctions[PLUS] = listOf(
                        createOperatorFunction(PLUS, StandardNames.INT32, StandardNames.INT32),

                        )
                    operatorFunctions[DIV] =
                        listOf(createOperatorFunction(DIV, StandardNames.INT32, StandardNames.INT32))
                    operatorFunctions[MINUS] =
                        listOf(createOperatorFunction(MINUS, StandardNames.INT32, StandardNames.INT32))
                    operatorFunctions[MINUS] =
                        listOf(createOperatorFunction(MINUS, StandardNames.INT32, StandardNames.INT32))
                    operatorFunctions[REM] =
                        listOf(createOperatorFunction(REM, StandardNames.INT32, StandardNames.INT32))

                    operatorFunctions[EXPONENTIATION] =
                        listOf(createOperatorFunction(EXPONENTIATION, StandardNames.INT32, StandardNames.INT32))

                    operatorFunctions[TIMES] =
                        listOf(createOperatorFunction(TIMES, StandardNames.INT32, StandardNames.INT32))



                    operatorFunctions[AND] =
                        listOf(createOperatorFunction(AND, StandardNames.INT32, StandardNames.INT32))
                    operatorFunctions[XOR] =
                        listOf(createOperatorFunction(XOR, StandardNames.INT32, StandardNames.INT32))
                    operatorFunctions[OR] =
                        listOf(createOperatorFunction(OR, StandardNames.INT32, StandardNames.INT32))

                    operatorFunctions[LEFT_SHIFT] =
                        listOf(createOperatorFunction(LEFT_SHIFT, StandardNames.INT32, StandardNames.INT32))
                    operatorFunctions[RIGHT_SHIFT] =
                        listOf(createOperatorFunction(RIGHT_SHIFT, StandardNames.INT32, StandardNames.INT32))


                    //                    自增自减
                    operatorFunctions[INC] =
                        listOf(createOperatorFunction(INC, null, StandardNames.INT32))

                    operatorFunctions[DEC] =
                        listOf(createOperatorFunction(DEC, null, StandardNames.INT32))
//                    负号
                    operatorFunctions[UNARY_MINUS] =
                        listOf(createOperatorFunction(UNARY_MINUS, null, StandardNames.INT32))

//                    operatorFunctions[COMPARE_LTEQ] =
//                        listOf(createOperatorFunction(COMPARE_LTEQ, StandardNames.INT32, StandardNames.BOOL))
//                    operatorFunctions[COMPARE_LT] =
//                        listOf(createOperatorFunction(COMPARE_LT, StandardNames.INT32, StandardNames.BOOL))
////                    operatorFunctions[COMPARE_GT] =
////                        listOf(createOperatorFunction(COMPARE_GT, StandardNames.INT32, StandardNames.BOOL))
//                    operatorFunctions[COMPARE_LTEQ] =
//                        listOf(createOperatorFunction(COMPARE_LTEQ, StandardNames.INT32, StandardNames.BOOL))
//
//                    operatorFunctions[EQUALS] =
//                        listOf(createOperatorFunction(EQUALS, StandardNames.INT32, StandardNames.BOOL))
//                    operatorFunctions[NOT_EQUALS] =
//                        listOf(createOperatorFunction(NOT_EQUALS, StandardNames.INT32, StandardNames.BOOL))
                    operatorFunctions[NOT] =
                        listOf(createOperatorFunction(NOT))
                }

                "Int8" -> {
                    operatorFunctions[PLUS] = listOf(
                        createOperatorFunction(PLUS, StandardNames.INT8, StandardNames.INT8),

                        )

                    operatorFunctions[DIV] =
                        listOf(createOperatorFunction(DIV, StandardNames.INT8, StandardNames.INT8))
                    operatorFunctions[MINUS] =
                        listOf(createOperatorFunction(MINUS, StandardNames.INT8, StandardNames.INT8))
                    operatorFunctions[MINUS] =
                        listOf(createOperatorFunction(MINUS, StandardNames.INT8, StandardNames.INT8))
                    operatorFunctions[REM] =
                        listOf(createOperatorFunction(REM, StandardNames.INT8, StandardNames.INT8))

                    operatorFunctions[EXPONENTIATION] =
                        listOf(createOperatorFunction(EXPONENTIATION, StandardNames.INT8, StandardNames.INT8))

                    operatorFunctions[TIMES] =
                        listOf(createOperatorFunction(TIMES, StandardNames.INT8, StandardNames.INT8))


                    operatorFunctions[AND] =
                        listOf(createOperatorFunction(AND, StandardNames.INT8, StandardNames.INT8))
                    operatorFunctions[XOR] =
                        listOf(createOperatorFunction(XOR, StandardNames.INT8, StandardNames.INT8))
                    operatorFunctions[OR] =
                        listOf(createOperatorFunction(OR, StandardNames.INT8, StandardNames.INT8))

                    operatorFunctions[LEFT_SHIFT] =
                        listOf(createOperatorFunction(LEFT_SHIFT, StandardNames.INT8, StandardNames.INT8))
                    operatorFunctions[RIGHT_SHIFT] =
                        listOf(createOperatorFunction(RIGHT_SHIFT, StandardNames.INT8, StandardNames.INT8))
//                    自增自减
                    operatorFunctions[INC] =
                        listOf(createOperatorFunction(INC, null, StandardNames.INT8))

                    operatorFunctions[DEC] =
                        listOf(createOperatorFunction(DEC, null, StandardNames.INT8))
//                    负号
                    operatorFunctions[UNARY_MINUS] =
                        listOf(createOperatorFunction(UNARY_MINUS, null, StandardNames.INT8))

//                    operatorFunctions[COMPARE_LTEQ] =
//                        listOf(createOperatorFunction(COMPARE_LTEQ, StandardNames.INT8, StandardNames.BOOL))
//                    operatorFunctions[COMPARE_LT] =
//                        listOf(createOperatorFunction(COMPARE_LT, StandardNames.INT8, StandardNames.BOOL))
////                    operatorFunctions[COMPARE_GT] =
////                        listOf(createOperatorFunction(COMPARE_GT, StandardNames.INT8, StandardNames.BOOL))
//                    operatorFunctions[COMPARE_LTEQ] =
//                        listOf(createOperatorFunction(COMPARE_LTEQ, StandardNames.INT8, StandardNames.BOOL))
//
//                    operatorFunctions[EQUALS] =
//                        listOf(createOperatorFunction(EQUALS, StandardNames.INT8, StandardNames.BOOL))
//                    operatorFunctions[NOT_EQUALS] =
//                        listOf(createOperatorFunction(NOT_EQUALS, StandardNames.INT8, StandardNames.BOOL))
                    operatorFunctions[NOT] =
                        listOf(createOperatorFunction(NOT))
                }

                "Int16" -> {
                    operatorFunctions[PLUS] = listOf(
                        createOperatorFunction(PLUS, StandardNames.INT16, StandardNames.INT16),

                        )
                    operatorFunctions[DIV] =
                        listOf(createOperatorFunction(DIV, StandardNames.INT16, StandardNames.INT16))
                    operatorFunctions[MINUS] =
                        listOf(createOperatorFunction(MINUS, StandardNames.INT16, StandardNames.INT16))
                    operatorFunctions[MINUS] =
                        listOf(createOperatorFunction(MINUS, StandardNames.INT16, StandardNames.INT16))
                    operatorFunctions[REM] =
                        listOf(createOperatorFunction(REM, StandardNames.INT16, StandardNames.INT16))

                    operatorFunctions[EXPONENTIATION] =
                        listOf(createOperatorFunction(EXPONENTIATION, StandardNames.INT16, StandardNames.INT16))


                    operatorFunctions[TIMES] =
                        listOf(createOperatorFunction(TIMES, StandardNames.INT16, StandardNames.INT16))

                    operatorFunctions[AND] =
                        listOf(createOperatorFunction(AND, StandardNames.INT16, StandardNames.INT16))
                    operatorFunctions[XOR] =
                        listOf(createOperatorFunction(XOR, StandardNames.INT16, StandardNames.INT16))
                    operatorFunctions[OR] =
                        listOf(createOperatorFunction(OR, StandardNames.INT16, StandardNames.INT16))

                    operatorFunctions[LEFT_SHIFT] =
                        listOf(createOperatorFunction(LEFT_SHIFT, StandardNames.INT16, StandardNames.INT16))
                    operatorFunctions[RIGHT_SHIFT] =
                        listOf(createOperatorFunction(RIGHT_SHIFT, StandardNames.INT16, StandardNames.INT16))
//                    自增自减
                    operatorFunctions[INC] =
                        listOf(createOperatorFunction(INC, null, StandardNames.INT16))

                    operatorFunctions[DEC] =
                        listOf(createOperatorFunction(DEC, null, StandardNames.INT16))
//                    负号
                    operatorFunctions[UNARY_MINUS] =
                        listOf(createOperatorFunction(UNARY_MINUS, null, StandardNames.INT16))

//                    operatorFunctions[COMPARE_LTEQ] =
//                        listOf(createOperatorFunction(COMPARE_LTEQ, StandardNames.INT16, StandardNames.BOOL))
//                    operatorFunctions[COMPARE_LT] =
//                        listOf(createOperatorFunction(COMPARE_LT, StandardNames.INT16, StandardNames.BOOL))
////                    operatorFunctions[COMPARE_GT] =
////                        listOf(createOperatorFunction(COMPARE_GT, StandardNames.INT16, StandardNames.BOOL))
//                    operatorFunctions[COMPARE_LTEQ] =
//                        listOf(createOperatorFunction(COMPARE_LTEQ, StandardNames.INT16, StandardNames.BOOL))
//
//                    operatorFunctions[EQUALS] =
//                        listOf(createOperatorFunction(EQUALS, StandardNames.INT16, StandardNames.BOOL))
//                    operatorFunctions[NOT_EQUALS] =
//                        listOf(createOperatorFunction(NOT_EQUALS, StandardNames.INT16, StandardNames.BOOL))
                    operatorFunctions[NOT] =
                        listOf(createOperatorFunction(NOT))
                }


                "Float16" -> {
                    operatorFunctions[PLUS] = listOf(
                        createOperatorFunction(PLUS, StandardNames.FLOAT16, StandardNames.FLOAT16),

                        )
                    operatorFunctions[DIV] =
                        listOf(createOperatorFunction(DIV, StandardNames.FLOAT16, StandardNames.FLOAT16))
                    operatorFunctions[MINUS] =
                        listOf(createOperatorFunction(MINUS, StandardNames.FLOAT16, StandardNames.FLOAT16))
                    operatorFunctions[MINUS] =
                        listOf(createOperatorFunction(MINUS, StandardNames.FLOAT16, StandardNames.FLOAT16))
                    operatorFunctions[REM] =
                        listOf(createOperatorFunction(REM, StandardNames.FLOAT16, StandardNames.FLOAT16))

                    operatorFunctions[EXPONENTIATION] =
                        listOf(createOperatorFunction(EXPONENTIATION, StandardNames.FLOAT16, StandardNames.INT64))

                    operatorFunctions[TIMES] =
                        listOf(createOperatorFunction(TIMES, StandardNames.FLOAT16, StandardNames.FLOAT16))

//                    负号
                    operatorFunctions[UNARY_MINUS] =
                        listOf(createOperatorFunction(UNARY_MINUS, null, StandardNames.FLOAT16))

//                    operatorFunctions[COMPARE_LTEQ] =
//                        listOf(createOperatorFunction(COMPARE_LTEQ, StandardNames.FLOAT16, StandardNames.BOOL))
//                    operatorFunctions[COMPARE_LT] =
//                        listOf(createOperatorFunction(COMPARE_LT, StandardNames.FLOAT16, StandardNames.BOOL))
////                    operatorFunctions[COMPARE_GT] =
////                        listOf(createOperatorFunction(COMPARE_GT, StandardNames.FLOAT16, StandardNames.BOOL))
//                    operatorFunctions[COMPARE_LTEQ] =
//                        listOf(createOperatorFunction(COMPARE_LTEQ, StandardNames.FLOAT16, StandardNames.BOOL))
//
//                    operatorFunctions[EQUALS] =
//                        listOf(createOperatorFunction(EQUALS, StandardNames.FLOAT16, StandardNames.BOOL))
//                    operatorFunctions[NOT_EQUALS] =
//                        listOf(createOperatorFunction(NOT_EQUALS, StandardNames.FLOAT16, StandardNames.BOOL))

                }

                "Float32" -> {
                    operatorFunctions[PLUS] = listOf(
                        createOperatorFunction(PLUS, StandardNames.FLOAT32, StandardNames.FLOAT32),

                        )
                    operatorFunctions[DIV] =
                        listOf(createOperatorFunction(DIV, StandardNames.FLOAT32, StandardNames.FLOAT32))
                    operatorFunctions[MINUS] =
                        listOf(createOperatorFunction(MINUS, StandardNames.FLOAT32, StandardNames.FLOAT32))
                    operatorFunctions[MINUS] =
                        listOf(createOperatorFunction(MINUS, StandardNames.FLOAT32, StandardNames.FLOAT32))
                    operatorFunctions[REM] =
                        listOf(createOperatorFunction(REM, StandardNames.FLOAT32, StandardNames.FLOAT32))

                    operatorFunctions[EXPONENTIATION] =
                        listOf(createOperatorFunction(EXPONENTIATION, StandardNames.FLOAT32, StandardNames.INT64))

                    operatorFunctions[TIMES] =
                        listOf(createOperatorFunction(TIMES, StandardNames.FLOAT32, StandardNames.FLOAT32))
//                    operatorFunctions[COMPARE_LTEQ] =
//                        listOf(createOperatorFunction(COMPARE_LTEQ, StandardNames.FLOAT32, StandardNames.BOOL))
//                    operatorFunctions[COMPARE_LT] =
//                        listOf(createOperatorFunction(COMPARE_LT, StandardNames.FLOAT32, StandardNames.BOOL))
////                    operatorFunctions[COMPARE_GT] =
////                        listOf(createOperatorFunction(COMPARE_GT, StandardNames.FLOAT32, StandardNames.BOOL))
//                    operatorFunctions[COMPARE_LTEQ] =
//                        listOf(createOperatorFunction(COMPARE_LTEQ, StandardNames.FLOAT32, StandardNames.BOOL))
//
//                    operatorFunctions[EQUALS] =
//                        listOf(createOperatorFunction(EQUALS, StandardNames.FLOAT32, StandardNames.BOOL))
//                    operatorFunctions[NOT_EQUALS] =
//                        listOf(createOperatorFunction(NOT_EQUALS, StandardNames.FLOAT32, StandardNames.BOOL))
//                    负号
                    operatorFunctions[UNARY_MINUS] =
                        listOf(createOperatorFunction(UNARY_MINUS, null, StandardNames.FLOAT32))

                }

                "Float64" -> {
                    operatorFunctions[TIMES] =
                        listOf(createOperatorFunction(TIMES, StandardNames.FLOAT64, StandardNames.FLOAT64))
                    operatorFunctions[PLUS] = listOf(
                        createOperatorFunction(PLUS, StandardNames.FLOAT64, StandardNames.FLOAT64),

                        )
                    operatorFunctions[DIV] =
                        listOf(createOperatorFunction(DIV, StandardNames.FLOAT64, StandardNames.FLOAT64))
                    operatorFunctions[MINUS] =
                        listOf(createOperatorFunction(MINUS, StandardNames.FLOAT64, StandardNames.FLOAT64))
                    operatorFunctions[MINUS] =
                        listOf(createOperatorFunction(MINUS, StandardNames.FLOAT64, StandardNames.FLOAT64))
                    operatorFunctions[REM] =
                        listOf(createOperatorFunction(REM, StandardNames.FLOAT64, StandardNames.FLOAT64))

                    operatorFunctions[EXPONENTIATION] =
                        listOf(createOperatorFunction(EXPONENTIATION, StandardNames.FLOAT64, StandardNames.INT64))


//                    operatorFunctions[COMPARE_LTEQ] =
//                        listOf(createOperatorFunction(COMPARE_LTEQ, StandardNames.FLOAT64, StandardNames.BOOL))
//                    operatorFunctions[COMPARE_LT] =
//                        listOf(createOperatorFunction(COMPARE_LT, StandardNames.FLOAT64, StandardNames.BOOL))
//                    operatorFunctions[COMPARE_GT] =
//                        listOf(createOperatorFunction(COMPARE_GT, StandardNames.FLOAT64, StandardNames.BOOL))
//                    operatorFunctions[COMPARE_LTEQ] =
//                        listOf(createOperatorFunction(COMPARE_LTEQ, StandardNames.FLOAT64, StandardNames.BOOL))
//
//                    operatorFunctions[EQUALS] =
//                        listOf(createOperatorFunction(EQUALS, StandardNames.FLOAT64, StandardNames.BOOL))
//                    operatorFunctions[NOT_EQUALS] =
//                        listOf(createOperatorFunction(NOT_EQUALS, StandardNames.FLOAT64, StandardNames.BOOL))
//                    负号
                    operatorFunctions[UNARY_MINUS] =
                        listOf(createOperatorFunction(UNARY_MINUS, null, StandardNames.FLOAT64))

                }

                "Bool" -> {
                    operatorFunctions[NOT] =
                        listOf(createOperatorFunction(NOT, null, StandardNames.BOOL))
                    operatorFunctions[ANDAND] =
                        listOf(createOperatorFunction(ANDAND, null, StandardNames.BOOL))
                    operatorFunctions[OROR] =
                        listOf(createOperatorFunction(OROR, null, StandardNames.BOOL))
                }
            }


        }

        override fun getContributedFunctions(
            name: Name,
            location: LookupLocation
        ): Collection<SimpleFunctionDescriptor> {
            val result = mutableListOf<SimpleFunctionDescriptor>()

            if (operatorFunctions.isEmpty()) {
                fillOperatorFunctions()
            }
            result.addAll(operatorFunctions[name] ?: emptyList())

            this@BasicTypeDescriptor.extendClassDescriptors.forEach {
                result.addAll(it.unsubstitutedMemberScope.getContributedFunctions(name, location))
            }
            return result

        }

        override fun printScopeStructure(p: Printer) {

        }

        override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
            return null
        }

        override fun getExtendClass(name: Name): List<LazyExtendClassDescriptor> {
            return emptyList()
        }

        override fun getContributedDescriptors(
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean
        ): Collection<DeclarationDescriptor> {
            return emptyList()

        }

    }

//    val extendClassDescriptor = mutableSetOf<LazyExtendClassDescriptor>()

    private var constructors: MutableSet<ClassConstructorDescriptor>? = null

    val basicTypeMemberScope = BasicTypeMemberScope()

    //    基本类型返回扩展
    override fun getSuperTypeListEntries(): List<CjSuperTypeListEntry> {
        val result = mutableListOf<CjSuperTypeListEntry>()
        extendClassDescriptors.forEach {
            result.addAll(it.typeStatement.superTypeListEntries)
        }

        return result
    }

    companion object {

        fun create(
            memberScope: PackageFragmentDescriptorBasicImpl.BasicMemberScope,
            storageManager: StorageManager,
            name: Name
        ): BasicTypeDescriptor {
            return BasicTypeDescriptor(
                memberScope,
                storageManager,
                name
            )
        }
    }

//    fun initialize(
//
//        constructors: Set<ClassConstructorDescriptor>,
//
//        ) {
//
//        this.constructors = constructors
//
//    }

    override fun getUnsubstitutedMemberScope(cangjieTypeRefiner: CangJieTypeRefiner): MemberScope {


        return basicTypeMemberScope
    }


    override fun getUnsubstitutedMemberScope(): MemberScope {


        return basicTypeMemberScope
    }

    override fun getSource(): SourceElement {


        return SourceElement.NO_SOURCE
    }
    override fun getDefaultType(): BasicType {
        return BasicType(typeConstructor, basicTypeMemberScope)
    }

    //    val typeConstructor = ClassTypeConstructorImpl(this, emptyList(), emptyList(), storageManager)
    open val typeConstructor = BasicTypeConstructor(this, storageManager)
    override fun getTypeConstructor(): TypeConstructor = typeConstructor


    override fun getModality(): Modality = Modality.FINAL
    override fun setModality(modality: Modality) {

    }


    override fun getDeclaredTypeParameters(): MutableList<TypeParameterDescriptor> = mutableListOf()
    override fun getContextReceivers(): List<ReceiverParameterDescriptor> = emptyList()

    override fun getStaticScope(): MemberScope {
        return MemberScope.Empty
    }

    override fun getConstructors(): Set<ClassConstructorDescriptor> {
        if (constructors == null) {
            fillConstructors()
        }
        return constructors!!

    }


    override fun getKind(): ClassKind = ClassKind.BASIC
    override fun isFun(): Boolean = false

    override fun isValue(): Boolean = false
    override fun getUnsubstitutedPrimaryConstructor(): ClassConstructorDescriptor? {
        return null
    }

    override fun getSealedSubclasses(): List<ClassDescriptor> {
        return emptyList()
    }

    override fun getScopeForMemberDeclarationResolution(): LexicalScope {
        TODO("Not yet implemented")
    }

    override fun getDeclaredCallableMembers(): List<CallableMemberDescriptor> {
        return emptyList()
    }

    override fun getScopeForInitializerResolution(): LexicalScope {
        TODO("Not yet implemented")
    }

    override fun getScopeForClassHeaderResolution(): LexicalScope {
        TODO("Not yet implemented")
    }

    override fun getScopeForConstructorHeaderResolution(): LexicalScope {
        TODO("Not yet implemented")
    }

//    override val containingDeclaration: DeclarationDescriptor
//        get() = EmptyDeclarationDescriptor(null)
    override val containingDeclaration: DeclarationDescriptor
        get() = basicMemberScope.getBuiltIns().builtInsModule
    override val annotations: Annotations
        get() = Annotations.EMPTY


    override fun toString(): String {
        return name.asString()
    }

    private fun fillConstructors() {
        constructors = mutableSetOf()


        fun createValueParameterByConstructor(vararg typeName: Name) {
            constructors!!.add(BasicClassConstructorDescriptor().apply {
                val values = typeName.mapIndexed { index, value ->
                    ValueParameterDescriptorImpl.createWithDestructuringDeclarations(
                        this@apply, null, index, Annotations.EMPTY, Name.identifier("e$index"),
                        false, basicMemberScope.getContributedClassifier(
                            value, NoLookupLocation.FROM_BUILTINS
                        )!!.getDefaultType(), false, SourceElement.NO_SOURCE, null
                    )
                }
                initialize(values)
                returnType = this@BasicTypeDescriptor.getDefaultType()
            })


        }

        when (name.asString()) {
            "Rune" -> {
                createValueParameterByConstructor(StandardNames.INT64)
                createValueParameterByConstructor(StandardNames.INT32)
                createValueParameterByConstructor(StandardNames.INT16)
                createValueParameterByConstructor(StandardNames.INT8)

                createValueParameterByConstructor(StandardNames.UINT64)
                createValueParameterByConstructor(StandardNames.UINT32)
                createValueParameterByConstructor(StandardNames.UINT16)
                createValueParameterByConstructor(StandardNames.UINT8)
            }
            "UInt64" -> {
                createValueParameterByConstructor(StandardNames.FLOAT16)
                createValueParameterByConstructor(StandardNames.FLOAT32)
                createValueParameterByConstructor(StandardNames.FLOAT64)

                createValueParameterByConstructor(StandardNames.INT64)
                createValueParameterByConstructor(StandardNames.INT32)
                createValueParameterByConstructor(StandardNames.INT16)
                createValueParameterByConstructor(StandardNames.INT8)

                createValueParameterByConstructor(StandardNames.UINT64)
                createValueParameterByConstructor(StandardNames.UINT32)
                createValueParameterByConstructor(StandardNames.UINT16)
                createValueParameterByConstructor(StandardNames.UINT8)


                createValueParameterByConstructor(StandardNames.UINT_NATIVE)
                createValueParameterByConstructor(StandardNames.INT_NATIVE)
            }

            "UInt32" -> {
                createValueParameterByConstructor(StandardNames.FLOAT16)
                createValueParameterByConstructor(StandardNames.FLOAT32)
                createValueParameterByConstructor(StandardNames.FLOAT64)

                createValueParameterByConstructor(StandardNames.INT64)
                createValueParameterByConstructor(StandardNames.INT32)
                createValueParameterByConstructor(StandardNames.INT16)
                createValueParameterByConstructor(StandardNames.INT8)

                createValueParameterByConstructor(StandardNames.UINT64)
                createValueParameterByConstructor(StandardNames.UINT32)
                createValueParameterByConstructor(StandardNames.UINT16)
                createValueParameterByConstructor(StandardNames.UINT8)

                createValueParameterByConstructor(StandardNames.UINT_NATIVE)
                createValueParameterByConstructor(StandardNames.INT_NATIVE)
            }

            "UInt8" -> {
                createValueParameterByConstructor(StandardNames.FLOAT16)
                createValueParameterByConstructor(StandardNames.FLOAT32)
                createValueParameterByConstructor(StandardNames.FLOAT64)

                createValueParameterByConstructor(StandardNames.INT64)
                createValueParameterByConstructor(StandardNames.INT32)
                createValueParameterByConstructor(StandardNames.INT16)
                createValueParameterByConstructor(StandardNames.INT8)

                createValueParameterByConstructor(StandardNames.UINT64)
                createValueParameterByConstructor(StandardNames.UINT32)
                createValueParameterByConstructor(StandardNames.UINT16)
                createValueParameterByConstructor(StandardNames.UINT8)

                createValueParameterByConstructor(StandardNames.UINT_NATIVE)
                createValueParameterByConstructor(StandardNames.INT_NATIVE)
            }

            "Int64" -> {
                createValueParameterByConstructor(StandardNames.FLOAT16)
                createValueParameterByConstructor(StandardNames.FLOAT32)
                createValueParameterByConstructor(StandardNames.FLOAT64)

                createValueParameterByConstructor(StandardNames.INT64)
                createValueParameterByConstructor(StandardNames.INT32)
                createValueParameterByConstructor(StandardNames.INT16)
                createValueParameterByConstructor(StandardNames.INT8)

                createValueParameterByConstructor(StandardNames.UINT64)
                createValueParameterByConstructor(StandardNames.UINT32)
                createValueParameterByConstructor(StandardNames.UINT16)
                createValueParameterByConstructor(StandardNames.UINT8)

                createValueParameterByConstructor(StandardNames.UINT_NATIVE)
                createValueParameterByConstructor(StandardNames.INT_NATIVE)
            }

            "Int32" -> {
                createValueParameterByConstructor(StandardNames.FLOAT16)
                createValueParameterByConstructor(StandardNames.FLOAT32)
                createValueParameterByConstructor(StandardNames.FLOAT64)

                createValueParameterByConstructor(StandardNames.INT64)
                createValueParameterByConstructor(StandardNames.INT32)
                createValueParameterByConstructor(StandardNames.INT16)
                createValueParameterByConstructor(StandardNames.INT8)

                createValueParameterByConstructor(StandardNames.UINT64)
                createValueParameterByConstructor(StandardNames.UINT32)
                createValueParameterByConstructor(StandardNames.UINT16)
                createValueParameterByConstructor(StandardNames.UINT8)

                createValueParameterByConstructor(StandardNames.UINT_NATIVE)
                createValueParameterByConstructor(StandardNames.INT_NATIVE)
            }

            "Int8" -> {
                createValueParameterByConstructor(StandardNames.FLOAT16)
                createValueParameterByConstructor(StandardNames.FLOAT32)
                createValueParameterByConstructor(StandardNames.FLOAT64)

                createValueParameterByConstructor(StandardNames.INT64)
                createValueParameterByConstructor(StandardNames.INT32)
                createValueParameterByConstructor(StandardNames.INT16)
                createValueParameterByConstructor(StandardNames.INT8)

                createValueParameterByConstructor(StandardNames.UINT64)
                createValueParameterByConstructor(StandardNames.UINT32)
                createValueParameterByConstructor(StandardNames.UINT16)
                createValueParameterByConstructor(StandardNames.UINT8)

                createValueParameterByConstructor(StandardNames.UINT_NATIVE)
                createValueParameterByConstructor(StandardNames.INT_NATIVE)
            }

            "Int16" -> {
                createValueParameterByConstructor(StandardNames.FLOAT16)
                createValueParameterByConstructor(StandardNames.FLOAT32)
                createValueParameterByConstructor(StandardNames.FLOAT64)

                createValueParameterByConstructor(StandardNames.INT64)
                createValueParameterByConstructor(StandardNames.INT32)
                createValueParameterByConstructor(StandardNames.INT16)
                createValueParameterByConstructor(StandardNames.INT8)

                createValueParameterByConstructor(StandardNames.UINT64)
                createValueParameterByConstructor(StandardNames.UINT32)
                createValueParameterByConstructor(StandardNames.UINT16)
                createValueParameterByConstructor(StandardNames.UINT8)

                createValueParameterByConstructor(StandardNames.UINT_NATIVE)
                createValueParameterByConstructor(StandardNames.INT_NATIVE)
            }


            "Float16" -> {
                createValueParameterByConstructor(StandardNames.FLOAT16)
                createValueParameterByConstructor(StandardNames.FLOAT32)
                createValueParameterByConstructor(StandardNames.FLOAT64)


                createValueParameterByConstructor(StandardNames.INT64)
                createValueParameterByConstructor(StandardNames.INT32)
                createValueParameterByConstructor(StandardNames.INT16)
                createValueParameterByConstructor(StandardNames.INT8)

                createValueParameterByConstructor(StandardNames.UINT64)
                createValueParameterByConstructor(StandardNames.UINT32)
                createValueParameterByConstructor(StandardNames.UINT16)
                createValueParameterByConstructor(StandardNames.UINT8)

                createValueParameterByConstructor(StandardNames.UINT_NATIVE)
                createValueParameterByConstructor(StandardNames.INT_NATIVE)
            }

            "Float32" -> {
                createValueParameterByConstructor(StandardNames.FLOAT16)
                createValueParameterByConstructor(StandardNames.FLOAT32)
                createValueParameterByConstructor(StandardNames.FLOAT64)

                createValueParameterByConstructor(StandardNames.INT64)
                createValueParameterByConstructor(StandardNames.INT32)
                createValueParameterByConstructor(StandardNames.INT16)
                createValueParameterByConstructor(StandardNames.INT8)

                createValueParameterByConstructor(StandardNames.UINT64)
                createValueParameterByConstructor(StandardNames.UINT32)
                createValueParameterByConstructor(StandardNames.UINT16)
                createValueParameterByConstructor(StandardNames.UINT8)

                createValueParameterByConstructor(StandardNames.UINT_NATIVE)
                createValueParameterByConstructor(StandardNames.INT_NATIVE)

            }
            "Float64" -> {

                createValueParameterByConstructor(StandardNames.FLOAT16)
                createValueParameterByConstructor(StandardNames.FLOAT32)
                createValueParameterByConstructor(StandardNames.FLOAT64)


                createValueParameterByConstructor(StandardNames.INT64)
                createValueParameterByConstructor(StandardNames.INT32)
                createValueParameterByConstructor(StandardNames.INT16)
                createValueParameterByConstructor(StandardNames.INT8)

                createValueParameterByConstructor(StandardNames.UINT64)
                createValueParameterByConstructor(StandardNames.UINT32)
                createValueParameterByConstructor(StandardNames.UINT16)
                createValueParameterByConstructor(StandardNames.UINT8)

                createValueParameterByConstructor(StandardNames.UINT_NATIVE)
                createValueParameterByConstructor(StandardNames.INT_NATIVE)
            }

            "IntNative" -> {

                createValueParameterByConstructor(StandardNames.FLOAT16)
                createValueParameterByConstructor(StandardNames.FLOAT32)
                createValueParameterByConstructor(StandardNames.FLOAT64)


                createValueParameterByConstructor(StandardNames.INT64)
                createValueParameterByConstructor(StandardNames.INT32)
                createValueParameterByConstructor(StandardNames.INT16)
                createValueParameterByConstructor(StandardNames.INT8)

                createValueParameterByConstructor(StandardNames.UINT64)
                createValueParameterByConstructor(StandardNames.UINT32)
                createValueParameterByConstructor(StandardNames.UINT16)
                createValueParameterByConstructor(StandardNames.UINT8)

                createValueParameterByConstructor(StandardNames.UINT_NATIVE)
                createValueParameterByConstructor(StandardNames.INT_NATIVE)
            }

            "UIntNative" -> {

                createValueParameterByConstructor(StandardNames.FLOAT16)
                createValueParameterByConstructor(StandardNames.FLOAT32)
                createValueParameterByConstructor(StandardNames.FLOAT64)


                createValueParameterByConstructor(StandardNames.INT64)
                createValueParameterByConstructor(StandardNames.INT32)
                createValueParameterByConstructor(StandardNames.INT16)
                createValueParameterByConstructor(StandardNames.INT8)

                createValueParameterByConstructor(StandardNames.UINT64)
                createValueParameterByConstructor(StandardNames.UINT32)
                createValueParameterByConstructor(StandardNames.UINT16)
                createValueParameterByConstructor(StandardNames.UINT8)

                createValueParameterByConstructor(StandardNames.UINT_NATIVE)
                createValueParameterByConstructor(StandardNames.INT_NATIVE)
            }

            "Bool" -> {

            }
        }
    }

    inner class BasicClassConstructorDescriptor : ClassConstructorDescriptorImpl(
        this@BasicTypeDescriptor,
        null,
        Annotations.EMPTY,
        false,
        CallableMemberDescriptor.Kind.DECLARATION,
        SourceElement.NO_SOURCE
    ) {
//        init {
//
//            super.initialize(unsubstitutedValueParameters)
//        }
    }
}


