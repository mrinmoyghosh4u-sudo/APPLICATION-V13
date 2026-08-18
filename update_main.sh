sed -i 's/onConnectAngelOne = { code, pwd, totp, key ->/onConnectAngelOne = { _, _, _, _ ->/g' app/src/main/java/com/example/MainActivity.kt
sed -i 's/viewModel.connectAngelOne(code, pwd, totp, key)//g' app/src/main/java/com/example/MainActivity.kt
sed -i 's/onConnectDhan = { id, token ->/onConnectDhan = { _, _ ->/g' app/src/main/java/com/example/MainActivity.kt
sed -i 's/viewModel.connectDhan(id, token)//g' app/src/main/java/com/example/MainActivity.kt
