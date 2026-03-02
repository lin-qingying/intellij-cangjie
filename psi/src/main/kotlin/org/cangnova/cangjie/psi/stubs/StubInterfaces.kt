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

package org.cangnova.cangjie.psi.stubs

import com.intellij.psi.PsiNamedElement
import com.intellij.psi.stubs.*
import org.cangnova.cangjie.lexer.CjKeywordToken
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.*

enum class ConstantValueKind {

    BOOLEAN_CONSTANT,
    FLOAT_CONSTANT,
    RUNE_CONSTANT,

    CHARACTER_BYTE_CONSTANT,
    INTEGER_CONSTANT,
    UNIT_CONSTANT,
}

interface CangJiePropertyAccessorStub : StubElement<CjPropertyAccessor> {
    fun isGetter(): Boolean
    fun hasBody(): Boolean
    fun hasBlockBody(): Boolean
}

interface CangJieCollectionLiteralExpressionStub : StubElement<CjCollectionLiteralExpression>

interface CangJieConstantExpressionStub : StubElement<CjConstantExpression> {
    fun kind(): ConstantValueKind
    fun value(): String
}

interface CangJieStubElement<T : CjElement> : StubElement<T> {
    /** Returns a copy of this stub with the parent set to [newParent] */
    fun copyInto(newParent: StubElement<*>?): CangJieStubElement<T>
}


interface CangJieFileStub : PsiFileStub<CjFile>, CangJieStubElement<CjFile> {
    fun getPackageFqName(): FqName

    val kind: CangJieFileStubKind
}

/**
 * CangJiePlaceHolderStub接口定义了一个通用的占位符 Stub 元素
 * 它继承自StubElement，用于表示CangJie解析树中的占位符节点
 * 这个接口是泛型的，允许它用于任何CjElement的子类
 *
 * @param T 表示泛型参数，限定了T必须是CjElement的子类
 */
interface CangJiePlaceHolderStub<T : CjElement> : StubElement<T>

interface CangJieAnnotationStub : StubElement<CjAnnotation> {
    fun getShortName(): String?
    fun hasValueArguments(): Boolean
}

interface CangJieMacroExpressionStub : StubElement<CjMacroExpression> {
    fun getShortName(): String?
    fun hasValueArguments(): Boolean
}

interface CangJieModifierListStub : StubElement<CjDeclarationModifierList> {
    fun hasModifier(modifierToken: CjKeywordToken): Boolean
}

interface CangJieContextReceiverStub : StubElement<CjContextReceiver> {
    fun getLabel(): String?
}

interface CangJieValueArgumentStub<T : CjValueArgument> : CangJiePlaceHolderStub<T> {
    fun isSpread(): Boolean
}

interface CangJieBasicTypeStub : StubElement<CjBasicType> {
    val basicType: String
}

interface CangJieUserTypeStub : StubElement<CjUserType>
interface CangJieTupleTypeStub : StubElement<CjTupleType>

interface CangJieClassifierStub {
    fun getClassId(): ClassId?
}

interface CangJieTypeAliasStub : CangJieClassifierStub, CangJieStubWithFqName<CjTypeAlias> {

}

/**
 * 变量声明模式类型
 *
 * 变量声明支持的模式匹配类型（不包含类型模式）
 */
enum class PatternKind {
    /** 通配符模式: `_` */
    WILDCARD,

    /** 绑定模式: `identifier` */
    BINDING,

    /** 元组模式: `(a, b)` */
    TUPLE,

    /** 枚举模式: `Some(x)` */
    ENUM;

    companion object {
        fun fromOrdinal(ordinal: Int): PatternKind = entries[ordinal]
    }
}

/**
 * 变量声明的 Stub 接口
 *
 * 变量声明是模式匹配的声明方式，用于顶层变量和局部变量。
 * 与 CangJieFieldStub 不同，Variable 支持模式匹配解构。
 *
 * 变量本身没有 fqName，因为一个变量声明可能包含多个绑定（如元组解构）。
 * fqName 存储在子模式（CangJieBindingPatternStub）中。
 */
interface CangJieVariableStub : StubElement<CjPatternVariable> {
    /** 获取模式类型 */
    fun getPatternKind(): PatternKind

    /** 是否为 var 声明（可变） */
    fun isVar(): Boolean

    /** 是否为顶层变量 */
    fun isTopLevel(): Boolean

