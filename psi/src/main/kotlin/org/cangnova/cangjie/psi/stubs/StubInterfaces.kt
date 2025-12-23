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
import com.intellij.util.io.StringRef
import org.cangnova.cangjie.lexer.CjKeywordToken
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.*
import java.io.IOException

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

interface CangJieVariableStub : CangJieCallableStubBase<CjVariable> {
    fun isVar(): Boolean

    fun hasInitializer(): Boolean
    fun hasReturnTypeRef(): Boolean

    data class ChildInfo(
        val name: StringRef?,
        val fqName: FqName?,
    ) {

        fun serialize(dataStream: StubOutputStream) {
            dataStream.writeName(name?.string)

            dataStream.writeName(fqName?.asString())
        }

        companion object {
            @Throws(IOException::class)
            fun deserialize(dataStream: StubInputStream): ChildInfo {
                val name = dataStream.readName()
                val fqNameAsString = dataStream.readName()
                val fqName = if (fqNameAsString != null) FqName(fqNameAsString.toString()) else null

                return ChildInfo(name, fqName)
            }
        }
    }

    //    处于模式匹配的子模块
    val childNamesByPattern: List<ChildInfo> get() = emptyList()
}

interface CangJiePropertyStub : CangJieCallableStubBase<CjProperty> {
    fun hasReturnTypeRef(): Boolean

    override fun isTopLevel(): Boolean = false
    override fun isExtension(): Boolean = false
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

interface CangJieEnumStub : CangJieTypeStatementStub<CjEnum>

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
interface CangJieEnumConstructorStub : CangJieStubWithFqName<CjEnumConstructor> {
    /**
     * 获取所属枚举类型的完全限定名
     */
    fun getParentEnumFqName(): FqName?

    /**
     * 获取构造器的参数数量（用于区分重载）
     */
    fun getParameterCount(): Int

    /**
     * 获取参数类型名称列表
     */
    fun getParameterTypeNames(): List<String>

    /**
     * 是否为本地枚举条目
     */
    fun isLocal(): Boolean

    /**
     * 获取 ClassId（用于元数据查找）
     */
    fun getClassId(): ClassId?
}

interface CangJieScriptStub : CangJieStubWithFqName<CjScript> {
    override fun getFqName(): FqName
}

interface CangJieExtendStub : CangJieTypeStatementStub<CjExtend> {

}


interface CangJieTypeStatementStub<T : CjTypeStatement> : CangJieClassifierStub, CangJieStubWithFqName<T> {
    fun isLocal(): Boolean
    fun getSuperNames(): List<String>
}

interface CangJieConstructorStub<T : CjConstructor<T>> :
    CangJieCallableStubBase<T> {
    fun hasBody(): Boolean
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

    fun getPackageFqName(): FqName?
}

interface CangJieImportDirectiveItemStub : StubElement<CjImportDirectiveItem> {
    fun isAllUnder(): Boolean
    fun getImportedFqName(): FqName?
    fun isValid(): Boolean

    fun getPackageFqName(): FqName?
}

interface CangJieTypeProjectionStub : StubElement<CjTypeProjection> {
    fun getProjectionKind(): CjProjectionKind
}

interface CangJiePlaceHolderWithTextStub<T : CjElement> : CangJiePlaceHolderStub<T> {
    fun text(): String
}

interface CangJieFunctionTypeStub : StubElement<CjFunctionType>
