package com.linqingying.cangjie.resolve.calls.components

import com.linqingying.cangjie.config.LanguageFeature
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.CallableDescriptor
import com.linqingying.cangjie.descriptors.CallableMemberDescriptor
import com.linqingying.cangjie.descriptors.ValueParameterDescriptor
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.calls.model.*
import com.linqingying.cangjie.utils.compactIfPossible

class ArgumentsToParametersMapper(
    languageVersionSettings: LanguageVersionSettings

) {
    data class ArgumentMapping(
        // This map should be ordered by arguments as written, e.g.:
        //      fun foo(a: Int, b: Int) {}
        //      foo(b = bar(), a = qux())
        // parameterToCallArgumentMap.values() should be [ 'bar()', 'foo()' ]
        val parameterToCallArgumentMap: Map<ValueParameterDescriptor, ResolvedCallArgument>,
        val diagnostics: List<CangJieCallDiagnostic>
    )

    private class CallArgumentProcessor(
        val descriptor: CallableDescriptor,
        val languageSettingsAllowMixedNamedAndPositionArguments: Boolean
    ) {
        val result: MutableMap<ValueParameterDescriptor, ResolvedCallArgument> = LinkedHashMap()
        private var state = State.POSITION_ARGUMENTS

        private enum class State {
            POSITION_ARGUMENTS,
            VARARG_POSITION,
            NAMED_ONLY_ARGUMENTS
        }

        private var currentPositionedParameterIndex = 0
        private val parameters: List<ValueParameterDescriptor> get() = descriptor.valueParameters

        private var diagnostics: MutableList<CangJieCallDiagnostic>? = null
        private var nameToParameter: Map<Name, ValueParameterDescriptor>? = null
        private var varargArguments: MutableList<CangJieCallArgument>? = null
        private fun addDiagnostic(diagnostic: CangJieCallDiagnostic) {
            if (diagnostics == null) {
                diagnostics = ArrayList()
            }
            diagnostics!!.add(diagnostic)
        }

//        private fun addVarargArgument(argument: CangJieCallArgument) {
//            if (varargArguments == null) {
//                varargArguments = ArrayList()
//            }
//            varargArguments!!.add(argument)
//        }

        private fun processPositionArgument(argument: CangJieCallArgument): Boolean {
            if (state == State.NAMED_ONLY_ARGUMENTS) {
                addDiagnostic(MixingNamedAndPositionArguments(argument))
                return false
            }

            val parameter = parameters.getOrNull(currentPositionedParameterIndex)
            if (parameter == null) {
                addDiagnostic(TooManyArguments(argument, descriptor))
                return false
            }

            if (!parameter.isVararg) {
                currentPositionedParameterIndex++

                result[parameter.original] = ResolvedCallArgument.SimpleArgument(argument)
                return false
            }
            // all position arguments will be mapped to current vararg parameter
            else {
                addVarargArgument(argument)
                return true
            }
        }

        private fun addVarargArgument(argument: CangJieCallArgument) {
            if (varargArguments == null) {
                varargArguments = ArrayList()
            }
            varargArguments!!.add(argument)
        }

        private fun completeVarargPositionArguments() {
            assert(state == State.VARARG_POSITION) { "Incorrect state: $state" }
            val parameter = parameters[currentPositionedParameterIndex]
            result[parameter.original] = ResolvedCallArgument.VarargArgument(varargArguments!!)
        }

        fun processArgumentsInParenthesis(arguments: List<CangJieCallArgument>) {
            //            查找出所有命名参数
            fun findNameToParameterMap(): Map<Name, ValueParameterDescriptor> {
//                根据isNamed查找
                val parameters = parameters.filter { it.isNamed }.associateBy { it.name }

//                查找已经传递的
                val passedArguments = result.filter { it.key.isNamed }
//    清除与passedArguments相同的项
                val filteredParameters = parameters.filter  { it.value !in  passedArguments .keys    }

                return filteredParameters


            }

//            若处理过命名参数，但是又处理位置参数，应报错
            var isNamed = false
//            是否报告过前缀错误
            var isReportedPrefix = false
            for (argument in arguments ) {

                val argumentName = argument.argumentName


                // process position argument
                if (argumentName == null) {


//                    if (argument is PSICangJieCallArgument && argument.valueArgument.isNamed()) {
////                        缺少命名参数前缀
////                        addDiagnostic(MissingNamedArgumentPrefix(argument.valueArgument))
//                        TODO()
//                    }
                    if (currentPositionedParameterIndex < parameters.size && parameters[currentPositionedParameterIndex].isNamed && !isReportedPrefix) {
//                        缺少命名参数前缀
                        val names = findNameToParameterMap().keys

                        addDiagnostic(MissingNamedArgumentPrefix(argument, names))
                        isReportedPrefix = true
                    }
                    if (isNamed) {
//                        POSITIONAL_ARGUMENT_AFTER_NAMED_ARGUMENT
                        addDiagnostic(PositionalAfierNamedArgument(argument))
                    }


                    if (processPositionArgument(argument)) {
                        state = State.VARARG_POSITION
                    }
                }
                // process named argument
                else {
                    isNamed = true
                    if (state == State.VARARG_POSITION) {
                        completeVarargPositionArguments()
                    }

                    processNamedArgument(argument, argumentName)
                }
            }
            if (state == State.VARARG_POSITION) {
                completeVarargPositionArguments()
            }
        }

        private fun getParameterByName(name: Name): ValueParameterDescriptor? {
            if (nameToParameter == null) {
                nameToParameter = parameters.associateBy { it.name }
            }
            return nameToParameter!![name]
        }

        private fun ValueParameterDescriptor.getOverriddenParameterWithOtherName() = overriddenDescriptors.firstOrNull {
            it.containingDeclaration.hasStableParameterNames() && it.name != name
        }

        private fun findParameterByName(argument: CangJieCallArgument, name: Name): ValueParameterDescriptor? {
            val parameter = getParameterByName(name)

            if (descriptor is CallableMemberDescriptor && descriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
                if (parameter == null) {
                    for (valueParameter in descriptor.valueParameters) {
                        val matchedParameter = valueParameter.overriddenDescriptors.firstOrNull {
                            it.containingDeclaration.hasStableParameterNames() && it.name == name
                        }
                        if (matchedParameter != null) {
                            addDiagnostic(NamedArgumentReference(argument, valueParameter))
                            addDiagnostic(NameForAmbiguousParameter(argument, valueParameter, matchedParameter))
                            return valueParameter
                        }
                    }
                } else {
                    parameter.getOverriddenParameterWithOtherName()?.let {
                        addDiagnostic(NameForAmbiguousParameter(argument, parameter, it))
                    }
                }
            }

            if (parameter == null || !parameter.isNamed) addDiagnostic(NameNotFound(argument, descriptor))

            return parameter
        }

        private fun processNamedArgument(argument: CangJieCallArgument, name: Name) {
//            if (!descriptor.hasStableParameterNames()) {
//                addDiagnostic(NamedArgumentNotAllowed(argument, descriptor))
//            }

            val stateAllowsMixedNamedAndPositionArguments = state != State.NAMED_ONLY_ARGUMENTS
            state = State.NAMED_ONLY_ARGUMENTS

            val parameter = findParameterByName(argument, name) ?: return

            addDiagnostic(NamedArgumentReference(argument, parameter))

            result[parameter.original]?.let {
                addDiagnostic(ArgumentPassedTwice(argument, parameter, it))
                return
            }

            result[parameter.original] = ResolvedCallArgument.SimpleArgument(argument)

            if (stateAllowsMixedNamedAndPositionArguments && languageSettingsAllowMixedNamedAndPositionArguments &&
                parameters.getOrNull(currentPositionedParameterIndex)?.original == parameter.original
            ) {
                state = State.POSITION_ARGUMENTS
                currentPositionedParameterIndex++
            }
        }

        fun processExternalArgument(externalArgument: CangJieCallArgument) {
            val lastParameter = parameters.lastOrNull()
            if (lastParameter == null) {
                addDiagnostic(TooManyArguments(externalArgument, descriptor))
                return
            }

            if (lastParameter.isVararg) {
                addDiagnostic(VarargArgumentOutsideParentheses(externalArgument, lastParameter))
                return
            }

            val previousOccurrence = result[lastParameter.original]
            if (previousOccurrence != null) {
                addDiagnostic(TooManyArguments(externalArgument, descriptor))
                return
            }


            result[lastParameter.original] = ResolvedCallArgument.SimpleArgument(externalArgument)
        }

        fun processDefaultsAndRunChecks() {
            for ((parameter, resolvedArgument) in result) {
                if (!parameter.isVararg) {
                    if (resolvedArgument !is ResolvedCallArgument.SimpleArgument) {
                        error("Incorrect resolved argument for parameter $parameter :$resolvedArgument")
                    } else {
                        if (resolvedArgument.callArgument.isSpread) {
                            addDiagnostic(NonVarargSpread(resolvedArgument.callArgument))
                        }
                    }
                }
            }

            for (parameter in parameters) {
                if (!result.containsKey(parameter.original)) {
                    if (parameter.hasDefaultValue()) {
                        result[parameter.original] = ResolvedCallArgument.DefaultArgument
                    } else if (parameter.isVararg) {
                        result[parameter.original] = ResolvedCallArgument.VarargArgument(emptyList())
                    } else {
                        addDiagnostic(NoValueForParameter(parameter, descriptor))
                    }
                }
            }
        }

        fun getDiagnostics() = diagnostics ?: emptyList()

    }

    private val EmptyArgumentMapping = ArgumentMapping(emptyMap(), emptyList())

    private val allowMixedNamedAndPositionArguments =
        languageVersionSettings.supportsFeature(LanguageFeature.MixedNamedArgumentsInTheirOwnPosition)

    fun mapArguments(call: CangJieCall, descriptor: CallableDescriptor): ArgumentMapping =
        mapArguments(call.argumentsInParenthesis, call.externalArgument, descriptor)

    private fun mapArguments(
        argumentsInParenthesis: List<CangJieCallArgument>,
        externalArgument: CangJieCallArgument?,
        descriptor: CallableDescriptor
    ): ArgumentMapping {
        // optimization for case of variable
        if (argumentsInParenthesis.isEmpty() && externalArgument == null && descriptor.valueParameters.isEmpty()) {
            return EmptyArgumentMapping
        } else {
            val processor = CallArgumentProcessor(descriptor, allowMixedNamedAndPositionArguments)
            processor.processArgumentsInParenthesis(argumentsInParenthesis)

            if (externalArgument != null) {
                processor.processExternalArgument(externalArgument)
            }
            processor.processDefaultsAndRunChecks()

            return ArgumentMapping(processor.result.compactIfPossible(), processor.getDiagnostics())
        }
    }
}
