sed -i '/suspend fun checkAndSeedInitialData()/,/^    }/c\
    suspend fun checkAndSeedInitialData() {\
        if (dao.getUserProfile().firstOrNull() == null) {\
            dao.insertOrUpdateProfile(UserProfileEntity())\
        }\
    }' app/src/main/java/com/example/data/repository/TradingRepository.kt
