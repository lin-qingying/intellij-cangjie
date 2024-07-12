package com.huawei.cangjie.types

import com.huawei.cangjie.types.model.TypeSystemInferenceExtensionContext

abstract class AbstractTypeApproximator(
    val ctx: TypeSystemInferenceExtensionContext,
//    protected val languageVersionSettings: LanguageVersionSettings,
) : TypeSystemInferenceExtensionContext by ctx{

}
