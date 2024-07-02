package com.huawei.cangjie.resolve.calls.util

import com.huawei.cangjie.resolve.calls.results.ResolutionStatus
import com.huawei.cangjie.resolve.calls.tower.CandidateApplicability


fun CandidateApplicability.toResolutionStatus(): ResolutionStatus = when (this) {
    CandidateApplicability.RESOLVED,
    CandidateApplicability.RESOLVED_LOW_PRIORITY,
    CandidateApplicability.RESOLVED_WITH_ERROR,
    CandidateApplicability.RESOLVED_NEED_PRESERVE_COMPATIBILITY -> ResolutionStatus.SUCCESS
    CandidateApplicability.INAPPLICABLE_WRONG_RECEIVER -> ResolutionStatus.RECEIVER_TYPE_ERROR
    CandidateApplicability.UNSAFE_CALL -> ResolutionStatus.UNSAFE_CALL_ERROR
    else -> ResolutionStatus.OTHER_ERROR
}
