#!/bin/bash
sed -i '233,244c\
        viewModelScope.launch {\
            try {\
                repository.checkAndSeedInitialData()\
                brokerAuthManager.initialize()\
                if (sessionManager.isFyersConnected && !sessionManager.fyersAccessToken.isNullOrBlank()) {\
                    runCatching { brokerManager.fyersMarketDataService.connect() }\
                }\
            } catch(e: Exception) { \
                e.printStackTrace() \
            } finally {\
                validateAndRestoreSession()\
            }\
        }' app/src/main/java/com/example/viewmodel/MainViewModel.kt
