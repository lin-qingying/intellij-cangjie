#!/bin/bash

# 测试数据生成脚本

# 默认参数
OUTPUT_FILE="telemetry_test_data.json"
COUNT=5
DIVERSE=false

# 解析命令行参数
for arg in "$@"; do
  case $arg in
    --output=*)
      OUTPUT_FILE="${arg#*=}"
      shift
      ;;
    --count=*)
      COUNT="${arg#*=}"
      shift
      ;;
    --diverse)
      DIVERSE=true
      shift
      ;;
    --help)
      echo "遥测测试数据生成器"
      echo ""
      echo "用法:"
      echo "  ./generate-test-data.sh [选项]"
      echo ""
      echo "选项:"
      echo "  --output=<文件路径>  指定输出文件路径 (默认: telemetry_test_data.json)"
      echo "  --count=<数量>      指定生成的数据点数量 (默认: 5)"
      echo "  --diverse          生成多样化的测试数据"
      echo "  --help             显示此帮助信息"
      exit 0
      ;;
  esac
done

# 构建运行命令
CMD="./gradlew run --args=\""

if [ "$DIVERSE" = true ]; then
  CMD="${CMD} --diverse"
else
  CMD="${CMD} --count=${COUNT}"
fi

CMD="${CMD} --output=${OUTPUT_FILE}\""

# 运行命令
echo "生成测试数据..."
echo "执行: $CMD"
eval $CMD

echo "完成！测试数据已保存到 ${OUTPUT_FILE}" 