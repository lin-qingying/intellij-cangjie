package com.huawei.cangjie.analyzer.api

import com.huawei.cangjie.analyzer.CjAnalysisSession
import com.huawei.cangjie.analyzer.components.CjAnalysisSessionComponent
import com.huawei.cangjie.analyzer.components.CjAnalysisSessionMixIn
import com.huawei.cangjie.analyzer.lifetime.withValidityAssertion
import com.huawei.cangjie.name.ClassId
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.*

public abstract class CjSymbolProvider : CjAnalysisSessionComponent() {
    public open fun getSymbol(psi: CjDeclaration): CjDeclarationSymbol = when (psi) {
//        is CjParameter -> getParameterSymbol(psi)
//        is CjNamedFunction -> getFunctionLikeSymbol(psi)
//        is CjConstructor<*> -> getConstructorSymbol(psi)
        is CjTypeParameter -> getTypeParameterSymbol(psi)
//        is CjTypeAlias -> getTypeAliasSymbol(psi)
//        is CjEnumEntry -> getEnumEntrySymbol(psi)
//        is CjFunctionLiteral -> getAnonymousFunctionSymbol(psi)
//        is CjProperty -> getVariableSymbol(psi)
////        is CjTypeStatement -> {
////            val literalExpression = (psi as? CjObjectDeclaration)?.parent as? CjObjectLiteralExpression
////            literalExpression?.let(::getAnonymousObjectSymbol) ?: getClassOrObjectSymbol(psi)!!
////        }
//        is CjPropertyAccessor -> getPropertyAccessorSymbol(psi)
//        is CjClassInitializer -> getClassInitializerSymbol(psi)
//        is CjDestructuringDeclarationEntry -> getDestructuringDeclarationEntrySymbol(psi)
//
//
//        is CjDestructuringDeclaration -> getDestructuringDeclarationSymbol(psi)
        else -> error("Cannot build symbol for ${psi::class}")
    }

//    public abstract fun getParameterSymbol(psi: CjParameter): CjVariableLikeSymbol
//    public abstract fun getFileSymbol(psi: CjFile): CjFileSymbol
//    public abstract fun getScriptSymbol(psi: CjScript): CjScriptSymbol
//    public abstract fun getFunctionLikeSymbol(psi: CjNamedFunction): CjFunctionLikeSymbol
//    public abstract fun getConstructorSymbol(psi: CjConstructor<*>): CjConstructorSymbol
    public abstract fun getTypeParameterSymbol(psi: CjTypeParameter): CjTypeParameterSymbol
//    public abstract fun getTypeAliasSymbol(psi: CjTypeAlias): CjTypeAliasSymbol
//    public abstract fun getEnumEntrySymbol(psi: CjEnumEntry): CjEnumEntrySymbol
//    public abstract fun getAnonymousFunctionSymbol(psi: CjNamedFunction): CjAnonymousFunctionSymbol
//    public abstract fun getAnonymousFunctionSymbol(psi: CjFunctionLiteral): CjAnonymousFunctionSymbol
//    public abstract fun getVariableSymbol(psi: CjProperty): CjVariableSymbol
//    public abstract fun getAnonymousObjectSymbol(psi: CjObjectLiteralExpression): CjAnonymousObjectSymbol
//    public abstract fun getClassOrObjectSymbol(psi: CjTypeStatement): CjTypeStatementSymbol?
//    public abstract fun getNamedClassOrObjectSymbol(psi: CjTypeStatement): CjNamedClassOrObjectSymbol?
//    public abstract fun getPropertyAccessorSymbol(psi: CjPropertyAccessor): CjPropertyAccessorSymbol
//    public abstract fun getClassInitializerSymbol(psi: CjClassInitializer): CjClassInitializerSymbol
//    public abstract fun getDestructuringDeclarationEntrySymbol(psi: CjDestructuringDeclarationEntry): CjLocalVariableSymbol
//    public abstract fun getDestructuringDeclarationSymbol(psi: CjDestructuringDeclaration): CjDestructuringDeclarationSymbol
//
//    public abstract fun getPackageSymbolIfPackageExists(packageFqName: FqName): CjPackageSymbol?
//
//    public abstract fun getClassOrObjectSymbolByClassId(classId: ClassId): CjTypeStatementSymbol?
//
//    public abstract fun getTypeAliasByClassId(classId: ClassId): CjTypeAliasSymbol?
//
//    public abstract fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): Sequence<CjCallableSymbol>
//
//    @Suppress("PropertyName")
//    public abstract val ROOT_PACKAGE_SYMBOL: CjPackageSymbol
}

