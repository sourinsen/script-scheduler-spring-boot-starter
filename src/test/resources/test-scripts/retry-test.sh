#!/bin/sh
# Test script for retry logic

COUNTER_FILE="/tmp/retry-test-counter.txt"

echo "Script started at $(date)"

# Check if counter file exists
if [ ! -f "$COUNTER_FILE" ]; then
    echo "First execution - creating counter file and failing"
    echo "1" > "$COUNTER_FILE"
    exit 1
else
    # Read counter
    COUNT=$(cat "$COUNTER_FILE")
    echo "Execution attempt #$((COUNT + 1))"
    
    if [ "$COUNT" -lt "2" ]; then
        echo "Still failing, incrementing counter"
        echo "$((COUNT + 1))" > "$COUNTER_FILE"
        exit 1
    else
        echo "Success on attempt #$((COUNT + 1))"
        rm -f "$COUNTER_FILE"
        exit 0
    fi
fi
