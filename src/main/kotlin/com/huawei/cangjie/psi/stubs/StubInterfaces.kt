package com.huawei.cangjie.psi.stubs

import com.huawei.cangjie.lexer.CjModifierKeywordToken
import com.huawei.cangjie.name.ClassId
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.*
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.stubs.NamedStub
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.stubs.StubElement

enum class ConstantValueKind {

    BOOLEAN_CONSTANT,
    FLOAT_CONSTANT,
    CHARACTER_CONSTANT,
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
interface CangJieFileStub : PsiFileStub<CjFile> {
    fun getPackageFqName(): FqName

//    fun findImportsByAlias(alias: String): List<CangJieImportDirectiveStub>
}
interface CangJiePlaceHolderStub<T : CjElement> : StubElement<T>


interface CangJieModifierListStub : StubElement<CjDeclarationModifierList> {
    fun hasModifier(modifierToken: CjModifierKeywordToken): Boolean
}
interface CangJieContextReceiverStub : StubElement<CjContextReceiver> {
    fun getLabel(): String?
}
interface CangJieValueArgumentStub<T : CjValueArgument> : CangJiePlaceHolderStub<T> {
    fun isSpread(): Boolean
}
interface CangJieUserTypeStub : StubElement<CjUserType>
interface CangJieTupleTypeStub : StubElement<CjTupleType>
interface CangJieBasicTypeStub : StubElement<CjBasicType>

interface CangJieClassifierStub {
    fun getClassId(): ClassId?
}
interface CangJieVariableStub : CangJieCallableStubBase<CjVariable> {
    fun isVar(): Boolean

    fun hasInitializer(): Boolean
    fun hasReturnTypeRef(): Boolean
}

interface CangJiePropertyStub:CangJieStubWithFqName<CjProperty>
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
interface CangJieNameReferenceExpressionStub : StubElement<CjNameReferenceExpression> {
    fun getReferencedName(): String
}
interface CangJieParameterStub : CangJieStubWithFqName<CjParameter> {
    fun isMutable(): Boolean
    fun hasValOrVar(): Boolean
    fun hasDefaultValue(): Boolean
}
interface CangJieClassStub : CangJieClassOrStructStub<CjClass> {
//    fun isInterface(): Boolean
//    fun isEnumEntry(): Boolean
}
interface  CangJieStructStub: CangJieClassOrStructStub<CjStruct> {

}
interface  CangJieInterfaceStub: CangJieClassOrStructStub<CjInterface> {

}

interface  CangJieEnumStub : CangJieClassOrStructStub<CjEnum>
interface  CangJieExtendStub : CangJieClassOrStructStub<CjExtend>

interface CangJieClassOrStructStub<T : CjClassOrStruct> : CangJieClassifierStub, CangJieStubWithFqName<T> {
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


interface CangJieFunctionStub  : CangJieCallableStubBase<CjFunctionImpl> {
    fun hasBlockBody(): Boolean
    fun hasBody(): Boolean
    fun hasTypeParameterListBeforeFunctionName(): Boolean
//    fun mayHaveContract(): Boolean
}

interface CangJieImportDirectiveItemStub : StubElement<CjImportDirectiveItem> {
//    fun isAllUnder(): Boolean
//    fun getImportedFqName(): FqName?
//    fun isValid(): Boolean
}


interface CangJieImportDirectiveStub : StubElement<CjImportDirective> {
    fun isAllUnder(): Boolean
    fun getImportedFqName(): FqName?
    fun isValid(): Boolean
}

interface CangJieTypeProjectionStub : StubElement<CjTypeProjection> {
    fun getProjectionKind(): CjProjectionKind
}
//interface CangJieMainFunctionStub : CangJieFunctionStub
interface CangJiePlaceHolderWithTextStub<T : CjElement> : CangJiePlaceHolderStub<T> {
    fun text(): String
}