public interface CjSymbolProviderMixIn : CjAnalysisSessionMixIn {
    public fun CjDeclaration.getSymbol(): CjDeclarationSymbol =
        withValidityAssertion { analysisSession.symbolProvider.getSymbol(this) }

//    /**
//     * Creates [CjVariableLikeSymbol] by [CjParameter].
//     *
//     * Unfortunately, [CjParameter] in PSI stands for many things, and not all of them are represented by a single type of symbol,
//     * so this function does not work for all possible [CjParameter]s.
//     *
//     * If [CjParameter.isFunctionTypeParameter] is `true`, i.e., if the given [CjParameter] is used as a function type parameter,
//     * it is not possible to create [CjValueParameterSymbol], hence an error will be raised.
//     *
//     * If [CjParameter.isLoopParameter] is `true`, i.e. if the given [CjParameter] is a loop variable in `for` expression, then the function
//     * returns [CjLocalVariableSymbol].
//     *
//     * Otherwise, returns [CjValueParameterSymbol].
//     */
//    public fun CjParameter.getParameterSymbol(): CjVariableLikeSymbol =
//        withValidityAssertion { analysisSession.symbolProvider.getParameterSymbol(this) }
//
//    /**
//     * Creates [CjFunctionLikeSymbol] by [CjNamedFunction]
//     *
//     * If [CjNamedFunction.getName] is `null` then returns [CjAnonymousFunctionSymbol]
//     * Otherwise, returns [CjFunctionSymbol]
//     */
//    public fun CjNamedFunction.getFunctionLikeSymbol(): CjFunctionLikeSymbol =
//        withValidityAssertion { analysisSession.symbolProvider.getFunctionLikeSymbol(this) }
//
//    public fun CjConstructor<*>.getConstructorSymbol(): CjConstructorSymbol =
//        withValidityAssertion { analysisSession.symbolProvider.getConstructorSymbol(this) }
//
//    public fun CjTypeParameter.getTypeParameterSymbol(): CjTypeParameterSymbol =
//        withValidityAssertion { analysisSession.symbolProvider.getTypeParameterSymbol(this) }
//
//    public fun CjTypeAlias.getTypeAliasSymbol(): CjTypeAliasSymbol =
//        withValidityAssertion { analysisSession.symbolProvider.getTypeAliasSymbol(this) }
//
//    public fun CjEnumEntry.getEnumEntrySymbol(): CjEnumEntrySymbol =
//        withValidityAssertion { analysisSession.symbolProvider.getEnumEntrySymbol(this) }
//
//    public fun CjNamedFunction.getAnonymousFunctionSymbol(): CjAnonymousFunctionSymbol =
//        withValidityAssertion { analysisSession.symbolProvider.getAnonymousFunctionSymbol(this) }
//
//    public fun CjFunctionLiteral.getAnonymousFunctionSymbol(): CjAnonymousFunctionSymbol =
//        withValidityAssertion { analysisSession.symbolProvider.getAnonymousFunctionSymbol(this) }
//
//    public fun CjProperty.getVariableSymbol(): CjVariableSymbol =
//        withValidityAssertion { analysisSession.symbolProvider.getVariableSymbol(this) }
//
//    public fun CjObjectLiteralExpression.getAnonymousObjectSymbol(): CjAnonymousObjectSymbol =
//        withValidityAssertion { analysisSession.symbolProvider.getAnonymousObjectSymbol(this) }
//
//    /** Returns a symbol for a given [CjTypeStatement]. Returns `null` for `CjEnumEntry` declarations. */
//    public fun CjTypeStatement.getClassOrObjectSymbol(): CjTypeStatementSymbol? =
//        withValidityAssertion { analysisSession.symbolProvider.getClassOrObjectSymbol(this) }
//
//    /** Returns a symbol for a given named [CjTypeStatement]. Returns `null` for `CjEnumEntry` declarations and object literals. */
//    public fun CjTypeStatement.getNamedClassOrObjectSymbol(): CjNamedClassOrObjectSymbol? =
//        withValidityAssertion { analysisSession.symbolProvider.getNamedClassOrObjectSymbol(this) }
//
//    public fun CjPropertyAccessor.getPropertyAccessorSymbol(): CjPropertyAccessorSymbol =
//        withValidityAssertion { analysisSession.symbolProvider.getPropertyAccessorSymbol(this) }
//
//    public fun CjFile.getFileSymbol(): CjFileSymbol =
//        withValidityAssertion { analysisSession.symbolProvider.getFileSymbol(this) }
//
//    public fun CjScript.getScriptSymbol(): CjScriptSymbol =
//        withValidityAssertion { analysisSession.symbolProvider.getScriptSymbol(this) }
//
//    /**
//     * Returns [CjPackageSymbol] corresponding to [packageFqName] if corresponding package is exists and visible from current uses-site scope,
//     * `null` otherwise
//     */
//    public fun getPackageSymbolIfPackageExists(packageFqName: FqName): CjPackageSymbol? =
//        withValidityAssertion { analysisSession.symbolProvider.getPackageSymbolIfPackageExists(packageFqName) }
//
//    /**
//     * @return symbol with specified [this@getClassOrObjectSymbolByClassId] or `null` in case such symbol is not found
//     */
//    public fun getClassOrObjectSymbolByClassId(classId: ClassId): CjTypeStatementSymbol? =
//        withValidityAssertion { analysisSession.symbolProvider.getClassOrObjectSymbolByClassId(classId) }
//
//    /**
//     * @return [CjTypeAliasSymbol] with specified [classId] or `null` in case such symbol is not found
//     */
//    public fun getTypeAliasByClassId(classId: ClassId): CjTypeAliasSymbol? =
//        withValidityAssertion { analysisSession.symbolProvider.getTypeAliasByClassId(classId) }
//
//    /**
//     * @return list of top-level functions and properties which are visible from current use-site module
//     *
//     * @param packageFqName package name in which callable symbols should be declared
//     * @param name callable symbol name
//     */
//    public fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): Sequence<CjCallableSymbol> =
//        withValidityAssertion { analysisSession.symbolProvider.getTopLevelCallableSymbols(packageFqName, name) }
//
//    /**
//     * @return symbol corresponding to the local variable introduced by individual destructuring declaration entries.
//     * E.g. `val (x, y) = p` has two declaration entries, one corresponding to `x`, one to `y`.
//     */
//    public fun CjDestructuringDeclarationEntry.getDestructuringDeclarationEntrySymbol(): CjLocalVariableSymbol =
//        analysisSession.symbolProvider.getDestructuringDeclarationEntrySymbol(this)
//
//    @Suppress("PropertyName")
//    public val ROOT_PACKAGE_SYMBOL: CjPackageSymbol
//        get() = withValidityAssertion { analysisSession.symbolProvider.ROOT_PACKAGE_SYMBOL }
}

context(CjAnalysisSession)
public inline fun <reified S : CjSymbol> CjDeclaration.getSymbolOfType(): S =
    withValidityAssertion { getSymbol() } as S

context(CjAnalysisSession)
public inline fun <reified S : CjSymbol> CjDeclaration.getSymbolOfTypeSafe(): S? =
    withValidityAssertion { getSymbol() } as? S



