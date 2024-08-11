package com.huawei.cangjie.resolve

import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.ValueParameterDescriptor
import com.huawei.cangjie.types.checker.CangJieTypeRefiner

class OverrideResolver(
    private val trace: BindingTrace,
//    private val overridesBackwardCompatibilityHelper: OverridesBackwardCompatibilityHelper,
    private val languageVersionSettings: LanguageVersionSettings,
    private val kotlinTypeRefiner: CangJieTypeRefiner,
//    private val platformSpecificDiagnosticComponents: PlatformSpecificDiagnosticComponents
) {
    companion object{


        fun shouldReportParameterNameOverrideWarning(
            parameterFromSubclass: ValueParameterDescriptor,
            parameterFromSuperclass: ValueParameterDescriptor
        ): Boolean {
            return parameterFromSubclass.containingDeclaration.hasStableParameterNames() &&
                    parameterFromSuperclass.containingDeclaration.hasStableParameterNames() &&
                    parameterFromSuperclass.name != parameterFromSubclass.name
        }

    }
}
