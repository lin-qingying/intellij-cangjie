/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.descriptors

import org.cangnova.cangjie.name.FqName

interface PackageData : DeclarationDescriptor {
    /**
     * 表示包的完全限定名（Fully Qualified Name）
     */
    /**
     * 获取包的完全限定名
     * @return 返回包的FqName对象
     */
    val fqName: FqName


    /**
     * 判断当前包是否可以访问另一个包的内部成员
     * @param whatPackage 目标包
     * @return 如果当前包是目标包或其子包则返回true，否则返回false
     */
    fun shouldSeeInternalsOf(whatPackage: PackageData): Boolean {
//判断自己是不是 whatPackage 的子包或本包


//        val f1 = FqName.topLevel(Name.identifier("a"))
//        val f2 = f1.child(Name.identifier("b"))

        if (this.fqName == whatPackage.fqName.parent()) return true
        return this.fqName.startsWith(whatPackage.fqName)

    }

    /**
     * 判断当前包是否可以访问包片段的内部成员
     * @param whatPackage 目标包片段
     * @return 如果当前包是目标包片段的子包则返回true，否则返回false
     */
    fun shouldSeeInternalsOf(whatPackage: PackageFragmentDescriptor): Boolean {
//判断自己是不是 whatPackage 的子包或本包


//        val f1 = FqName.topLevel(Name.identifier("a"))
//        val f2 = f1.child(Name.identifier("b"))

        return this.fqName.startsWith(whatPackage.fqName)

    }

    /**
     * 判断当前包是否可以访问另一个包的保护成员
     * @param whatPackage 目标包
     * @return 如果当前包与目标包同属一个模块则返回true，否则返回false
     */
    fun shouldProtectedsOf(whatPackage: PackageData): Boolean {
// 判断模块名是否相同
        return this.fqName.moduleName == whatPackage.fqName.moduleName

    }
}