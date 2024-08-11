package com.huawei.cangjie.resolve.calls.inference

import com.huawei.cangjie.resolve.calls.inference.constraintPosition.ConstraintPosition

open class ConstraintError(val constraintPosition: ConstraintPosition)
class ParameterConstraintError(constraintPosition: ConstraintPosition) : ConstraintError(constraintPosition)

class ErrorInConstrainingType(constraintPosition: ConstraintPosition) : ConstraintError(constraintPosition)

class TypeInferenceError(constraintPosition: ConstraintPosition) : ConstraintError(constraintPosition)

class CannotCapture(constraintPosition: ConstraintPosition, val typeVariable: TypeVariable) : ConstraintError(constraintPosition)

fun newTypeInferenceOrParameterConstraintError(constraintPosition: ConstraintPosition) =
    if (constraintPosition.isParameter()) ParameterConstraintError(constraintPosition) else TypeInferenceError(constraintPosition)
