//package com.huawei.cangjie.ide.project.tools.projectWizard.core.services
//
//import kotlin.reflect.KClass
//import kotlin.reflect.full.isSubclassOf
//
//class ServicesManager(
//    private val services: List<WizardService>,
//    private val serviceSelector: (List<WizardService>) -> WizardService?
//) {
//    @Suppress("UNCHECKED_CAST")
//    fun <S : WizardService> serviceByClass(klass: KClass<S>, filter: (S) -> Boolean): S? =
//        services.filter { service ->
//            service::class.isSubclassOf(klass) && filter(service as S)
//        }.let(serviceSelector) as? S
//
//    fun withAdditionalServices(services: List<WizardService>) =
//        ServicesManager(this.services + services, serviceSelector)
//}
