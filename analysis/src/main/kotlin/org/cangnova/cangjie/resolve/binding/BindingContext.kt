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

package org.cangnova.cangjie.resolve.binding

import com.google.common.collect.ImmutableMap
import com.google.common.collect.Multimap
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.Box
import org.cangnova.cangjie.ReadOnly
import org.cangnova.cangjie.contracts.description.EventOccurrencesRange
import org.cangnova.cangjie.contracts.model.Computation
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.annotations.AnnotationDescriptor
import org.cangnova.cangjie.descriptors.impl.PropertyAccessorDescriptor
import org.cangnova.cangjie.descriptors.macro.MacroDescriptor
import org.cangnova.cangjie.diagnostics.Diagnostics
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.FqNameUnsafe
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.binding.slicedMap.*
import org.cangnova.cangjie.resolve.binding.slicedMap.RewritePolicy.Companion.DO_NOTHING
import org.cangnova.cangjie.resolve.binding.slicedMap.Slices.COMPILE_TIME_VALUE_REWRITE_POLICY
import org.cangnova.cangjie.resolve.caches.PrimitiveNumericComparisonInfo
import org.cangnova.cangjie.resolve.calls.context.BasicCallResolutionContext
import org.cangnova.cangjie.resolve.calls.model.PartialCallContainer
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValue
import org.cangnova.cangjie.resolve.calls.smartcasts.ExplicitSmartCasts
import org.cangnova.cangjie.resolve.calls.tower.CangJieResolutionCallbacksImpl
import org.cangnova.cangjie.resolve.constants.CompileTimeConstant
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.resolve.scopes.receivers.QualifierReceiver
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.DeferredType
import org.cangnova.cangjie.types.DeferredTypeNoCache
import org.cangnova.cangjie.types.expressions.CangJieTypeInfo
import org.cangnova.cangjie.types.expressions.CaptureKind
import org.cangnova.cangjie.types.expressions.PreliminaryDeclarationVisitor
import org.cangnova.cangjie.types.expressions.match.Pattern
import org.jetbrains.annotations.TestOnly

/**
 * BindingContext 接口
 *
 * 绑定上下文，用于存储和查询编译过程中的各种绑定信息，如类型、声明、引用等。
 * 使用 SlicedMap 作为底层存储，提供类型安全的多维键值对访问。
 */
interface BindingContext {

    /**
     * 获取诊断信息
     *
     * @return 诊断信息集合
     */
    val diagnostics: Diagnostics

    /**
     * 从指定 slice 中获取值
     *
     * @param slice 切片
     * @param key 键
     * @return 对应的值，如果不存在则返回 null
     */
    operator fun <K : Any, V : Any> get(slice: ReadOnlySlice<K, V>, key: K): V?

    /**
     * 获取集合式 slice 的所有键
     *
     * 注意：slice.isCollective() 必须为 true
     *
     * @param slice 集合式切片
     * @return 所有键的集合
     */
    @ReadOnly
    fun <K : Any, V : Any> getKeys(slice: WritableSlice<K, V>): Collection<K>

    /**
     * 获取指定 slice 的所有内容
     *
     * 此方法仅用于调试和测试
     *
     * @param slice 切片
     * @return 不可变的 Map，包含所有内容
     */
    @TestOnly
    fun <K : Any, V : Any> getSliceContents(slice: ReadOnlySlice<K, V>): ImmutableMap<K, V>

    /**
     * 获取表达式的类型
     *
     * @param expression 表达式
     * @return 类型，如果不存在则返回 null
     */
    fun getType(expression: CjExpression): CangJieType?

    /**
     * 将自己的数据添加到指定的 trace 中
     *
     * @param trace 绑定跟踪
     * @param commitDiagnostics 是否提交诊断信息
     */
    fun addOwnDataTo(trace: BindingTrace, commitDiagnostics: Boolean)

