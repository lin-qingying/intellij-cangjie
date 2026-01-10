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

package org.cangnova.cangjie.psi.dummpholder

import org.cangnova.cangjie.lang.CangJieLanguage
import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.source.DummyHolder
import com.intellij.psi.impl.source.HolderFactory
import com.intellij.psi.impl.source.tree.TreeElement
import com.intellij.util.CharTable

/**
 * 工厂类，用于创建CangJie语言的DummyHolder实例。
 * 
 * 该工厂类实现了IntelliJ平台的HolderFactory接口，为CangJie语言提供了特定的DummyHolder实现。
 * DummyHolder用于在内存中表示临时的PSI元素，通常用于代码分析、重构和其他需要临时PSI树的操作。
 */
class CangJieDummyHolderFactory : HolderFactory {

    /**
     * 创建一个包含内容元素和上下文的CangJie DummyHolder实例。
     *
     * @param manager PSI管理器实例，用于管理PSI元素的生命周期
     * @param contentElement 要包含在DummyHolder中的树元素
     * @param context 创建DummyHolder的上下文PSI元素
     * @return 新创建的CangJieDummyHolder实例
     */
    override fun createHolder(manager: PsiManager, contentElement: TreeElement, context: PsiElement?): DummyHolder {
        return CangJieDummyHolder(manager, contentElement, context)
    }

    /**
     * 创建一个具有指定字符表和有效性的CangJie DummyHolder实例。
     *
     * @param manager PSI管理器实例
     * @param table 用于字符串内部化的字符表
     * @param validity 指示创建的DummyHolder是否有效
     * @return 新创建的CangJieDummyHolder实例
     */
    override fun createHolder(manager: PsiManager, table: CharTable?, validity: Boolean): DummyHolder {
        return CangJieDummyHolder(manager, table, validity)
    }

    /**
     * 创建一个只有上下文的CangJie DummyHolder实例。
     *
     * @param manager PSI管理器实例
     * @param context 创建DummyHolder的上下文PSI元素
     * @return 新创建的CangJieDummyHolder实例
     */
    override fun createHolder(manager: PsiManager, context: PsiElement?): DummyHolder {
        return CangJieDummyHolder(manager, context)
    }

    /**
     * 根据指定的语言和上下文创建DummyHolder实例。
     * 如果语言是CangJie语言，则创建CangJieDummyHolder实例，否则创建标准DummyHolder实例。
     *
     * @param manager PSI管理器实例
     * @param language 要使用的语言
     * @param context 创建DummyHolder的上下文PSI元素
     * @return 新创建的DummyHolder实例，根据语言类型可能是CangJieDummyHolder或标准DummyHolder
     */
    override fun createHolder(manager: PsiManager, language: Language , context: PsiElement?): DummyHolder {
        return if (language === CangJieLanguage) {
            CangJieDummyHolder(manager, context)
        } else {
            DummyHolder(
                manager,
                language,
                context,
            )
        }
    }

    /**
     * 创建一个包含内容元素、上下文和字符表的CangJie DummyHolder实例。
     *
     * @param manager PSI管理器实例
     * @param contentElement 要包含在DummyHolder中的树元素
     * @param context 创建DummyHolder的上下文PSI元素
     * @param table 用于字符串内部化的字符表
     * @return 新创建的CangJieDummyHolder实例
     */
    override fun createHolder(
        manager: PsiManager,
        contentElement: TreeElement?,
        context: PsiElement?,
        table: CharTable?,
    ): DummyHolder {
        return CangJieDummyHolder(manager, contentElement, context, table)
    }

    /**
     * 创建一个包含上下文和字符表的CangJie DummyHolder实例。
     *
     * @param manager PSI管理器实例
     * @param context 创建DummyHolder的上下文PSI元素
     * @param table 用于字符串内部化的字符表
     * @return 新创建的CangJieDummyHolder实例
     */
    override fun createHolder(manager: PsiManager, context: PsiElement?, table: CharTable?): DummyHolder {
        return CangJieDummyHolder(manager, context, table)
    }

    /**
     * 创建一个包含字符表和语言的CangJie DummyHolder实例。
     * 注意：此方法忽略了language参数，始终返回CangJie语言的DummyHolder。
     *
     * @param manager PSI管理器实例
     * @param table 用于字符串内部化的字符表
     * @param language 指定的语言（在此实现中被忽略）
     * @return 新创建的CangJieDummyHolder实例
     */
    override fun createHolder(manager: PsiManager, table: CharTable?, language: Language ): DummyHolder {
        return CangJieDummyHolder(manager, table)
    }
}
