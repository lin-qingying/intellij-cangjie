package com.huawei.cangjie.container


fun composeContainer(
    id: String,
    parent: StorageComponentContainer? = null,
    init: StorageComponentContainer.() -> Unit
): StorageComponentContainer {
    val c = StorageComponentContainer(id, parent)
    c.init()
    c.compose()
    return c
}
inline fun <reified T : Any> StorageComponentContainer.useImpl() {
    registerSingleton(T::class.java)
}


fun StorageComponentContainer.useInstance(instance: Any) {
    registerInstance(instance)
}

fun StorageComponentContainer.useInstanceIfNotNull(instance: Any?) {
    if (instance != null) registerInstance(instance)
}
