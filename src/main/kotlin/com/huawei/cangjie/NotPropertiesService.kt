package com.huawei.cangjie


import com.huawei.cangjie.name.FqNameUnsafe
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.profile.codeInspection.InspectionProjectProfileManager
import com.intellij.psi.PsiElement


interface NotPropertiesService {
    fun getNotProperties(element: PsiElement): Set<FqNameUnsafe>

    companion object {

        val DEFAULT: List<String> = ArrayList<String>()

        fun getNotProperties(element: PsiElement): Set<FqNameUnsafe> {
            val notProperties = element.project.serviceOrNull<NotPropertiesService>()
                ?: return DEFAULT.mapTo(LinkedHashSet()) { FqNameUnsafe(it) }

            return notProperties.getNotProperties(element)
        }
    }
}
