package com.linqingying.cangjie.test

import com.intellij.core.CoreProjectEnvironment
import com.intellij.openapi.Disposable


open class CangJieCoreProjectEnvironment(
    disposable: Disposable,
    applicationEnvironment: CangJieCoreApplicationEnvironment
) : CoreProjectEnvironment(disposable, applicationEnvironment)