    companion object {
        /**
         * 空的 BindingContext
         */

        val EMPTY: BindingContext = object : BindingContext {
            override val diagnostics: Diagnostics
                get() = Diagnostics.EMPTY

            override fun <K : Any, V : Any> get(slice: ReadOnlySlice<K, V>, key: K): V? = null

            override fun <K : Any, V : Any> getKeys(slice: WritableSlice<K, V>): Collection<K> = emptyList()

            @TestOnly
            override fun <K : Any, V : Any> getSliceContents(slice: ReadOnlySlice<K, V>): ImmutableMap<K, V> {

                return ImmutableMap.of<K, V>()
            }

            override fun getType(expression: CjExpression): CangJieType? = null

            override fun addOwnDataTo(trace: BindingTrace, commitDiagnostics: Boolean) {
                // Do nothing
            }
        }

        // ========================================================================
        // 类型相关的 Slice
        // ========================================================================


        val DEFERRED_TYPE_NO_CACHE: WritableSlice<Box<DeferredTypeNoCache>, Boolean> =
            Slices.createCollectiveSetSlice()


        val DEFERRED_TYPE: WritableSlice<Box<DeferredType>, Boolean> = Slices.createCollectiveSetSlice()


        val TYPE: WritableSlice<CjTypeReference, CangJieType> = Slices.createSimpleSlice()


        val ABBREVIATED_TYPE: WritableSlice<CjTypeReference, CangJieType> = Slices.createSimpleSlice()


        val EXPECTED_EXPRESSION_TYPE: WritableSlice<CjExpression, CangJieType> = BasicWritableSlice(DO_NOTHING)


        val EXPRESSION_TYPE_INFO: WritableSlice<CjExpression, CangJieTypeInfo> = BasicWritableSlice(DO_NOTHING)


        val EXPECTED_RETURN_TYPE: WritableSlice<CjFunction, CangJieType> = BasicWritableSlice(DO_NOTHING)


        val THIS_TYPE_FOR_SUPER_EXPRESSION: WritableSlice<CjSuperExpression, CangJieType> =
            BasicWritableSlice(DO_NOTHING)

        // ========================================================================
        // 调用相关的 Slice
        // ========================================================================


        val COLLECTION_LITERAL_CALL: WritableSlice<CjCollectionLiteralExpression, ResolvedCall<FunctionDescriptor>> =
            Slices.createSimpleSlice()


        val RANGE_LITERAL_CALL: WritableSlice<CjRangeExpression, ResolvedCall<FunctionDescriptor>> =
            Slices.createSimpleSlice()


        val LOOP_RANGE_ITERATOR_RESOLVED_CALL: WritableSlice<CjExpression, ResolvedCall<FunctionDescriptor>> =
            Slices.createSimpleSlice()


        val LOOP_RANGE_HAS_NEXT_RESOLVED_CALL: WritableSlice<CjExpression, ResolvedCall<FunctionDescriptor>> =
            Slices.createSimpleSlice()


        val LOOP_RANGE_NEXT_RESOLVED_CALL: WritableSlice<CjExpression, ResolvedCall<FunctionDescriptor>> =
            Slices.createSimpleSlice()


        val INDEXED_LVALUE_SET: WritableSlice<CjExpression, ResolvedCall<FunctionDescriptor>> =
            Slices.createSimpleSlice()


        val INDEXED_LVALUE_GET: WritableSlice<CjExpression, ResolvedCall<FunctionDescriptor>> =
            Slices.createSimpleSlice()


        val RESOLVED_CALL: WritableSlice<Call, ResolvedCall<*>> = BasicWritableSlice(DO_NOTHING)


        val ONLY_RESOLVED_CALL: WritableSlice<Call, PartialCallContainer> = BasicWritableSlice(DO_NOTHING)


        val CALL: WritableSlice<CjElement, Call> = BasicWritableSlice(DO_NOTHING)


        val PARTIAL_CALL_RESOLUTION_CONTEXT: WritableSlice<Call, BasicCallResolutionContext> =
            BasicWritableSlice(DO_NOTHING)


        val DELEGATE_EXPRESSION_TO_PROVIDE_DELEGATE_CALL: WritableSlice<CjExpression, Call> =
            BasicWritableSlice(DO_NOTHING)


        val CONSTRUCTOR_RESOLVED_DELEGATION_CALL: WritableSlice<ConstructorDescriptor, ResolvedCall<ConstructorDescriptor>> =
            Slices.createSimpleSlice()

        // ========================================================================
        // 智能转换相关的 Slice
        // ========================================================================


        val UNSTABLE_SMARTCAST: WritableSlice<CjExpression, ExplicitSmartCasts> = BasicWritableSlice(DO_NOTHING)


        val SMARTCAST: WritableSlice<CjExpression, ExplicitSmartCasts> = BasicWritableSlice(DO_NOTHING)


        val SMARTCAST_NULL: WritableSlice<CjExpression, Boolean> = Slices.createSimpleSlice()

        // ========================================================================
        // 数据流相关的 Slice
        // ========================================================================


        val DATAFLOW_INFO_AFTER_CONDITION: WritableSlice<CjExpression, DataFlowInfo> = Slices.createSimpleSlice()


        val DATA_FLOW_INFO_BEFORE: WritableSlice<CjExpression, DataFlowInfo> = BasicWritableSlice(DO_NOTHING)


        val BOUND_INITIALIZER_VALUE: WritableSlice<VariableDescriptor, DataFlowValue> = Slices.createSimpleSlice()

        // ========================================================================
        // 变量相关的 Slice
        // ========================================================================


        val IS_UNINITIALIZED: WritableSlice<VariableDescriptor, Boolean> = Slices.createSimpleSetSlice()


        val VARIABLE_REASSIGNMENT: WritableSlice<CjExpression, Boolean> = BasicWritableSlice(DO_NOTHING)


        val CAPTURED_IN_CLOSURE: WritableSlice<VariableDescriptor, CaptureKind> = BasicWritableSlice(DO_NOTHING)


        val VARIABLE: WritableSlice<PsiElement, VariableDescriptor> = Slices.createSimpleSlice()


        val VALUE_PARAMETER: WritableSlice<CjParameterBase, VariableDescriptor> = Slices.createSimpleSlice()


        val PRIMARY_CONSTRUCTOR_PARAMETER: WritableSlice<PsiElement, VariableDescriptor> =
            Slices.createSimpleSlice()

        /**
         * 表示形参转变量声明
         */

        val VALUE_PARAMETER_AS_VARIABLE: WritableSlice<ValueParameterDescriptor, VariableDescriptor> =
            Slices.createSimpleSlice()


        val AUTO_CREATED_IT: WritableSlice<ValueParameterDescriptor, Boolean> = Slices.createSimpleSetSlice()


        val NEW_INFERENCE_CATCH_EXCEPTION_PARAMETER: WritableSlice<CjExpression, Ref<VariableDescriptor>> =
            Slices.createSimpleSlice()


        val NEW_INFERENCE_TRY_EXCEPTION_PARAMETER: WritableSlice<CjExpression, Ref<List<VariableDescriptor>>> =
            Slices.createSimpleSlice()

        /**
         * 像块插入局部变量
         */

        val NEW_INFENCE_BLOCK_EXCEPTION_PARAMETER: WritableSlice<CjExpression, Ref<List<VariableDescriptor>>> =
            Slices.createSimpleSlice()

        // ========================================================================
        // Lambda 相关的 Slice
        // ========================================================================


        val LAMBDA_INVOCATIONS: WritableSlice<CjLambdaExpression, EventOccurrencesRange> =
            Slices.createSimpleSlice()


        val USED_AS_RESULT_OF_LAMBDA: WritableSlice<CjElement, Boolean> = Slices.createSimpleSetSlice()


        val NEW_INFERENCE_LAMBDA_INFO: WritableSlice<CjFunction, CangJieResolutionCallbacksImpl.LambdaInfo> =
            BasicWritableSlice(DO_NOTHING)

        // ========================================================================
        // 表达式相关的 Slice
        // ========================================================================


        val PROCESSED: WritableSlice<CjExpression, Boolean> = Slices.createSimpleSlice()


        val USED_AS_EXPRESSION: WritableSlice<CjElement, Boolean> = BasicWritableSlice(DO_NOTHING)


        val COMPILE_TIME_VALUE: WritableSlice<CjExpression, CompileTimeConstant<*>> =
            BasicWritableSlice(COMPILE_TIME_VALUE_REWRITE_POLICY)


        val CAST_TYPE_USED_AS_EXPECTED_TYPE: WritableSlice<CjBinaryExpressionWithTypeRHS, Boolean> =
            Slices.createSimpleSlice()


        val PRIMITIVE_NUMERIC_COMPARISON_INFO: WritableSlice<CjExpression, PrimitiveNumericComparisonInfo> =
            Slices.createSimpleSlice()


        val QUALIFIER: WritableSlice<CjExpression, QualifierReceiver> = BasicWritableSlice(DO_NOTHING)


        val EXPRESSION_EFFECTS: WritableSlice<CjElement, Computation> = Slices.createSimpleSlice()

        // ========================================================================
        // 引用相关的 Slice
        // ========================================================================


        val REFERENCE_TARGET: WritableSlice<CjReferenceExpression, DeclarationDescriptor> =
            BasicWritableSlice(DO_NOTHING)


        val AMBIGUOUS_REFERENCE_TARGET: WritableSlice<CjExpression, Collection<DeclarationDescriptor>> =
            BasicWritableSlice(DO_NOTHING)


        val THIS_REFERENCE_TARGET: WritableSlice<CjReferenceExpression, ReceiverParameterDescriptor> =
            BasicWritableSlice(DO_NOTHING)


        val SHORT_REFERENCE_TO_COMPANION_OBJECT: WritableSlice<CjReferenceExpression, ClassifierDescriptorWithTypeParameters> =
            BasicWritableSlice(DO_NOTHING)


        val LABEL_TARGET: WritableSlice<CjReferenceExpression, PsiElement> = Slices.createSimpleSlice()


        val AMBIGUOUS_LABEL_TARGET: WritableSlice<CjReferenceExpression, Collection<PsiElement>> =
            Slices.createSimpleSlice()


        val IS_FUNC: WritableSlice<CjCallableReference, Boolean> = Slices.createSimpleSlice()

        // ========================================================================
        // 声明相关的 Slice
        // ========================================================================


        val CLASS: WritableSlice<PsiElement, ClassDescriptor> = Slices.createSimpleSlice()


        val FUNCTION: WritableSlice<PsiElement, SimpleFunctionDescriptor> = Slices.createSimpleSlice()


        val MACRO: WritableSlice<PsiElement, MacroDescriptor> = Slices.createSimpleSlice()


        val CONSTRUCTOR: WritableSlice<PsiElement, ConstructorDescriptor> = Slices.createSimpleSlice()


        val END_CONSTRUCTOR: WritableSlice<PsiElement, ConstructorDescriptor> = Slices.createSimpleSlice()


        val TYPE_PARAMETER: WritableSlice<CjTypeParameter, TypeParameterDescriptor> = Slices.createSimpleSlice()


        val TYPE_ALIAS: WritableSlice<PsiElement, TypeAliasDescriptor> = Slices.createSimpleSlice()


        val EXTEND: WritableSlice<PsiElement, org.cangnova.cangjie.descriptors.extend.ExtendDescriptor> = Slices.createSimpleSlice()


        val PROPERTY_ACCESSOR: WritableSlice<CjPropertyAccessor, PropertyAccessorDescriptor> =
            Slices.createSimpleSlice()


        val ANNOTATION: WritableSlice<CjAnnotation, AnnotationDescriptor> = Slices.createSimpleSlice()


        val PRELIMINARY_VISITOR: WritableSlice<CjDeclaration, PreliminaryDeclarationVisitor> =
            BasicWritableSlice(DO_NOTHING)

        /**
         * return语句的目标，可能是方法，lambda表达式
         */

        val RETURN_TARGET: WritableSlice<CjDeclaration, Set<CjExpression>> = Slices.createSimpleSlice()


        val DECLARATIONS_TO_DESCRIPTORS: Array<WritableSlice<*, *>> = arrayOf(
            CLASS,
            TYPE_PARAMETER,
            MACRO,
            FUNCTION,
            CONSTRUCTOR,
            VARIABLE,
            VALUE_PARAMETER,
            PROPERTY_ACCESSOR,
            PRIMARY_CONSTRUCTOR_PARAMETER,
            TYPE_ALIAS
        )


        @Suppress("UNCHECKED_CAST")
        val DECLARATION_TO_DESCRIPTOR: ReadOnlySlice<PsiElement, DeclarationDescriptor> =
            Slices.sliceBuilder<PsiElement, DeclarationDescriptor>()
                .setFurtherLookupSlices(*(DECLARATIONS_TO_DESCRIPTORS as Array<ReadOnlySlice<PsiElement, DeclarationDescriptor>>))
                .build()

        // ========================================================================
        // 其他 Slice
        // ========================================================================


        val FQNAME_TO_CLASS_DESCRIPTOR: WritableSlice<FqNameUnsafe, ClassDescriptor> =
            BasicWritableSlice(DO_NOTHING, true)


        val BACKING_FIELD_REQUIRED: WritableSlice<PropertyDescriptor, Boolean> =
            object : BasicWritableSlice<PropertyDescriptor, Boolean>(DO_NOTHING) {
                override fun computeValue(
                    map: SlicedMap,
                    key: PropertyDescriptor,
                    value: Boolean?,
                    valueNotFound: Boolean
                ): Boolean {
                    if (key.kind != CallableMemberDescriptor.Kind.DECLARATION) {
                        return false
                    }
                    // 以下代码已注释，保留以备将来使用
                    // val declarationPsiElement = DescriptorToSourceUtils.descriptorToDeclaration(propertyDescriptor)
                    // if (declarationPsiElement is CjParameter) {
                    //     return declarationPsiElement.hasLetOrVar() || backingFieldRequired
                    // }
                    // if (propertyDescriptor.modality == Modality.ABSTRACT) return false
                    // if (declarationPsiElement is CjProperty) return false
                    // val getter = propertyDescriptor.getter
                    // val setter = propertyDescriptor.setter
                    //
                    // if (getter == null) return true
                    // if (propertyDescriptor.isVar && setter == null) return true
                    // if (setter != null && !setter.hasBody() && setter.modality != Modality.ABSTRACT) return true
                    // if (!getter.hasBody() && getter.modality != Modality.ABSTRACT) return true
                    //
                    // return backingFieldRequired ?: false
                    return false
                }
            }


        val IMPLICIT_EXHAUSTIVE_WHEN: WritableSlice<CjMatchExpression, Boolean> = Slices.createSimpleSlice()


        val IMPLICIT_EXHAUSTIVE_MATCH: WritableSlice<CjMatchExpression, Boolean> = Slices.createSimpleSlice()


        val LEXICAL_SCOPE: WritableSlice<CjElement, LexicalScope> = Slices.createSimpleSlice()


        val PACKAGE_TO_FILES: WritableSlice<FqName, MutableCollection<CjFile>> = Slices.createSimpleSlice()


        val SUPER_EXPRESSION_FROM_ANY_MIGRATION: WritableSlice<CjSuperExpression, Boolean> =
            Slices.createSimpleSlice()


        val DEPRECATED_SHORT_NAME_ACCESS: WritableSlice<PsiElement, Boolean> = Slices.createSimpleSlice()


        val DESCRIPTOR_TO_CONTEXT_RECEIVER_MAP: WritableSlice<DeclarationDescriptor, Multimap<String, ReceiverParameterDescriptor>> =
            Slices.createSimpleSlice()


        val PATTERN: WritableSlice<CjCasePatternElement, Pattern> = Slices.createSimpleSlice()

        /**
         * 用于初始化 slice 调试名称的静态初始化器
         */
        @Suppress("UnusedPrivateMember", "DEPRECATION")
        @Deprecated("This field is needed only for the side effects of its initializer")
        private val _staticInitializer: Unit = BasicWritableSlice.initSliceDebugNames(BindingContext::class.java)
    }
}
