package cn.cangnova.cangjie.utils

import com.sun.jdi.AbsentInformationException
import com.sun.jdi.InternalException
import com.sun.jdi.LocalVariable
import com.sun.jdi.Method

fun Method.safeArguments(): List<LocalVariable>? {
    return wrapAbsentInformationException { arguments() }
}

private inline fun <T> wrapAbsentInformationException(block: () -> T): T? {
    return try {
        block()
    } catch (e: AbsentInformationException) {
        null
    } /*catch (e: AbsentInformationEvaluateException) {
        null
    } */catch (e: InternalException) {
        null
    } catch (e: UnsupportedOperationException) {
        null
    }
}