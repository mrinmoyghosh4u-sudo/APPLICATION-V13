#!/bin/bash
cat app/src/main/java/com/example/util/diagnostic/SelfDiagnosticEngine.kt | sed '/val recoveryLogs:/a \
\
    private val _fullAZReport = MutableStateFlow<List<AZDiagnosticResult>>(emptyList())\
    val fullAZReport: StateFlow<List<AZDiagnosticResult>> = _fullAZReport.asStateFlow()' > temp.kt

mv temp.kt app/src/main/java/com/example/util/diagnostic/SelfDiagnosticEngine.kt
