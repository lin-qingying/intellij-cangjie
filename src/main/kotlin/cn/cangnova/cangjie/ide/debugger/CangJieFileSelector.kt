package cn.cangnova.cangjie.ide.debugger

import cn.cangnova.cangjie.psi.CjFile
import com.sun.jdi.Location

interface CangJieFileSelector {
    suspend fun chooseMostApplicableFile(files: List<CjFile>, location: Location): CjFile
}