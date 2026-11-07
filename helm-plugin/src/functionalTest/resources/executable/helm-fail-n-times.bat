@echo off
rem Fake helm CLI which fails its first invocations and succeeds afterwards, to exercise a task's retry.
rem The test fills in the log file, the attempt counter and the number of failures.
set "LOG_FILE=%LOG_FILE%"
set "COUNTER_FILE=%COUNTER_FILE%"
set "FAILURES=%FAILURES%"
>> "%LOG_FILE%" echo %*
set "ATTEMPTS=0"
if exist "%COUNTER_FILE%" set /p ATTEMPTS=<"%COUNTER_FILE%"
set /a "ATTEMPTS+=1"
> "%COUNTER_FILE%" echo %ATTEMPTS%
if %ATTEMPTS% LEQ %FAILURES% (
  echo simulated transient failure on attempt %ATTEMPTS% 1>&2
  exit /b 1
)
exit /b 0
