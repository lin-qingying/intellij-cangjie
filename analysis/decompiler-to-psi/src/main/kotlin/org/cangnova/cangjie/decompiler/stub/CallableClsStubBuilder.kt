/*
 * Copyright 2026 LinQingYing. and contributors.
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
import com.intellij.util.io.StringRef
import org.cangnova.cangjie.decompiler.COMPILED_DEFAULT_INITIALIZER
import org.cangnova.cangjie.psi.stubs.PatternKind
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.extend.NameDemangler
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.metadata.model.util.toName
import org.cangnova.cangjie.metadata.model.wrapper.*
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import org.cangnova.cangjie.psi.stubs.impl.*

/**
 * 函数 Stub 构建器基类
 *
 * ## 架构作用
 *
 * BaseFunctionClsStubBuilder 是所有函数类型 Stub 构建器的抽象基类。
 * 它提供了函数 Stub 构建的通用逻辑，包括：
 * - 类型参数列表的创建
 * - 类型约束（where 子句）的创建
 * - 顶层函数识别
 *
 * ## 继承体系
 *
 * ```
 * BaseFunctionClsStubBuilder (抽象基类)
 *   ├─ FunctionClsStubBuilder - 普通函数
 *   ├─ MainFunctionClsStubBuilder - Main 入口函数
 *   └─ MacroClsStubBuilder - 宏函数
 * ```
 *
 * ## 类型参数处理
 *
 * 函数的类型参数使用与类相同的两段式结构：
 * 1. **类型参数列表**: 只包含参数名 `<T, U>`
 * 2. **类型约束列表**: where 子句 `where T: Comparable<T>`
 *
 * ### 示例
 *
 * ```kotlin
 * func sort<T>(array: Array<T>) where T: Comparable<T> { ... }
 * ```
 *
 * 对应的 Stub 结构：
 * ```
 * CjNamedFunction ("sort")
 *   ├─ CjTypeParameterList
 *   │   └─ CjTypeParameter ("T")
 *   ├─ CjParameterList
 *   │   └─ CjParameter ("array", Array<T>)
 *   ├─ TypeRef (返回类型)
 *   └─ CjTypeConstraintList
 *       └─ CjTypeConstraint (T: Comparable<T>)
 * ```
 *
 * ## 顶层函数 vs 成员函数
 *
 * | 特性 | 顶层函数 | 成员函数 |
 * |------|---------|---------|
 * | 容器 | Package | Class |
 * | fqName | 有（用于索引） | 无 |
 * | isTopLevel | true | false |
 * | 可见性 | public/internal | public/private/protected |
 *
 * ## 上下文管理
 *
 * 函数的类型参数会创建新的类型上下文（[ClsStubBuilderContext]）：
 * - **外部上下文** ([outerContext]): 类或包的类型参数
 * - **内部上下文**: 加上函数自己的类型参数
 *
 * 这允许在函数体中引用函数的类型参数。
 *
 * ## 抽象方法处理
 *
 * 对于接口或抽象类的抽象方法：
 * - `hasBody = false` (通过 `modality != Modality.ABSTRACT` 判断)
 * - 不生成方法体相关的 Stub
 *
 * @property parentStub 父 Stub（文件 Stub 或类体 Stub）
 * @property outerContext 外部构建上下文
 * @property metadataContainer 元数据容器（Package 或 Class）
 * @property functionWrapper 函数包装器（来自元数据）
 *
 * @see FunctionClsStubBuilder
 * @see MainFunctionClsStubBuilder
 * @see MacroClsStubBuilder
 * @see ClsStubBuilderContext
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
 *
 * ## 功能说明
 *
 * 为普通函数（包括顶层函数和成员方法）创建 [CangJieNamedFunctionStubImpl]。
 *
 * ## 支持的函数类型
 *
 * - **顶层函数**: 包级别的全局函数
 * - **成员方法**: 类、接口、结构体的方法
 * - **抽象方法**: 接口或抽象类中的抽象方法
 * - **默认方法**: 接口中的默认实现方法
 *
 * ## Stub 构建流程
 *
 * ```
 * FunctionClsStubBuilder.build()
 *   ├─ 创建 CangJieNamedFunctionStub
 *   │   ├─ 函数名
 *   │   ├─ fqName (仅顶层函数)
 *   │   ├─ isTopLevel 标志
 *   │   └─ hasBody (根据 modality 判断)
 *   ├─ createModifierListStubForDeclaration()
 *   │   └─ 可见性 + 模态修饰符
 *   ├─ createTypeParameterListStub()
 *   │   └─ 返回包含类型参数的新上下文
 *   ├─ createValueParameterListStub()
 *   │   └─ 函数参数列表
 *   ├─ createTypeReferenceStub()
 *   │   └─ 返回类型
 *   └─ createTypeConstraintListStub()
 *       └─ where 子句（如果有类型约束）
 * ```
 *
 * ## PSI 结构示例
 *
 * 对于函数 `public func map<T, R>(list: Array<T>, transform: (T) -> R): Array<R> where R: Comparable<R>`：
 *
 * ```
 * CangJieNamedFunctionStub ("map")
 *   ├─ CangJieModifierListStub (public)
 *   ├─ CjTypeParameterList
 *   │   ├─ CjTypeParameter ("T")
 *   │   └─ CjTypeParameter ("R")
 *   ├─ CjParameterList
 *   │   ├─ CjParameter ("list", Array<T>)
 *   │   └─ CjParameter ("transform", (T) -> R)
 *   ├─ TypeRef (Array<R>) - 返回类型
 *   └─ CjTypeConstraintList
 *       └─ CjTypeConstraint (R: Comparable<R>)
 * ```
 *
 * ## 特殊字段说明
 *
 * - **hasBlockBody**: 总是 `true`（从元数据无法区分表达式体和块体）
 * - **hasBody**: 根据 modality 判断（抽象方法为 `false`）
 * - **hasTypeParameterListBeforeFunctionName**: 是否有类型参数
 * - **isExtension**: 总是 `false`（扩展函数由 [ExtendClsStubBuilder] 处理）
 *
 * ## 与其他函数构建器的区别
 *
 * | 构建器 | 用途 | Stub 类型 | 特殊处理 |
 * |--------|------|----------|---------|
 * | [FunctionClsStubBuilder] | 普通函数 | [CangJieNamedFunctionStubImpl] | 支持类型参数 |
 * | [MainFunctionClsStubBuilder] | Main函数 | [CangJieMainFunctionStubImpl] | 无类型参数，无返回类型 |
 * | [MacroClsStubBuilder] | 宏 | [CangJieMacroStubImpl] | 类似普通函数 |
 *
 * @see BaseFunctionClsStubBuilder
 * @see CangJieNamedFunctionStubImpl
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

        // 先创建注解 Stub（注解在修饰符列表之前）
        createAnnotationsStub(functionStub, functionWrapper.annotations)

        createModifierListStubForDeclaration(
            functionStub,
            functionWrapper.visibility,
            functionWrapper.modality,
            buildList {
                if (functionWrapper.isOverride) add(CjTokens.OVERRIDE_KEYWORD)
                if (functionWrapper.isRedef) add(CjTokens.REDEF_KEYWORD)
            }
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
 *
 * ## 功能说明
 *
 * 为仓颉语言的程序入口函数（Main 函数）创建专门的 Stub。
 *
 * ## Main 函数特点
 *
 * Main 函数是仓颉程序的入口点，具有以下特殊约束：
 * 1. **无返回类型**: Main 函数不返回值
 * 2. **无类型参数**: 不支持泛型
 * 3. **固定签名**: 参数类型受限（通常为 `Array<String>` 或无参）
 * 4. **顶层定义**: 必须在包级别定义
 *
 * ## Stub 构建流程
 *
 * ```
 * MainFunctionClsStubBuilder.build()
 *   ├─ 创建 CangJieMainFunctionStub
 *   │   ├─ 函数名（通常为 "main"）
 *   │   └─ fqName（用于索引）
 *   ├─ createModifierListStubForDeclaration()
 *   │   └─ 可见性修饰符（通常为 public）
 *   └─ createValueParameterListStub()
 *       └─ 参数列表（可能为空或 Array<String>）
 * ```
 *
 * ## 与普通函数的区别
 *
 * | 特性 | Main 函数 | 普通函数 |
 * |------|----------|---------|
 * | 类型参数 | ❌ 不支持 | ✅ 支持 |
 * | 返回类型 | ❌ 无 | ✅ 有 |
 * | where 子句 | ❌ 无 | ✅ 可选 |
 * | Stub 类型 | [CangJieMainFunctionStubImpl] | [CangJieNamedFunctionStubImpl] |
 * | 位置 | 仅顶层 | 顶层或成员 |
 *
 * ## PSI 结构示例
 *
 * 对于 `main func main(args: Array<String>)`：
 *
 * ```
 * CangJieMainFunctionStub ("main")
 *   ├─ CangJieModifierListStub (public)
 *   └─ CjParameterList
 *       └─ CjParameter ("args", Array<String>)
 * ```
 *
 * ## IDE 支持
 *
 * Main 函数的 Stub 用于：
 * - **运行配置**: 识别可运行的程序入口
 * - **导航**: 快速跳转到 Main 函数
 * - **图标**: 在编辑器中显示运行图标
 *
 * @see BaseFunctionClsStubBuilder
 * @see CangJieMainFunctionStubImpl
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

        // 先创建注解 Stub（注解在修饰符列表之前）
        createAnnotationsStub(mainFunctionStub, functionWrapper.annotations)

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
 *
 * ## 功能说明
 *
 * 为仓颉语言的宏（Macro）创建 Stub。宏是编译时执行的特殊函数。
 *
 * ## 宏的特点
 *
 * - **编译时执行**: 在编译期间展开和执行
 * - **代码生成**: 可以生成代码
 * - **类型安全**: 支持类型参数和类型约束
 * - **语法相似**: 与普通函数的语法非常相似
 *
 * ## Stub 构建流程
 *
 * ```
 * MacroClsStubBuilder.build()
 *   ├─ 创建 CangJieMacroStub
 *   │   ├─ 宏名
 *   │   ├─ fqName (仅顶层宏)
 *   │   ├─ isTopLevel 标志
 *   │   └─ hasBody (总是 true)
 *   ├─ createModifierListStubForDeclaration()
 *   ├─ createTypeParameterListStub()
 *   ├─ createValueParameterListStub()
 *   ├─ TypeClsStubBuilder.createTypeReferenceStub() - 返回类型
 *   └─ createTypeConstraintListStub() - where 子句
 * ```
 *
 * ## PSI 结构示例
 *
 * 对于宏 `macro debug<T>(value: T): String where T: ToString`：
 *
 * ```
 * CangJieMacroStub ("debug")
 *   ├─ CangJieModifierListStub (public)
 *   ├─ CjTypeParameterList
 *   │   └─ CjTypeParameter ("T")
 *   ├─ CjParameterList
 *   │   └─ CjParameter ("value", T)
 *   ├─ TypeRef (String) - 返回类型
 *   └─ CjTypeConstraintList
 *       └─ CjTypeConstraint (T: ToString)
 * ```
 *
 * ## 与普通函数的区别
 *
 * | 特性 | 宏 | 普通函数 |
 * |------|-----|---------|
 * | 执行时机 | 编译时 | 运行时 |
 * | Stub 类型 | [CangJieMacroStubImpl] | [CangJieNamedFunctionStubImpl] |
 * | 语法结构 | 几乎相同 | 几乎相同 |
 * | 元数据标记 | `isMacro = true` | `isMacro = false` |
 *
 * ## IDE 支持
 *
 * 宏的 Stub 支持：
 * - **语法高亮**: 使用不同的颜色显示宏调用
 * - **代码补全**: 提供宏名称补全
 * - **导航**: 跳转到宏定义
 * - **文档**: 显示宏的参数和返回类型
 *
 * @see BaseFunctionClsStubBuilder
 * @see CangJieMacroStubImpl
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

        // 先创建注解 Stub（注解在修饰符列表之前）
        createAnnotationsStub(macroStub, functionWrapper.annotations)

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
 *
 * ## 功能说明
 *
 * 为类成员属性创建 [CangJiePropertyStubImpl]。属性与变量的主要区别是：
 * - 属性支持 getter/setter 访问器
 * - 属性通常用于类成员
 * - 属性可以有自定义访问器逻辑
 *
 * ## 属性类型
 *
 * ### 1. 只读属性 (let)
 * ```kotlin
 * let x: Int
 *   get() { return field }
 * ```
 *
 * ### 2. 可变属性 (var)
 * ```kotlin
 * var y: String
 *   get() { return field }
 *   set(value) { field = value }
 * ```
 *
 * ### 3. 计算属性
 * ```kotlin
 * let area: Float64
 *   get() { return width * height }
 * ```
 *
 * ## Stub 构建流程
 *
 * ```
 * PropertyClsStubBuilder.build()
 *   ├─ 创建 CangJiePropertyStub
 *   │   ├─ 属性名
 *   │   ├─ fqName (仅顶层属性)
 *   │   ├─ hasReturnTypeRef = true
 *   │   └─ isExtension = false
 *   ├─ createModifierListStubForDeclaration()
 *   │   └─ 可见性 + 模态修饰符
 *   ├─ TypeClsStubBuilder.createTypeReferenceStub()
 *   │   └─ 属性类型
 *   ├─ (可选) CangJiePropertyAccessorStub (getter)
 *   └─ (可选) CangJiePropertyAccessorStub (setter)
 * ```
 *
 * ## PSI 结构示例
 *
 * 对于属性 `public var name: String`：
 *
 * ```
 * CangJiePropertyStub ("name")
 *   ├─ CangJieModifierListStub (public)
 *   ├─ TypeRef (String)
 *   ├─ CangJiePropertyAccessorStub (getter)
 *   └─ CangJiePropertyAccessorStub (setter)
 * ```
 *
 * ## Accessor Stub 说明
 *
 * 访问器 Stub 非常简单，只包含：
 * - **isGetter**: 是否为 getter
 * - **isSetter**: 是否为 setter
 * - **hasBody**: 是否有方法体（元数据中总是 `true`）
 *
 * 不包含访问器的参数或返回类型（从属性类型推断）。
 *
 * ## 与变量的区别
 *
 * | 特性 | 属性 (Property) | 变量 (Variable) |
 * |------|----------------|----------------|
 * | Stub 类型 | [CangJiePropertyStubImpl] | [CangJieVariableStubImpl] |
 * | Accessor | ✅ 支持 getter/setter | ❌ 无 |
 * | 典型位置 | 类成员 | 包级别或局部 |
 * | 元数据 | [PropertyWrapper] | [VariableWrapper] |
 *
 * ## 顶层属性
 *
 * 顶层属性类似于包级别的全局变量，但：
 * - 有完全限定名（用于索引）
 * - 支持 getter/setter
 * - 可以在其他模块中访问
 *
 * @property parentStub 父 Stub（文件 Stub 或类体 Stub）
 * @property outerContext 外部构建上下文
 * @property metadataContainer 元数据容器（Package 或 Class）
 * @property propertyWrapper 属性包装器（来自元数据）
 *
 * @see CangJiePropertyStubImpl
 * @see CangJiePropertyAccessorStubImpl
 * @see VariableClsStubBuilder
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

        // 创建注解 Stub（注解在修饰符列表之前）
        createAnnotationsStub(propertyStub, propertyWrapper.annotations)

        createModifierListStubForDeclaration(
            propertyStub,
            propertyWrapper.visibility,
            propertyWrapper.modality,
            buildList {
                if (propertyWrapper.isOverride) add(CjTokens.OVERRIDE_KEYWORD)
                if (propertyWrapper.isRedef) add(CjTokens.REDEF_KEYWORD)
            }
        )

        TypeClsStubBuilder(propertyStub, outerContext).createTypeReferenceStub(propertyWrapper.returnType)

        // 创建 PROPERTY_BODY 包装器和 getter/setter stubs
        // PSI 结构要求 accessor 在 PROPERTY_BODY 内部
        if (propertyWrapper.getter != null || propertyWrapper.setter != null) {
            val propertyBody = CangJiePlaceHolderStubImpl<CjPropertyBody>(
                propertyStub,
                CjStubElementTypes.PROPERTY_BODY
            )
            if (propertyWrapper.getter != null) {
                val getterStub = CangJiePropertyAccessorStubImpl(propertyBody, true, false, true)
            }
            if (propertyWrapper.setter != null) {
                val setterStub = CangJiePropertyAccessorStubImpl(propertyBody, false, true, true)
                // setter 需要参数列表（根据 CangJieParsing 的语法，setter 有 VALUE_PARAMETER_LIST 节点）
                val paramListStub = CangJiePlaceHolderStubImpl<CjParameterList>(
                    setterStub,
                    CjStubElementTypes.VALUE_PARAMETER_LIST
                )
                // 创建 value 参数
                val paramStub = CangJieParameterStubImpl(
                    paramListStub,
                    fqName = null,
                    name = Name.identifier("value").ref(),
                    isMutable = false,
                    hasLetOrVar = false,
                    hasDefaultValue = false
                )

            }
        }
    }
}

/**
 * 变量 Stub 构建器
 *
 * ## 功能说明
 *
 * 为包级别变量和类成员变量创建 [CangJieVariableStubImpl]。
 * 变量是简单的数据存储，不像属性那样有访问器逻辑。
 *
 * ## 变量类型
 *
 * ### 1. 不可变变量 (let)
 * ```cangjie
 * let PI: Float64 = 3.14159
 * ```
 *
 * ### 2. 可变变量 (var)
 * ```cangjie
 * var counter: Int = 0
 * ```
 *
 * ### 3. 静态变量
 * ```cangjie
 * static let INSTANCE: MyClass = MyClass()
 * ```
 *
 * ## Stub 构建流程
 *
 * ```
 * VariableClsStubBuilder.build()
 *   ├─ 创建 CangJieVariableStub
 *   │   ├─ 变量名
 *   │   ├─ isVar (是否可变)
 *   │   ├─ isTopLevel 标志
 *   │   ├─ fqName (仅顶层变量)
 *   │   ├─ hasInitializer = false (无法从元数据恢复)
 *   │   └─ hasReturnTypeRef = true
 *   ├─ createModifierListStubForDeclaration()
 *   │   └─ 可见性 + 模态修饰符
 *   └─ TypeClsStubBuilder.createTypeReferenceStub()
 *       └─ 变量类型
 * ```
 *
 * ## PSI 结构示例
 *
 * 对于变量 `public var count: Int`：
 *
 * ```
 * CangJieVariableStub ("count")
 *   ├─ CangJieModifierListStub (public)
 *   └─ TypeRef (Int)
 * ```
 *
 * ## 初始化器处理
 *
 * - **hasInitializer**: 总是 `false`
 * - 原因：从元数据无法恢复初始化表达式
 * - 反编译时使用 [COMPILED_DEFAULT_INITIALIZER] 占位符
 *
 * ## 与属性的区别
 *
 * | 特性 | 变量 (Variable) | 属性 (Property) |
 * |------|----------------|----------------|
 * | Stub 类型 | [CangJieVariableStubImpl] | [CangJiePropertyStubImpl] |
 * | Accessor | ❌ 无 | ✅ getter/setter |
 * | 典型位置 | 包级别、类成员 | 类成员 |
 * | 元数据 | [VariableWrapper] | [PropertyWrapper] |
 * | 使用场景 | 简单数据存储 | 带逻辑的数据访问 |
 *
 * ## 顶层变量 vs 成员变量
 *
 * | 特性 | 顶层变量 | 成员变量 |
 * |------|---------|---------|
 * | fqName | 有（用于索引） | 无 |
 * | isTopLevel | true | false |
 * | 可见性 | public/internal | public/private/protected |
 * | 容器 | Package | Class |
 *
 * @property parentStub 父 Stub（文件 Stub 或类体 Stub）
 * @property outerContext 外部构建上下文
 * @property metadataContainer 元数据容器（Package 或 Class）
 * @property variableWrapper 变量包装器（来自元数据）
 *
 * @see CangJieVariableStubImpl
 * @see PropertyClsStubBuilder
 * @see COMPILED_DEFAULT_INITIALIZER
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
        // fqName 现在存储在绑定模式中，而不是变量本身
        val fqName = if (isTopLevel) outerContext.containerFqName.child(varName) else null

        val variableStub = CangJieVariableStubImpl(
            parentStub,
            PatternKind.BINDING, // Decompiled code always uses simple binding pattern
            variableWrapper.isVar,
            isTopLevel,
            hasInitializer = variableWrapper.declaresDefaultValue,
            hasReturnTypeRef = true,
            origin = null
        )

        // 创建注解 Stub（注解在修饰符列表之前）
        createAnnotationsStub(variableStub, variableWrapper.annotations)

        createModifierListStubForDeclaration(
            variableStub,
            variableWrapper.visibility,
            variableWrapper.modality
        )

        // 创建绑定模式 Stub（用于存储变量名和 fqName）
        CangJieBindingPatternStubImpl(variableStub, varName.ref(), fqName)
            .also {
                CangJieNameReferenceExpressionStubImpl(
                    it,
                    varName.ref()
                )

            }
        TypeClsStubBuilder(variableStub, outerContext).createTypeReferenceStub(variableWrapper.returnType)

        // 如果有初始化器，创建 REFERENCE_EXPRESSION stub 占位符
        if (variableWrapper.declaresDefaultValue) {
            CangJieNameReferenceExpressionStubImpl(
                variableStub,
                StringRef.fromString("COMPILED_CODE")!!
            )
        }
    }
}

/**
 * 成员字段 Stub 构建器
 *
 * ## 功能说明
 *
 * 为类成员字段（let/var/const 声明）创建 [CangJieFieldStubImpl]。
 * 成员字段是类、结构体、枚举的成员变量，不支持模式匹配解构。
 *
 * ## 与变量的区别
 *
 * | 特性 | 字段 (Field) | 变量 (Variable) |
 * |------|-------------|----------------|
 * | 位置 | 类成员 | 顶层或局部 |
 * | 模式匹配 | ❌ 不支持 | ✅ 支持 |
 * | Stub 类型 | [CangJieFieldStubImpl] | [CangJieVariableStubImpl] |
 *
 * @property parentStub 父 Stub（类体 Stub）
 * @property outerContext 外部构建上下文
 * @property metadataContainer 元数据容器（Class）
 * @property variableWrapper 变量包装器（来自元数据）
 *
 * @see CangJieFieldStubImpl
 * @see VariableClsStubBuilder
 */
