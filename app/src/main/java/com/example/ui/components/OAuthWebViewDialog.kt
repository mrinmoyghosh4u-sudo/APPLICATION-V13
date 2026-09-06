package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OAuthWebViewDialog(
    url: String,
    brokerName: String,
    onRedirectCaptured: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var progress by remember { mutableIntStateOf(0) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var pageTitle by remember { mutableStateOf("$brokerName OAuth Login") }
    var currentUrl by remember { mutableStateOf(url) }
    var hasCapturedRedirect by remember { mutableStateOf(false) }
    var webViewError by remember { mutableStateOf<String?>(null) }

    // Helper to test if a URL is an OAuth callback redirect
    fun isOAuthCallbackUrl(targetUrl: String): Boolean {
        if (targetUrl.isBlank()) return false
        val uri = try { Uri.parse(targetUrl) } catch (_: Exception) { return false }
        val scheme = uri.scheme?.lowercase() ?: ""

        // 1. Check query parameters for authorization codes/tokens
        val hasCodeOrToken = uri.getQueryParameter("code") != null ||
                uri.getQueryParameter("auth_code") != null ||
                uri.getQueryParameter("access_token") != null ||
                uri.getQueryParameter("token") != null ||
                uri.getQueryParameter("tokenId") != null ||
                uri.getQueryParameter("consentId") != null ||
                uri.getQueryParameter("error") != null ||
                uri.getQueryParameter("error_description") != null

        // 2. Custom deep links
        val isCustomScheme = scheme == "kingkhan" || scheme == "kingkhanapp" || scheme == "dhanapp" || scheme == "upstox" || scheme == "fyers"

        // 3. Registered redirect URIs or endpoints
        val isKnownRedirectHost = targetUrl.contains("application-beige-psi.vercel.app/oauth") ||
                targetUrl.contains("localhost") ||
                targetUrl.contains("127.0.0.1") ||
                (targetUrl.contains("api.dhan.co") && (targetUrl.contains("tokenId") || targetUrl.contains("consentId"))) ||
                targetUrl.contains("redirect_uri") ||
                (hasCodeOrToken && (isCustomScheme || targetUrl.contains("oauth") || targetUrl.contains("callback") || targetUrl.contains("redirect")))

        return (hasCodeOrToken && (isCustomScheme || isKnownRedirectHost)) || isCustomScheme
    }

    fun handleRedirectIfMatched(targetUrl: String): Boolean {
        if (hasCapturedRedirect) return true
        if (isOAuthCallbackUrl(targetUrl)) {
            hasCapturedRedirect = true
            android.util.Log.i("OAuthWebView", "Captured OAuth Redirect URL: $targetUrl")
            onRedirectCaptured(Uri.parse(targetUrl))
            return true
        }
        return false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(20.dp),
            color = DarkBackground,
            border = BorderStroke(1.2.dp, PrimaryGold.copy(alpha = 0.7f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Navigation / Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCard)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = ProfitGreenBg,
                            shape = CircleShape,
                            border = BorderStroke(1.dp, ProfitGreen.copy(alpha = 0.6f)),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = "SSL Secured",
                                    tint = ProfitGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$brokerName Secure Login",
                                    color = PrimaryGold,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFF1E2838),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "SSL ENCRYPTED",
                                        color = TextGray,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = pageTitle.take(45),
                                color = TextGray,
                                fontSize = 10.5.sp,
                                maxLines = 1
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Refresh button
                        IconButton(
                            onClick = { webViewRef?.reload() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = TextWhite, modifier = Modifier.size(18.dp))
                        }

                        // Open in external browser fallback
                        IconButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl.ifBlank { url }))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = "External Browser", tint = PrimaryGold, modifier = Modifier.size(18.dp))
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Close Button
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFF2B2B2B), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Progress Indicator
                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { (progress.coerceIn(5, 100)) / 100f },
                        modifier = Modifier.fillMaxWidth().height(2.5.dp),
                        color = PrimaryGold,
                        trackColor = Color(0xFF222834)
                    )
                } else {
                    HorizontalDivider(color = DarkCardBorder, thickness = 1.dp)
                }

                // Security Banner Note
                Surface(
                    color = Color(0xFF12161F),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.6.dp, Color(0xFF222D3E))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Authenticating directly with official $brokerName servers. Tokens will be captured automatically upon login approval.",
                            color = TextGray,
                            fontSize = 9.5.sp,
                            lineHeight = 12.sp
                        )
                    }
                }

                // Error Banner if WebView fails
                if (webViewError != null) {
                    Surface(
                        color = LossRedBg,
                        border = BorderStroke(1.dp, LossRed.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Connection Warning", color = LossRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(webViewError ?: "", color = TextWhite, fontSize = 10.sp)
                            }
                            Button(
                                onClick = {
                                    webViewError = null
                                    webViewRef?.loadUrl(url)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = LossRed),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("RETRY", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Embedded Android WebView Container
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.White)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                webViewRef = this
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                    setSupportZoom(true)
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                    allowFileAccess = false
                                    allowContentAccess = false
                                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                    // Remove 'wv' flag from user agent so broker login portals do not block embedded WebView
                                    userAgentString = userAgentString.replace("; wv", "")
                                }

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        progress = newProgress
                                        isLoading = newProgress < 100
                                    }

                                    override fun onReceivedTitle(view: WebView?, title: String?) {
                                        if (!title.isNullOrBlank()) {
                                            pageTitle = title
                                        }
                                    }
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, targetUrl: String?, favicon: Bitmap?) {
                                        super.onPageStarted(view, targetUrl, favicon)
                                        isLoading = true
                                        targetUrl?.let {
                                            currentUrl = it
                                            if (handleRedirectIfMatched(it)) {
                                                view?.stopLoading()
                                            }
                                        }
                                    }

                                    override fun onPageFinished(view: WebView?, targetUrl: String?) {
                                        super.onPageFinished(view, targetUrl)
                                        isLoading = false
                                        targetUrl?.let {
                                            currentUrl = it
                                            handleRedirectIfMatched(it)
                                        }
                                    }

                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val targetUrl = request?.url?.toString() ?: return false
                                        currentUrl = targetUrl
                                        if (handleRedirectIfMatched(targetUrl)) {
                                            view?.stopLoading()
                                            return true
                                        }
                                        return false
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?
                                    ) {
                                        super.onReceivedError(view, request, error)
                                        if (request?.isForMainFrame == true) {
                                            val desc = error?.description?.toString() ?: "Network error"
                                            val targetUrl = request.url?.toString() ?: ""
                                            if (handleRedirectIfMatched(targetUrl)) {
                                                view?.stopLoading()
                                            } else if (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) {
                                                handleRedirectIfMatched(targetUrl)
                                            } else {
                                                webViewError = desc
                                            }
                                        }
                                    }
                                }

                                loadUrl(url)
                            }
                        },
                        update = { webView ->
                            webViewRef = webView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
