package com.huawei.cangjie.references

import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.resolve.BindingContext
import com.intellij.openapi.application.ApplicationManager

interface CjReferenceResolutionHelper {
    fun partialAnalyze(element: CjElement): BindingContext


    companion object {
        fun getInstance(): CjReferenceResolutionHelper = ApplicationManager.getApplication().getService(CjReferenceResolutionHelper::class.java)
    }
}