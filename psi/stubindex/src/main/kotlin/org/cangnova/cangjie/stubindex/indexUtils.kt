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

package org.cangnova.cangjie.stubindex

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.stubs.*
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.NamedStub
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubIndexKey
import com.intellij.util.indexing.FileBasedIndexTumbler
import org.cangnova.cangjie.utils.invokeAndWaitIfNeeded
import org.cangnova.cangjie.utils.isInternal
import org.cangnova.cangjie.utils.isUnitTestMode
import org.cangnova.telemetry.performance.IndexingPerformanceTelemetry
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.TimeSource
import kotlin.time.toDuration

fun indexTypeAliasExpansion(stub: CangJieTypeAliasStub, sink: IndexSink) {
    val declaration = stub.psi
    val typeReference = declaration.getTypeReference() ?: return
    val typeElement = typeReference.typeElement ?: return
    typeElement.index(declaration, typeReference) { typeName ->
        sink.occurrence(CangJieTypeAliasByExpansionShortNameIndex.indexKey, typeName)
    }
}

/**
 * 索引内部成员的函数
 * 此函数用于处理给定的stub对象，并根据条件向sink中添加索引信息
 * 主要关注是否为顶层元素，以及是否具有开放或抽象修饰符
 *
 * @param stub 一个CangJieCallableStubBase的实例，代表一个可调用的stub对象
 * @param sink 一个IndexSink实例，用于接收索引信息
 */
fun indexInternals(stub: CangJieCallableStubBase<*>, sink: IndexSink) {
    // 获取stub的名称，如果名称为空，则直接返回
    val name = stub.name ?: return

    // 获取stub的修饰符列表，如果不存在，则直接返回
    val modifierListStub = stub.modifierList ?: return

    // 如果stub是顶层元素，则直接返回，因为不处理顶层元素
    if (stub.isTopLevel()) return

    // 如果修饰符列表中包含开放或抽象修饰符，则向sink中添加相应的索引信息
    if (modifierListStub.hasModifier(CjTokens.OPEN_KEYWORD) || modifierListStub.hasModifier(CjTokens.ABSTRACT_KEYWORD)) {
        sink.occurrence(CangJieOverridableInternalMembersShortNameIndex.indexKey, name)
    }
}





private fun CjTypeElement.index(
    declaration: CjTypeParameterListOwner,
    containingTypeReference: CjTypeReference,
    occurrence: (String) -> Unit
) {
    /**
     * 对当前类型元素进行索引，如果已经访问过则直接返回
     * 此函数用于递归地索引类型元素及其嵌套的类型元素
     *
     * @param declaration 当前上下文的类型参数所有者，用于访问其类型参数
     * @param containingTypeReference 包含当前类型元素的类型引用
     * @param visited 一个集合，用于记录已经访问过的类型元素，以避免重复访问
     * @param occurrence 一个消费型函数，用于处理每个类型元素的出现情况
     */
    fun CjTypeElement.indexWithVisited(
        declaration: CjTypeParameterListOwner,
        containingTypeReference: CjTypeReference,
        visited: MutableSet<CjTypeElement>,
        occurrence: (String) -> Unit
    ) {
        // 如果当前类型元素已经访问过，则直接返回，避免重复处理
        if (this in visited) return

        if (this is CjBasicType) return
        // 将当前类型元素标记为已访问
        visited.add(this)

        // 根据当前类型元素的不同类型，执行相应的处理逻辑
        when (this) {
            is CjUserType -> {
                // 获取当前用户定义类型的引用名称，如果为空则直接返回
                val referenceName = referencedName ?: return

                // 尝试在声明的类型参数中找到与引用名称匹配的类型参数
                val typeParameter = declaration.typeParameters.firstOrNull { it.name == referenceName }
                if (typeParameter != null) {
                    // 如果找到了匹配的类型参数，并且它有上界，则递归索引上界类型
                    val bound = typeParameter.extendsBound
                    if (bound != null) {
                        bound.typeElement?.indexWithVisited(declaration, containingTypeReference, visited, occurrence)
                    } else {
                        // 如果类型参数没有上界，则记录"Any"类型
                        occurrence("Any")
                    }
                    return
                }

                // 如果没有找到匹配的类型参数，则记录引用名称
                occurrence(referenceName)
            }

            is CjOptionType -> {
                // 对于可选类型，递归索引其内部类型
                getInnerType()?.indexWithVisited(
                    declaration,
                    containingTypeReference,
                    visited,
                    occurrence
                )
            }

            is CjTupleType -> {
                // 对于元组类型，根据其元素数量记录相应的元组类型名称
                val arity = typeArgumentsAsTypes.size

                occurrence("Tuple$arity")
            }

            is CjFunctionType -> {
                // 对于函数类型，根据其参数数量（包括接收者类型）记录相应的函数类型名称
                val arity = parameters.size + (if (receiverTypeReference != null) 1 else 0)

                occurrence("Function$arity")
            }

//            is CjBasicType -> {
//                // 对于基本类型，直接记录其名称
//                occurrence(this.name)
//            }

            else -> {
                // 如果遇到不支持的类型元素，抛出错误
                error("Unsupported type: $this")
            }
        }
    }

    indexWithVisited(declaration, containingTypeReference, mutableSetOf(), occurrence)
}


