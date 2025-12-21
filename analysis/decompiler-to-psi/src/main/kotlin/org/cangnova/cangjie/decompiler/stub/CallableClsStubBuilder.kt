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

package org.cangnova.cangjie.decompiler.stub

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.metadata.model.wrapper.*
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.psi.stubs.impl.*

const val COMPILED_DEFAULT_INITIALIZER = "COMPILED_CODE"

/**
 * 函数 Stub 构建器基类
 */
sealed class BaseFunctionClsStubBuilder(
    protected val parentStub: StubElement<out PsiElement>,
    protected val outerContext: ClsStubBuilderContext,
    protected val metadataContainer: MetadataContainer,
    protected val functionWrapper: FunctionWrapper
) {
    protected val isTopLevel = metadataContainer is MetadataContainer.Package

    abstract fun build()

    protected fun createTypeParameterListStub(
        functionStub: CangJieStubBaseImpl<*>
    ): ClsStubBuilderContext {
        val typeParameters = functionWrapper.typeParameters
        if (typeParameters.isEmpty()) {
            return outerContext
        }

        val typeParamListStub = CangJiePlaceHolderStubImpl<CjTypeParameterList>(
            functionStub,
            CjStubElementTypes.TYPE_PARAMETER_LIST
        )

        val innerContext = outerContext.child(typeParameters)

        for (typeParam in typeParameters) {
            // 只创建类型参数名称，不包含 bounds（bounds 放在 where 子句中）
            CangJieTypeParameterStubImpl(typeParamListStub, typeParam.name.ref())
        }

        return innerContext
    }

    protected fun createTypeConstraintListStub(
        functionStub: CangJieStubBaseImpl<*>,
        context: ClsStubBuilderContext
    ) {
        val typeParameters = functionWrapper.typeParameters
        val constraintsToCreate = mutableListOf<Pair<Name, TypeWrapper>>()

        for (typeParam in typeParameters) {
            for (upper in typeParam.uppers) {
                constraintsToCreate.add(Pair(typeParam.name, upper))
            }
        }

        if (constraintsToCreate.isEmpty()) return

        val constraintListStub = CangJiePlaceHolderStubImpl<CjTypeConstraintList>(
            functionStub,
            CjStubElementTypes.TYPE_CONSTRAINT_LIST
        )

        for ((paramName, upperBound) in constraintsToCreate) {
            val constraintStub = CangJiePlaceHolderStubImpl<CjTypeConstraint>(
                constraintListStub,
                CjStubElementTypes.TYPE_CONSTRAINT
            )
            // 创建类型参数名称引用
            CangJieNameReferenceExpressionStubImpl(constraintStub, paramName.ref(), false)
            // 创建 bound 类型引用
            TypeClsStubBuilder(constraintStub, context).createTypeReferenceStub(upperBound)
        }
    }
}

/**
 * 普通函数 Stub 构建器
 */
class FunctionClsStubBuilder(
    parentStub: StubElement<out PsiElement>,
    outerContext: ClsStubBuilderContext,
    metadataContainer: MetadataContainer,
    functionWrapper: FunctionWrapper
) : BaseFunctionClsStubBuilder(parentStub, outerContext, metadataContainer, functionWrapper) {

    override fun build() {
        val funcName = functionWrapper.name
        val fqName = if (isTopLevel) outerContext.containerFqName.child(funcName) else null

        val functionStub = CangJieNamedFunctionStubImpl(
            parentStub,
            CjStubElementTypes.FUNCTION,
            funcName.ref(),
            isTopLevel,
            fqName,
            isExtension = false,
            hasBlockBody = true,
            hasBody = functionWrapper.modality != Modality.ABSTRACT,
            hasTypeParameterListBeforeFunctionName = functionWrapper.typeParameters.isNotEmpty(),
            origin = null
        )

        createModifierListStubForDeclaration(
            functionStub,
            functionWrapper.visibility,
            functionWrapper.modality
        )

        val innerContext = createTypeParameterListStub(functionStub)

        createValueParameterListStub(functionStub, innerContext, functionWrapper.valueParameters)

        TypeClsStubBuilder(functionStub, innerContext).createTypeReferenceStub(functionWrapper.returnType)

        // 创建 where 子句（类型约束列表）
        createTypeConstraintListStub(functionStub, innerContext)
    }
}

/**
 * Main 函数 Stub 构建器
 */
