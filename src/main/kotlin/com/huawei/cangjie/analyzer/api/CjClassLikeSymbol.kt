package com.huawei.cangjie.analyzer.api

import com.huawei.cangjie.analyzer.CjAnalysisSession
import com.huawei.cangjie.analyzer.lifetime.withValidityAssertion
import com.huawei.cangjie.analyzer.api.types.CjType
import com.huawei.cangjie.name.ClassId
import com.huawei.cangjie.types.Variance

sealed class CjClassifierSymbol : CjSymbol, CjPossiblyNamedSymbol, CjDeclarationSymbol

interface CjSymbolWithKind : CjSymbol {
    val symbolKind: CjSymbolKind
}

enum class CjSymbolKind {
    TOP_LEVEL,
    CLASS_MEMBER,
    LOCAL,
    ACCESSOR,
    SAM_CONSTRUCTOR,
}

/**
 * A marker interface for symbols which could potentially be members of some class.
 *
 *
 */
interface CjPossibleMemberSymbol : CjSymbol

abstract class CjTypeParameterSymbol : CjClassifierSymbol(), CjNamedSymbol {
    context(CjAnalysisSession)
    abstract override fun createPointer(): CjSymbolPointer<CjTypeParameterSymbol>

    final override val typeParameters: List<CjTypeParameterSymbol>
        get() = withValidityAssertion { emptyList() }

    abstract val upperBounds: List<CjType>
    abstract val variance: Variance
    abstract val isReified: Boolean
}

interface CjSymbolWithMembers : CjSymbolWithDeclarations

interface CjSymbolWithDeclarations : CjSymbol
sealed class CjClassLikeSymbol : CjClassifierSymbol(), CjSymbolWithKind, CjPossibleMemberSymbol,
    CjPossibleMultiplatformSymbol {
    abstract val classIdIfNonLocal: ClassId?

    context(CjAnalysisSession)
    abstract override fun createPointer(): CjSymbolPointer<CjClassLikeSymbol>
}

sealed class CjClassOrObjectSymbol : CjClassLikeSymbol(), CjSymbolWithMembers {

    abstract val classKind: CjClassKind
    abstract val superTypes: List<CjType>

    context(CjAnalysisSession)
    abstract override fun createPointer(): CjSymbolPointer<CjClassOrObjectSymbol>
}

enum class CjClassKind {
    CLASS,
    ENUM_CLASS,
    ANNOTATION_CLASS,
    OBJECT,
    COMPANION_OBJECT,
    INTERFACE,
    ANONYMOUS_OBJECT;

    val isObject: Boolean get() = this == OBJECT || this == COMPANION_OBJECT || this == ANONYMOUS_OBJECT
    val isClass: Boolean get() = this == CLASS || this == ANNOTATION_CLASS || this == ENUM_CLASS
}
