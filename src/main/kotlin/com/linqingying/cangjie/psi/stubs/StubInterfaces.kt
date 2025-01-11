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

package com.linqingying.cangjie.psi.stubs

import com.intellij.psi.PsiNamedElement
import com.intellij.psi.stubs.*
import com.intellij.util.io.StringRef
import com.linqingying.cangjie.descriptors.DescriptorVisibility
import com.linqingying.cangjie.lexer.CjKeywordToken
import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.psi.*
import java.io.IOException

enum class ConstantValueKind {

    BOOLEAN_CONSTANT,
    FLOAT_CONSTANT,
    RUNE_CONSTANT,

    CHARACTER_BYTE_CONSTANT,
    INTEGER_CONSTANT,
    UNIT_CONSTANT
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

interface CangJieFilesStub {
    fun getPackageFqName(): FqName

}

//interface CangJieDeclarationsFileStub : PsiFileStub<CjDeclarationsFile > ,CangJieFileStub{
//
////    fun findImportsByAlias(alias: String): List<CangJieImportDirectiveItemStub>
//}
interface CangJieFileStub : PsiFileStub<CjFile>, CangJieFilesStub {

//    fun findImportsByAlias(alias: String): List<CangJieImportDirectiveItemStub>
}

/**
 * CangJiePlaceHolderStub接口定义了一个通用的占位符 Stub 元素
 * 它继承自StubElement，用于表示CangJie解析树中的占位符节点
 * 这个接口是泛型的，允许它用于任何CjElement的子类
 *
 * @param T 表示泛型参数，限定了T必须是CjElement的子类
 */
interface CangJiePlaceHolderStub<T : CjElement> : StubElement<T>

interface CangJieAnnotationEntryStub : StubElement<CjAnnotationEntry> {
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
//interface CangJieBasicTypeStub : StubElement<CjBasicType>

interface CangJieClassifierStub {
    fun getClassId(): ClassId?
}

interface CangJieTypeAliasStub : CangJieClassifierStub, CangJieStubWithFqName<CjTypeAlias> {
//    fun isTopLevel(): Boolean

}

interface CangJieVariableStub : CangJieCallableStubBase<CjVariable> {
    fun isVar(): Boolean

    fun hasInitializer(): Boolean
    fun hasReturnTypeRef(): Boolean

    data class ChildInfo(
        val name: StringRef?, val fqName: FqName?
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
    fun hasValOrVar(): Boolean
    fun hasDefaultValue(): Boolean
}

interface CangJieClassStub : CangJieTypeStatementStub<CjClass> {
//    fun isInterface(): Boolean
//    fun isEnumEntry(): Boolean
}

interface CangJieStructStub : CangJieTypeStatementStub<CjStruct>

interface CangJieInterfaceStub : CangJieTypeStatementStub<CjInterface>

interface CangJieEnumStub : CangJieTypeStatementStub<CjEnum>
interface CangJieEnumEntryStub : CangJieTypeStatementStub<CjEnumEntry> {
    val fqNameByPackage: FqName?
}
interface CangJieScriptStub : CangJieStubWithFqName<CjScript> {
    override fun getFqName(): FqName
}

interface CangJieExtendStub : CangJieTypeStatementStub<CjExtend> {

//    fun getClassId(): ClassId?
}

//interface CangJieExtendStub : StubElement<CjExtend>{
//    fun getSuperNames(): List<String>
//    fun getFqName(): FqName?
////    fun getClassId(): ClassId?
//}
interface CangJieTypeStatementStub<T : CjTypeStatement> : CangJieClassifierStub, CangJieStubWithFqName<T> {
    fun isLocal(): Boolean
    fun getSuperNames(): List<String>
//    fun isTopLevel(): Boolean
}

interface CangJieConstructorStub<T : CjConstructor<T>> :
    CangJieCallableStubBase<T> {
    fun hasBody(): Boolean
    fun isDelegatedCallToThis(): Boolean
}

interface CangJieImportAliasStub : StubElement<CjImportAlias> {
    fun getName(): String?
}
//interface CangJieFunctionStub : CangJieCallableStubBase<CjNamedFunction> {
//    fun hasBlockBody(): Boolean
//    fun hasBody(): Boolean
//    fun hasTypeParameterListBeforeFunctionName(): Boolean
//    fun mayHaveContract(): Boolean
//}

interface CangJieFunctionForExtendStub : CangJieFunctionStub

interface CangJieFunctionStub : CangJieCallableStubBase<CjFunctionImpl> {
    fun hasBlockBody(): Boolean
    fun hasBody(): Boolean
    fun hasTypeParameterListBeforeFunctionName(): Boolean

}

interface CangJieForeignDirectiveStub : StubElement<CjForeignDirective>


//}
interface CangJiePackageDirectiveStub : StubElement<CjPackageDirective> {
    fun getModifierVisibility(): DescriptorVisibility

}

//interface CangJieImportStub<T : PsiElement> : StubElement<T> {
//    fun getPackageFqName(): FqName?
//
//
//    fun getModifierVisibility(): DescriptorVisibility
//}

interface CangJieImportDirectiveStub : StubElement<CjImportDirective >{

    fun getModifierVisibility(): DescriptorVisibility
    fun getPackageFqName(): FqName?
}
interface CangJieImportDirectiveItemStub : StubElement<CjImportDirectiveItem> {
    fun isAllUnder(): Boolean
    fun getImportedFqName(): FqName?
    fun isValid(): Boolean


      fun getModifierVisibility(): DescriptorVisibility
      fun getPackageFqName(): FqName?

}

interface CangJieTypeProjectionStub : StubElement<CjTypeProjection> {
    fun getProjectionKind(): CjProjectionKind
}

//interface CangJieMainFunctionStub : CangJieFunctionStub
interface CangJiePlaceHolderWithTextStub<T : CjElement> : CangJiePlaceHolderStub<T> {
    fun text(): String
}

interface CangJieFunctionTypeStub : StubElement<CjFunctionType>
