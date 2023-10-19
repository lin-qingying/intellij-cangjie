package com.huawei.cangjie1.psi.stubs

import com.huawei.cangjie1.psi.CjFile
import com.huawei.cangjie1.lexer.CjModifierKeywordToken
import com.huawei.cangjie1.name.ClassId
import com.huawei.cangjie1.name.FqName
import com.huawei.cangjie1.psi.*
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.stubs.NamedStub
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.stubs.StubElement


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
interface CangJieBasicTypeStub : StubElement<CjBasicType>

interface CangJieClassifierStub {
    fun getClassId(): ClassId?
}
interface CangJiePropertyStub : CangJieCallableStubBase<CjProperty> {
    fun isVar(): Boolean

    fun hasInitializer(): Boolean
    fun hasReturnTypeRef(): Boolean
}
interface CangJieCallableStubBase<TDeclaration : CjCallableDeclaration> : CangJieStubWithFqName<TDeclaration> {
    fun isTopLevel(): Boolean
    fun isExtension(): Boolean
}
interface CangJieStubWithFqName<T : PsiNamedElement> : NamedStub<T> {
    fun getFqName(): FqName?
}
interface CangJieTypeParameterStub : CangJieStubWithFqName<CjTypeParameter> {
    fun isInVariance(): Boolean

}
interface CangJieNameReferenceExpressionStub : StubElement<CjNameReferenceExpression> {
    fun getReferencedName(): String
}
interface CangJieParameterStub : CangJieStubWithFqName<CjParameter> {
    fun isMutable(): Boolean
    fun hasValOrVar(): Boolean
    fun hasDefaultValue(): Boolean
}
interface CangJieClassStub : CangJieClassOrObjectStub<CjClass> {
    fun isInterface(): Boolean
//    fun isEnumEntry(): Boolean
}
interface CangJieClassOrObjectStub<T : CjClassOrObject> : CangJieClassifierStub, CangJieStubWithFqName<T> {
    fun isLocal(): Boolean
    fun getSuperNames(): List<String>
    fun isTopLevel(): Boolean
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
    fun mayHaveContract(): Boolean
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
