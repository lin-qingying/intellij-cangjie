package com.linqingying.cangjie.metadata.decompiler

import com.google.protobuf.MessageLite
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import com.linqingying.cangjie.builtins.StandardNames
import com.linqingying.cangjie.lexer.CjModifierKeywordToken
import com.linqingying.cangjie.metadata.FlagsToModifiers
import com.linqingying.cangjie.metadata.ProtoBuf
import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.name.SpecialNames
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.stubs.CangJieUserTypeStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.linqingying.cangjie.psi.stubs.impl.*
import com.linqingying.cangjie.serialization.deserialization.AnnotatedCallableKind

fun computeParameterName(name: Name): Name {
    return when {
        name == SpecialNames.IMPLICIT_SET_PARAMETER -> StandardNames.DEFAULT_VALUE_PARAMETER
        SpecialNames.isAnonymousParameterName(name) -> Name.identifier("_")
        else -> name
    }
}
fun createIncompatibleAbiVersionFileStub() = createFileStub(FqName.ROOT )
fun createFileStub(packageFqName: FqName ): CangJieFileStubImpl {
    val fileStub = CangJieFileStubImpl.forFile(packageFqName, )
    setupFileStub(fileStub, packageFqName)
    return fileStub
}
private fun setupFileStub(fileStub: CangJieFileStubImpl, packageFqName: FqName) {
    val packageDirectiveStub = CangJiePlaceHolderStubImpl<CjPackageDirective>(fileStub, CjStubElementTypes.PACKAGE_DIRECTIVE)
    createStubForPackageName(packageDirectiveStub, packageFqName)
    CangJiePlaceHolderStubImpl<CjImportList>(fileStub, CjStubElementTypes.IMPORT_LIST)
}
fun createStubForPackageName(packageDirectiveStub: CangJiePlaceHolderStubImpl<CjPackageDirective>, packageFqName: FqName) {
    val segments = packageFqName.pathSegments()
    val iterator = segments.listIterator(segments.size)

    fun recCreateStubForPackageName(current: StubElement<out PsiElement>) {
        when (iterator.previousIndex()) {
            -1 -> return
            0 -> {
                CangJieNameReferenceExpressionStubImpl(current, iterator.previous().ref())
                return
            }
            else -> {
                val lastSegment = iterator.previous()
                val receiver = CangJiePlaceHolderStubImpl<CjDotQualifiedExpression>(current, CjStubElementTypes.DOT_QUALIFIED_EXPRESSION)
                recCreateStubForPackageName(receiver)
                CangJieNameReferenceExpressionStubImpl(receiver, lastSegment.ref())
            }
        }
    }

    recCreateStubForPackageName(packageDirectiveStub)
}
fun Name.ref() = StringRef.fromString(this.asString())!!

fun FqName.ref() = StringRef.fromString(this.asString())!!

fun createModifierListStub(
    parent: StubElement<out PsiElement>,
    modifiers: Collection<CjModifierKeywordToken>
): CangJieModifierListStubImpl? {
    if (modifiers.isEmpty()) {
        return null
    }
    return CangJieModifierListStubImpl(
        parent,
        ModifierMaskUtils.computeMask { it in modifiers },
        CjStubElementTypes.MODIFIER_LIST
    )
}

fun createTargetedAnnotationStubs(
    annotations: List<AnnotationWithTarget>,
    parent: CangJieStubBaseImpl<*>
) {
    if (annotations.isEmpty()) return

    annotations.forEach { annotation ->
        val (annotationWithArgs, target) = annotation
        val annotationEntryStubImpl = CangJieAnnotationEntryStubImpl(
            parent,
            shortName = annotationWithArgs.classId.shortClassName.ref(),
            hasValueArguments = false,
//            annotationWithArgs.args
        )
//        if (target != null) {
//            CangJieAnnotationUseSiteTargetStubImpl(annotationEntryStubImpl, StringRef.fromString(target.name)!!)
//        }
        val constructorCallee =
            CangJiePlaceHolderStubImpl<CjConstructorCalleeExpression>(annotationEntryStubImpl, CjStubElementTypes.CONSTRUCTOR_CALLEE)
        val typeReference = CangJiePlaceHolderStubImpl<CjTypeReference>(constructorCallee, CjStubElementTypes.TYPE_REFERENCE)
        createStubForTypeName(annotationWithArgs.classId, typeReference)
    }
}

