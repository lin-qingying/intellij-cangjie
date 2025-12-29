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

import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.macro.MacroDescriptor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjImportItem
import org.cangnova.cangjie.resolve.source.CangJieSourceElement

/**
 * 重导出声明描述符
 *
 * 这是一个委托描述符，代表通过 public/protected/internal import 重导出的声明。
 * 它包装了原始声明，并提供重导出相关的额外信息。
 *
 * ## 设计思路
 *
 * 根据仓颉编译器的实现，重导出的声明会被添加到包的 declMap 中，作为包的成员之一。
 * 这个描述符类通过委托模式，将所有描述符接口方法委托给原始声明，同时保留重导出的元信息。
 *
 * @property originalDescriptor 原始声明描述符
 * @property reexportVisibility 重导出的可见性（来自 import 语句的修饰符）
 * @property sourceFile 包含重导出语句的源文件
 * @property importDirective 重导出的 import 指令
 * @property aliasName 重导出时的别名（如果有）
 */
sealed class ReexportedDeclarationDescriptor {

    /**
     * 重导出的分类器描述符（类、接口、枚举等）
     */
    class Classifier(
        val originalDescriptor: ClassifierDescriptor,
        val reexportVisibility: DescriptorVisibility,
        val sourceFile: CjFile,
        val importDirective: CjImportItem,
        val aliasName: Name? = null
    ) : ClassifierDescriptor by originalDescriptor {


        val effectiveName: Name
            get() = aliasName ?: originalDescriptor.name

        override val name: Name
            get() = effectiveName

        override val containingDeclaration: DeclarationDescriptor
            get() = originalDescriptor.containingDeclaration

        override val annotations: Annotations
            get() = originalDescriptor.annotations

        override val original: ClassifierDescriptor
            get() = originalDescriptor.original as ClassifierDescriptor

        override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R? {
            return originalDescriptor.accept(visitor, data)
        }

        override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Unit, Unit>) {
            originalDescriptor.acceptVoid(visitor)
        }
    }

    /**
     * 重导出的函数描述符
     */
    class Function(
        val originalDescriptor: SimpleFunctionDescriptor,
        val reexportVisibility: DescriptorVisibility,
        val sourceFile: CjFile,
        val importDirective: CjImportItem,
        val aliasName: Name? = null
    ) : SimpleFunctionDescriptor by originalDescriptor {

        val effectiveName: Name
            get() = aliasName ?: originalDescriptor.name

        override val name: Name
            get() = effectiveName

        override val containingDeclaration: DeclarationDescriptor
            get() = originalDescriptor.containingDeclaration

        override val annotations: Annotations
            get() = originalDescriptor.annotations

        override val original: SimpleFunctionDescriptor
            get() = originalDescriptor.original as SimpleFunctionDescriptor

        override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R? {
            return originalDescriptor.accept(visitor, data)
        }

        override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Unit, Unit>) {
            originalDescriptor.acceptVoid(visitor)
        }
    }

    /**
     * 重导出的变量描述符
     */
    class Variable(
        val originalDescriptor: VariableDescriptor,
        val reexportVisibility: DescriptorVisibility,
        val sourceFile: CjFile,
        val importDirective: CjImportItem,
        val aliasName: Name? = null
    ) : VariableDescriptor by originalDescriptor {

        val effectiveName: Name
            get() = aliasName ?: originalDescriptor.name

        override val name: Name
            get() = effectiveName

        override val containingDeclaration: DeclarationDescriptor
            get() = originalDescriptor.containingDeclaration

        override val annotations: Annotations
            get() = originalDescriptor.annotations

        override val original: VariableDescriptor
            get() = originalDescriptor.original as VariableDescriptor

        override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R? {
            return originalDescriptor.accept(visitor, data)
        }

        override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Unit, Unit>) {
            originalDescriptor.acceptVoid(visitor)
        }
    }

    /**
     * 重导出的属性描述符
     */
    class Property(
        val originalDescriptor: PropertyDescriptor,
        val reexportVisibility: DescriptorVisibility,
        val sourceFile: CjFile,
        val importDirective: CjImportItem,
        val aliasName: Name? = null
    ) : PropertyDescriptor by originalDescriptor {

        val effectiveName: Name
            get() = aliasName ?: originalDescriptor.name

        override val name: Name
            get() = effectiveName

        override val containingDeclaration: DeclarationDescriptor
            get() = originalDescriptor.containingDeclaration

        override val annotations: Annotations
            get() = originalDescriptor.annotations

        override val original: PropertyDescriptor
            get() = originalDescriptor.original as PropertyDescriptor

        override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R? {
            return originalDescriptor.accept(visitor, data)
        }

        override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Unit, Unit>) {
            originalDescriptor.acceptVoid(visitor)
        }
    }

    /**
     * 重导出的宏描述符
     */
    class Macro(
        val originalDescriptor: MacroDescriptor,
        val reexportVisibility: DescriptorVisibility,
        val sourceFile: CjFile,
        val importDirective: CjImportItem,
        val aliasName: Name? = null
    ) : MacroDescriptor by originalDescriptor {

        val effectiveName: Name
            get() = aliasName ?: originalDescriptor.name

        override val name: Name
            get() = effectiveName

        override val containingDeclaration: DeclarationDescriptor
            get() = originalDescriptor.containingDeclaration

        override val annotations: Annotations
            get() = originalDescriptor.annotations

        override val original: MacroDescriptor
            get() = originalDescriptor.original as MacroDescriptor

        override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R? {
            return originalDescriptor.accept(visitor, data)
        }

        override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Unit, Unit>) {
            originalDescriptor.acceptVoid(visitor)
        }
    }
}
