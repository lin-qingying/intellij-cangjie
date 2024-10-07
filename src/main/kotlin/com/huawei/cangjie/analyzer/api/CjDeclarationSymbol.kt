package com.huawei.cangjie.analyzer.api

import com.huawei.cangjie.psi.CjAnnotated


interface CjAnnotatedSymbol : CjSymbol, CjAnnotated

/**
 * Represents a symbol of declaration which can be directly expressed in source code.
 * Eg, classes, type parameters or functions are [CjDeclarationSymbol], but files and packages are not
 */
sealed interface CjDeclarationSymbol : CjSymbol, CjSymbolWithTypeParameters, CjAnnotatedSymbol
