package com.linqingying.cangjie.diagnostics

import com.linqingying.cangjie.name.CallableId
import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.name.IClassId


sealed class MatchMissingCase {
    abstract val branchConditionText: String

    object Unknown : MatchMissingCase() {
        override fun toString(): String = "unknown"

        override val branchConditionText: String = "else"
    }

    sealed class ConditionTypeIsExpect(val typeOfDeclaration: String) : MatchMissingCase() {
        object SealedClass : ConditionTypeIsExpect("sealed class")
        object SealedInterface : ConditionTypeIsExpect("sealed interface")
        object Enum : ConditionTypeIsExpect("enum")

        override val branchConditionText: String = "else"

        override fun toString(): String = "unknown"
    }

    object NullIsMissing : MatchMissingCase() {
        override val branchConditionText: String = "null"
    }

    sealed class BooleanIsMissing(val value: Boolean) : MatchMissingCase() {
        object TrueIsMissing : BooleanIsMissing(true)
        object FalseIsMissing : BooleanIsMissing(false)

        override val branchConditionText: String = value.toString()
    }

    class IsTypeCheckIsMissing(val classId: IClassId, val isSingleton: Boolean) : MatchMissingCase() {
        override val branchConditionText: String = run {
            val fqName = classId.asSingleFqName().toString()
            if (isSingleton) fqName else "is $fqName"
        }

        override fun toString(): String {
            val className = classId.shortClassName
            val name = if (className.isSpecial) className.asString() else className.identifier
            return if (isSingleton) name else "is $name"
        }
    }
    class OtherCheckIsMissing(): MatchMissingCase() {
        override val branchConditionText: String
            get() = "Other"
    }

    class EnumCheckIsMissing(val callableId: CallableId) : MatchMissingCase() {
        override val branchConditionText: String = callableId.asSingleFqName().toString()

        override fun toString(): String {
            return callableId.callableName.identifier
        }
    }
    class TupleCheckIsMissing(val callableId: CallableId) : MatchMissingCase() {
        override val branchConditionText: String = callableId.asSingleFqName().toString()

        override fun toString(): String {
            return callableId.callableName.identifier
        }
    }
    override fun toString(): String {
        return branchConditionText
    }
}
