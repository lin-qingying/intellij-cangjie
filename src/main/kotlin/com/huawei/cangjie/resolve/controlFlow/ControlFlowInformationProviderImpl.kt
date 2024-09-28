package com.huawei.cangjie.resolve.controlFlow

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.cfg.pseudocodeTraverser.TraversalOrder
import com.huawei.cangjie.cfg.pseudocodeTraverser.traverse
import com.huawei.cangjie.cfg.pseudocodeTraverser.traverseIncludingDeadCode
import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.FunctionDescriptor
import com.huawei.cangjie.diagnostics.Errors.*
import com.huawei.cangjie.diagnostics.MatchMissingCase
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.BindingContext.*
import com.huawei.cangjie.resolve.controlFlow.pseudocode.Pseudocode
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.CjElementInstruction
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.Instruction
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.InstructionVisitor
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.eval.*
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.jumps.*
import com.huawei.cangjie.resolve.controlFlow.pseudocode.instructions.special.MarkInstruction
import com.huawei.cangjie.resolve.controlFlow.variable.PseudocodeVariablesData
import com.huawei.cangjie.resolve.descriptorUtil.module
import com.huawei.cangjie.resolve.isUsedAsExpression
import com.huawei.cangjie.resolve.isUsedAsResultOfLambda
import com.huawei.cangjie.resolve.recordUsedAsExpression
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.expressions.MatchChecker
import com.huawei.cangjie.types.isFlexible
import com.huawei.cangjie.types.util.TypeUtils.DONT_CARE
import com.huawei.cangjie.types.util.TypeUtils.NO_EXPECTED_TYPE
import com.huawei.cangjie.types.util.TypeUtils.noExpectedType
import com.huawei.cangjie.types.util.isBooleanOrNullableBoolean
import com.intellij.psi.PsiElement

interface ControlFlowInformationProvider {
    fun checkForLocalClassOrObjectMode()

    fun checkDeclaration()

    fun checkFunction(expectedReturnType: CangJieType?)

    interface Factory {
        fun createControlFlowInformationProvider(
            declaration: CjElement,
            trace: BindingTrace,
            languageVersionSettings: LanguageVersionSettings,
//            diagnosticSuppressor: PlatformDiagnosticSuppressor,
//            enumMatchTracker: EnumMatchTracker
        ): ControlFlowInformationProvider
    }
}

