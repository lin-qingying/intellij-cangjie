package com.huawei.cangjie


import com.huawei.cangjie.name.FqNameUnsafe
import com.intellij.openapi.components.serviceOrNull
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.profile.codeInspection.InspectionProjectProfileManager
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus


//interface NotPropertiesService {
//    fun getNotProperties(element: PsiElement): Set<FqNameUnsafe>
//
//    companion object {
//
//        val DEFAULT: List<String> = ArrayList<String>().apply {
//            add("java.net.Socket.getInputStream")
//            add("java.net.Socket.getOutputStream")
//            add("java.net.URLConnection.getInputStream")
//            add("java.net.URLConnection.getOutputStream")
//
//            val atomicMethods = listOf("getAndIncrement", "getAndDecrement", "getAcquire", "getOpaque", "getPlain")
//
//            for (atomicClass in listOf("AtomicInteger", "AtomicLong")) {
//                for (atomicMethod in atomicMethods) {
//                    add("java.util.concurrent.atomic.$atomicClass.$atomicMethod")
//                }
//            }
//            for (byteBufferMethod in listOf("getChar", "getDouble", "getFloat", "getInt", "getLong", "getShort")) {
//                add("java.nio.ByteBuffer.$byteBufferMethod")
//            }
//        }
//
//        fun getNotProperties(element: PsiElement): Set<FqNameUnsafe> {
//            val notProperties = element.project.serviceOrNull<NotPropertiesService>()
//                ?: return DEFAULT.mapTo(LinkedHashSet()) { FqNameUnsafe(it) }
//
//            return notProperties.getNotProperties(element)
//        }
//    }
//}
//class NotPropertiesServiceImpl(private val project: Project) : NotPropertiesService {
//    override fun getNotProperties(element: PsiElement): Set<FqNameUnsafe> {
//        val profile = InspectionProjectProfileManager.getInstance(project).currentProfile
//        val tool = profile.getUnwrappedTool(USE_PROPERTY_ACCESS_INSPECTION, element)
//        return (tool?.fqNameList ?: NotPropertiesService.DEFAULT.map(::FqNameUnsafe)).toSet()
//    }
//
//    companion object {
//        val USE_PROPERTY_ACCESS_INSPECTION: Key<UsePropertyAccessSyntaxInspection> = Key.create("UsePropertyAccessSyntax")
//    }
//}
