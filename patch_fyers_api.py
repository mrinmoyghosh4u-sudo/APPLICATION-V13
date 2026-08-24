import os

filepath = "/app/applet/app/src/main/java/com/example/data/network/FyersApi.kt"
with open(filepath, "r") as f:
    content = f.read()

new_classes = """data class FyersRefreshTokenRequest(
    val grant_type: String = "refresh_token",
    val appIdHash: String,
    val refresh_token: String,
    val pin: String
)

"""
if "FyersRefreshTokenRequest" not in content:
    content = content.replace("data class FyersTokenRequest", new_classes + "data class FyersTokenRequest")

new_endpoint = """
    @POST("api/v3/validate-refresh-token")
    suspend fun validateRefreshToken(
        @Body request: FyersRefreshTokenRequest
    ): Response<FyersTokenResponse>

"""
if "validateRefreshToken" not in content:
    content = content.replace("@POST(\"api/v3/validate-authcode\")", new_endpoint + "    @POST(\"api/v3/validate-authcode\")")

with open(filepath, "w") as f:
    f.write(content)
