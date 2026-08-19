sed -i 's|var isLoaded = false|private val _isLoaded = kotlinx.coroutines.flow.MutableStateFlow(false)\n    val isLoadedFlow = _isLoaded.asStateFlow()\n    val isLoaded: Boolean get() = _isLoaded.value|g' app/src/main/java/com/example/data/network/InstrumentMasterService.kt
sed -i 's|isLoaded = true|_isLoaded.value = true|g' app/src/main/java/com/example/data/network/InstrumentMasterService.kt
