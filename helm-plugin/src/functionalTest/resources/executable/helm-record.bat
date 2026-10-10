@echo off
rem Fake helm CLI which records every invocation as one line in a log file, and succeeds.
rem Server options arrive as flags and the kubeconfig as an environment variable, so both are recorded.
rem The first line of the standard input is recorded too (empty for most commands), so that secrets passed
rem with flags like --password-stdin can be checked.
set "STDIN_VALUE="
set /p STDIN_VALUE=
echo %* STDIN=%STDIN_VALUE% KUBECONFIG=%KUBECONFIG%>> "%LOG_FILE%"
if "%~1"=="ls" echo []
exit /b 0