//private val STRING_TEMPLATE_EMPTY_ARRAY = emptyArray<CjStringTemplateExpression>()
//private val STRING_TEMPLATE_TYPES = TokenSet.create(CjStubElementTypes.STRING_TEMPLATE)

//private fun ValueArgument.stringTemplateExpression(): CjStringTemplateExpression? {
//    if (this is StubBasedPsiElement<*>) {
//        stub?.let {
//            val constantExpressions = it.getChildrenByType(STRING_TEMPLATE_TYPES, STRING_TEMPLATE_EMPTY_ARRAY)
//            return constantExpressions.firstOrNull()
//        }
//    }
//    return getArgumentExpression() as? CjStringTemplateExpression
//}


private val StubElement<*>.annotatedJvmNameElementName: String?
    get() = when (this) {
        is CangJieFileStub -> psi.name
        is NamedStub -> name ?: ""

        is CangJiePlaceHolderStub -> parentStub?.annotatedJvmNameElementName
        else -> null
    }

private val CangJieStubWithFqName<*>.modifierList: CangJieModifierListStub?
    get() = findChildStubByType(CjStubElementTypes.MODIFIER_LIST)



fun runAfterIndexing(project: Project, callback: Runnable) {
    DumbService.getInstance(project).runWhenSmart(callback)
}

fun updateIndex() {
    invokeAndWaitIfNeeded {
        val tumbler = FileBasedIndexTumbler("Reindex")
        try {
            tumbler.turnOff()
        } finally {
            tumbler.turnOn()
        }
    }
}

fun getByKeyMaxDuration(): Duration =
    Registry.intValue("cangjie.indices.timing.threshold.single").toDuration(DurationUnit.MILLISECONDS)


inline fun <T> getByKeyAndMeasure(index: StubIndexKey<*, *>, log: Logger, crossinline block: () -> T): T =
    measureIndexCall(index, "getByKey", getByKeyMaxDuration(), log, block)


inline fun <T> measureIndexCall(
    index: StubIndexKey<*, *>,
    prefix: String,
    threshold: Duration,
    log: Logger,
    crossinline block: () -> T
): T {
    val operationId = IndexingPerformanceTelemetry.startIndexing("stub_index_${prefix}")
    val mark = TimeSource.Monotonic.markNow()

    try {
        val t = block()
        val elapsed = mark.elapsedNow()
        val elapsedMs = elapsed.inWholeMilliseconds

        // 发送性能遥测事件
        IndexingPerformanceTelemetry.endIndexing(
            operationId,
            // Stub索引操作通常不直接对应文件数量
            additionalInfo = mapOf(
                "index_name" to index.name,
                "operation_type" to prefix,
                "threshold_ms" to threshold.inWholeMilliseconds.toString()
            )
        )

        if (elapsed > threshold) {
            if (isInternal && !isUnitTestMode && Registry.`is`("cangjie.indices.timing.enabled")) {
                log.error("${index.name} $prefix took $elapsed more than expected $threshold")
            }

            // 发送性能警告遥测事件
            org.cangnova.telemetry.error.ErrorTelemetry.sendPerformanceWarning(
                "stub_index_operation",
                elapsedMs,
                threshold.inWholeMilliseconds,
                "stub_index_${prefix}",
                mapOf(
                    "index_name" to index.name,
                    "operation_type" to prefix
                )
            )
        }

        return t
    } catch (e: Exception) {
        // 发送索引错误遥测事件
        IndexingPerformanceTelemetry.sendIndexingError(
            "stub_index_${prefix}",
            "Error during ${index.name} $prefix operation",
            e,
            mapOf(
                "index_name" to index.name,
                "operation_type" to prefix
            )
        )
        throw e
    }
}

inline fun <T> processElementsAndMeasure(index: StubIndexKey<*, *>, log: Logger, crossinline block: () -> T): T =
    measureIndexCall(
        index,
        "processElements",
        processElementsMaxDuration(),
        log,
        block
    )


inline fun <T> getAllKeysAndMeasure(index: StubIndexKey<*, *>, log: Logger, crossinline block: () -> T): T =
    measureIndexCall(index, "getAllKeys", processElementsMaxDuration(), log, block)


fun processElementsMaxDuration(): Duration =
    Registry.intValue("cangjie.indices.timing.threshold.batch").toDuration(DurationUnit.MILLISECONDS)


inline fun <T> processAllKeysAndMeasure(index: StubIndexKey<*, *>, log: Logger, crossinline block: () -> T): T =
    measureIndexCall(index, "processAllKeys", processElementsMaxDuration(), log, block)


