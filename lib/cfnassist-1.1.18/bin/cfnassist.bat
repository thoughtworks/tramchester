@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  cfnassist startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

@rem Add default JVM options here. You can also use JAVA_OPTS and CFNASSIST_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto init

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto init

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:init
@rem Get command-line arguments, handling Windowz variants

if not "%OS%" == "Windows_NT" goto win9xME_args
if "%@eval[2+2]" == "4" goto 4NT_args

:win9xME_args
@rem Slurp the command line arguments.
set CMD_LINE_ARGS=
set _SKIP=2

:win9xME_args_slurp
if "x%~1" == "x" goto execute

set CMD_LINE_ARGS=%*
goto execute

:4NT_args
@rem Get arguments from the 4NT Shell from JP Software
set CMD_LINE_ARGS=%$

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\cfnassist-all-1.1.18.jar;%APP_HOME%\lib\slf4j-api-1.7.5.jar;%APP_HOME%\lib\logback-classic-1.0.10.jar;%APP_HOME%\lib\aws-java-sdk-core-1.11.301.jar;%APP_HOME%\lib\aws-java-sdk-s3-1.11.301.jar;%APP_HOME%\lib\aws-java-sdk-ec2-1.11.301.jar;%APP_HOME%\lib\aws-java-sdk-cloudformation-1.11.301.jar;%APP_HOME%\lib\aws-java-sdk-elasticloadbalancing-1.11.301.jar;%APP_HOME%\lib\aws-java-sdk-sns-1.11.301.jar;%APP_HOME%\lib\aws-java-sdk-sqs-1.11.301.jar;%APP_HOME%\lib\aws-java-sdk-rds-1.11.301.jar;%APP_HOME%\lib\aws-java-sdk-iam-1.11.301.jar;%APP_HOME%\lib\commons-io-2.5.jar;%APP_HOME%\lib\commons-cli-1.3.1.jar;%APP_HOME%\lib\commons-net-3.3.jar;%APP_HOME%\lib\ant-1.9.6.jar;%APP_HOME%\lib\logback-core-1.0.10.jar;%APP_HOME%\lib\httpclient-4.5.5.jar;%APP_HOME%\lib\ion-java-1.0.2.jar;%APP_HOME%\lib\jackson-databind-2.6.7.1.jar;%APP_HOME%\lib\jackson-dataformat-cbor-2.6.7.jar;%APP_HOME%\lib\joda-time-2.8.1.jar;%APP_HOME%\lib\aws-java-sdk-kms-1.11.301.jar;%APP_HOME%\lib\jmespath-java-1.11.301.jar;%APP_HOME%\lib\ant-launcher-1.9.6.jar;%APP_HOME%\lib\httpcore-4.4.9.jar;%APP_HOME%\lib\commons-codec-1.10.jar;%APP_HOME%\lib\jackson-annotations-2.6.0.jar;%APP_HOME%\lib\jackson-core-2.6.7.jar;%APP_HOME%\lib\commons-logging-1.2.jar

@rem Execute cfnassist
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %CFNASSIST_OPTS%  -classpath "%CLASSPATH%" tw.com.commandline.Main %CMD_LINE_ARGS%

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable CFNASSIST_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%CFNASSIST_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