class FieldClsStubBuilder(
    private val parentStub: StubElement<out PsiElement>,
    private val outerContext: ClsStubBuilderContext,
    private val metadataContainer: MetadataContainer,
    private val variableWrapper: VariableWrapper
) {
    fun build() {
        val fieldName = variableWrapper.name
        // 成员字段的 fqName = 类的 fqName + 字段名
        val fqName = outerContext.containerFqName.child(fieldName)

        val fieldStub = CangJieFieldStubImpl(
            parentStub,
            fieldName.ref(),
            fqName,
            variableWrapper.isVar,
            variableWrapper.isConst,
            hasInitializer = variableWrapper.declaresDefaultValue,
            hasReturnTypeRef = true,
            origin = null
        )

        // 创建注解 Stub（注解在修饰符列表之前）
        createAnnotationsStub(fieldStub, variableWrapper.annotations)

        createModifierListStubForDeclaration(
            fieldStub,
            variableWrapper.visibility,
            variableWrapper.modality
        )

        TypeClsStubBuilder(fieldStub, outerContext).createTypeReferenceStub(variableWrapper.returnType)

        // 如果有初始化器，创建 REFERENCE_EXPRESSION stub 占位符
        if (variableWrapper.declaresDefaultValue) {
            CangJieNameReferenceExpressionStubImpl(
                fieldStub,
                StringRef.fromString("COMPILED_CODE")!!
            )
        }
    }
}

