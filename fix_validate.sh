sed -i '/suspend fun validateAndRestoreSession(): Boolean = sessionRestoreMutex.withLock {/,/    }/c\
    suspend fun validateAndRestoreSession(): Boolean = sessionRestoreMutex.withLock {\
        _isSessionRestoring.value = true\
        val hasSession = sessionManager.hasValidSession()\
        if (!hasSession) {\
            android.util.Log.d("SessionRestore", "session exists: false")\
            _isSessionValid.value = false\
            _isSessionRestoring.value = false\
            return@withLock false\
        }\
        android.util.Log.d("SessionRestore", "session exists: true")\
        _isSessionValid.value = true\
        _isSessionRestoring.value = false\
        repository.syncWithBroker()\
        return@withLock true\
    }' app/src/main/java/com/example/viewmodel/MainViewModel.kt
