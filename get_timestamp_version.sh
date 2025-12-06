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

# Path to your gradle.properties file
GRADLE_PROPERTIES_FILE="gradle.properties"

# Read the current version from gradle.properties, ignoring spaces around the equals sign
CURRENT_VERSION=$(grep -E "pluginVersion\s*=\s*" "$GRADLE_PROPERTIES_FILE" | sed -E 's/.*=\s*([^[:space:]]*).*/\1/')

# Extract the version part before any hyphen
BASE_VERSION=$(echo "$CURRENT_VERSION" | awk -F'-' '{print $1}')

# Replace any suffix following a hyphen with a timestamp in the format YYYYMMDD-HHmmSS
TIMESTAMP=$(date +'%Y%m%d-%H%M%S')
NEW_VERSION="${BASE_VERSION}-$TIMESTAMP"

# Use awk to update the gradle.properties file, allowing spaces around the equals sign
awk -v new_version="$NEW_VERSION" '/pluginVersion\s*=\s*/{sub(/=.*/, "=" new_version)}1' "$GRADLE_PROPERTIES_FILE" > tmpfile && mv tmpfile "$GRADLE_PROPERTIES_FILE"

echo $NEW_VERSION
