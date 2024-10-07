package com.huawei.cangjie.analyzer.api

import com.huawei.cangjie.analyzer.components.CjAnalysisSessionComponent
import com.huawei.cangjie.analyzer.components.CjAnalysisSessionMixIn
import com.huawei.cangjie.analyzer.lifetime.CjLifetimeOwner
import com.huawei.cangjie.analyzer.lifetime.withValidityAssertion
import com.huawei.cangjie.analyzer.api.types.CjType
import com.huawei.cangjie.analyzer.api.types.CjTypeNullability
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjTypeReference


interface CjTypeProviderMixIn : CjAnalysisSessionMixIn {


    /**
     * Resolve [CjTypeReference] and return corresponding [CjType] if resolved.
     *
     * This may raise an exception if the resolution ends up with an unexpected kind.
     */
    public   fun CjTypeReference.getCjType(): CjType =
        withValidityAssertion { analysisSession.typeProvider.getCjType(this) }

}

@Suppress("PropertyName")
abstract class CjBuiltinTypes : CjLifetimeOwner {
    abstract val INT: CjType
    abstract val LONG: CjType
    abstract val SHORT: CjType
    abstract val BYTE: CjType

    abstract val FLOAT: CjType
    abstract val DOUBLE: CjType

    abstract val BOOLEAN: CjType
    abstract val CHAR: CjType
    abstract val STRING: CjType

    abstract val UNIT: CjType
    abstract val NOTHING: CjType
    abstract val ANY: CjType

    abstract val THROWABLE: CjType

    abstract val NULLABLE_ANY: CjType
    abstract val NULLABLE_NOTHING: CjType
}


abstract class CjTypeProvider : CjAnalysisSessionComponent() {
    abstract val builtinTypes: CjBuiltinTypes

    abstract fun approximateToSuperPublicDenotableType(type: CjType, approximateLocalTypes: Boolean): CjType?

    abstract fun approximateToSubPublicDenotableType(type: CjType, approximateLocalTypes: Boolean): CjType?

    abstract fun getEnhancedType(type: CjType): CjType?

//    public abstract fun buildSelfClassType(symbol: CjNamedClassOrObjectSymbol): CjType

    abstract fun commonSuperType(types: Collection<CjType>): CjType?

    abstract fun getCjType(ktTypeReference: CjTypeReference): CjType


    abstract fun withNullability(type: CjType, newNullability: CjTypeNullability): CjType

    abstract fun haveCommonSubtype(a: CjType, b: CjType): Boolean

    abstract fun getImplicitReceiverTypesAtPosition(position: CjElement): List<CjType>

    abstract fun getDirectSuperTypes(type: CjType, shouldApproximate: Boolean): List<CjType>

    abstract fun getAllSuperTypes(type: CjType, shouldApproximate: Boolean): List<CjType>

//    public abstract fun getDispatchReceiverType(symbol: CjCallableSymbol): CjType?

    abstract fun getArrayElementType(type: CjType): CjType?
}
