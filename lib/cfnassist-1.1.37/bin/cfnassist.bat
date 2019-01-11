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

set CLASSPATH=%APP_HOME%\lib\cfnassist-all-1.1.37.jar;%APP_HOME%\lib\slf4j-api-1.7.25.jar;%APP_HOME%\lib\logback-classic-1.2.3.jar;%APP_HOME%\lib\protocol-core-2.2.0.jar;%APP_HOME%\lib\aws-query-protocol-2.2.0.jar;%APP_HOME%\lib\ec2-2.2.0.jar;%APP_HOME%\lib\cloudformation-2.2.0.jar;%APP_HOME%\lib\elasticloadbalancing-2.2.0.jar;%APP_HOME%\lib\sns-2.2.0.jar;%APP_HOME%\lib\sqs-2.2.0.jar;%APP_HOME%\lib\iam-2.2.0.jar;%APP_HOME%\lib\rds-2.2.0.jar;%APP_HOME%\lib\s3-2.2.0.jar;%APP_HOME%\lib\cloudwatchlogs-2.2.0.jar;%APP_HOME%\lib\commons-io-2.6.jar;%APP_HOME%\lib\commons-cli-1.4.jar;%APP_HOME%\lib\commons-net-3.6.jar;%APP_HOME%\lib\ant-1.9.6.jar;%APP_HOME%\lib\logback-core-1.2.3.jar;%APP_HOME%\lib\sdk-core-2.2.0.jar;%APP_HOME%\lib\annotations-2.2.0.jar;%APP_HOME%\lib\utils-2.2.0.jar;%APP_HOME%\lib\http-client-spi-2.2.0.jar;%APP_HOME%\lib\aws-core-2.2.0.jar;%APP_HOME%\lib\auth-2.2.0.jar;%APP_HOME%\lib\regions-2.2.0.jar;%APP_HOME%\lib\apache-client-2.2.0.jar;%APP_HOME%\lib\netty-nio-client-2.2.0.jar;%APP_HOME%\lib\aws-xml-protocol-2.2.0.jar;%APP_HOME%\lib\aws-json-protocol-2.2.0.jar;%APP_HOME%\lib\ant-launcher-1.9.6.jar;%APP_HOME%\lib\jackson-core-2.9.7.jar;%APP_HOME%\lib\jackson-databind-2.9.7.jar;%APP_HOME%\lib\reactive-streams-1.0.2.jar;%APP_HOME%\lib\flow-1.7.jar;%APP_HOME%\lib\profiles-2.2.0.jar;%APP_HOME%\lib\jackson-annotations-2.9.7.jar;%APP_HOME%\lib\httpclient-4.5.6.jar;%APP_HOME%\lib\httpcore-4.4.10.jar;%APP_HOME%\lib\netty-codec-http-4.1.32.Final.jar;%APP_HOME%\lib\netty-codec-http2-4.1.32.Final.jar;%APP_HOME%\lib\netty-codec-4.1.32.Final.jar;%APP_HOME%\lib\netty-transport-4.1.32.Final.jar;%APP_HOME%\lib\netty-common-4.1.32.Final.jar;%APP_HOME%\lib\netty-buffer-4.1.32.Final.jar;%APP_HOME%\lib\netty-handler-4.1.32.Final.jar;%APP_HOME%\lib\netty-transport-native-epoll-4.1.32.Final-linux-x86_64.jar;%APP_HOME%\lib\netty-reactive-streams-http-2.0.0.jar;%APP_HOME%\lib\commons-logging-1.2.jar;%APP_HOME%\lib\commons-codec-1.10.jar;%APP_HOME%\lib\netty-resolver-4.1.32.Final.jar;%APP_HOME%\lib\netty-transport-native-unix-common-4.1.32.Final.jar;%APP_HOME%\lib\netty-reactive-streams-2.0.0.jar

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
