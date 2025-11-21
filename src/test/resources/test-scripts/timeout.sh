#!/bin/sh
# Test script that times out

echo "Script started at $(date)"
echo "This script will sleep for a long time to test timeout"
sleep 120  # Sleep for 2 minutes
echo "This line should not be reached if timeout is less than 120 seconds"
exit 0
