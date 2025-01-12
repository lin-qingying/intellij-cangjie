//package com.linqingying.cangjie.ide.debugger
//
//import com.intellij.openapi.application.runReadAction
//import com.intellij.openapi.components.serviceOrNull
//import com.intellij.openapi.diagnostic.Logger
//import com.intellij.openapi.progress.ProcessCanceledException
//import com.intellij.openapi.progress.blockingContext
//import com.intellij.psi.PsiElement
//import com.intellij.psi.util.isAncestor
//import com.linqingying.cangjie.codegen.state.CangJieTypeMapper
//import com.linqingying.cangjie.descriptors.CallableMemberDescriptor
//import com.linqingying.cangjie.descriptors.ClassDescriptor
//import com.linqingying.cangjie.descriptors.DeclarationDescriptorWithVisibility
//import com.linqingying.cangjie.descriptors.DescriptorVisibilities
//import com.linqingying.cangjie.descriptors.Modality
//import com.linqingying.cangjie.diagnostics.DiagnosticUtils
//import com.linqingying.cangjie.psi.CjCallExpression
//import com.linqingying.cangjie.psi.CjCallableDeclaration
//import com.linqingying.cangjie.psi.CjConstructor
//import com.linqingying.cangjie.psi.CjDeclaration
//import com.linqingying.cangjie.psi.CjElement
//import com.linqingying.cangjie.psi.CjFile
//import com.linqingying.cangjie.psi.CjFunction
//import com.linqingying.cangjie.psi.CjFunctionLiteral
//import com.linqingying.cangjie.psi.CjNamedFunction
//import com.linqingying.cangjie.psi.CjProperty
//import com.linqingying.cangjie.psi.CjPropertyAccessor
//import com.linqingying.cangjie.psi.CjTypeStatement
//import com.linqingying.cangjie.psi.CjVariable
//import com.linqingying.cangjie.psi.psiUtil.getParentOfType
//import com.linqingying.cangjie.psi.psiUtil.isFunctionalExpression
//import com.linqingying.cangjie.resolve.BindingContext
//import com.linqingying.cangjie.resolve.DescriptorUtils
//import com.linqingying.cangjie.resolve.caches.analyze
//import com.linqingying.cangjie.resolve.lazy.BodyResolveMode
//import com.linqingying.cangjie.utils.keysToMap
//import com.sun.jdi.AbsentInformationException
//import com.sun.jdi.Accessible
//import com.sun.jdi.ClassNotLoadedException
//import com.sun.jdi.ClassType
//import com.sun.jdi.InternalException
//import com.sun.jdi.Location
//import com.sun.jdi.Method
//import com.sun.jdi.ReferenceType
//import com.sun.jdi.VMDisconnectedException
//import com.sun.jdi.VirtualMachine
//import com.sun.jdi.VoidType
//import kotlin.jvm.internal.FunctionBase
//import com.linqingying.cangjie.ide.debugger.FileRankingCalculator.Ranking.Companion.LOW
//import com.linqingying.cangjie.ide.debugger.FileRankingCalculator.Ranking.Companion.MAJOR
//import com.linqingying.cangjie.ide.debugger.FileRankingCalculator.Ranking.Companion.MINOR
//import com.linqingying.cangjie.ide.debugger.FileRankingCalculator.Ranking.Companion.NORMAL
//import com.linqingying.cangjie.ide.debugger.FileRankingCalculator.Ranking.Companion.ZERO
//import com.linqingying.cangjie.psi.CjAnonymousInitializer
//import com.linqingying.cangjie.psi.CjClassInitializer
//import com.linqingying.cangjie.psi.psiUtil.getLineStartOffset
//import com.linqingying.cangjie.psi.psiUtil.getParentOfTypes2
//import com.linqingying.cangjie.psi.psiUtil.getParentOfTypes3
//import com.linqingying.cangjie.utils.capitalizeAsciiOnly
//import com.linqingying.cangjie.utils.safeArguments
//
//interface CangJieDebuggerLegacyFacade {
//    val editorTextProvider: CangJieEditorTextProvider
//    val fileSelector: CangJieFileSelector
//
//    companion object {
//        @JvmStatic
//        fun getInstance():  CangJieDebuggerLegacyFacade? = serviceOrNull()
//    }
//}
//internal class CangJieDebuggerLegacyFacadeImpl : CangJieDebuggerLegacyFacade {
//    override val editorTextProvider: CangJieEditorTextProvider
//        get() = LegacyCangJieEditorTextProvider
//
//    override val fileSelector: CangJieFileSelector
//        get() = FileRankingCalculatorForIde
//
//}
//object FileRankingCalculatorForIde : FileRankingCalculator() {
//    override fun analyze(element: CjElement) = element.analyze(BodyResolveMode.PARTIAL)
//}
//
//abstract class FileRankingCalculator(private val checkClassFqName: Boolean = true) : CangJieFileSelector {
//    abstract fun analyze(element: CjElement): BindingContext
//
//    override suspend fun chooseMostApplicableFile(files: List<CjFile>, location: Location): CjFile {
//        val fileWithRankings = blockingContext {
//            runReadAction { rankFiles(files, location) }
//        }
//        val fileWithMaxScore = fileWithRankings.maxByOrNull { it.value }!!
//        return fileWithMaxScore.key
//    }
//
//    fun rankFiles(files: Collection<CjFile>, location: Location): Map<CjFile, Int> {
//        assert(files.isNotEmpty())
//        return files.keysToMap { fileRankingSafe(it, location).value }
//    }
//
//    private class Ranking(val value: Int) : Comparable<Ranking> {
//        companion object {
//            val LOW = Ranking(-1000)
//            val ZERO = Ranking(0)
//            val MINOR = Ranking(1)
//            val NORMAL = Ranking(5)
//            val MAJOR = Ranking(10)
//
//            fun minor(condition: Boolean) = if (condition) MINOR else ZERO
//        }
//
//        operator fun unaryMinus() = Ranking(-value)
//        operator fun plus(other: Ranking) = Ranking(value + other.value)
//        override fun compareTo(other: Ranking) = this.value - other.value
//        override fun toString() = value.toString()
//    }
//
//    private fun collect(vararg conditions: Any): Ranking {
//        return conditions
//            .map { condition ->
//                when (condition) {
//                    is Boolean -> Ranking.minor(condition)
//                    is Int -> Ranking(condition)
//                    is Ranking -> condition
//                    else -> error("Invalid condition type ${condition.javaClass.name}")
//                }
//            }.fold(ZERO) { sum, r -> sum + r }
//    }
//
//    private fun rankingForClass(clazz: CjTypeStatement, fqName: String, virtualMachine: VirtualMachine): Ranking {
//        val bindingContext = analyze(clazz)
//        val descriptor = bindingContext[BindingContext.CLASS, clazz] ?: return ZERO
//
//        val jdiType = virtualMachine.classesByName(fqName).firstOrNull() ?: run {
//            // Check at least the class name if not found
//            return rankingForClassName(fqName, descriptor, bindingContext)
//        }
//
//        return rankingForClass(clazz, jdiType)
//    }
//
//    private fun rankingForClass(clazz: CjTypeStatement, type: ReferenceType): Ranking {
//        val bindingContext = analyze(clazz)
//        val descriptor = bindingContext[BindingContext.CLASS, clazz] ?: return ZERO
//
//        return collect(
//            rankingForClassName(type.name(), descriptor, bindingContext),
//            Ranking.minor(type.isAbstract && descriptor.modality == Modality.ABSTRACT),
//            Ranking.minor(type.isFinal && descriptor.modality == Modality.FINAL),
//            Ranking.minor(type.isStatic && !descriptor.isInner),
//            rankingForVisibility(descriptor, type)
//        )
//    }
//
//    private fun rankingForClassName(fqName: String, descriptor: ClassDescriptor, bindingContext: BindingContext): Ranking {
//        if (DescriptorUtils.isLocal(descriptor)) return ZERO
//return MAJOR
////        val expectedFqName = makeTypeMapper(bindingContext).mapType(descriptor).className
////        return when {
////            checkClassFqName -> if (expectedFqName == fqName) MAJOR else LOW
////            else -> if (expectedFqName.simpleName() == fqName.simpleName()) MAJOR else LOW
////        }
//    }
//
//    private fun rankingForMethod(function: CjFunction, method: Method): Ranking {
//        val bindingContext = analyze(function)
//        val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, function] as? CallableMemberDescriptor ?: return ZERO
//
//        if (function !is CjConstructor<*> && method.name() != descriptor.name.asString())
//            return LOW
//
//        return collect(
//            method.isConstructor && function is CjConstructor<*>,
//            method.isAbstract && descriptor.modality == Modality.ABSTRACT,
//            method.isFinal && descriptor.modality == Modality.FINAL,
////            method.isVarArgs && descriptor.varargParameterPosition() >= 0,
//            rankingForVisibility(descriptor, method),
//            descriptor.valueParameters.size == (method.safeArguments()?.size ?: 0)
//        )
//    }
//
//    private fun rankingForAccessor(accessor: CjPropertyAccessor, method: Method): Ranking {
//        val methodName = method.name()
//        val expectedPropertyName = accessor.property.name ?: return ZERO
//
//        if (accessor.isSetter) {
//            if (!methodName.startsWith("set") || method.returnType() !is VoidType || method.argumentTypes().size != 1)
//                return -MAJOR
//        }
//
//        if (accessor.isGetter) {
//            if (!methodName.startsWith("get") && !methodName.startsWith("is"))
//                return -MAJOR
//            else if (method.returnType() is VoidType || method.argumentTypes().isNotEmpty())
//                return -NORMAL
//        }
//
//        val actualPropertyName = getPropertyName(methodName, accessor.isSetter)
//        return if (expectedPropertyName == actualPropertyName) NORMAL else -NORMAL
//    }
//
//    private fun getPropertyName(accessorMethodName: String, isSetter: Boolean): String {
//        if (isSetter) {
//            return accessorMethodName.drop(3)
//        }
//
//        return accessorMethodName.drop(if (accessorMethodName.startsWith("is")) 2 else 3)
//    }
//    private fun rankingForVariable(property: CjVariable, method: Method): Ranking {
//        val methodName = method.name()
//        val propertyName = property.name ?: return ZERO
//
//        if (property.isTopLevel && method.name() == "<clinit>") {
//            // For top-level property initializers
//            return MINOR
//        }
//
//        if (!methodName.startsWith("get") && !methodName.startsWith("set"))
//            return -MAJOR
//
//        // boolean is
//        return if (methodName.drop(3) == propertyName.capitalizeAsciiOnly()) MAJOR else -NORMAL
//    }
//
//    private fun rankingForProperty(property: CjProperty, method: Method): Ranking {
//        val methodName = method.name()
//        val propertyName = property.name ?: return ZERO
////
////        if (property.isTopLevel && method.name() == "<clinit>") {
////            // For top-level property initializers
////            return MINOR
////        }
//
//        if (!methodName.startsWith("get") && !methodName.startsWith("set"))
//            return -MAJOR
//
//        // boolean is
//        return if (methodName.drop(3) == propertyName.capitalizeAsciiOnly()) MAJOR else -NORMAL
//    }
//
//    private fun rankingForVisibility(descriptor: DeclarationDescriptorWithVisibility, accessible: Accessible): Ranking {
//        return collect(
//            accessible.isPublic && descriptor.visibility == DescriptorVisibilities.PUBLIC,
//            accessible.isProtected && descriptor.visibility == DescriptorVisibilities.PROTECTED,
//            accessible.isPrivate && descriptor.visibility == DescriptorVisibilities.PRIVATE
//        )
//    }
//
//    private fun fileRankingSafe(file: CjFile, location: Location): Ranking {
//        return try {
//            fileRanking(file, location)
//        } catch (e: ClassNotLoadedException) {
//            LOG.error("ClassNotLoadedException should never happen in FileRankingCalculator", e)
//            ZERO
//        } catch (e: AbsentInformationException) {
//            ZERO
//        } catch (e: InternalException) {
//            ZERO
//        } catch (e: VMDisconnectedException) {
//            throw e
//        } catch (e: ProcessCanceledException) {
//            throw e
//        } catch (e: RuntimeException) {
//            LOG.error("Exception during CangJie sources ranking", e)
//            ZERO
//        }
//    }
//
//    private fun fileRanking(file: CjFile, location: Location): Ranking {
//        val locationLineNumber = location.lineNumber() - 1
//        val lineStartOffset = file.getLineStartOffset(locationLineNumber) ?: return LOW
//        val elementAt = file.findElementAt(lineStartOffset) ?: return ZERO
//
//        var overallRanking = ZERO
//        val method = location.method()
//
//        if (method.isIndyLambda() || method.isAnonymousClassLambda()) {
//            val (className, methodName) = method.getContainingClassAndMethodNameForLambda() ?: return ZERO
//            if (method.isBridge && method.isSynthetic) {
//                // It might be a static lambda field accessor
//                val containingClass = elementAt.getParentOfType<CjTypeStatement>(false) ?: return LOW
//                return rankingForClass(containingClass, className, location.virtualMachine())
//            } else {
//                val containingFunctionLiteral =
//                    findFunctionLiteralOnLine(elementAt)
//                        ?: findAnonymousFunctionInParent(elementAt)
//                        ?: return LOW
//
//                val containingCallable = findNonLocalCallableParent(containingFunctionLiteral) ?: return LOW
//                when (containingCallable) {
//                    is CjFunction -> if (containingCallable.name == methodName) overallRanking += MAJOR
//                    is CjProperty -> if (containingCallable.name == methodName) overallRanking += MAJOR
//                    is CjPropertyAccessor -> if (containingCallable.property.name == methodName) overallRanking += MAJOR
//                }
//
//                val containingClass = containingCallable.getParentOfType<CjTypeStatement>(false)
//                if (containingClass != null) {
//                    overallRanking += rankingForClass(containingClass, className, location.virtualMachine())
//                }
//
//                return overallRanking
//            }
//        }
//
//        // TODO support <clinit>
//        if (method.name() == "<init>") {
//            val containingClass = elementAt.getParentOfType<CjTypeStatement>(false) ?: return LOW
//            val constructorOrInitializer =
//                elementAt.getParentOfTypes2<CjConstructor<*>, CjClassInitializer>()?.takeIf { containingClass.isAncestor(it) }
//                    ?: containingClass.primaryConstructor?.takeIf { it.getLine() == containingClass.getLine() }
//
//            if (constructorOrInitializer == null
//                && locationLineNumber < containingClass.getLine()
//                && locationLineNumber > containingClass.lastChild.getLine()
//            ) {
//                return LOW
//            }
//
//            overallRanking += rankingForClass(containingClass, location.declaringType())
//
//            if (constructorOrInitializer is CjConstructor<*>)
//                overallRanking += rankingForMethod(constructorOrInitializer, method)
//        } else {
//            val callable = findNonLocalCallableParent(elementAt) ?: return LOW
//            overallRanking += when (callable) {
//                is CjFunction -> rankingForMethod(callable, method)
//                is CjPropertyAccessor -> rankingForAccessor(callable, method)
//                is CjProperty -> rankingForProperty(callable, method)
//                is CjVariable -> rankingForVariable(callable, method)
//                else -> return LOW
//            }
//
//            val containingClass = elementAt.getParentOfType<CjTypeStatement>(false)
//            if (containingClass != null)
//                overallRanking += rankingForClass(containingClass, location.declaringType())
//        }
//
//        return overallRanking
//    }
//
//    private fun findFunctionLiteralOnLine(element: PsiElement): CjFunctionLiteral? {
//        val literal = element.getParentOfType<CjFunctionLiteral>(false)
//        if (literal != null) {
//            return literal
//        }
//
//        val callExpression = element.getParentOfType<CjCallExpression>(false) ?: return null
//
//        for (lambdaArgument in callExpression.lambdaArguments) {
//            if (element.getLine() == lambdaArgument.getLine()) {
//                val functionLiteral = lambdaArgument.getLambdaExpression()?.functionLiteral
//                if (functionLiteral != null) {
//                    return functionLiteral
//                }
//            }
//        }
//
//        return null
//    }
//
//    private fun findAnonymousFunctionInParent(element: PsiElement): CjNamedFunction? {
//        val parentFun = element.getParentOfType<CjNamedFunction>(false)
//        if (parentFun != null && parentFun.isFunctionalExpression()) {
//            return parentFun
//        }
//        return null
//    }
//
//    private tailrec fun findNonLocalCallableParent(element: PsiElement): PsiElement? {
//        fun PsiElement.isCallableDeclaration() =this is CjVariable || this is CjProperty || this is CjFunction || this is CjAnonymousInitializer
//
//        // org.jetbrains.kotlin.psi.CjPsiUtil.isLocal
//        fun PsiElement.isLocalDeclaration(): Boolean {
//            val containingDeclaration = getParentOfType<CjDeclaration>(true)
//            return containingDeclaration is CjCallableDeclaration || containingDeclaration is CjPropertyAccessor
//        }
//
//        if (element.isCallableDeclaration() && !element.isLocalDeclaration()) {
//            return element
//        }
//
//        val containingCallable = element.getParentOfTypes3<CjProperty, CjFunction, CjAnonymousInitializer>()
//            ?: return null
//
//        if (containingCallable.isLocalDeclaration()) {
//            return findNonLocalCallableParent(containingCallable)
//        }
//
//        return containingCallable
//    }
//
//    private fun Method.getContainingClassAndMethodNameForLambda(): Pair<String, String>? {
//        if (isIndyLambda()) {
//            return getContainingClassAndMethodNameForIndyLambda()
//        }
//        // TODO this breaks nested classes
//        val declaringClass = declaringType() as ClassType
//        val (className, methodName) = declaringClass.name().split('$', limit = 3)
//            .takeIf { it.size == 3 }
//            ?: return null
//
//        return Pair(className, methodName)
//    }
//
//    private fun Method.isAnonymousClassLambda(): Boolean {
//        val declaringClass = declaringType() as? ClassType ?: return false
//
//        tailrec fun ClassType.isLambdaClass(): Boolean {
//            if (interfaces().any { it.name() == FunctionBase::class.java.name }) {
//                return true
//            }
//
//            val superClass = superclass() ?: return false
//            return superClass.isLambdaClass()
//        }
//
//        return declaringClass.superclass()?.isLambdaClass() ?: false
//    }
//
//    private fun Method.isIndyLambda(): Boolean {
//        return name().matches(INDY_LAMBDA_NAME_REGEX)
//    }
//
//    private fun Method.getContainingClassAndMethodNameForIndyLambda(): Pair<String, String>? {
//        val match = INDY_LAMBDA_NAME_REGEX.matchEntire(this.name()) ?: return null
//        val values = match.groupValues
//        if (values.size != 2) {
//            return null
//        }
//        val methodName = values[1]
//        val className = this.declaringType().name()
//        return className to methodName
//    }
//
//    private fun makeTypeMapper(bindingContext: BindingContext): CangJieTypeMapper {
//        return CangJieTypeMapper(
//            bindingContext,
////            ClassBuilderMode.LIGHT_CLASSES,
//            "debugger",
//            CangJieTypeMapper.LANGUAGE_VERSION_SETTINGS_DEFAULT, // TODO use proper LanguageVersionSettings
//            useOldInlineClassesManglingScheme = false
//        )
//    }
//
//    companion object {
//        private val INDY_LAMBDA_NAME_REGEX = "([^\$]+)\\\$lambda\\\$\\d+.*".toRegex()
//
//        val LOG = Logger.getInstance("FileRankingCalculator")
//    }
//}
//private fun String.simpleName() = substringAfterLast('.').substringAfterLast('$')
//
//private fun PsiElement.getLine(): Int {
//    return DiagnosticUtils.getLineAndColumnInPsiFile(containingFile, textRange).line
//}
