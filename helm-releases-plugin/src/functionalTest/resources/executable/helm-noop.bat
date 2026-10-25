@echo off
rem Dummy executable which stands in for a real helm CLI.
rem "helm ls -o json" has to return parseable JSON, because HelmCommandSupport.getRelease parses it.
rem Every other command just succeeds, which lets a build run to completion without a Kubernetes cluster.
if "%~1"=="ls" echo []
exit /b 0