    /** 是否有初始化器 */
    fun hasInitializer(): Boolean

    /** 是否有类型声明 */
    fun hasReturnTypeRef(): Boolean
}

// ==================== 模式 Stub 接口 ====================

/**
 * 模式 Stub 基础接口
 */
interface CangJiePatternStub<T : CjCasePatternElement> : StubElement<T>

/**
 * 绑定模式 Stub
 *
 * 存储绑定变量的名称和 fqName。
 * 如 `let a = 1` 中的 `a`，fqName 为 `package.a`。
 *
 * 对于 `let (a, b) = tuple`，会有两个绑定模式 Stub，
 * 分别存储 `a` 和 `b` 的 fqName。
 */
interface CangJieBindingPatternStub : CangJiePatternStub<CjBindingPattern>, NamedStub<CjBindingPattern> {
    /** 获取完全限定名（仅顶层变量的绑定有效） */
    val fqName: FqName?

}

/**
 * 元组模式 Stub
 *
 * 子 stub 包含元组中的各个模式
 * 如 `let (a, b) = tuple` 中的 `(a, b)`
 */
interface CangJieTuplePatternStub : CangJiePatternStub<CjTuplePattern>

/**
 * 类型模式 Stub
 *
 * ```cangjie
 * match(a){
 *   case b:Int => {}
 * }
 * ```
 */
interface CangJieTypePatternStub : CangJiePatternStub<CjTypePattern>, NamedStub<CjTypePattern>

/**
 * 枚举模式 Stub
 *
 * 存储枚举类型引用，子 stub 包含参数模式
 * 如 `let Some(x) = optional` 中的 `Some(x)`
 */
interface CangJieEnumPatternStub : CangJiePatternStub<CjEnumPattern>

/**
 * 通配符模式 Stub
 *
 * 如 `let _ = ignored` 中的 `_`
 */
interface CangJieWildcardPatternStub : CangJiePatternStub<CjWildcardPattern>

/**
 * 常量模式 Stub
 *
 * 如 `case 1 => ...` 中的常量
 */
interface CangJieConstantPatternStub : CangJiePatternStub<CjConstantPattern>

/**
 * Match 条件表达式模式 Stub
 */
interface CangJieMatchConditionStub : CangJiePatternStub<CjMatchConditionWithExpression>

interface CangJiePropertyStub : CangJieCallableStubBase<CjProperty> {
    fun hasReturnTypeRef(): Boolean

    override fun isTopLevel(): Boolean = false
    override fun isExtension(): Boolean = false
}

/**
 * 类成员字段的 Stub 接口
 *
 * 与 CangJieVariableStub 不同，Field 是类/结构体/接口的成员变量声明，
 * 不支持模式匹配，只有简单的标识符名称。
 *
 * 示例:
 * ```cangjie
 * class Person {
 *     let name: String      // Field
 *     var age: Int64        // Field
 *     const MAX_AGE = 150   // Field
 * }
 * ```
 */
interface CangJieFieldStub : CangJieCallableStubBase<CjFieldVariable> {
    fun isVar(): Boolean
    fun isConst(): Boolean
    fun hasInitializer(): Boolean
    fun hasReturnTypeRef(): Boolean

    override fun isTopLevel(): Boolean = false
}

interface CangJieCallableStubBase<TDeclaration : CjCallableDeclaration> : CangJieStubWithFqName<TDeclaration> {
    fun isTopLevel(): Boolean
    fun isExtension(): Boolean
}

interface CangJieStubWithFqName<T : PsiNamedElement> : NamedStub<T> {
    fun getFqName(): FqName?
}

interface CangJieTypeParameterStub : CangJieStubWithFqName<CjTypeParameter> {
//    fun isInVariance(): Boolean
}

interface CangJieNameBasicReferenceExpressionStub : StubElement<CjNameBasicReferenceExpression> {
    fun getReferencedName(): String
}

interface CangJieNameReferenceExpressionStub : StubElement<CjNameReferenceExpression> {
    fun getReferencedName(): String
}

interface CangJieParameterStubBase<T : PsiNamedElement> : CangJieStubWithFqName<T>
interface CangJieCatchParameterStub : CangJieParameterStubBase<CjCatchParameter>

interface CangJieParameterStub : CangJieParameterStubBase<CjParameter> {
    fun isMutable(): Boolean
    fun hasLetOrVar(): Boolean
    fun hasDefaultValue(): Boolean
    fun isNamed(): Boolean
}

