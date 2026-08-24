import os

filepath = "app/src/main/java/com/example/data/network/BrokerNetworkClient.kt"
with open(filepath, "r") as f:
    content = f.read()

fyers_api = """

    // =========================================
    // FYERS
    // =========================================
    private val fyersClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(errorLoggingInterceptor)
        .addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
            
            // Fyers usually doesn't need Bearer here unless we call other APIs.
            // But if we do:
            // sessionManager.fyersAccessToken?.let {
            //     requestBuilder.header("Authorization", "$appId:$it")
            // }
            
            chain.proceed(requestBuilder.build())
        }
        .build()

    val fyersApi: FyersApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api-t1.fyers.in/")
            .client(fyersClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(FyersApi::class.java)
    }
"""

if "fyersApi:" not in content:
    content = content + fyers_api
    with open(filepath, "w") as f:
        f.write(content)
    print("Added FyersApi to BrokerNetworkClient")
else:
    print("Already exists")
