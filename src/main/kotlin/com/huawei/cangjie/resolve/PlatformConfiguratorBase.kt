package com.huawei.cangjie.resolve

import com.huawei.cangjie.container.*
import com.huawei.cangjie.resolve.caches.DeclarationChecker
import com.huawei.cangjie.resolve.calls.checkers.AdditionalTypeChecker
import com.huawei.cangjie.resolve.calls.checkers.AssignmentChecker
import com.huawei.cangjie.resolve.calls.checkers.CallChecker
import com.huawei.cangjie.resolve.calls.checkers.configureDefaultCheckers
import com.huawei.cangjie.resolve.lazy.DelegationFilter
import com.huawei.cangjie.types.DynamicTypesSettings

fun createContainer(
    id: String,
    analyzerServices: PlatformDependentAnalyzerServices,
    init: StorageComponentContainer.() -> Unit
) =
    composeContainer(id, analyzerServices.platformConfigurator.platformSpecificContainer, init)

//根据参数可扩展
abstract class PlatformConfiguratorBase (
    private val dynamicTypesSettings: DynamicTypesSettings? = null,
    private val additionalDeclarationCheckers: List<DeclarationChecker> = emptyList(),
    private val additionalCallCheckers: List<CallChecker> = emptyList(),
    private val additionalAssignmentCheckers: List<AssignmentChecker> = emptyList(),
    private val additionalTypeCheckers: List<AdditionalTypeChecker> = emptyList(),
//    private val additionalClassifierUsageCheckers: List<ClassifierUsageChecker> = emptyList(),
//    private val additionalAnnotationCheckers: List<AdditionalAnnotationChecker> = emptyList(),
    private val additionalClashResolvers: List<PlatformExtensionsClashResolver<*>> = emptyList(),
    private val identifierChecker: IdentifierChecker? = null,
    private val overloadFilter: OverloadFilter? = null,
//    private val platformToKotlinClassMapper: PlatformToCangJieClassMapper? = null,
//    private val platformSpecificCastChecker: PlatformSpecificCastChecker? = null,
    private val delegationFilter: DelegationFilter? = null,
    private val overridesBackwardCompatibilityHelper: OverridesBackwardCompatibilityHelper? = null,
    private val declarationReturnTypeSanitizer: DeclarationReturnTypeSanitizer? = null
): PlatformConfigurator {



    fun configureExtensionsAndCheckers(container: StorageComponentContainer) {
        with(container) {
            useInstanceIfNotNull(dynamicTypesSettings)
            additionalDeclarationCheckers.forEach { useInstance(it) }
            additionalCallCheckers.forEach { useInstance(it) }
            additionalAssignmentCheckers.forEach { useInstance(it) }
            additionalTypeCheckers.forEach { useInstance(it) }
//            additionalClassifierUsageCheckers.forEach { useInstance(it) }
//            additionalAnnotationCheckers.forEach { useInstance(it) }
            additionalClashResolvers.forEach { useClashResolver(it) }
            useInstanceIfNotNull(identifierChecker)
            useInstanceIfNotNull(overloadFilter)
//            useInstanceIfNotNull(platformToCangJieClassMapper)
//            useInstanceIfNotNull(platformSpecificCastChecker)
            useInstanceIfNotNull(delegationFilter)
            useInstanceIfNotNull(overridesBackwardCompatibilityHelper)
            useInstanceIfNotNull(declarationReturnTypeSanitizer)
        }
    }
    override val platformSpecificContainer = composeContainer(this::class.java.simpleName) {
        configureDefaultCheckers()
        configureExtensionsAndCheckers(this)
    }

}

object CangJiePlatformConfigurator : PlatformConfiguratorBase() {

    override fun configureModuleComponents(container: StorageComponentContainer) {

    }

    override fun configureModuleDependentCheckers(container: StorageComponentContainer) {
//        container.useImpl<OptInMarkerDeclarationAnnotationChecker>()
    }


}