interface CangJieClassStub : CangJieTypeStatementStub<CjClass> {

}

interface CangJieStructStub : CangJieTypeStatementStub<CjStruct>

interface CangJieInterfaceStub : CangJieTypeStatementStub<CjInterface>

interface CangJieEnumStub : CangJieTypeStatementStub<CjEnum> {
    /**
     * 是否非穷尽枚举
     */
    fun isNonExhaustive(): Boolean


}

/**
 * 枚举构造器的 Stub 接口（全量重构版本）
 *
 * 根据仓颉语言规范，枚举条目是构造器（constructors），而非类型声明。
 * 因此此接口直接继承 CangJieStubWithFqName，不再继承 CangJieTypeStatementStub。
 *
 * 示例:
 * ```cangjie
 * enum RGBColor {
 *     | Red | Green | Blue              // 无参数构造器
 *     | Red(UInt8) | Green(UInt8) | Blue(UInt8)  // 有参数构造器
 * }
 * ```
 */
/**
 * 枚举构造器 Stub 接口
 *
 * 枚举构造器不是独立声明，只存储名称和参数类型数量（用于重载区分）。
 * 参数类型详情通过 TYPE_LIST 子树获取。
 */
interface CangJieEnumConstructorStub : NamedStub<CjEnumConstructor> {
    /**
     * 获取参数类型数量（用于区分重载的枚举构造器）
     */
    fun getTypeCount(): Int

    /**
     * 获取所属枚举的完全限定名（用于索引）
     */
    fun getEnumFqName(): FqName?
}

interface CangJieScriptStub : CangJieStubWithFqName<CjScript> {
    override fun getFqName(): FqName
}

interface CangJieExtendStub : CangJieTypeStatementStub<CjExtend> {
    val extendId: String;

    //被扩展类型名称
    val receiverTypeName: String?;
}


interface CangJieTypeStatementStub<T : CjTypeStatement> : CangJieClassifierStub, CangJieStubWithFqName<T> {
    fun isLocal(): Boolean
    fun getSuperNames(): List<String>
}

interface CangJieConstructorStub<T : CjConstructor<T>> :
    CangJieCallableStubBase<T> {
    fun hasBody(): Boolean
    val isPrimary: Boolean get() = false
    fun isDelegatedCallToThis(): Boolean
}

interface CangJieImportAliasStub : StubElement<CjImportAlias> {
    fun getName(): String?
}


interface CangJieFunctionStub<F : CjFunction> : CangJieCallableStubBase<F> {
    fun hasBlockBody(): Boolean
    fun hasBody(): Boolean
    fun hasTypeParameterListBeforeFunctionName(): Boolean
}

interface CangJieNamedFunctionStub : CangJieFunctionStub<CjNamedFunction>

interface CangJieMainFunctionStub : CangJieFunctionStub<CjMainFunction> {
    override fun hasBlockBody(): Boolean {
        return true
    }

    override fun hasBody(): Boolean {
        return true
    }

    override fun hasTypeParameterListBeforeFunctionName(): Boolean {
        return false
    }
}

interface CangJieMacroStub : CangJieFunctionStub<CjMacroDeclaration>


interface CangJieForeignDirectiveStub : StubElement<CjForeignDirective>


interface CangJiePackageDirectiveStub : StubElement<CjPackageDirective>


interface CangJieImportDirectiveStub : StubElement<CjImportDirective> {
    /**
     * 获取包的完全限定名
     */
    fun getPackageFqName(): FqName?

    /**
     * 获取所有导入项的信息
     * 返回 List<ImportItemInfo>，每个元素包含：
     * - importedFqName: 导入的完全限定名
     * - isAllUnder: 是否是通配符导入
     * - aliasName: 别名(如果有)
     */
    fun getImportItems(): List<ImportItemInfo>

    /**
     * 导入项信息数据类
     */
    data class ImportItemInfo(
        val importedFqName: FqName?,
        val isAllUnder: Boolean,
        val aliasName: String?
    )
}


interface CangJieTypeProjectionStub : StubElement<CjTypeProjection> {
    fun getProjectionKind(): CjProjectionKind
}

interface CangJiePlaceHolderWithTextStub<T : CjElement> : CangJiePlaceHolderStub<T> {
    fun text(): String
}

interface CangJieFunctionTypeStub : StubElement<CjFunctionType>
