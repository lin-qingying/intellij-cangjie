package com.huawei.cangjie.psi.stubs

import com.huawei.cangjie.descriptors.DescriptorVisibility
import com.huawei.cangjie.lang.declarations.CjDeclarationsFile
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
    RUNE_CONSTANT,

    //    CHARACTER_BYTE_LITERAL,
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

interface CangJieFilesStub{
    fun getPackageFqName(): FqName

}
//interface CangJieDeclarationsFileStub : PsiFileStub<CjDeclarationsFile > ,CangJieFileStub{
//
////    fun findImportsByAlias(alias: String): List<CangJieImportDirectiveStub>
//}
interface CangJieFileStub : PsiFileStub<CjFile > ,CangJieFilesStub{

//    fun findImportsByAlias(alias: String): List<CangJieImportDirectiveStub>
}

interface CangJiePlaceHolderStub<T : CjElement> : StubElement<T>

interface CangJieAnnotationEntryStub : StubElement<CjAnnotationEntry> {
    fun getShortName(): String?
    fun hasValueArguments(): Boolean
}

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

interface CangJieNameReferenceExpressionStub : StubElement<CjNameReferenceExpression> {
    fun getReferencedName(): String
}

interface CangJieParameterStub : CangJieStubWithFqName<CjParameter> {
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
interface CangJieExtendStub : CangJieTypeStatementStub<CjExtend>

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


interface CangJieFunctionStub : CangJieCallableStubBase<CjFunctionImpl> {
    fun hasBlockBody(): Boolean
    fun hasBody(): Boolean
    fun hasTypeParameterListBeforeFunctionName(): Boolean
//    fun mayHaveContract(): Boolean
}

interface CangJieForeignDirectiveStub : StubElement<CjForeignDirective>

//interface CangJieImportDirectiveItemStub : StubElement<CjImportDirectiveItem> {
////    fun isAllUnder(): Boolean
////    fun get_importedFqName(): FqName?
////    fun isValid(): Boolean
//}
interface CangJiePackageDirectiveStub :  StubElement<CjPackageDirective>{
    fun getModifierVisibility(): DescriptorVisibility

}

interface CangJieImportDirectiveStub : StubElement<CjImportDirective> {
    fun isAllUnder(): Boolean
    fun getImportedFqName(): FqName?
//    fun getImportedFqNames(): List<FqName>
    fun isValid(): Boolean


    fun getModifierVisibility(): DescriptorVisibility


}

interface CangJieTypeProjectionStub : StubElement<CjTypeProjection> {
    fun getProjectionKind(): CjProjectionKind
}

//interface CangJieMainFunctionStub : CangJieFunctionStub
interface CangJiePlaceHolderWithTextStub<T : CjElement> : CangJiePlaceHolderStub<T> {
    fun text(): String
}
