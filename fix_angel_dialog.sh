sed -i '154,165c\
                            val apiKey = com.example.util.BrokerConfig.angelApiKey\
                            if (apiKey.isBlank()) {\
                                localErrorMsg = "ANGEL_ONE_API_KEY is missing in configuration."\
                            } else {\
                                localErrorMsg = null\
                                val redirectUri = java.net.URLEncoder.encode(com.example.util.BrokerConfig.angelRedirectUri, "UTF-8")\
                                val url = "https://smartapi.angelone.in/publisher-login?api_key=$apiKey&redirect_url=$redirectUri&state=angel"\
                                android.util.Log.d("OAuth", "Angel One OAuth URL: $url")\
                                android.util.Log.d("OAuth", "Redirect URI: ${com.example.util.BrokerConfig.angelRedirectUri}")\
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))\
                                context.startActivity(intent)\
                            }' app/src/main/java/com/example/ui/components/BrokerConnectDialog.kt
