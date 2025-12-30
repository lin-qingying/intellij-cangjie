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
package org.cangnova.cangjie.types

import org.cangnova.cangjie.Box
import org.cangnova.cangjie.ReenteringLazyValueComputationException
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.storage.NotNullLazyValue
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.types.ErrorUtils.createErrorType
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
import org.cangnova.cangjie.types.error.ErrorTypeKind


/**
 * DeferredType 类代表一个延迟计算的类型。它继承自 WrappedType，用于封装类型计算的延迟评估机制。
 * 它主要通过 NotNullLazyValue 来管理延迟计算的过程，并提供了处理递归类型计算的能力。
 */
class DeferredType
/**
 * 构造函数，初始化 DeferredType 实例。
 *
 * @param lazyValue 延迟计算的值。
 */ private constructor(
    /**
     * lazyValue 是一个私有字段，表示延迟计算的值。
     */
    private val lazyValue: NotNullLazyValue<CangJieType>
) : WrappedType() {
    /**
     * 通过给定的类型细化器细化类型。
     *
     * @param cangjieTypeRefiner 类型细化器。
     * @return 返回细化后的 DeferredType。
     */
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): CangJieType {
        return DeferredType(object : NotNullLazyValue<CangJieType> {
            override fun renderDebugInformation(): String {
                return lazyValue.renderDebugInformation()
            }

            override fun isComputed(): Boolean {
                return lazyValue.isComputed()
            }

            override fun isComputing(): Boolean {
                return lazyValue.isComputing()
            }

            override fun invoke(): CangJieType {
                return cangjieTypeRefiner.refineType(lazyValue.invoke())
            }
        })
    }

    val isComputing: Boolean
        /**
         * 检查是否正在计算类型。
         *
         * @return 如果正在计算类型，则返回 true；否则返回 false。
         */
        get() = lazyValue.isComputing()

    /**
     * 检查类型是否已计算。
     *
     * @return 如果类型已计算，则返回 true；否则返回 false。
     */
    override fun isComputed(): Boolean {
        return lazyValue.isComputed()
    }

    public override val delegate: CangJieType
        /**
         * 获取代理的 CangJieType。
         *
         * @return 返回代理的 CangJieType。
         */
        get() = lazyValue.invoke()

    /**
     * 重写 toString 方法，提供 DeferredType 的字符串表示。
     *
     * @return 返回 DeferredType 的字符串表示。
     */
    override fun toString(): String {
        return try {
            if (lazyValue.isComputed()) {
                delegate.toString()
            } else {
                "<Not computed yet>"
            }
        } catch (e: ReenteringLazyValueComputationException) {
            "<Failed to compute this type>"
        }
    }

    companion object {
        /**
         * RECURSION_PREVENTER 是一个静态常量，用于防止递归计算。
         * 当第一次尝试访问未计算的值时，它会抛出 ReenteringLazyValueComputationException 异常，
         * 并返回一个表示错误类型的 CangJieType。
         */
        private val RECURSION_PREVENTER: (Boolean) -> CangJieType = { firstTime: Boolean ->
            if (firstTime) throw ReenteringLazyValueComputationException()
            createErrorType(ErrorTypeKind.RECURSIVE_TYPE)
        }

        /**
         * 创建一个 DeferredType 实例。
         *
         * @param storageManager 存储管理器，用于创建懒值。
         * @param trace 绑定跟踪对象，用于记录类型信息。
         * @param compute 计算类型值的函数。
         * @return 返回创建的 DeferredType 实例。
         */
        fun create(
            storageManager: StorageManager,
            trace: BindingTrace,
            compute: () -> CangJieType
        ): DeferredType {
            val deferredType = DeferredType(storageManager.createLazyValue(compute))
            trace.record(BindingContext.DEFERRED_TYPE, Box(deferredType))
            return deferredType
        }

        /**
         * 创建一个不容忍递归计算的 DeferredType 实例。
         *
         * @param storageManager 存储管理器，用于创建懒值。
         * @param trace 绑定跟踪对象，用于记录类型信息。
         * @param compute 计算类型值的函数。
         * @return 返回创建的 DeferredType 实例。
         */
        fun createRecursionIntolerant(
            storageManager: StorageManager,
            trace: BindingTrace,
            compute: () -> CangJieType
        ): DeferredType {
            val deferredType = DeferredType(storageManager.createLazyValue(compute, RECURSION_PREVENTER))
            trace.record(BindingContext.DEFERRED_TYPE, Box(deferredType))
            return deferredType
        }
    }
}


