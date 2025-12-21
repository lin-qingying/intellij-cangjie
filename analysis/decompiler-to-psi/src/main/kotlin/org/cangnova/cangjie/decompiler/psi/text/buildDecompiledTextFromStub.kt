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

package org.cangnova.cangjie.decompiler.psi.text

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.decompiler.stub.COMPILED_DEFAULT_INITIALIZER
import org.cangnova.cangjie.decompiler.stub.COMPILED_DEFAULT_PARAMETER_VALUE
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.quoteIfNeeded
import org.cangnova.cangjie.psi.stubs.CangJieFileStubKind
import org.cangnova.cangjie.psi.stubs.impl.CangJieFileStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieModifierListStubImpl
import org.cangnova.cangjie.renderer.render
import org.cangnova.cangjie.utils.printer.PrettyPrinter

private const val DECOMPILED_CODE_COMMENT = "/* compiled code */"
private const val FLEXIBLE_TYPE_COMMENT = "/* platform type */"

/**
 * 基于 Stub 构建反编译文本。
 *
 * 这是一个高性能的实现，直接从 stub 树生成反编译文本，
 * 而不需要创建完整的 descriptor 树。
 *
 * @param fileStub 文件 stub
 * @return 反编译后的文本
 */
fun buildDecompiledText(fileStub: CangJieFileStubImpl): DecompiledText {
    val text = PrettyPrinter(indentSize = 4).apply {
        // 处理无效的 stub
        (fileStub.kind as? CangJieFileStubKind.Invalid)?.errorMessage?.let {
            return DecompiledText(it)
        }

        appendLine("// IntelliJ API Decompiler stub source generated from a cjo file")
        appendLine("// Implementation of methods is not available")
        appendLine()

        val packageFqName = fileStub.getPackageFqName()
        if (!packageFqName.isRoot) {
            append("package ")
            appendLine(packageFqName.render())
            appendLine()
        }

        // 定义访问器
        val visitor = object : CjVisitor<Unit, Unit>() {
            private inline val explicitThis get() = this

            override fun visitClass(klass: CjClass, data: Unit): Unit? {
                printClassOrInterface(klass, "class")
                return null
            }

            override fun visitInterface(cinterface: CjInterface, data: Unit): Unit? {
                printClassOrInterface(cinterface, "interface")
                return null
            }

            override fun visitStruct(cstruct: CjStruct, data: Unit): Unit? {
                printClassOrInterface(cstruct, "struct")
                return null
            }

            private fun printClassOrInterface(typeStatement: CjTypeStatement, keyword: String) {
                withSuffix(" ") { typeStatement.modifierList?.accept(explicitThis, Unit) }
                append(keyword)
                withPrefix(" ") { append(typeStatement.name?.quoteIfNeeded()) }
                typeStatement.typeParameterList?.accept(explicitThis, Unit)
                withPrefix(" ") { typeStatement.primaryConstructor?.accept(explicitThis, Unit) }
                withPrefix(" <: ") { typeStatement.getSuperTypeList()?.accept(explicitThis, Unit) }

                // 渲染 where 子句（如果存在）
                withPrefix(" ") { typeStatement.typeConstraintList?.accept(explicitThis, Unit) }

                appendLine(" {")
                withIndent {
                    val declarations = typeStatement.body?.declarations ?: emptyList()
                    withSuffix("\n") {
                        printCollectionIfNotEmpty(declarations, separator = "\n\n") {
                            it.accept(explicitThis, Unit)
                        }
                    }
                }
                append('}')
            }

            override fun visitTypeConstraintList(list: CjTypeConstraintList, data: Unit): Unit? {
                append("where ")
                printCollection(list.constraints, separator = ", ") {
                    it.accept(explicitThis, Unit)
                }
                return null
            }

            override fun visitEnum(cenum: CjEnum, data: Unit): Unit? {
                withSuffix(" ") { cenum.modifierList?.accept(explicitThis, Unit) }
                append("enum")
                withPrefix(" ") { append(cenum.name?.quoteIfNeeded()) }
                cenum.typeParameterList?.accept(explicitThis, Unit)
                withPrefix(" <: ") { cenum.getSuperTypeList()?.accept(explicitThis, Unit) }

                // 渲染 where 子句（如果存在）
                withPrefix(" ") { cenum.typeConstraintList?.accept(explicitThis, Unit) }

                appendLine(" {")
                withIndent {
                    val entries = cenum.entry
                    val members = cenum.body?.declarations ?: emptyList()

                    withSuffix("\n") {
                        "\n\n".separated(
                            {
                                printCollection(entries, separator = "|\n\n", postfix = ";") {
                                    it.accept(explicitThis, Unit)
                                }
                            },
                            {
                                printCollectionIfNotEmpty(members, separator = "\n\n") {
                                    it.accept(explicitThis, Unit)
                                }
                            },
                        )
                    }
                }
                append('}')
                return null
            }

            override fun visitEnumEntry(cjEnumEntry: CjEnumEntry, data: Unit): Unit? {
                withSuffix(" ") { cjEnumEntry.modifierList?.accept(explicitThis, Unit) }
                append(cjEnumEntry.name?.quoteIfNeeded())
                return null
            }

            override fun visitExtend(cjExtend: CjExtend, data: Unit): Unit? {
                withSuffix(" ") { cjExtend.modifierList?.accept(explicitThis, Unit) }
                append("extend")
                withPrefix(" ") { cjExtend.receiverTypeReceiver?.accept(explicitThis, Unit) }
                withPrefix(" <: ") { cjExtend.getSuperTypeList()?.accept(explicitThis, Unit) }

                // 渲染 where 子句（如果存在）
                withPrefix(" ") { cjExtend.typeConstraintList?.accept(explicitThis, Unit) }

                appendLine(" {")
                withIndent {
                    val declarations = cjExtend.body?.declarations ?: emptyList()
                    withSuffix("\n") {
                        printCollectionIfNotEmpty(declarations, separator = "\n\n") {
                            it.accept(explicitThis, Unit)
                        }
                    }
                }
                append('}')
                return null
            }

            override fun visitNamedFunction(function: CjNamedFunction, data: Unit): Unit? {
                withSuffix(" ") { function.modifierList?.accept(explicitThis, Unit) }
                append("func ")
                append(function.name?.quoteIfNeeded())
                function.typeParameterList?.accept(explicitThis, Unit)
                function.valueParameterList?.accept(explicitThis, Unit)
                withPrefix(": ") { function.typeReference?.accept(explicitThis, Unit) }

                // 渲染 where 子句（如果存在）
                withPrefix(" ") { function.typeConstraintList?.accept(explicitThis, Unit) }

                printFunctionBody(function)
                return null
            }

            override fun visitPrimaryConstructor(constructor: CjPrimaryConstructor, data: Unit): Unit? {
                withSuffix(" ") { constructor.modifierList?.accept(explicitThis, Unit) }
                constructor.valueParameterList?.accept(explicitThis, Unit)
                return null
            }

            override fun visitSecondaryConstructor(constructor: CjSecondaryConstructor, data: Unit): Unit? {
                withSuffix(" ") { constructor.modifierList?.accept(explicitThis, Unit) }
                append("init")
                constructor.valueParameterList?.accept(explicitThis, Unit)
                append(" { $DECOMPILED_CODE_COMMENT }")
                return null
            }

            private fun printFunctionBody(function: CjNamedFunction) {
                val modifierList = function.modifierList
                val isAbstract = modifierList?.hasModifier(CjTokens.ABSTRACT_KEYWORD) == true

                if (!isAbstract && function.hasBody()) {
                    append(" { $DECOMPILED_CODE_COMMENT }")
                }
            }

            override fun visitVariable(variable: CjVariable, data: Unit): Unit? {
                withSuffix(" ") { variable.modifierList?.accept(explicitThis, Unit) }
                if (variable.isVar) {
                    append("var ")
                } else {
                    append("let ")
                }
                append(variable.name?.quoteIfNeeded())
                withPrefix(": ") { variable.typeReference?.accept(explicitThis, Unit) }

                if (variable.hasInitializer()) {
                    append(" = $COMPILED_DEFAULT_INITIALIZER")
                }
                return null
            }

            override fun visitProperty(property: CjProperty, data: Unit): Unit? {
                withSuffix(" ") { property.modifierList?.accept(explicitThis, Unit) }
                if (property.isVar) {
                    append("mut prop ")
                } else {
                    append("prop ")
                }
                append(property.name?.quoteIfNeeded())
                withPrefix(": ") { property.typeReference?.accept(explicitThis, Unit) }

                appendLine(" {")
                withIndent {
                    for (accessor in property.accessors) {
                        accessor.accept(explicitThis, Unit)
                        appendLine()
                    }
                }
                append('}')
                return null
            }

            override fun visitPropertyAccessor(accessor: CjPropertyAccessor, data: Unit): Unit? {
                withSuffix(" ") { accessor.modifierList?.accept(explicitThis, Unit) }
                if (accessor.isGetter) {
                    append("get()")
                } else {
                    append("set(")
                    accessor.parameter?.let {
                        append(it.name?.quoteIfNeeded())
                    }
                    append(")")
                }
                append(" { $DECOMPILED_CODE_COMMENT }")
                return null
            }

            override fun visitTypeAlias(typeAlias: CjTypeAlias, data: Unit): Unit? {
                withSuffix(" ") { typeAlias.modifierList?.accept(explicitThis, Unit) }
                append("type ")
                append(typeAlias.name?.quoteIfNeeded())
                typeAlias.typeParameterList?.accept(explicitThis, Unit)
                withPrefix(" = ") { typeAlias.getTypeReference()?.accept(explicitThis, Unit) }
                return null
            }

            override fun visitTypeParameter(parameter: CjTypeParameter, data: Unit): Unit? {
                withSuffix(" ") { parameter.modifierList?.accept(explicitThis, Unit) }
                append(parameter.name?.quoteIfNeeded())
            // 注意：类型约束应该在 where 子句中处理，不在类型参数列表中
                // extendsBound 会通过 typeConstraintList 来渲染
                return null
            }

            override fun visitTypeParameterList(list: CjTypeParameterList, data: Unit): Unit? {
                printCollection(list.parameters, prefix = "<", postfix = ">") {
                    it.accept(explicitThis, Unit)
                }
                return null
            }

            override fun visitParameterList(cjParameterList: CjParameterList, data: Unit): Unit? {
                printCollection(cjParameterList.parameters, prefix = "(", postfix = ")") {
                    it.accept(explicitThis, Unit)
                }
                return null
            }

            override fun visitParameter(cjParameter: CjParameter, data: Unit): Unit? {
                withSuffix(" ") { cjParameter.modifierList?.accept(explicitThis, Unit) }
                append(cjParameter.name?.quoteIfNeeded())
                // 如果是命名参数，添加 ! 标记
                if (cjParameter.isNamed) {
                    append("!")
                }
                append(": ")
                cjParameter.typeReference?.accept(explicitThis, Unit)
                if (cjParameter.hasDefaultValue()) {
                    append(" = $COMPILED_DEFAULT_PARAMETER_VALUE")
                }
                return null
            }

            override fun visitTypeReference(typeReference: CjTypeReference, data: Unit): Unit? {
                typeReference.typeElement?.accept(explicitThis, Unit)
                return null
            }

            override fun visitUserType(type: CjUserType, data: Unit): Unit? {
                withSuffix(".") { type.qualifier?.accept(explicitThis, Unit) }
                val name = type.referencedName
                if (!name.isNullOrEmpty()) {
                    append(name.quoteIfNeeded())
                }
                type.typeArgumentList?.accept(explicitThis, Unit)
                return null
            }

            override fun visitFunctionType(type: CjFunctionType, data: Unit): Unit? {
                printCollection(type.parameters, prefix = "(", postfix = ")") { param ->
                    withSuffix(": ") { param.name?.let(::append) }
                    param.typeReference?.accept(explicitThis, Unit)
                }
                type.returnTypeReference?.let { returnType ->
                    append(" -> ")
                    returnType.accept(explicitThis, Unit)
                }
                return null
            }

            override fun visitOptionType(optionType: CjOptionType, data: Unit): Unit? {
                optionType.getInnerType()?.accept(explicitThis, Unit)
                append("?")
                return null
            }

            override fun visitTupleType(cjTupleType: CjTupleType, data: Unit): Unit? {
                printCollection(cjTupleType.typeArgumentsAsTypes, prefix = "(", postfix = ")") {
                    it.accept(explicitThis, Unit)
                }
                return null
            }

            override fun visitBasicType(basicType: CjBasicType, data: Unit): Unit? {
                append(basicType.name)
                return null
            }

            override fun visitTypeArgumentList(typeArgumentList: CjTypeArgumentList, data: Unit): Unit? {
                printCollection(typeArgumentList.arguments, prefix = "<", postfix = ">") {
                    it.accept(explicitThis, Unit)
                }
                return null
            }

            override fun visitTypeProjection(typeProjection: CjTypeProjection, data: Unit): Unit? {
                typeProjection.typeReference?.accept(explicitThis, Unit)
                return null
            }

            override fun visitSuperTypeList(list: CjSuperTypeList, data: Unit): Unit? {
                printCollection(list.entries, separator = " & ") {
                    it.accept(explicitThis, Unit)
                }
                return null
            }

            override fun visitSuperTypeEntry(specifier: CjSuperTypeEntry, data: Unit): Unit? {
                specifier.typeReference?.accept(explicitThis, Unit)
                return null
            }

            override fun visitSuperTypeCallEntry(call: CjSuperTypeCallEntry, data: Unit): Unit? {
                call.typeReference?.accept(explicitThis, Unit)
                return null
            }


            override fun visitTypeConstraint(constraint: CjTypeConstraint, data: Unit): Unit? {
                constraint.subjectTypeParameterName?.accept(explicitThis, Unit)
                append(" <: ")
                constraint.boundTypeReference?.accept(explicitThis, Unit)
                return null
            }

            override fun visitModifierList(list: CjModifierList, data: Unit): Unit? {
                // 注解由 visitAnnotation 处理，这里只处理修饰符
                printModifiers(list)
                return null
            }

            private fun visitAnnotationEntry(annotation: CjAnnotation) {
                append('@')
                annotation.typeReference?.accept(explicitThis, Unit)
            }

            override fun visitAnnotation(annotation: CjAnnotations, data: Unit): Unit? {
                printCollectionIfNotEmpty(annotation.entries, separator = " ") {
                    visitAnnotationEntry(it)
                }
                return null
            }

            private fun printModifiers(list: CjModifierList) {
                val stub = list.stub as? CangJieModifierListStubImpl ?: return

                var hadValue = false
                for (modifier in CjTokens.MODIFIER_KEYWORDS_ARRAY) {
                    if (!stub.hasModifier(modifier)) continue
                    if (hadValue) {
                        append(" ")
                    } else {
                        hadValue = true
                    }
                    append(modifier.value)
                }
            }

            override fun visitSimpleNameExpression(expression: CjSimpleNameExpression, data: Unit): Unit? {
                append(expression.referencedName)
                return null
            }

            override fun visitElement(element: PsiElement) {
                // 默认实现：输出占位符
                append("/* !${element::class.simpleName}! */")
                super.visitElement(element)
            }
        }

        // 获取声明并遍历
        val declarations = fileStub.getChildrenByType(
            CjFile.FILE_DECLARATION_TYPES,
            CjDeclaration.ARRAY_FACTORY
        ).asList()

        printCollectionIfNotEmpty(declarations, separator = "\n\n", postfix = "\n") {
            it.accept(visitor, Unit)
        }
    }.toString()

    return DecompiledText(text)
}