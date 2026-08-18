sed -i '174,228c\
                    // Dhan OAuth Flow\
                    Text(\
                        "Secure Login via Dhan HQ OAuth",\
                        color = TextWhite,\
                        fontSize = 14.sp,\
                        fontWeight = FontWeight.Medium\
                    )\
                    Spacer(modifier = Modifier.height(8.dp))\
                    Text(\
                        "You will be redirected to Dhan'"'"'s official login page to authorize this app.",\
                        color = TextGray,\
                        fontSize = 12.sp\
                    )\
                    Spacer(modifier = Modifier.height(24.dp))\
                    Button(\
                        onClick = {\
                            localErrorMsg = null\
                            isDhanConsentLoading = true\
                            coroutineScope.launch {\
                                com.example.util.DhanAuthHelper.generateConsent().onSuccess { consentAppId ->\
                                    isDhanConsentLoading = false\
                                    val url = "https://auth.dhan.co/login/consentApp-login?consentAppId=$consentAppId"\
                                    android.util.Log.d("OAuth", "Dhan OAuth URL: $url")\
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))\
                                    context.startActivity(intent)\
                                }.onFailure { err ->\
                                    isDhanConsentLoading = false\
                                    localErrorMsg = err.localizedMessage ?: "Failed to generate Dhan consent session."\
                                }\
                            }\
                        },' app/src/main/java/com/example/ui/components/BrokerConnectDialog.kt
