package com.huawei.cangjie.resolve

import com.huawei.cangjie.container.StorageComponentContainer
import com.huawei.cangjie.container.composeContainer

fun createContainer(
    id: String,
    analyzerServices: PlatformDependentAnalyzerServices,
    init: StorageComponentContainer.() -> Unit
) =
    composeContainer(id, analyzerServices.platformConfigurator.platformSpecificContainer, init)


abstract class PlatformConfiguratorBase : PlatformConfigurator {

    fun StorageComponentContainer.configureDefaultCheckers() {
//        DEFAULT_DECLARATION_CHECKERS.forEach { useInstance(it) }
//        DEFAULT_CALL_CHECKERS.forEach { useInstance(it) }
//        DEFAULT_TYPE_CHECKERS.forEach { useInstance(it) }
//        DEFAULT_CLASSIFIER_USAGE_CHECKERS.forEach { useInstance(it) }
//        DEFAULT_ANNOTATION_CHECKERS.forEach { useInstance(it) }
//        DEFAULT_CLASH_RESOLVERS.forEach { useClashResolver(it) }
    }

    override val platformSpecificContainer = composeContainer(this::class.java.simpleName) {
        configureDefaultCheckers()
        configureExtensionsAndCheckers(this)
    }

    fun configureExtensionsAndCheckers(container: StorageComponentContainer) {
//        with(container) {
//            useInstanceIfNotNull(dynamicTypesSettings)
//            additionalDeclarationCheckers.forEach { useInstance(it) }
//            additionalCallCheckers.forEach { useInstance(it) }
//            additionalAssignmentCheckers.forEach { useInstance(it) }
//            additionalTypeCheckers.forEach { useInstance(it) }
//            additionalClassifierUsageCheckers.forEach { useInstance(it) }
//            additionalAnnotationCheckers.forEach { useInstance(it) }
//            additionalClashResolvers.forEach { useClashResolver(it) }
//            useInstanceIfNotNull(identifierChecker)
//            useInstanceIfNotNull(overloadFilter)
//            useInstanceIfNotNull(platformToKotlinClassMapper)
//            useInstanceIfNotNull(platformSpecificCastChecker)
//            useInstanceIfNotNull(delegationFilter)
//            useInstanceIfNotNull(overridesBackwardCompatibilityHelper)
//            useInstanceIfNotNull(declarationReturnTypeSanitizer)
//        }
    }
}

object CangJiePlatformConfigurator : PlatformConfiguratorBase() {

    override fun configureModuleComponents(container: StorageComponentContainer) {

    }

    override fun configureModuleDependentCheckers(container: StorageComponentContainer) {
//        container.useImpl<OptInMarkerDeclarationAnnotationChecker>()
    }


}