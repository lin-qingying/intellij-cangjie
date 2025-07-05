/*
 * Copyright 2024 LinQingYing. and contributors.
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
package cn.cangnova.cangjie.utils

/**
 * 一个简单的包装类，用于包装任意类型的数据。
 * 
 * 这个类的主要目的是屏蔽被包装对象的hashCode和equals方法，
 * 确保即使包装的对象重写了这些方法，Box实例也会使用默认的引用相等性比较。
 * 这在需要基于引用而非内容比较对象的场景中特别有用，例如在某些缓存或标识场景中。
 *
 * @param T 被包装的数据类型
 * @property data 被包装的数据实例
 */
class Box<T>(val data: T) {
    /**
     * 返回此对象的哈希码。
     * 
     * 此实现调用父类Object的hashCode方法，而不是委托给包装的数据对象，
     * 这确保了即使包装的对象重写了hashCode方法，Box实例也会使用默认的引用哈希码。
     *
     * @return 此Box实例的哈希码
     */
    override fun hashCode(): Int {
        return super.hashCode() // This class is needed to screen from calling data's hashCode()
    }

    /**
     * 比较此对象与指定对象是否相等。
     * 
     * 此实现调用父类Object的equals方法，而不是委托给包装的数据对象，
     * 这确保了即使包装的对象重写了equals方法，Box实例也会使用默认的引用相等性比较。
     *
     * @param other 要与之比较的对象
     * @return 如果此对象与指定对象引用相同，则返回true；否则返回false
     */
    override fun equals(other: Any?): Boolean {
        return super.equals(other) // This class is needed to screen from calling data's equals()
    }
}
