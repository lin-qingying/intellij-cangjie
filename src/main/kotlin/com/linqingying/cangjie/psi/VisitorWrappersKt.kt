package com.linqingying.cangjie.psi


fun declarationVisitor(block: (CjDeclaration) -> Unit) =
    object : CjVisitorVoid() {
        override fun visitDeclaration(declaration: CjDeclaration) {
            block(declaration)
        }
    }
