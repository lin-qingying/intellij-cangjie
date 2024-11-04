package com.linqingying.cangjie.ide.refactoring.move

import com.linqingying.cangjie.psi.CjFile
import com.linqingying.cangjie.psi.CjNamedDeclaration


fun CangJieMoveSource(declaration: CjNamedDeclaration) = CangJieMoveSource.Elements(listOf(declaration))

fun CangJieMoveSource(declarations: Collection<CjNamedDeclaration>) = CangJieMoveSource.Elements(declarations)

fun CangJieMoveSource(file: CjFile) = CangJieMoveSource.File(file)

sealed interface CangJieMoveSource {
    val elementsToMove: Collection<CjNamedDeclaration>

    class Elements(override val elementsToMove: Collection<CjNamedDeclaration>) : CangJieMoveSource

    class File(val file: CjFile) : CangJieMoveSource {
        override val elementsToMove: Collection<CjNamedDeclaration> get() = file.declarations.filterIsInstance<CjNamedDeclaration>()
    }
}
