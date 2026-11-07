#!/bin/sh
# Fake helm CLI which fails its first invocations and succeeds afterwards, to exercise a task's retry.
# The test fills in the log file, the attempt counter and the number of failures.
LOG_FILE="%LOG_FILE%"
COUNTER_FILE="%COUNTER_FILE%"
FAILURES=%FAILURES%
echo "$*" >> "$LOG_FILE"
ATTEMPTS=0
if [ -f "$COUNTER_FILE" ]; then
  ATTEMPTS="$(cat "$COUNTER_FILE")"
fi
ATTEMPTS=$((ATTEMPTS + 1))
echo "$ATTEMPTS" > "$COUNTER_FILE"
if [ "$ATTEMPTS" -le "$FAILURES" ]; then
  echo "simulated transient failure on attempt $ATTEMPTS" >&2
  exit 1
fi
exit 0
