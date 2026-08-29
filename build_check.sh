#!/bin/bash
echo "Starting testDebugUnitTest..."
./gradlew :app:testDebugUnitTest
TEST_STATUS=$?
echo "Starting assembleDebug..."
./gradlew assembleDebug
DEBUG_STATUS=$?
echo "Starting assembleRelease..."
./gradlew assembleRelease
RELEASE_STATUS=$?
echo "Test Status: $TEST_STATUS"
echo "Debug Status: $DEBUG_STATUS"
echo "Release Status: $RELEASE_STATUS"
