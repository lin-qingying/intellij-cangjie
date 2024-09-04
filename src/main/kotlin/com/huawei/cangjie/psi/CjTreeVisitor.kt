package com.huawei.cangjie.psi

open class CjTreeVisitor<D> : CjVisitor<Void , D>() {
    override fun visitCjElement(element: CjElement, data: D): Void? {
        element.acceptChildren(this, data)
        return null
    }

    override fun visitCjFile(file: CjFile, data: D): Void ?{
        super.visitCjFile(file, data)
        file.acceptChildren<D>(this, data)
        return null
    }
}
