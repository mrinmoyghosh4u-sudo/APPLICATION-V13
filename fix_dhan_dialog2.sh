sed -i 's/val cid = runCatching { BuildConfig.DHAN_CLIENT_ID }.getOrNull() ?: ""/val cid = com.example.util.BrokerConfig.dhanClientId/g' app/src/main/java/com/example/ui/components/BrokerConnectDialog.kt
sed -i 's/if (cid.isBlank() || cid.contains("DEFAULT_VALUE")) {/if (cid.isBlank()) {/g' app/src/main/java/com/example/ui/components/BrokerConnectDialog.kt
sed -i 's/localErrorMsg = "Dhan Client ID is missing in configuration."/localErrorMsg = "DHAN_CLIENT_ID is missing in configuration."/g' app/src/main/java/com/example/ui/components/BrokerConnectDialog.kt
