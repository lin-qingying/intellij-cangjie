package com.linqingying.cangjie.psi



fun packageDirectiveVisitor(block: (CjPackageDirective) -> Unit) =
    object : CjVisitorVoid() {
        override fun visitPackageDirective(packageDirective: CjPackageDirective) {
            block(packageDirective)
        }
    }
fun namedDeclarationVisitor(block: (CjNamedDeclaration) -> Unit) =
    object : CjVisitorVoid() {
        override fun visitNamedDeclaration(namedDeclaration: CjNamedDeclaration) {
            block(namedDeclaration)
        }
    }

fun declarationVisitor(block: (CjDeclaration) -> Unit) =
    object : CjVisitorVoid() {
        override fun visitDeclaration(dcl: CjDeclaration) {
            block(dcl)
        }
    }
