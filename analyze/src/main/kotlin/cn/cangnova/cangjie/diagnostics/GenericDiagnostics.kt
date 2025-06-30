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

package cn.cangnova.cangjie.diagnostics

/**
 * 通用诊断接口
 * 
 * 定义了诊断集合的基本操作，支持遍历和查询功能
 *
 * @param T 诊断类型，必须是未绑定诊断的子类型
 */
interface GenericDiagnostics<T : UnboundDiagnostic> : Iterable<T> {
    /**
     * 获取所有诊断
     * 
     * @return 所有诊断的集合
     */
    fun all(): Collection<T>

    /**
     * 检查诊断集合是否为空
     * 
     * @return 如果集合为空则返回true，否则返回false
     */
    fun isEmpty(): Boolean = all().isEmpty()

    /**
     * 获取诊断迭代器
     * 
     * @return 诊断迭代器
     */
    override fun iterator(): Iterator<T> = all().iterator()
}
