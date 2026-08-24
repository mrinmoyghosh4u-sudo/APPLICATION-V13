sed -i '/LaunchedEffect(Unit) {/i \
                val context = LocalContext.current\
                LaunchedEffect(authErrorMessage) {\
                    authErrorMessage?.let {\
                        android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()\
                    }\
                }' app/src/main/java/com/example/MainActivity.kt