/**
 * @param abbreviatedType The abbreviated type of the expanded type [typeClassId], if applicable. The abbreviated type always applies to the
 *  innermost type. For example, if we have `typealias Alias = Map.Entry`, `Alias` refers to `Entry` but not `Map`. It would be correct to
 *  refer back from `Entry` to `Alias`, but not from `Map` to `Alias`.
 *
 *  Furthermore, an outer type in an already expanded type (e.g. `Map` in `Map.Entry`) cannot have originated from a type alias, because
 */
fun createStubForTypeName(
    typeClassId: ClassId,
    parent: StubElement<out PsiElement>,
    abbreviatedType: CangJieClassTypeBean? = null,
    upperBoundFun: ((Int) -> CangJieTypeBean?)? = null,
    bindTypeArguments: (CangJieUserTypeStub, Int) -> Unit = { _, _ -> }
): CangJieUserTypeStub {
    val substituteWithAny = typeClassId.isLocal

    val fqName = if (substituteWithAny) StandardNames.FqNames.anyUFqName
    else typeClassId.asSingleFqName().toUnsafe()

    val segments = fqName.pathSegments().asReversed()
    assert(segments.isNotEmpty())
    val classesNestedLevel = segments.size - if (substituteWithAny) 1 else typeClassId.packageFqName.pathSegments().size

    fun recCreateStubForType(current: StubElement<out PsiElement>, level: Int): CangJieUserTypeStub {
        val lastSegment = segments[level]
        val userTypeStub = CangJieUserTypeStubImpl(current, upperBoundFun?.invoke(level), abbreviatedType.takeIf { level == 0 })
        if (level + 1 < segments.size) {
            recCreateStubForType(userTypeStub, level + 1)
        }
        CangJieNameReferenceExpressionStubImpl(userTypeStub, lastSegment.ref(), level < classesNestedLevel)
        if (!substituteWithAny) {
            bindTypeArguments(userTypeStub, level)
        }
        return userTypeStub
    }

    return recCreateStubForType(parent, level = 0)
}
fun createEmptyModifierListStub(parent: CangJieStubBaseImpl<*>): CangJieModifierListStubImpl {
    return CangJieModifierListStubImpl(
        parent,
        ModifierMaskUtils.computeMask { false },
       CjStubElementTypes.MODIFIER_LIST
    )
}
fun createAnnotationStubs(annotations: List<AnnotationWithArgs>, parent: CangJieStubBaseImpl<*>) {
    return createTargetedAnnotationStubs(annotations.map { AnnotationWithTarget(it, null) }, parent)
}
fun createStubForTypeName(
    typeClassId: ClassId,
    parent: StubElement<out PsiElement>,
    upperBoundFun: ((Int) -> CangJieTypeBean?)? = null,
    bindTypeArguments: (CangJieUserTypeStub, Int) -> Unit = { _, _ -> }
): CangJieUserTypeStub {
    val substituteWithAny = typeClassId.isLocal

    val fqName = if (substituteWithAny) StandardNames.FqNames.anyUFqName
    else typeClassId.asSingleFqName().toUnsafe()

    val segments = fqName.pathSegments().asReversed()
    assert(segments.isNotEmpty())
    val classesNestedLevel = segments.size - if (substituteWithAny) 1 else typeClassId.packageFqName.pathSegments().size

    fun recCreateStubForType(current: StubElement<out PsiElement>, level: Int): CangJieUserTypeStub {
        val lastSegment = segments[level]
        val userTypeStub = CangJieUserTypeStubImpl(current, upperBoundFun?.invoke(level))
        if (level + 1 < segments.size) {
            recCreateStubForType(userTypeStub, level + 1)
        }
        CangJieNameReferenceExpressionStubImpl(userTypeStub, lastSegment.ref(), level < classesNestedLevel)
        if (!substituteWithAny) {
            bindTypeArguments(userTypeStub, level)
        }
        return userTypeStub
    }

    return recCreateStubForType(parent, level = 0)
}
val MessageLite.annotatedCallableKind: AnnotatedCallableKind
    get() = when (this) {
        is ProtoBuf.Property -> AnnotatedCallableKind.PROPERTY
        is ProtoBuf.Function, is ProtoBuf.Constructor -> AnnotatedCallableKind.FUNCTION
        else -> throw IllegalStateException("Unsupported message: $this")
    }

fun createModifierListStubForDeclaration(
    parent: StubElement<out PsiElement>,
    flags: Int,
    flagsToTranslate: List<FlagsToModifiers> = listOf(),
    additionalModifiers: List<CjModifierKeywordToken> = listOf()
): CangJieModifierListStubImpl {
    assert(flagsToTranslate.isNotEmpty())

    val modifiers = flagsToTranslate.mapNotNull { it.getModifiers(flags) } + additionalModifiers
    return createModifierListStub(parent, modifiers)!!
}
