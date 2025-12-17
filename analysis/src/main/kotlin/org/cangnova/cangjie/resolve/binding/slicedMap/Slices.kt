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

package org.cangnova.cangjie.resolve.binding.slicedMap

import com.intellij.openapi.diagnostic.Logger
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.psiUtil.getElementTextWithContext
import org.cangnova.cangjie.resolve.constants.IntegerValueTypeConstant
import org.cangnova.cangjie.resolve.constants.fromUInt16ToLong
import org.cangnova.cangjie.resolve.constants.fromUInt32ToLong
import org.cangnova.cangjie.resolve.constants.fromUInt8ToLong
import org.cangnova.cangjie.types.TypeUtils

/**
 * Slices 工具类，提供创建各种类型 Slice 的便捷方法
 */
object Slices {
    private val LOG = Logger.getInstance(Slices::class.java)

    /**
     * 仅允许重写为相等对象的策略
     */
    @JvmField
    val ONLY_REWRITE_TO_EQUAL: RewritePolicy = object : RewritePolicy {
        override fun <K : Any> rewriteProcessingNeeded(key: K): Boolean = true

        override fun <K : Any, V: Any> processRewrite(
            slice: WritableSlice<K, V>,
            key: K,
            oldValue: V?,
            newValue: V
        ): Boolean {
            if (oldValue != newValue && oldValue != null && oldValue != newValue) {
                logErrorAboutRewritingNonEqualObjects(slice, key, oldValue, newValue)
            }
            return true
        }
    }

    /**
     * 编译时常量值重写策略
     * 允许相等对象的重写，以及有符号常量值到无符号常量值的转换
     * 这是为了避免使 CompileTimeConstant 可变
     */
    @JvmField
    val COMPILE_TIME_VALUE_REWRITE_POLICY: RewritePolicy = object : RewritePolicy {
        override fun <K : Any> rewriteProcessingNeeded(key: K): Boolean = true

        override fun <K : Any, V: Any> processRewrite(
            slice: WritableSlice<K, V>,
            key: K,
            oldValue: V?,
            newValue: V
        ): Boolean {
            // 如果值相等，直接允许
            if (oldValue == newValue) return true

            // 处理整数常量的无符号转换
            if (oldValue is IntegerValueTypeConstant && newValue is IntegerValueTypeConstant) {
                if (oldValue.parameters.isPure && newValue.parameters.isUnsignedNumberLiteral) {
                    val oldConstantValue = oldValue.getValue(TypeUtils.NO_EXPECTED_TYPE).toLong()
                    val newConstantValue = newValue.getValue(TypeUtils.NO_EXPECTED_TYPE)

                    val matches = oldConstantValue == newConstantValue.toLong() ||
                            oldConstantValue == newConstantValue.toInt().fromUInt32ToLong() ||
                            oldConstantValue == newConstantValue.toByte().fromUInt8ToLong() ||
                            oldConstantValue == newConstantValue.toShort().fromUInt16ToLong()

                    if (matches) return true
                }
            }

            logErrorAboutRewritingNonEqualObjects(slice, key, oldValue, newValue)
            return true
        }
    }

    /**
     * 创建集合式的 Set Slice
     */
    @JvmStatic
    fun <K : Any> createCollectiveSetSlice(): WritableSlice<K, Boolean> {
        return SetSlice(RewritePolicy.Companion.DO_NOTHING, isCollective = true)
    }

    /**
     * 创建简单的 Slice
     */
    @JvmStatic
    fun <K : Any, V: Any> createSimpleSlice(): WritableSlice<K, V> {
        return BasicWritableSlice(ONLY_REWRITE_TO_EQUAL)
    }

    /**
     * 创建简单的 Set Slice
     */
    @JvmStatic
    fun <K : Any> createSimpleSetSlice(): WritableSlice<K, Boolean> {
        return SetSlice(RewritePolicy.Companion.DO_NOTHING)
    }

    /**
     * 创建 Slice 构建器
     */
    @JvmStatic
    fun <K : Any, V: Any> sliceBuilder(): SliceBuilder<K, V> {
        return SliceBuilder(ONLY_REWRITE_TO_EQUAL)
    }

    /**
     * 记录关于重写不相等对象的错误
     */
    private fun <K : Any, V: Any> logErrorAboutRewritingNonEqualObjects(
        slice: WritableSlice<K, V>,
        key: K,
        oldValue: V?,
        newValue: V
    ) {
        // 注意：使用 BindingTraceContext.TRACK_REWRITES 来调试此异常
        val message = buildString {
            append("Rewrite at slice $slice")
            append(" key: $key")
            append(" old value: $oldValue@${System.identityHashCode(oldValue)}")
            append(" new value: $newValue@${System.identityHashCode(newValue)}")
            if (key is CjElement) {
                append("\n")
                append(key.getElementTextWithContext())
            }
        }
        LOG.error(message)
    }

    /**
     * Slice 构建器，用于构建带有高级功能的 Slice
     */
    class SliceBuilder<K : Any, V: Any> internal constructor(
        private val rewritePolicy: RewritePolicy
    ) {
        private var furtherLookupSlices: List<ReadOnlySlice<K, V>>? = null
        private var debugName: String? = null

        /**
         * 设置进一步查找的 Slice 列表
         */
        fun setFurtherLookupSlices(vararg furtherLookupSlices: ReadOnlySlice<K, V>): SliceBuilder<K, V> {
            this.furtherLookupSlices = furtherLookupSlices.toList()
            return this
        }

        /**
         * 设置调试名称
         */
        fun setDebugName(debugName: String): SliceBuilder<K, V> {
            this.debugName = debugName
            return this
        }

        /**
         * 构建 WritableSlice
         */
        fun build(): WritableSlice<K, V> {
            val result = doBuild()
            debugName?.let { result.setDebugName(it) }
            return result
        }

        private fun doBuild(): BasicWritableSlice<K, V> {
            val lookupSlices = furtherLookupSlices

            return if (lookupSlices != null) {
                object : BasicWritableSlice<K, V>(rewritePolicy) {
                    override fun computeValue(
                        map: SlicedMap,
                        key: K,
                        value: V?,
                        valueNotFound: Boolean
                    ): V? {
                        if (valueNotFound) {
                            for (slice in lookupSlices) {
                                val v = map[slice, key]
                                if (v != null) return v
                            }
                            return null
                        }
                        return super.computeValue(map, key, value, false)
                    }
                }
            } else {
                BasicWritableSlice(rewritePolicy)
            }
        }
    }
}
