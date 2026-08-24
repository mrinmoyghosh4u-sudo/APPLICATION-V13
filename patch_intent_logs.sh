sed -i 's/val uri = intent?.data/val uri = intent?.data\n        android.util.Log.d("AuthIntent", "Received intent with data: $uri")/g' app/src/main/java/com/example/MainActivity.kt
