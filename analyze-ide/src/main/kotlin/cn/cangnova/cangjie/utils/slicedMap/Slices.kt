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

package cn.cangnova.cangjie.utils.slicedMap

import cn.cangnova.cangjie.psi.CjElement
import cn.cangnova.cangjie.psi.psiUtil.getElementTextWithContext
import cn.cangnova.cangjie.resolve.constants.IntegerValueTypeConstant
import cn.cangnova.cangjie.resolve.constants.fromUInt16ToLong
import cn.cangnova.cangjie.resolve.constants.fromUInt32ToLong
import cn.cangnova.cangjie.resolve.constants.fromUInt8ToLong
import cn.cangnova.cangjie.types.util.TypeUtils
import com.intellij.openapi.diagnostic.Logger

/**
 * 工具类，用于创建和管理切片映射相关功能
 */
object Slices {

    private val LOG = Logger.getInstance(Slices::class.java)

    /**
     * 仅允许重写为相等值的策略
     */
    @JvmField
    val ONLY_REWRITE_TO_EQUAL: RewritePolicy = object : RewritePolicy {
        override fun <K> rewriteProcessingNeeded(key: K): Boolean {
            return true
        }

        override fun <K, V> processRewrite(slice: WritableSlice<K, V>, key: K, oldValue: V, newValue: V): Boolean {
            if (!((oldValue == null && newValue == null) || (oldValue != null && oldValue == newValue))) {
                logErrorAboutRewritingNonEqualObjects(slice, key, oldValue, newValue)
            }
            return true
        }
    }

    /**
     * 编译时值重写策略
     * 允许重写相等对象和已转换为无符号值的有符号常量值
     * 这是为了避免使 `CompileTimeConstant` 可变
     */
    @JvmField
    val COMPILE_TIME_VALUE_REWRITE_POLICY: RewritePolicy = object : RewritePolicy {
        override fun <K> rewriteProcessingNeeded(key: K): Boolean {
            return true
        }

        override fun <K, V> processRewrite(slice: WritableSlice<K, V>, key: K, oldValue: V, newValue: V): Boolean {
            if ((oldValue == null && newValue == null) || (oldValue != null && oldValue == newValue)) return true

            if (oldValue is IntegerValueTypeConstant && newValue is IntegerValueTypeConstant) {
                if (oldValue.parameters.isPure && newValue.parameters.isUnsignedNumberLiteral) {
                    val oldConstantValue = oldValue.getValue(TypeUtils.NO_EXPECTED_TYPE).toLong()
                    val newConstantValue = newValue.getValue(TypeUtils.NO_EXPECTED_TYPE)
                    if (oldConstantValue == newConstantValue.toLong() ||
                        oldConstantValue == newConstantValue.toInt().fromUInt32ToLong() ||
                        oldConstantValue == newConstantValue.toByte().fromUInt8ToLong() ||
                        oldConstantValue == newConstantValue.toShort().fromUInt16ToLong()
                    ) {
                        return true
                    }
                }
            }

            logErrorAboutRewritingNonEqualObjects(slice, key, oldValue, newValue)

            return true
        }
    }

    /**
     * 创建一个集合式切片
     */
    @JvmStatic
    fun <K> createCollectiveSetSlice(): WritableSlice<K, Boolean> {
        return SetSlice(RewritePolicy.DO_NOTHING, true)
    }

    /**
     * 创建一个切片构建器
     */
    @JvmStatic
    fun <K, V> sliceBuilder(): SliceBuilder<K, V> {
        return SliceBuilder(ONLY_REWRITE_TO_EQUAL)
    }

    /**
     * 记录关于重写不相等对象的错误
     */
    private fun <K, V> logErrorAboutRewritingNonEqualObjects(
        slice: WritableSlice<K, V>,
        key: K,
        oldValue: V,
        newValue: V
    ) {
        // NOTE: 使用 BindingTraceContext.TRACK_REWRITES 调试此异常
        LOG.error(
            "Rewrite at slice $slice " +
                    "key: $key " +
                    "old value: $oldValue@${System.identityHashCode(oldValue)} " +
                    "new value: $newValue@${System.identityHashCode(newValue)} " +
                    (if (key is CjElement) "\n${key.getElementTextWithContext()}" else "")
        )
    }

    /**
     * 创建一个简单切片
     */
    @JvmStatic
    fun <K, V> createSimpleSlice(): WritableSlice<K, V> {
        return BasicWritableSlice(ONLY_REWRITE_TO_EQUAL)
    }

    /**
     * 创建一个简单集合切片
     */
    @JvmStatic
    fun <K> createSimpleSetSlice(): WritableSlice<K, Boolean> {
        return SetSlice(RewritePolicy.DO_NOTHING)
    }

    /**
     * 切片构建器类
     */
    class SliceBuilder<K, V>(private val rewritePolicy: RewritePolicy) {
        private var furtherLookupSlices: List<ReadOnlySlice<K, V>>? = null
        private var debugName: String? = null

        /**
         * 设置进一步查找的切片
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
         * 构建切片
         */
        fun build(): WritableSlice<K, V> {
            val result = doBuild()
            if (debugName != null) {
                result.debugName = debugName
            }
            return result
        }

        /**
         * 执行构建操作
         */
        private fun doBuild(): BasicWritableSlice<K, V> {
            return if (furtherLookupSlices != null) {
                object : BasicWritableSlice<K, V>(rewritePolicy) {
                    override fun computeValue(map: SlicedMap?, key: K, value: V?, valueNotFound: Boolean): V? {
                        if (valueNotFound) {
                            for (slice in furtherLookupSlices!!) {
                                val v = map?.get(slice, key)
                                if (v != null) {
                                    return v
                                }
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