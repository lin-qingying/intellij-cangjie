
@file:JvmName("ClassNameKt")

package com.linqingying.cangjie.metadata.node
/**
 *
 * 该名称中的包名用 '/' 分隔，类名用 '.' 分隔，例如：`"org/foo/bar/Baz.Nested"`。
 *
 */
public typealias ClassName = String

/**
 * 检查类名 [this] 是否表示一个局部类或匿名对象。
 *
 * 如果类名以 '.'（点）开头，则表示一个局部类或匿名对象。
 */
public fun ClassName.isLocalClassName(): Boolean = this.startsWith(".")