/**
 * 扩展声明 Stub 构建器
 *
 * ## 功能说明
 *
 * 为仓颉语言的扩展声明（Extend Declaration）创建 [CangJieExtendStubImpl]。
 * 扩展声明允许为现有类型添加新的方法和属性，类似于 Kotlin 的扩展函数。
 *
 * ## 扩展声明特点
 *
 * - **类型扩展**: 为已有类型（包括跨包类型）添加成员
 * - **接口实现**: 扩展声明可以让类型实现新的接口
 * - **类型参数**: 支持泛型约束的扩展
 * - **成员添加**: 可以添加函数、属性、变量
 *
 * ## Stub 构建流程
 *
 * ```
 * ExtendClsStubBuilder.build()
 *   ├─ 创建 CangJieExtendStub
 *   │   ├─ 扩展 ID（生成的唯一标识）
 *   │   ├─ fqName（完全限定名）
 *   │   └─ superTypeRefs（实现的接口列表）
 *   ├─ createTypeReferenceStub()
 *   │   └─ 被扩展的类型引用
 *   ├─ createSuperTypeListStub()
 *   │   └─ 实现的接口列表
 *   ├─ createTypeParameterListStub()
 *   │   └─ 扩展的类型参数（如果有）
 *   └─ createClassBodyStub()
 *       ├─ 创建扩展函数 Stubs
 *       ├─ 创建扩展属性 Stubs
 *       └─ 创建扩展变量 Stubs
 * ```
 *
 * ## PSI 结构示例
 *
 * 对于扩展声明 `extend ArrayList<T> : Comparable<ArrayList<T>> where T: Comparable<T>`：
 *
 * ```
 * CangJieExtendStub (ID: "extend_ArrayList_123")
 *   ├─ TypeReference (ArrayList<T>) - 被扩展类型
 *   ├─ CjSuperTypeList
 *   │   └─ CjSuperTypeEntry
 *   │       └─ TypeReference (Comparable<ArrayList<T>>)
 *   ├─ CjTypeParameterList
 *   │   └─ CjTypeParameter ("T")
 *   └─ CjClassBody
 *       ├─ CjNamedFunction (扩展方法 1)
 *       ├─ CjNamedFunction (扩展方法 2)
 *       └─ CjProperty (扩展属性)
 * ```
 *
 * ## 扩展 ID 生成
 *
 * 扩展声明没有用户定义的名称，使用生成的 ID：
 * - 格式：基于 [ExtendWrapper.id]
 * - 用途：唯一标识一个扩展声明
 * - fqName：`packageName.extendId`
 *
 * ## 类型参数上下文
 *
 * 扩展声明的类型参数创建新的上下文：
 * - **外部上下文** ([outerContext]): 包的类型参数
 * - **内部上下文** ([innerContext]): 加上扩展自己的类型参数
 *
 * 扩展成员可以引用这些类型参数。
 *
 * ## 成员创建
 *
 * 扩展体中的成员被视为包级声明：
 * - 使用 [MetadataContainer.Package] 容器
 * - 支持函数、属性、变量
 * - 不支持嵌套类
 *
 * ## 与类声明的区别
 *
 * | 特性 | 扩展声明 | 类声明 |
 * |------|---------|--------|
 * | 名称 | 生成的 ID | 用户定义 |
 * | 类型 | 已有类型 + 新接口 | 新类型 |
 * | 成员 | 只能添加方法/属性 | 可以有构造函数 |
 * | Stub 类型 | [CangJieExtendStubImpl] | [CangJieClassStubImpl] |
 *
 * ## 使用场景
 *
 * 1. **接口实现**: 为已有类型实现新接口
 *    ```kotlin
 *    extend MyClass : Serializable { ... }
 *    ```
 *
 * 2. **方法扩展**: 为已有类型添加方法
 *    ```kotlin
 *    extend String {
 *        func reverse(): String { ... }
 *    }
 *    ```
 *
 * 3. **泛型扩展**: 为泛型类型添加约束实现
 *    ```kotlin
 *    extend Array<T> : Comparable<Array<T>> where T: Comparable<T> { ... }
 *    ```
 *
 * @property parentStub 父 Stub（通常是文件 Stub）
 * @property outerContext 外部构建上下文
 * @property extendWrapper 扩展声明包装器（来自元数据）
 *
 * @see CangJieExtendStubImpl
 * @see ExtendWrapper
 */
