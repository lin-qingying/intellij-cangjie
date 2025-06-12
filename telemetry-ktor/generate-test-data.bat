@echo off
REM 测试数据生成脚本

REM 默认参数
set OUTPUT_FILE=telemetry_test_data.json
set COUNT=5
set DIVERSE=false

REM 解析命令行参数
:parse_args
if "%~1"=="" goto :run

if "%~1"=="--help" (
    echo 遥测测试数据生成器
    echo.
    echo 用法:
    echo   generate-test-data.bat [选项]
    echo.
    echo 选项:
    echo   --output=^<文件路径^>  指定输出文件路径 (默认: telemetry_test_data.json)
    echo   --count=^<数量^>      指定生成的数据点数量 (默认: 5)
    echo   --diverse          生成多样化的测试数据
    echo   --help             显示此帮助信息
    exit /b 0
)

set arg=%~1
if "%arg:~0,9%"=="--output=" (
    set OUTPUT_FILE=%arg:~9%
    shift
    goto :parse_args
)

if "%arg:~0,8%"=="--count=" (
    set COUNT=%arg:~8%
    shift
    goto :parse_args
)

if "%~1"=="--diverse" (
    set DIVERSE=true
    shift
    goto :parse_args
)

shift
goto :parse_args

:run
REM 构建运行命令
set CMD=gradlew.bat run --args="

if "%DIVERSE%"=="true" (
    set CMD=%CMD% --diverse
) else (
    set CMD=%CMD% --count=%COUNT%
)

set CMD=%CMD% --output=%OUTPUT_FILE%"

REM 运行命令
echo 生成测试数据...
echo 执行: %CMD%
%CMD%

echo 完成！测试数据已保存到 %OUTPUT_FILE% 