#!/bin/sh
# Dummy executable which stands in for a real helm CLI.
# "helm ls -o json" has to return parseable JSON, because HelmCommandSupport.getRelease parses it.
# Every other command just succeeds, which lets a build run to completion without a Kubernetes cluster.
if [ "$1" = "ls" ]; then
  echo "[]"
fi
exit 0