/**
 * DeferredType 类代表一个延迟计算的类型。它继承自 WrappedType，用于封装类型计算的延迟评估机制。
 * 它主要通过 NotNullLazyValue 来管理延迟计算的过程，并提供了处理递归类型计算的能力。
 */
class DeferredTypeNoCache
/**
 * 构造函数，初始化 DeferredType 实例。
 *
 * @param lazyValue 延迟计算的值。
 */ private constructor(
    /**
     * lazyValue 是一个私有字段，表示延迟计算的值。
     */
    private val lazyValue: () -> CangJieType
) : WrappedType() {
    /**
     * 通过给定的类型细化器细化类型。
     *
     * @param cangjieTypeRefiner 类型细化器。
     * @return 返回细化后的 DeferredType。
     */
    override fun refine(cangjieTypeRefiner: CangJieTypeRefiner): CangJieType {
        return this
    }


    override val delegate: CangJieType
        /**
         * 获取代理的 CangJieType。
         *
         * @return 返回代理的 CangJieType。
         */
        get() = lazyValue.invoke()

    /**
     * 重写 toString 方法，提供 DeferredType 的字符串表示。
     *
     * @return 返回 DeferredType 的字符串表示。
     */
    override fun toString(): String {
        return try {

            delegate.toString()

        } catch (e: ReenteringLazyValueComputationException) {
            "<Failed to compute this type>"
        }
    }

    companion object {
        /**
         * RECURSION_PREVENTER 是一个静态常量，用于防止递归计算。
         * 当第一次尝试访问未计算的值时，它会抛出 ReenteringLazyValueComputationException 异常，
         * 并返回一个表示错误类型的 CangJieType。
         */
        private val RECURSION_PREVENTER: (Boolean) -> CangJieType = { firstTime: Boolean ->
            if (firstTime) throw ReenteringLazyValueComputationException()
            createErrorType(ErrorTypeKind.RECURSIVE_TYPE)
        }

        /**
         * 创建一个 DeferredType 实例。
         *
         * @param storageManager 存储管理器，用于创建懒值。
         * @param trace 绑定跟踪对象，用于记录类型信息。
         * @param compute 计算类型值的函数。
         * @return 返回创建的 DeferredType 实例。
         */
        fun create(
            storageManager: StorageManager,
            trace: BindingTrace,
            compute: () -> CangJieType
        ): DeferredTypeNoCache {
            val deferredType = DeferredTypeNoCache(storageManager.createLazyValue(compute))
            trace.record(BindingContext.DEFERRED_TYPE_NO_CACHE, Box(deferredType))
            return deferredType
        }

        /**
         * 创建一个不容忍递归计算的 DeferredType 实例。
         *
         * @param storageManager 存储管理器，用于创建懒值。
         * @param trace 绑定跟踪对象，用于记录类型信息。
         * @param compute 计算类型值的函数。
         * @return 返回创建的 DeferredType 实例。
         */
        fun createRecursionIntolerant(
            storageManager: StorageManager,
            trace: BindingTrace,
            compute: () -> CangJieType
        ): DeferredTypeNoCache {
            val deferredType = DeferredTypeNoCache(compute)
            trace.record(BindingContext.DEFERRED_TYPE_NO_CACHE, Box(deferredType))
            return deferredType
        }
    }
}