class MainFunctionClsStubBuilder(
    parentStub: StubElement<out PsiElement>,
    outerContext: ClsStubBuilderContext,
    metadataContainer: MetadataContainer,
    functionWrapper: FunctionWrapper
) : BaseFunctionClsStubBuilder(parentStub, outerContext, metadataContainer, functionWrapper) {

    override fun build() {
        val funcName = functionWrapper.name
        val fqName = if (isTopLevel) outerContext.containerFqName.child(funcName) else null

        val mainFunctionStub = CangJieMainFunctionStubImpl(
            parentStub,
            CjStubElementTypes.MAIN_FUNC,
            funcName.ref(),
            fqName,
            origin = null
        )

        createModifierListStubForDeclaration(
            mainFunctionStub,
            functionWrapper.visibility,
            functionWrapper.modality
        )

        createValueParameterListStub(mainFunctionStub, outerContext, functionWrapper.valueParameters)
    }
}

/**
 * Macro Stub 构建器
 */
class MacroClsStubBuilder(
    parentStub: StubElement<out PsiElement>,
    outerContext: ClsStubBuilderContext,
    metadataContainer: MetadataContainer,
    functionWrapper: FunctionWrapper
) : BaseFunctionClsStubBuilder(parentStub, outerContext, metadataContainer, functionWrapper) {

    override fun build() {
        val macroName = functionWrapper.name
        val fqName = if (isTopLevel) outerContext.containerFqName.child(macroName) else null

        val macroStub = CangJieMacroStubImpl(
            parentStub,
            CjStubElementTypes.MACRO,
            macroName.ref(),
            isTopLevel,
            fqName,
            isExtension = false,
            hasBlockBody = true,
            hasBody = true,
            hasTypeParameterListBeforeFunctionName = functionWrapper.typeParameters.isNotEmpty(),
            origin = null
        )

        createModifierListStubForDeclaration(
            macroStub,
            functionWrapper.visibility,
            functionWrapper.modality
        )

        val innerContext = createTypeParameterListStub(macroStub)

        createValueParameterListStub(macroStub, innerContext, functionWrapper.valueParameters)

        TypeClsStubBuilder(macroStub, innerContext).createTypeReferenceStub(functionWrapper.returnType)

        // 创建 where 子句（类型约束列表）
        createTypeConstraintListStub(macroStub, innerContext)
    }
}

/**
 * 属性 Stub 构建器
 */
class PropertyClsStubBuilder(
    private val parentStub: StubElement<out PsiElement>,
    private val outerContext: ClsStubBuilderContext,
    private val metadataContainer: MetadataContainer,
    private val propertyWrapper: PropertyWrapper
) {
    private val isTopLevel = metadataContainer is MetadataContainer.Package

    fun build() {
        val propertyName = propertyWrapper.name
        val fqName = if (isTopLevel) outerContext.containerFqName.child(propertyName) else null

        val propertyStub = CangJiePropertyStubImpl(
            parentStub,
            propertyName.ref(),
            hasReturnTypeRef = true,
            fqName = fqName,
            isExtension = false
        )

        createModifierListStubForDeclaration(
            propertyStub,
            propertyWrapper.visibility,
            propertyWrapper.modality
        )

        TypeClsStubBuilder(propertyStub, outerContext).createTypeReferenceStub(propertyWrapper.returnType)

        // 创建 getter/setter stubs
        if (propertyWrapper.getter != null) {
            CangJiePropertyAccessorStubImpl(propertyStub, true, false, true)
        }
        if (propertyWrapper.setter != null) {
            CangJiePropertyAccessorStubImpl(propertyStub, false, true, true)
        }
    }
}

/**
 * 变量 Stub 构建器
 */
class VariableClsStubBuilder(
    private val parentStub: StubElement<out PsiElement>,
    private val outerContext: ClsStubBuilderContext,
    private val metadataContainer: MetadataContainer,
    private val variableWrapper: VariableWrapper
) {
    private val isTopLevel = metadataContainer is MetadataContainer.Package

    fun build() {
        val varName = variableWrapper.name
        val fqName = if (isTopLevel) outerContext.containerFqName.child(varName) else null

        val variableStub = CangJieVariableStubImpl(
            parentStub,
            varName.ref(),
            variableWrapper.isVar,
            isTopLevel,
            hasInitializer = false,
            isExtension = false,
            hasReturnTypeRef = true,
            fqName = fqName,
            origin = null
        )

        createModifierListStubForDeclaration(
            variableStub,
            variableWrapper.visibility,
            variableWrapper.modality
        )

        TypeClsStubBuilder(variableStub, outerContext).createTypeReferenceStub(variableWrapper.returnType)
    }
}

