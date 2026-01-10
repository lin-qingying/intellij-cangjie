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

package org.cangnova.cangjie.psi

import org.cangnova.cangjie.lexer.cdoc.psi.CDoc
import org.cangnova.cangjie.psi.psiUtil.findDocComment
import org.cangnova.cangjie.psi.stubs.CangJieTypeStatementStub
import com.intellij.lang.ASTNode
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.util.PsiTreeUtil
import java.util.concurrent.atomic.AtomicLong

/**
 * 支持 Stub 索引的仓颉声明基类
 *
 * 该类是所有支持 Stub 索引的声明 PSI 元素的基类,提供了:
 * - Stub 索引支持,用于快速符号解析而无需完全解析文件
 * - 修改时间戳跟踪,用于缓存失效
 * - 文档注释访问
 * - 导航策略支持
 *
 * Stub 是 IntelliJ 平台的性能优化机制,它允许在不解析完整 PSI 树的情况下
 * 访问关键信息(如类名、函数签名等),大幅提升大型项目的索引速度。
 *
 * @param T Stub 元素类型,必须继承自 [StubElement]
 *
 * @see CjDeclaration 声明接口
 * @see CjModifierListOwnerStub 支持修饰符列表的 Stub 基类
 * @see StubElement IntelliJ 平台的 Stub 元素接口
 */
abstract class CjDeclarationStub<T : StubElement<*>> : CjModifierListOwnerStub<T>, CjDeclaration {
    /**
     * 修改时间戳,每次子树发生变化时递增
     *
     * 用于缓存失效和增量更新判断
     */
    private val modificationStamp: AtomicLong = AtomicLong()

    constructor(stub: T, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    constructor(node: ASTNode) : super(node)

    /**
     * 获取该声明关联的表达式
     *
     * @return 关联的表达式,如果不存在则返回 null
     */
    override val expression: CjExpression?
        get() = PsiTreeUtil.getStubChildOfType(
            this,
            CjExpression::class.java,
        )

    /**
     * 当 PSI 子树发生变化时调用,递增修改时间戳
     *
     * 该方法由 IntelliJ 平台自动调用,用于通知缓存系统 PSI 树已改变
     */
    override fun subtreeChanged() {
        super.subtreeChanged()
        modificationStamp.getAndIncrement()
    }

    /**
     * 获取当前的修改时间戳
     *
     * @return 修改时间戳值,每次子树变化时递增
     */
    fun getModificationStamp(): Long {
        return modificationStamp.get()
    }

    /**
     * 获取该声明的文档注释
     *
     * @return 文档注释对象,如果不存在则返回 null
     */
    override val docComment: CDoc?
        get() {
            return findDocComment(this)
        }

    /**
     * 获取父元素
     *
     * 该方法优先使用 Stub 树结构获取父元素,以提升性能。
     * 对于本地类/对象(局部作用域中的声明),由于 Stub 父元素不正确,
     * 会回退到普通 PSI 树查找。
     *
     * @return 父 PSI 元素
     */
    override fun getParent(): PsiElement? {
        val stub = stub
        // 我们也为局部类/对象构建 Stub,但它们的父元素是错误的
        if (stub != null && !(stub is CangJieTypeStatementStub<*> && stub.isLocal())) {
            return stub.parentStub.psi
        }
        return super.getParent()
    }

    /**
     * 获取原始元素
     *
     * 原始元素用于导航到元素的源代码定义位置。
     * 通过 [CangJieDeclarationNavigationPolicy] 服务自定义导航行为。
     *
     * @return 原始元素,如果没有导航策略则返回自身
     */
    override fun getOriginalElement(): PsiElement {
        val navigationPolicy: CangJieDeclarationNavigationPolicy? = ApplicationManager.getApplication().getService(
            CangJieDeclarationNavigationPolicy::class.java,
        )
        return navigationPolicy?.getOriginalElement(this) ?: this
    }

    /**
     * 获取导航元素
     *
     * 导航元素是用户点击"转到定义"时实际跳转的目标元素。
     * 通过 [CangJieDeclarationNavigationPolicy] 服务自定义导航行为,
     * 例如可以导航到反编译的代码或库源码。
     *
     * @return 导航目标元素,如果没有导航策略则返回自身
     */
    override fun getNavigationElement(): PsiElement {
        val navigationPolicy: CangJieDeclarationNavigationPolicy? = ApplicationManager.getApplication().getService(
            CangJieDeclarationNavigationPolicy::class.java,
        )
        return navigationPolicy?.getNavigationElement(this) ?: this
    }
}