class ExtendClsStubBuilder(
    private val parentStub: StubElement<out PsiElement>,
    private val outerContext: ClsStubBuilderContext,
    private val extendWrapper: ExtendWrapper
) {
    fun build() {

        // 从被扩展类型中提取真实的类型名称，用于 Stub 索引


        // 例如：extend Int64 <: Comparable 应该提取 "Int64" 而不是编码的 exportId


        // 这确保 CangJieExtendByReceiverIndex 使用正确的类型名称作为索引键


        val receiverTypeName = TypeClsStubBuilder.extractTypeName(extendWrapper.type, outerContext)
        val extendId = extendWrapper.id.toName()
        val fqName = extendWrapper.packageFqName.child(extendId)

        val superTypeRefs = extendWrapper.superTypes
            .mapNotNull { TypeClsStubBuilder.extractTypeName(it, outerContext) }
            .map { it.ref() }
            .toTypedArray()

        val extendStub = CangJieExtendStubImpl(
            CjStubElementTypes.EXTEND,
            parentStub,
            fqName.ref(),
            null,
            extendId.ref(),
            superTypeRefs,
            receiverTypeName?.asString() ?: extendId.asString()
        )

        // 先创建注解 Stub（注解在修饰符列表之前）
        createAnnotationsStub(extendStub, extendWrapper.annotations)

        // 创建修饰符列表（extend 声明没有可见性修饰符，创建空的）
        createEmptyModifierListStub(extendStub)

        // 创建类型参数列表（对于 extend，类型参数在类型引用之前）
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

        // 创建被扩展类型引用
        TypeClsStubBuilder(extendStub, innerContext).createTypeReferenceStub(extendWrapper.type)

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
                TypeClsStubBuilder(superTypeEntryStub, innerContext).createTypeReferenceStub(superType)
            }
        }

        // 创建类型约束列表（where 子句）
        val constraintsToCreate = mutableListOf<Pair<Name, TypeWrapper>>()
        for (typeParam in extendWrapper.typeParameters) {
            for (upper in typeParam.uppers) {
                constraintsToCreate.add(Pair(typeParam.name, upper))
            }
        }
        if (constraintsToCreate.isNotEmpty()) {
            val constraintListStub = CangJiePlaceHolderStubImpl<CjTypeConstraintList>(
                extendStub,
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
                TypeClsStubBuilder(constraintStub, innerContext).createTypeReferenceStub(upperBound)
            }
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

        // 创建扩展字段 Stubs
        for (variable in extendWrapper.variables) {
            FieldClsStubBuilder(classBody, innerContext, metadataContainer, variable).build()
        }
    }
}

