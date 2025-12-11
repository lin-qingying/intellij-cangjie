#!/bin/bash
#
# Copyright 2025 LinQingYing. and contributors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# The use of this source code is governed by the Apache License 2.0,
# which allows users to freely use, modify, and distribute the code,
# provided they adhere to the terms of the license.
#
# The software is provided "as-is", and the authors are not responsible for
# any damages or issues arising from its use.
#
#

# ModuleInfo 全局替换脚本

ANALYSIS_DIR="D:/code/intellij/intellij-cangjie/analysis/src/main/kotlin"

echo "开始批量替换 ModuleInfo 为 AnalysisContext..."

# 1. 导入语句替换
find "$ANALYSIS_DIR" -name "*.kt" -type f -exec sed -i 's/import org\.cangnova\.cangjie\.descriptors\.ModuleInfo/import org.cangnova.cangjie.descriptors.AnalysisContext/g' {} \;

# 2. 类型参数替换
find "$ANALYSIS_DIR" -name "*.kt" -type f -exec sed -i 's/<M : ModuleInfo>/<M : AnalysisContext>/g' {} \;

# 3. 函数参数替换
find "$ANALYSIS_DIR" -name "*.kt" -type f -exec sed -i 's/moduleInfo: ModuleInfo/context: AnalysisContext/g' {} \;
find "$ANALYSIS_DIR" -name "*.kt" -type f -exec sed -i 's/moduleInfo: M/context: M/g' {} \;

# 4. 变量名替换
find "$ANALYSIS_DIR" -name "*.kt" -type f -exec sed -i 's/val moduleInfo/val context/g' {} \;
find "$ANALYSIS_DIR" -name "*.kt" -type f -exec sed -i 's/moduleInfo\./context\./g' {} \;

# 5. 函数名替换
find "$ANALYSIS_DIR" -name "*.kt" -type f -exec sed -i 's/diagnoseUnknownModuleInfo/diagnoseUnknownContext/g' {} \;

# 6. 属性访问替换
find "$ANALYSIS_DIR" -name "*.kt" -type f -exec sed -i 's/\.contentScope/.scope/g' {} \;

echo "批量替换完成！"
echo "请手动检查以下项："
echo "1. ModuleInfo.Capability -> AnalysisContextCapability"
echo "2. moduleOrigin -> isSourceContext"
echo "3. CangJieModuleInfo -> (可能需要删除此检查)"
echo "4. 特殊情况的手动调整"
