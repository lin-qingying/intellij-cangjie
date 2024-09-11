package com.huawei.cangjie.cfg

import com.huawei.cangjie.cfg.pseudocode.Pseudocode
import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.psi.CjElement

class ControlFlowProcessor(
    private val trace: BindingTrace,
    private val languageVersionSettings: LanguageVersionSettings
)
{
//    fun generatePseudocode(subroutine: CjElement): Pseudocode {
//        val pseudocode = generate(subroutine, null)
//        (pseudocode as PseudocodeImpl).postProcess()
//        return pseudocode
//    }
}