/**
 * 类型别名 Stub 构建器
 *
 * ## 功能说明
 *
 * 为仓颉语言的类型别名（Type Alias）创建 [CangJieTypeAliasStubImpl]。
 * 类型别名允许为复杂的类型定义简短的别名，提高代码可读性。
 *
 * ## 类型别名特点
 *
 * - **透明替换**: 类型别名在编译时被完全展开为实际类型
 * - **无运行时开销**: 不创建新的类型，只是语法糖
 * - **可见性控制**: 支持 public/internal/private 可见性
 * - **跨包引用**: 别名的展开类型可以来自其他包
 *
 * ## Stub 构建流程
 *
 * ```
 * TypeAliasClsStubBuilder.build()
 *   ├─ 创建 CangJieTypeAliasStub
 *   │   ├─ 别名名称
 *   │   └─ fqName（完全限定名）
 *   ├─ createModifierListStubForDeclaration()
 *   │   └─ 可见性修饰符
 *   └─ TypeClsStubBuilder.createTypeReferenceStub()
 *       └─ 展开类型（实际类型）
 * ```
 *
 * ## PSI 结构示例
 *
 * 对于类型别名 `public type StringMap = HashMap<String, String>`：
 *
 * ```
 * CangJieTypeAliasStub ("StringMap")
 *   ├─ CangJieModifierListStub (public)
 *   └─ TypeReference
 *       └─ UserType (HashMap<String, String>)
 *           ├─ NameReferenceExpression ("HashMap")
 *           └─ TypeArgumentList
 *               ├─ TypeProjection
 *               │   └─ UserType ("String")
 *               └─ TypeProjection
 *                   └─ UserType ("String")
 * ```
 *
 * ## 展开类型处理
 *
 * 展开类型（[TypeAliasWrapper.expandedType]）可以是任何类型：
 * - **简单类型**: `type Int32Alias = Int32`
 * - **泛型类型**: `type List<T> = ArrayList<T>`
 * - **函数类型**: `type Handler = (Int) -> Bool`
 * - **元组类型**: `type Pair = (Int, String)`
 * - **跨包类型**: `type MyList = std.collection.ArrayList<Int>`
 *
 * 通过 [TypeClsStubBuilder] 递归处理，支持嵌套的复杂类型。
 *
 * ## 与类声明的区别
 *
 * | 特性 | 类型别名 | 类声明 |
 * |------|---------|--------|
 * | 本质 | 类型的同义词 | 新的类型 |
 * | 运行时 | 无开销 | 有实例 |
 * | 成员 | 无 | 可以有方法/属性 |
 * | 构造函数 | 无 | 可以有 |
 * | Stub 类型 | [CangJieTypeAliasStubImpl] | [CangJieClassStubImpl] |
 *
 * ## 使用场景
 *
 * 1. **简化复杂类型**: 为长类型名创建短别名
 *    ```kotlin
 *    type Callback = (Result<Data, Error>) -> Unit
 *    ```
 *
 * 2. **提高可读性**: 为业务概念定义语义化名称
 *    ```kotlin
 *    type UserId = Int64
 *    type ProductId = Int64
 *    ```
 *
 * 3. **平台兼容**: 为平台特定类型创建统一接口
 *    ```kotlin
 *    type NativeHandle = IntNative
 *    ```
 *
 * ## 限制
 *
 * - **不支持类型参数**: 仓颉的类型别名不能有自己的类型参数
 * - **无递归定义**: 类型别名不能直接或间接引用自己
 * - **无成员**: 不能为类型别名添加方法或属性
 *
 * ## IDE 支持
 *
 * 类型别名的 Stub 支持：
 * - **代码补全**: 提供别名名称补全
 * - **跳转定义**: 从别名跳转到定义
 * - **查找引用**: 查找所有使用别名的位置
 * - **重命名重构**: 安全重命名别名
 *
 * @property parentStub 父 Stub（通常是文件 Stub）
 * @property outerContext 外部构建上下文
 * @property typeAliasWrapper 类型别名包装器（来自元数据）
 *
 * @see CangJieTypeAliasStubImpl
 * @see TypeAliasWrapper
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

        // 先创建注解 Stub（注解在修饰符列表之前）
        createAnnotationsStub(typeAliasStub, typeAliasWrapper.annotations)

        createModifierListStubForDeclaration(typeAliasStub, typeAliasWrapper.visibility, null)

        // 创建展开类型引用
        TypeClsStubBuilder(typeAliasStub, outerContext).createTypeReferenceStub(typeAliasWrapper.expandedType)
    }
}