/**
 * 扩展声明 Stub 构建器
 */
class ExtendClsStubBuilder(
    private val parentStub: StubElement<out PsiElement>,
    private val outerContext: ClsStubBuilderContext,
    private val extendWrapper: ExtendWrapper
) {
    fun build() {
        val extendName = Name.identifier(extendWrapper.id)
        val fqName = extendWrapper.packageFqName.child(extendName)

        val superTypeRefs = extendWrapper.superTypes
            .mapNotNull { TypeClsStubBuilder.extractTypeName(it, outerContext) }
            .map { it.ref() }
            .toTypedArray()

        val extendStub = CangJieExtendStubImpl(
            CjStubElementTypes.EXTEND,
            parentStub,
            fqName.ref(),
            null,
            extendName.ref(),
            superTypeRefs
        )

        // 创建被扩展类型引用
        TypeClsStubBuilder(extendStub, outerContext).createTypeReferenceStub(extendWrapper.type)

        // 创建父类型列表
        if (extendWrapper.superTypes.isNotEmpty()) {
            val superTypeListStub = CangJiePlaceHolderStubImpl<CjSuperTypeList>(
                extendStub,
                CjStubElementTypes.SUPER_TYPE_LIST
            )

            for (superType in extendWrapper.superTypes) {
                val superTypeEntryStub = CangJiePlaceHolderStubImpl<CjSuperTypeEntry>(
                    superTypeListStub,
                    CjStubElementTypes.SUPER_TYPE_ENTRY
                )
                TypeClsStubBuilder(superTypeEntryStub, outerContext).createTypeReferenceStub(superType)
            }
        }

        // 创建类型参数列表
        val innerContext = if (extendWrapper.typeParameters.isNotEmpty()) {
            val typeParamListStub = CangJiePlaceHolderStubImpl<CjTypeParameterList>(
                extendStub,
                CjStubElementTypes.TYPE_PARAMETER_LIST
            )

            val context = outerContext.child(extendWrapper.typeParameters)

            for (typeParam in extendWrapper.typeParameters) {
                CangJieTypeParameterStubImpl(typeParamListStub, typeParam.name.ref())
            }

            context
        } else {
            outerContext
        }

        // 创建类体
        val classBody = CangJiePlaceHolderStubImpl<CjClassBody>(
            extendStub,
            CjStubElementTypes.CLASS_BODY
        )

        // 创建扩展函数 Stubs
        val metadataContainer = MetadataContainer.Package(extendWrapper.packageFqName, outerContext.typeTable)
        for (function in extendWrapper.functions) {
            val builder = when {
                function.isMainEntry -> MainFunctionClsStubBuilder(
                    classBody,
                    innerContext,
                    metadataContainer,
                    function
                )
                function.isMacro -> MacroClsStubBuilder(
                    classBody,
                    innerContext,
                    metadataContainer,
                    function
                )
                else -> FunctionClsStubBuilder(
                    classBody,
                    innerContext,
                    metadataContainer,
                    function
                )
            }
            builder.build()
        }

        // 创建扩展属性 Stubs
        for (property in extendWrapper.propertys) {
            PropertyClsStubBuilder(classBody, innerContext, metadataContainer, property).build()
        }

        // 创建扩展变量 Stubs
        for (variable in extendWrapper.variables) {
            VariableClsStubBuilder(classBody, innerContext, metadataContainer, variable).build()
        }
    }
}

/**
 * 类型别名 Stub 构建器
 */
class TypeAliasClsStubBuilder(
    private val parentStub: StubElement<out PsiElement>,
    private val outerContext: ClsStubBuilderContext,
    private val typeAliasWrapper: TypeAliasWrapper
) {
    fun build() {
        val aliasName = typeAliasWrapper.name
        val fqName = outerContext.containerFqName.child(aliasName)

        val typeAliasStub = CangJieTypeAliasStubImpl(
            parentStub,
            aliasName.ref(),
            fqName.ref(),
            null
        )

        createModifierListStubForDeclaration(typeAliasStub, typeAliasWrapper.visibility, null)

        // 创建展开类型引用
        TypeClsStubBuilder(typeAliasStub, outerContext).createTypeReferenceStub(typeAliasWrapper.expandedType)
    }
}