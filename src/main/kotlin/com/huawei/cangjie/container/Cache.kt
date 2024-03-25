package com.huawei.cangjie.container

import com.intellij.util.containers.ContainerUtil
import java.lang.reflect.*
import java.util.concurrent.ConcurrentHashMap

fun Class<*>.getInfo(): ClassInfo {
    return ClassTraversalCache.getClassInfo(this)
}

private fun getConstructorInfo(c: Class<*>): ConstructorInfo? {
    if (Modifier.isAbstract(c.modifiers) || c.isPrimitive)
        return null

    val publicConstructors = c.constructors.filter { Modifier.isPublic(it.modifiers) && !it.isSynthetic }
    if (publicConstructors.size != 1) return null

    val constructor = publicConstructors.single()
    val parameterTypes =
        if (c.declaringClass != null && !Modifier.isStatic(c.modifiers))
            listOf(c.declaringClass, *constructor.genericParameterTypes)
        else constructor.genericParameterTypes.toList()
    return ConstructorInfo(constructor, parameterTypes)
}

private fun getRegistrations(klass: Class<*>): List<Type> {
    val registrations = java.util.ArrayList<Type>()

    val superClasses = generateSequence<Type>(klass) {
        when (it) {
            is Class<*> -> it.genericSuperclass
            is ParameterizedType -> (it.rawType as? Class<*>)?.genericSuperclass
            else -> null
        }
    }
    registrations.addAll(superClasses)

    val interfaces = LinkedHashSet<Type>()
    superClasses.forEach { collectInterfacesRecursive(it, interfaces) }
    registrations.addAll(interfaces)
    registrations.remove(Any::class.java)
    return registrations
}


private fun collectInterfacesRecursive(type: Type, result: MutableSet<Type>) {
    // TODO: should apply generic substitution through hierarchy
    val klass: Class<*>? = when (type) {
        is Class<*> -> type
        is ParameterizedType -> type.rawType as? Class<*>
        else -> null
    }
    klass?.genericInterfaces?.forEach {
        if (result.add(it)) {
            collectInterfacesRecursive(it, result)
        }
    }
}
private fun getDefaultImplementation(klass: Class<*>): Class<*>? {
    return klass.getAnnotation(DefaultImplementation::class.java)?.impl?.java
}
private fun traverseClass(c: Class<*>): ClassInfo {
    return ClassInfo(getConstructorInfo(c), getSetterInfos(c), getRegistrations(c), getDefaultImplementation(c))
}

private fun getSetterInfos(c: Class<*>): List<SetterInfo> {
    val setterInfos = ArrayList<SetterInfo>()
    for (method in c.methods) {
        for (annotation in method.declaredAnnotations) {
            if (annotation.annotationClass.java.name.endsWith(".Inject")) {
                setterInfos.add(SetterInfo(method, method.genericParameterTypes.toList()))
            }
        }
    }
    return setterInfos
}

private object ClassTraversalCache {
    private val cache =
        if (System.getProperty("idea.system.path") != null) ConcurrentHashMap<Class<*>, ClassInfo>()
        else ContainerUtil.createConcurrentWeakKeySoftValueMap<Class<*>, ClassInfo>()

    fun getClassInfo(c: Class<*>): ClassInfo {
        val classInfo = cache.get(c)
        if (classInfo == null) {
            val newClassInfo = traverseClass(c)
            cache.put(c, newClassInfo)
            return newClassInfo
        }
        return classInfo
    }
}

data class ClassInfo(
    val constructorInfo: ConstructorInfo?,
    val setterInfos: List<SetterInfo>,
    val registrations: List<Type>,
    val defaultImplementation: Class<*>?
)

data class ConstructorInfo(
    val constructor: Constructor<*>,
    val parameters: List<Type>
)

data class SetterInfo(
    val method: Method,
    val parameters: List<Type>
)