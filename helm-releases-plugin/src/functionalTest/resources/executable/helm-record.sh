#!/bin/sh
# Fake helm CLI which records every invocation as one line in a log file, and succeeds.
# Server options arrive as flags and the kubeconfig as an environment variable, so both are recorded.
echo "$* KUBECONFIG=$KUBECONFIG" >> "%LOG_FILE%"
if [ "$1" = "ls" ]; then
  echo "[]"
fi
exit 0
