package com.huawei.cangjie.resolve

import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.container.DefaultImplementation
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.types.UnwrappedType
import com.huawei.cangjie.types.WrappedTypeFactory

@DefaultImplementation(impl = DeclarationReturnTypeSanitizer.Default::class)
interface DeclarationReturnTypeSanitizer {
    fun sanitizeReturnType(
        inferred: UnwrappedType,
        wrappedTypeFactory: WrappedTypeFactory,
        trace: BindingTrace,
        languageVersionSettings: LanguageVersionSettings
    ): UnwrappedType

    object Default : DeclarationReturnTypeSanitizer {
        override fun sanitizeReturnType(
            inferred: UnwrappedType,
            wrappedTypeFactory: WrappedTypeFactory,
            trace: BindingTrace,
            languageVersionSettings: LanguageVersionSettings
        ) = inferred
    }
}
