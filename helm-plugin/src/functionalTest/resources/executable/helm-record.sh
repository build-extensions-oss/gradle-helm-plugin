#!/bin/sh
# Fake helm CLI which records every invocation as one line in a log file, and succeeds.
# Server options arrive as flags and the kubeconfig as an environment variable, so both are recorded.
# The first line of the standard input is recorded too (empty for most commands), so that secrets passed
# with flags like --password-stdin can be checked.
STDIN_VALUE=""
read -r STDIN_VALUE || true
echo "$* STDIN=$STDIN_VALUE KUBECONFIG=$KUBECONFIG" >> "%LOG_FILE%"
if [ "$1" = "ls" ]; then
  echo "[]"
fi
exit 0
