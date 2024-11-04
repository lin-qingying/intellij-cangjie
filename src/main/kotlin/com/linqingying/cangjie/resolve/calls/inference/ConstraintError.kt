package com.linqingying.cangjie.resolve.calls.inference

import com.linqingying.cangjie.resolve.calls.inference.constraintPosition.ConstraintPosition
import com.linqingying.cangjie.resolve.calls.inference.model.TypeVariable

open class ConstraintError(val constraintPosition: ConstraintPosition)
class ParameterConstraintError(constraintPosition: ConstraintPosition) : ConstraintError(constraintPosition)

class ErrorInConstrainingType(constraintPosition: ConstraintPosition) : ConstraintError(constraintPosition)

class TypeInferenceError(constraintPosition: ConstraintPosition) : ConstraintError(constraintPosition)

class CannotCapture(constraintPosition: ConstraintPosition, val typeVariable: TypeVariable) : ConstraintError(constraintPosition)

fun newTypeInferenceOrParameterConstraintError(constraintPosition: ConstraintPosition) =
    if (constraintPosition.isParameter()) ParameterConstraintError(constraintPosition) else TypeInferenceError(constraintPosition)
