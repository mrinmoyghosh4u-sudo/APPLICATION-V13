#!/bin/bash
sed -i 's/HealthState.PENDING/HealthState.OFFLINE/g' app/src/main/java/com/example/ui/screens/HealthAndAutoFixScreen.kt
