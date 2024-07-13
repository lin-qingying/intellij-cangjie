package com.huawei.cangjie.types

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.config.LanguageFeature
import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.resolve.calls.components.ClassicTypeSystemContextForCS
import com.huawei.cangjie.types.checker.CangJieTypeRefiner


class TypeApproximator(
    builtIns: CangJieBuiltIns,
    languageVersionSettings: LanguageVersionSettings,
) : AbstractTypeApproximator(
    ClassicTypeSystemContextForCS(builtIns, CangJieTypeRefiner.Default),
    languageVersionSettings
) {
    fun approximateDeclarationType(baseType: CangJieType, local: Boolean): UnwrappedType {
        if (!languageVersionSettings.supportsFeature(LanguageFeature.NewInference)) return baseType.unwrap()

        val configuration = if (local) TypeApproximatorConfiguration.LocalDeclaration else TypeApproximatorConfiguration.PublicDeclaration.SaveAnonymousTypes
        val preparedType = if (local) baseType.unwrap() else substituteAlternativesInPublicType(baseType)
        return approximateToSuperType(preparedType, configuration) ?: preparedType
    }

    // null means that this input type is the result, i.e. input type not contains not-allowed kind of types
    // type <: resultType
    fun approximateToSuperType(type: UnwrappedType, conf: TypeApproximatorConfiguration): UnwrappedType? =
        super.approximateToSuperType(type, conf) as UnwrappedType?

//    // resultType <: type
    fun approximateToSubType(type: UnwrappedType, conf: TypeApproximatorConfiguration): UnwrappedType? =
        super.approximateToSubType(type, conf) as UnwrappedType?

    fun approximateTo(type: UnwrappedType, conf: TypeApproximatorConfiguration, toSuperType: Boolean): UnwrappedType? =
        if (toSuperType) approximateToSuperType(type, conf) else approximateToSubType(type, conf)
}
