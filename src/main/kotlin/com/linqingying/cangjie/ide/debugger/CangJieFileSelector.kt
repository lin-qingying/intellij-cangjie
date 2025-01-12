package com.linqingying.cangjie.ide.debugger

import com.linqingying.cangjie.psi.CjFile
import com.sun.jdi.Location

interface CangJieFileSelector {
    suspend fun chooseMostApplicableFile(files: List<CjFile>, location: Location): CjFile
}