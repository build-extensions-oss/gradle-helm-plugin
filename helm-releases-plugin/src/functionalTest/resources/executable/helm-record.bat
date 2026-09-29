@echo off
rem Fake helm CLI which records every invocation as one line in a log file, and succeeds.
rem Server options arrive as flags and the kubeconfig as an environment variable, so both are recorded.
echo %* KUBECONFIG=%KUBECONFIG%>> "%LOG_FILE%"
if "%~1"=="ls" echo []
exit /b 0
