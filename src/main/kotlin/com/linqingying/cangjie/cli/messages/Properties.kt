package com.linqingying.cangjie.cli.messages

enum class CompilerSystemProperties(val property: String, val alwaysDirectAccess: Boolean = false) {
    OS_NAME("os.name", alwaysDirectAccess = true),
    CANGJIE_COLORS_ENABLED_PROPERTY("cangjie.colors.enabled"),

    CANGJIE_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY("cangjie.environment.keepalive");
    private fun <T> getProperFunction(custom: T?, default: T): T {
        if (alwaysDirectAccess) return default
        return custom ?: default
    }

    companion object {
        var systemPropertyGetter: ((String) -> String?)? = null

        var systemPropertySetter: ((String, String) -> String?)? = null

        var systemPropertyCleaner: ((String) -> String?)? = null
    }
    var value: String?
        get() {
            return getProperFunction(systemPropertyGetter, System::getProperty)(property)
        }
        set(value) {
            getProperFunction(systemPropertySetter, System::setProperty)(property, value!!)
        }
}
fun String?.toBooleanLenient(): Boolean? = when (this?.lowercase()) {
    null -> false
    in listOf("", "yes", "true", "on", "y") -> true
    in listOf("no", "false", "off", "n") -> false
    else -> null
}
val isWindows: Boolean
    get() = CompilerSystemProperties.OS_NAME.value!!.lowercase().startsWith("windows")
