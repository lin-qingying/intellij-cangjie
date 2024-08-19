package com.huawei.cangjie.psi



fun packageDirectiveVisitor(block: (CjPackageDirective) -> Unit) =
    object : CjVisitorVoid() {
        override fun visitPackageDirective(packageDirective: CjPackageDirective) {
            block(packageDirective)
        }
    }
