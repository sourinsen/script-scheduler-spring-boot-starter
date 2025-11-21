#!/bin/sh
# Test script that always fails

echo "Script started at $(date)"
echo "This script will fail intentionally"
echo "Error: Something went wrong!" >&2
exit 1