class ControlFlowInformationProviderImpl private constructor(
    private val subroutine: CjElement,
    private val trace: BindingTrace,
    private val pseudocode: Pseudocode,
    private val languageVersionSettings: LanguageVersionSettings,
//    private val diagnosticSuppressor: PlatformDiagnosticSuppressor,
//    private val enumMatchTracker: EnumMatchTracker?
) : ControlFlowInformationProvider {
    private val pseudocodeVariablesData by lazy {
        PseudocodeVariablesData(pseudocode, trace.bindingContext)
    }


    object Factory : ControlFlowInformationProvider.Factory {
        override fun createControlFlowInformationProvider(
            declaration: CjElement,
            trace: BindingTrace,
            languageVersionSettings: LanguageVersionSettings,
//        diagnosticSuppressor: PlatformDiagnosticSuppressor,
//        enumMatchTracker: EnumMatchTracker
        ): ControlFlowInformationProvider =
            ControlFlowInformationProviderImpl(
                declaration,
                trace,
                languageVersionSettings,/* diagnosticSuppressor, enumMatchTracker*/
            )
    }

    constructor(
        declaration: CjElement,
        trace: BindingTrace,
        languageVersionSettings: LanguageVersionSettings,
//        diagnosticSuppressor: PlatformDiagnosticSuppressor,
//        enumMatchTracker: EnumMatchTracker? = null
    ) : this(
        declaration,
        trace,
        ControlFlowProcessor(trace, languageVersionSettings).generatePseudocode(declaration),
        languageVersionSettings,
//        diagnosticSuppressor,
//        enumMatchTracker
    )

    private data class ReturnedExpressionsInfo(
        val returnedExpressions: Collection<CjElement>,
        val hasReturnsInInlinedLambda: Boolean
    )

    private fun collectReturnExpressions(): ReturnedExpressionsInfo {
        val instructions = pseudocode.instructions.toHashSet()
        val exitInstruction = pseudocode.exitInstruction
//
        val returnedExpressions = arrayListOf<CjElement>()
        var hasReturnsInInlinedLambda = false
//
        for (previousInstruction in exitInstruction.previousInstructions) {
            previousInstruction.accept(object : InstructionVisitor() {
                override fun visitReturnValue(instruction: ReturnValueInstruction) {
                    if (instructions.contains(instruction)) { //exclude non-local return expressions
                        returnedExpressions.add(instruction.element)
                    }

                    if (instruction.owner.isInlined) {
                        hasReturnsInInlinedLambda = true
                    }
                }

                override fun visitReturnNoValue(instruction: ReturnNoValueInstruction) {
                    if (instructions.contains(instruction)) {
                        returnedExpressions.add(instruction.element)
                    }

                    if (instruction.owner.isInlined) {
                        hasReturnsInInlinedLambda = true
                    }
                }

                override fun visitUnconditionalJump(instruction: UnconditionalJumpInstruction) {
                    redirectToPrevInstructions(instruction)
                }

                override fun visitConditionalJump(instruction: ConditionalJumpInstruction) {
                    redirectToPrevInstructions(instruction)
                }

                // Note that there's no need to overload `visitThrowException`, because
                // it can never be a predecessor of EXIT (throwing always leads to ERROR)

                private fun redirectToPrevInstructions(instruction: Instruction) {
                    for (redirectInstruction in instruction.previousInstructions) {
                        redirectInstruction.accept(this)
                    }
                }

                override fun visitNondeterministicJump(instruction: NondeterministicJumpInstruction) {
                    redirectToPrevInstructions(instruction)
                }

                override fun visitMarkInstruction(instruction: MarkInstruction) {
                    redirectToPrevInstructions(instruction)
                }

                override fun visitInstruction(instruction: Instruction) {
                    if (instruction is CjElementInstruction) {
                        // Caveats:
                        // - for empty block-bodies, read(Unit) is emitted and will be processed here
                        // - for Unit-coerced blocks, last expression will be processed here
                        returnedExpressions.add(instruction.element)
                    } else {
                        throw IllegalStateException("$instruction precedes the exit point")
                    }
                }
            })
        }

        return ReturnedExpressionsInfo(returnedExpressions, hasReturnsInInlinedLambda)

    }

    private fun checkDefiniteReturn(expectedReturnType: CangJieType, unreachableCode: UnreachableCode) {
        val function = subroutine as? CjDeclarationWithBody
            ?: throw AssertionError("checkDefiniteReturn is called for ${subroutine.text} which is not CjDeclarationWithBody")

        if (!function.hasBody()) return

        val (returnedExpressions, hasReturnsInInlinedLambdas) = collectReturnExpressions()

        val blockBody = function.hasBlockBody()

        var noReturnError = false
        for (returnedExpression in returnedExpressions) {
            returnedExpression.accept(object : CjVisitorVoid() {
                override fun visitReturnExpression(expression: CjReturnExpression) {
                    noReturnError = false

                    if (!blockBody) {
                        trace.report(RETURN_IN_FUNCTION_WITH_EXPRESSION_BODY.on(expression))
                    }
                }

                override fun visitExpression(expression: CjExpression) {


                }

                override fun visitBlockExpression(expression: CjBlockExpression) {
                    visitCjElement(expression)
                }

                override fun visitCjElement(element: CjElement) {
                    if (!(element is CjExpression || element is CjCasePattern)) return

                    if (blockBody && !noExpectedType(expectedReturnType)
                        && !CangJieBuiltIns.isUnit(expectedReturnType)
                        && !unreachableCode.elements.contains(element)
                    ) {
                        noReturnError = true
                    }

                }
            })
        }

        if (noReturnError) {
            if (hasReturnsInInlinedLambdas) {
                trace.report(NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY_MIGRATION.on(function))
            } else {
                trace.report(NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY.on(function))
            }
        }
    }

    override fun checkForLocalClassOrObjectMode() {
//        recordInitializedVariables()
    }

    //    private fun recordInitializedVariables() {
//        val pseudocode = pseudocodeVariablesData.pseudocode
//        val initializers = pseudocodeVariablesData.variableInitializers
//        recordInitializedVariables(pseudocode, initializers)
//        for (instruction in pseudocode.localDeclarations) {
//            recordInitializedVariables(instruction.body, initializers)
//        }
//    }

    fun getLocalFunctions(): Set<Pair<CjFunction, FunctionDescriptor?>> {
        return pseudocode.localDeclarations.mapNotNull {
            if (it.element is CjFunction) {
                Pair(
                    it.element as CjFunction,
                    trace.bindingContext.get(DECLARATION_TO_DESCRIPTOR, it.element) as? FunctionDescriptor
                )
            } else {
                null
            }
        }.toSet()


    }

    private fun checkLocalFunctions() {
        for (localDeclarationInstruction in pseudocode.localDeclarations) {
            val element = localDeclarationInstruction.element
            if (element is CjDeclarationWithBody) {

                val functionDescriptor =
                    trace.bindingContext.get(DECLARATION_TO_DESCRIPTOR, element) as? CallableDescriptor

                val expectedType = functionDescriptor?.returnType

                val providerForLocalDeclaration = ControlFlowInformationProviderImpl(
                    element,
                    trace,
                    localDeclarationInstruction.body,
                    languageVersionSettings/*, diagnosticSuppressor, enumMatchTracker*/
                )

                providerForLocalDeclaration.checkFunction(expectedType)
            }
        }
    }

    private fun checkMainFunction() {

    }

    override fun checkDeclaration() {
//        recordInitializedVariables()

        checkLocalFunctions()
        checkMainFunction()
        markUninitializedVariables()

//        if (trace.wantsDiagnostics()) {
//            markUnusedVariables()
//        }
//
//        checkForSuspendLambdaAndMarkParameters(pseudocode)
//
        markStatements()
//        markAnnotationArguments()
//
//        markUnusedExpressions()
//
//        if (trace.wantsDiagnostics()) {
//            checkIfExpressions()
//        }
//
        checkMatchExpressions()
//
//        checkConstructorConsistency()

    }

    private fun CjExpression.recordUsedAsExpression() {
        recordUsedAsExpression(trace, true)
    }

    private fun markStatements() {
        pseudocode.traverseIncludingDeadCode { instruction ->
            val value = (instruction as? InstructionWithValue)?.outputValue
            val pseudocode = instruction.owner
            val usages = pseudocode.getUsages(value)
            val isUsedAsExpression = usages.isNotEmpty()
            val isUsedAsResultOfLambda = isUsedAsResultOfLambda(usages)
            for (element in pseudocode.getValueElements(value)) {
                element.recordUsedAsExpression(trace, isUsedAsExpression)
                trace.record(USED_AS_RESULT_OF_LAMBDA, element, isUsedAsResultOfLambda)
                if (isUsedAsExpression) {
                    when (element) {
                        is CjTryExpression -> {
                            element.tryBlock.recordUsedAsExpression()
                            for (catchClause in element.catchClauses) {
                                catchClause.catchBody?.recordUsedAsExpression()
                            }
                        }

                        is CjIfExpression -> {
                            (element.then as? CjBlockExpression)?.recordUsedAsExpression()
                            (element.`else` as? CjBlockExpression)?.recordUsedAsExpression()
                        }

                        is CjMatchExpression -> {
                            for (entry in element.entries) {
                                (entry.expression as? CjBlockExpression)?.recordUsedAsExpression()
                            }
                        }
                    }
                }
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    //  Uninitialized variables analysis
    private fun checkMatchExpressions() {
        val initializers = pseudocodeVariablesData.variableInitializers
        pseudocode.traverse(TraversalOrder.FORWARD) { instruction ->
            if (instruction is MagicInstruction) {
                if (instruction.kind === MagicKind.EXHAUSTIVE_MATCH_ELSE) {
                    val next = instruction.next
                    if (next is MergeInstruction) {
                        val mergeInfo = initializers[next]?.incoming
                        val magicInfo = initializers[instruction]?.outgoing
                        if (mergeInfo != null && magicInfo != null) {
                            if (next.element is CjMatchExpression && magicInfo.checkDefiniteInitializationInMatch(
                                    mergeInfo
                                )
                            ) {
                                trace.record(IMPLICIT_EXHAUSTIVE_MATCH, next.element)
                            }
                        }
                    }
                }
            }
            val value = (instruction as? InstructionWithValue)?.outputValue
            for (element in instruction.owner.getValueElements(value)) {
                if (element !is CjMatchExpression) continue

//                是否将match语句作为表达式赋值
                val usedAsExpression = element.isUsedAsExpression(trace.bindingContext)
                if (usedAsExpression) {
                    checkImplicitCastOnConditionalExpression(element)
                }

                val context = trace.bindingContext
                val missingCases = MatchChecker.getMissingCases(element, context)

                val elseEntry = element.entries.find { it.isElse }
                val subjectExpression = element.subjectExpression
                if (/*usedAsExpression &&*/ missingCases.isNotEmpty()) {
                    if (elseEntry != null) continue
                    trace.report(NO_ELSE_IN_MATCH.on(element, missingCases))
                    missingCases.firstOrNull { it is MatchMissingCase.ConditionTypeIsExpect }?.let {
                        require(it is MatchMissingCase.ConditionTypeIsExpect)
                        trace.report(EXPECT_TYPE_IN_MATCH_WITHOUT_ELSE.on(element, it.typeOfDeclaration))
                    }
                } else if (subjectExpression != null) {
                    val subjectType = MatchChecker.matchSubjectType(element, trace.bindingContext)
                    if (elseEntry != null) {
                        if (missingCases.isEmpty() && subjectType != null && !subjectType.isFlexible()) {
                            val subjectClass = subjectType.constructor.declarationDescriptor as? ClassDescriptor
                            val pseudocodeElement = instruction.owner.correspondingElement
                            val pseudocodeDescriptor = trace[DECLARATION_TO_DESCRIPTOR, pseudocodeElement]
                            if (subjectClass == null ||
                                CangJieBuiltIns.isBooleanOrNullableBoolean(subjectType) ||
                                subjectClass.module == pseudocodeDescriptor?.module
                            ) {
                                trace.report(REDUNDANT_ELSE_IN_MATCH.on(elseEntry))
                            }
                        }
                        continue
                    }

//                    enumMatchTracker?.record(subjectType, subjectExpression, elseEntry)

                    if (!usedAsExpression) {
//                        if (languageVersionSettings.supportsFeature(LanguageFeature.WarnAboutNonExhaustiveMatchOnAlgebraicTypes)) {
                        // report warnings on all non-exhaustive when's with algebraic subject
                        checkExhaustiveMatchStatement(subjectType, element, missingCases)
//                        } else {
//                             report info if subject is sealed class and warning if it is enum
//                            checkMatchStatement(subjectType, element, context)
//                        }
                    }
                }
//                if (
//                    !usedAsExpression &&
//                    missingCases.isNotEmpty() &&
//                    elseEntry == null &&
//                    !languageVersionSettings.supportsFeature(LanguageFeature.ProhibitNonExhaustiveIfInRhsOfElvis)
//                ) {
//                    val parent = element.deparenthesizedParent
//                    if (parent is CjBinaryExpression) {
//                        if (parent.operationToken === CjTokens.ELVIS) {
//                            trace.report(NO_ELSE_IN_MATCH_WARNING.on(element, missingCases))
//                        }
//                    }
//                }
            }
        }
    }

    private fun checkExhaustiveMatchStatement(
        subjectType: CangJieType?,
        element: CjMatchExpression,
        missingCases: List<MatchMissingCase>
    ) {
        if (missingCases.isEmpty()) return
        val kind = when {
            MatchChecker.getClassDescriptorOfTypeIfSealed(subjectType) != null -> AlgebraicTypeKind.Sealed
            MatchChecker.getClassDescriptorOfTypeIfEnum(subjectType) != null -> AlgebraicTypeKind.Enum
            subjectType?.isBooleanOrNullableBoolean() == true -> AlgebraicTypeKind.Boolean
            else -> null
        }

        if (kind != null) {

            trace.report(NO_ELSE_IN_MATCH.on(element, missingCases))

        }
    }

    private enum class AlgebraicTypeKind(val displayName: String) {
        Sealed("sealed class/interface"),
        Enum("enum"),
        Boolean("Boolean")
    }

    private fun checkMatchStatement(
        subjectType: CangJieType?,
        element: CjMatchExpression,
        context: BindingContext
    ) {
//        val enumClassDescriptor = MatchChecker.getClassDescriptorOfTypeIfEnum(subjectType)
//        if (enumClassDescriptor != null) {
//            val enumMissingCases = MatchChecker.getEnumMissingCases(element, context, enumClassDescriptor)
//            if (enumMissingCases.isNotEmpty()) {
//                trace.report(NON_EXHAUSTIVE_WHEN.on(element, enumMissingCases))
//            }
//        }
//        val sealedClassDescriptor = MatchChecker.getClassDescriptorOfTypeIfSealed(subjectType)
//        if (sealedClassDescriptor != null) {
//            val sealedMissingCases = MatchChecker.getSealedMissingCases(element, context, sealedClassDescriptor)
//            if (sealedMissingCases.isNotEmpty()) {
//                trace.report(NON_EXHAUSTIVE_WHEN_ON_SEALED_CLASS.on(element, sealedMissingCases))
//            }
//        }
    }

    private val PsiElement.deparenthesizedParent: PsiElement
        get() {
            var result = parent
            while (result is CjParenthesizedExpression /*|| result is CjLabeledExpression || result is CjAnnotatedExpression*/) {
                result = result.parent
            }
            return result
        }

    private fun checkImplicitCastOnConditionalExpression(expression: CjExpression) {
        val branchExpressions = collectResultingExpressionsOfConditionalExpression(expression)

        val expectedExpressionType = trace.get(EXPECTED_EXPRESSION_TYPE, expression)
        if (expectedExpressionType != null && expectedExpressionType !== DONT_CARE) return

        val expressionType = trace.getType(expression) ?: return
        if (CangJieBuiltIns.isAnyOrNullableAny(expressionType)) {
            val isUsedAsResultOfLambda = expression.isUsedAsResultOfLambda(trace.bindingContext)
            for (branchExpression in branchExpressions) {
                val branchType = trace.getType(branchExpression) ?: return
                if (CangJieBuiltIns.isAnyOrNullableAny(branchType) ||
                    isUsedAsResultOfLambda && CangJieBuiltIns.isUnitOrNullableUnit(branchType)
                ) {
                    return
                }
            }
            for (branchExpression in branchExpressions) {
                val branchType = trace.getType(branchExpression) ?: continue
                if (CangJieBuiltIns.isNothing(branchType)) continue
                trace.report(
                    IMPLICIT_CAST_TO_ANY.on(
                        getResultingExpression(branchExpression),
                        branchType,
                        expressionType
                    )
                )
            }
        }
    }

    private fun markUninitializedVariables() {
//        val varWithUninitializedErrorGenerated = hashSetOf<VariableDescriptor>()
//        val varWithValReassignErrorGenerated = hashSetOf<VariableDescriptor>()
//        val processClassOrObject = subroutine is CjClassOrObject || subroutine is CjSecondaryConstructor
//
//        val initializers = pseudocodeVariablesData.variableInitializers
//        val declaredVariables = pseudocodeVariablesData.getDeclaredVariables(pseudocode, true)
//        val blockScopeVariableInfo = pseudocodeVariablesData.blockScopeVariableInfo
//
//        val reportedDiagnosticMap = hashMapOf<Instruction, DiagnosticFactory<*>>()
//
//        pseudocode.traverse(TraversalOrder.FORWARD, initializers) { instruction: Instruction,
//                                                                    enterData: VariableInitReadOnlyControlFlowInfo,
//                                                                    exitData: VariableInitReadOnlyControlFlowInfo ->
//
//            val ctxt =
//                VariableInitContext(instruction, reportedDiagnosticMap, enterData, exitData, blockScopeVariableInfo)
//            if (ctxt.variableDescriptor == null) return@traverse
//            if (instruction is ReadValueInstruction) {
//                val element = instruction.element
//                if (PseudocodeUtil.isThisOrNoDispatchReceiver(instruction, trace.bindingContext)
//                    && declaredVariables.contains(ctxt.variableDescriptor)
//                ) {
//                    checkIsInitialized(ctxt, element, varWithUninitializedErrorGenerated)
//                }
//                return@traverse
//            }
//            if (instruction !is WriteValueInstruction) return@traverse
//            val element = instruction.lValue as? CjExpression ?: return@traverse
//            var error = checkValReassignment(
//                ctxt, element, instruction,
//                varWithValReassignErrorGenerated
//            )
//            if (!error && processClassOrObject) {
//                error = checkAssignmentBeforeDeclaration(ctxt, element)
//            }
//            if (!error && processClassOrObject) {
//                checkInitializationForCustomSetter(ctxt, element)
//            }
//        }
    }

    private fun reportUnreachableCode(unreachableCode: UnreachableCode) {
        for (element in unreachableCode.elements) {
            trace.report(
                UNREACHABLE_CODE.on(
                    element,
                    unreachableCode.reachableElements,
                    unreachableCode.unreachableElements
                )
            )
        }
    }

    private fun collectUnreachableCode(): UnreachableCode {
        val reachableElements = hashSetOf<CjElement>()
        val unreachableElements = hashSetOf<CjElement>()
        for (instruction in pseudocode.instructionsIncludingDeadCode) {
            if (instruction !is CjElementInstruction
                || instruction is LoadUnitValueInstruction
                || instruction is MergeInstruction
                || instruction is MagicInstruction && instruction.synthetic
            )
                continue

            val element = instruction.element

            if (instruction is JumpInstruction) {
                val isJumpElement = element is CjBreakExpression
                        || element is CjContinueExpression
                        || element is CjReturnExpression
                        || element is CjThrowExpression
                if (!isJumpElement) continue
            }

            if (instruction.dead) {
                unreachableElements.add(element)
            } else {
                reachableElements.add(element)
            }
        }
        return UnreachableCodeImpl(reachableElements, unreachableElements)
    }

    override fun checkFunction(expectedReturnType: CangJieType?) {
        val unreachableCode = collectUnreachableCode()
        reportUnreachableCode(unreachableCode)


        if (subroutine is CjFunctionLiteral) return

        checkDefiniteReturn(expectedReturnType ?: NO_EXPECTED_TYPE, unreachableCode)

        markAndCheckTailCalls()
    }

    ////////////////////////////////////////////////////////////////////////////////
    // Tail calls

    private fun markAndCheckTailCalls() {
        val subroutineDescriptor = trace.get(DECLARATION_TO_DESCRIPTOR, subroutine) as? FunctionDescriptor ?: return

//        markAndCheckRecursiveTailCalls(subroutineDescriptor)
    }

    companion object {

        private fun isUsedAsResultOfLambda(usages: List<Instruction>): Boolean {
            for (usage in usages) {
                if (usage is ReturnValueInstruction) {
                    val returnElement = usage.element
                    val parentElement = returnElement.parent
                    if (returnElement !is CjReturnExpression &&
                        (parentElement !is CjDeclaration || parentElement is CjFunctionLiteral)
                    ) {
                        return true
                    }
                }
            }
            return false
        }

        private fun collectResultingExpressionsOfConditionalExpression(expression: CjExpression): List<CjExpression> {
            val leafBranches = ArrayList<CjExpression>()
            collectResultingExpressionsOfConditionalExpressionRec(expression, leafBranches)
            return leafBranches
        }

        private fun getResultingExpression(expression: CjExpression): CjExpression {
            var finger = expression
            while (true) {
                var deparenthesized = CjPsiUtil.deparenthesize(finger)
                deparenthesized = CjPsiUtil.getExpressionOrLastStatementInBlock(deparenthesized)
                if (deparenthesized == null || deparenthesized === finger) break
                finger = deparenthesized
            }
            return finger
        }

        private fun collectResultingExpressionsOfConditionalExpressionRec(
            expression: CjExpression?,
            resultingExpressions: MutableList<CjExpression>
        ) {
            when (expression) {
                is CjIfExpression -> {
                    collectResultingExpressionsOfConditionalExpressionRec(expression.then, resultingExpressions)
                    collectResultingExpressionsOfConditionalExpressionRec(expression.`else`, resultingExpressions)
                }

                is CjMatchExpression -> for (whenEntry in expression.entries) {
                    collectResultingExpressionsOfConditionalExpressionRec(whenEntry.expression, resultingExpressions)
                }

                is Any -> {
                    val resultingExpression = getResultingExpression(expression)
                    if (resultingExpression is CjIfExpression || resultingExpression is CjMatchExpression) {
                        collectResultingExpressionsOfConditionalExpressionRec(resultingExpression, resultingExpressions)
                    } else {
                        resultingExpressions.add(resultingExpression)
                    }
                }
            }
        }

    }
}
