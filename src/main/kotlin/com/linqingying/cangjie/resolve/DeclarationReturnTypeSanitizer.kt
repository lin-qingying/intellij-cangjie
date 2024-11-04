package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.container.DefaultImplementation
import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.types.UnwrappedType
import com.linqingying.cangjie.types.WrappedTypeFactory

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
