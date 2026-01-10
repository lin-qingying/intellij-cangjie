/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.container

import com.intellij.util.containers.ContainerUtil
import java.lang.reflect.*
import java.util.concurrent.ConcurrentHashMap

// ==================== 反射辅助函数 ====================

/**
 * 执行代码块并解包反射调用异常
 *
 * 当通过反射调用方法或构造函数时,实际异常会被包装在 [InvocationTargetException] 中。
 * 此函数会自动解包,抛出原始异常。
 *
 * @param T 代码块的返回类型
 * @param block 要执行的代码块
 * @return 代码块的执行结果
 * @throws Throwable 如果代码块抛出异常,抛出解包后的原始异常
 */
inline fun <T> runWithUnwrappingInvocationException(block: () -> T) =
    try {
        block()
    } catch (e: InvocationTargetException) {
        throw e.targetException ?: e
    }

// ==================== 类信息缓存 ====================

/**
 * 获取类的元信息
 *
 * 返回包含构造函数、setter 方法、注册信息和默认实现的类元数据。
 * 结果会被缓存以提高性能。
 *
 * @return 类的元信息对象
 */
fun Class<*>.getInfo(): ClassInfo {
    return ClassTraversalCache.getClassInfo(this)
}

/**
 * 获取构造函数信息
 *
 * 查找类的唯一公共构造函数。对于非静态内部类,会自动添加外部类作为第一个参数。
 *
 * @param c 要分析的类
 * @return 构造函数信息,如果类是抽象类、原始类型或没有唯一公共构造函数则返回 null
 */
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

/**
 * 获取类的注册信息
 *
 * 收集类的所有超类和接口,用于依赖注入的类型匹配。
 *
 * @param klass 要分析的类
 * @return 类的所有超类和接口列表(不包括 [Any])
 */
private fun getRegistrations(klass: Class<*>): List<Type> {
    val registrations = ArrayList<Type>()

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

/**
 * 递归收集接口
 *
 * 遍历类型的接口继承树,收集所有直接和间接实现的接口。
 *
 * @param type 要分析的类型
 * @param result 收集结果的集合
 */
private fun collectInterfacesRecursive(type: Type, result: MutableSet<Type>) {
    // TODO: 应该通过继承层次应用泛型替换
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

/**
 * 获取默认实现类
 *
 * 查找类上的 [DefaultImplementation] 注解,返回注解指定的默认实现类。
 *
 * @param klass 要分析的类
 * @return 默认实现类,如果没有注解则返回 null
 */
private fun getDefaultImplementation(klass: Class<*>): Class<*>? {
    return klass.getAnnotation(DefaultImplementation::class.java)?.impl?.java
}

/**
 * 遍历类并收集元信息
 *
 * @param c 要分析的类
 * @return 包含构造函数、setter、注册信息和默认实现的类元信息
 */
private fun traverseClass(c: Class<*>): ClassInfo {
    return ClassInfo(getConstructorInfo(c), getSetterInfos(c), getRegistrations(c), getDefaultImplementation(c))
}

/**
 * 获取 setter 注入信息
 *
 * 查找类中所有标记了 @Inject 注解的方法,用于属性注入。
 *
 * @param c 要分析的类
 * @return setter 信息列表
 */
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

/**
 * 类遍历缓存
 *
 * 缓存类的元信息以避免重复的反射操作。
 * 在 IDEA 环境中使用强引用的 ConcurrentHashMap,
 * 在其他环境中使用弱键软值的映射以避免内存泄漏。
 */
private object ClassTraversalCache {
    private val cache =
        if (System.getProperty("idea.system.path") != null) ConcurrentHashMap<Class<*>, ClassInfo>()
        else ContainerUtil.createConcurrentWeakKeySoftValueMap<Class<*>, ClassInfo>()

    /**
     * 获取类的元信息
     *
     * @param c 要查询的类
     * @return 类的元信息,如果缓存中不存在则遍历并缓存
     */
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

// ==================== 数据类 ====================

/**
 * 类元信息
 *
 * 包含类的构造函数、setter 方法、类型注册信息和默认实现。
 *
 * @property constructorInfo 构造函数信息,如果没有合适的构造函数则为 null
 * @property setterInfos setter 注入方法列表
 * @property registrations 类型注册列表(所有超类和接口)
 * @property defaultImplementation 默认实现类,如果没有则为 null
 */
data class ClassInfo(
    val constructorInfo: ConstructorInfo?,
    val setterInfos: List<SetterInfo>,
    val registrations: List<Type>,
    val defaultImplementation: Class<*>?
)

/**
 * 构造函数信息
 *
 * @property constructor 构造函数对象
 * @property parameters 构造函数参数类型列表
 */
data class ConstructorInfo(
    val constructor: Constructor<*>,
    val parameters: List<Type>
)

/**
 * Setter 方法信息
 *
 * @property method 方法对象
 * @property parameters 方法参数类型列表
 */
data class SetterInfo(
    val method: Method,
    val parameters: List<Type>
)
