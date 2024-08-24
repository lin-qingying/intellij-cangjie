package com.huawei.cangjie.ide

import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.TypeSubstitutor

//
//interface ByTypeFilter {
//    fun matchingSubstitutor(descriptorType: FuzzyType): TypeSubstitutor?
//
//    val fuzzyType: FuzzyType?
//        get() = null
//
//    val multipleFuzzyTypes: Collection<FuzzyType>
//        get() = listOfNotNull(fuzzyType)
//
//    object All : ByTypeFilter {
//        override fun matchingSubstitutor(descriptorType: FuzzyType) = TypeSubstitutor.EMPTY
//    }
//
//    object None : ByTypeFilter {
//        override fun matchingSubstitutor(descriptorType: FuzzyType) = null
//    }
//}
//enum class Tail {
//    COMMA,
//    RPARENTH,
//    RBRACKET,
//    ELSE,
//    RBRACE
//}
//data /* for copy() */
//class ExpectedInfo(
//    val filter: ByTypeFilter,
//    val expectedName: String?,
//    val tail: Tail?,
//    val itemOptions: ItemOptions = ItemOptions.DEFAULT,
//    val additionalData: AdditionalData? = null
//) {
//    // just a marker interface
//    interface AdditionalData {}
//
//    constructor(
//        fuzzyType: FuzzyType,
//        expectedName: String?,
//        tail: Tail?,
//        itemOptions: ItemOptions = ItemOptions.DEFAULT,
//        additionalData: AdditionalData? = null
//    ) : this(ByExpectedTypeFilter(fuzzyType), expectedName, tail, itemOptions, additionalData)
//
//    constructor(
//        type: CangJieType,
//        expectedName: String?,
//        tail: Tail?,
//        itemOptions: ItemOptions = ItemOptions.DEFAULT,
//        additionalData: AdditionalData? = null
//    ) : this(type.toFuzzyType(emptyList()), expectedName, tail, itemOptions, additionalData)
//
//    fun matchingSubstitutor(descriptorType: FuzzyType): TypeSubstitutor? = filter.matchingSubstitutor(descriptorType)
//
//    fun matchingSubstitutor(descriptorType: CangJieType): TypeSubstitutor? = matchingSubstitutor(descriptorType.toFuzzyType(emptyList()))
//
//    companion object {
//        fun createForArgument(
//            type: CangJieType,
//            expectedName: String?,
//            tail: Tail?,
//            argumentData: ArgumentPositionData,
//            itemOptions: ItemOptions = ItemOptions.DEFAULT
//        ): ExpectedInfo {
//            return ExpectedInfo(type.toFuzzyType(argumentData.function.typeParameters), expectedName, tail, itemOptions, argumentData)
//        }
//
//        fun createForNamedArgumentExpected(argumentData: ArgumentPositionData): ExpectedInfo {
//            return ExpectedInfo(ByTypeFilter.None, null, null/*TODO?*/, ItemOptions.DEFAULT, argumentData)
//        }
//
//        fun createForReturnValue(type: CangJieType?, callable: CallableDescriptor): ExpectedInfo {
//            val filter = if (type != null) ByExpectedTypeFilter(type.toFuzzyType(emptyList())) else ByTypeFilter.All
//            return ExpectedInfo(filter, callable.name.asString(), null, additionalData = ReturnValueAdditionalData(callable))
//        }
//    }
//}
