package com.linqingying.cangjie.test

sealed interface CangJieCoreApplicationEnvironmentMode {
    object Production : CangJieCoreApplicationEnvironmentMode

    object UnitTest : CangJieCoreApplicationEnvironmentMode

    companion object {
        fun fromUnitTestModeFlag(isUnitTestMode: Boolean): CangJieCoreApplicationEnvironmentMode =
            if (isUnitTestMode) UnitTest else Production
    }
}
