sed -i 's/!accessToken.isNullOrBlank() -> "Dhan"/!accessToken.isNullOrBlank() || uri.getQueryParameter("tokenId") != null -> "Dhan"/g' app/src/main/java/com/example/viewmodel/MainViewModel.kt
