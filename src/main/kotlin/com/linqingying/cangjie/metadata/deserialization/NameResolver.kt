/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.linqingying.cangjie.metadata.deserialization

/**
 * NameResolver 接口用于从给定索引解析名称。
 * 主要功能包括获取字符串值、完全限定类名以及判断类名是否为局部类名。
 */
interface NameResolver {
    /**
     * 获取指定索引处的字符串值。
     *
     * @param index 要检索的字符串值的索引。
     * @return 指定索引处的字符串值。
     */
    fun getString(index: Int): String

    /**
     * 获取指定索引处的类的完全限定名。
     * 完全限定名格式为：`org/foo/bar/Test.Inner`。
     *
     * @param index 要检索的类名的索引。
     * @return 指定索引处的类的完全限定名。
     */
    fun getQualifiedClassName(index: Int): String

    /**
     * 判断指定索引处的类名是否为局部类名。
     *
     * @param index 要检查的类名的索引。
     * @return 如果类名是局部类名则返回 `true`，否则返回 `false`。
     */
    fun isLocalClassName(index: Int): Boolean
}

