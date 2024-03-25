package com.huawei.cangjie.resolve

import com.huawei.cangjie.container.StorageComponentContainer
import com.huawei.cangjie.container.composeContainer

fun createContainer(
    id: String,
    analyzerServices: PlatformDependentAnalyzerServices,
    init: StorageComponentContainer.() -> Unit
) =
    composeContainer(id, analyzerServices.platformConfigurator.platformSpecificContainer, init)
