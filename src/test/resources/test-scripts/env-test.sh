#!/bin/sh
# Test script that uses environment variables

echo "Script started at $(date)"
echo "Testing environment variables..."

if [ -z "$TEST_ENV_VAR" ]; then
    echo "ERROR: TEST_ENV_VAR is not set!" >&2
    exit 1
fi

if [ -z "$ANOTHER_VAR" ]; then
    echo "ERROR: ANOTHER_VAR is not set!" >&2
    exit 1
fi

echo "TEST_ENV_VAR=$TEST_ENV_VAR"
echo "ANOTHER_VAR=$ANOTHER_VAR"

if [ "$TEST_ENV_VAR" = "test-value" ] && [ "$ANOTHER_VAR" = "another-value" ]; then
    echo "Environment variables are correctly set"
    exit 0
else
    echo "ERROR: Environment variables have incorrect values!" >&2
    exit 1
fi
