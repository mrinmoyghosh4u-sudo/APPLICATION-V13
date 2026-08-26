package com.example.ui.components

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TradingViewChart(
    symbol: String,
    modifier: Modifier = Modifier
) {
    val mappedSymbol = when {
        symbol.contains("NIFTY 50") || symbol == "NIFTY" -> "NSE:NIFTY"
        symbol.contains("BANKNIFTY") || symbol == "BANK NIFTY" -> "NSE:BANKNIFTY"
        symbol.contains("FINNIFTY") -> "NSE:FINNIFTY"
        symbol.contains("MIDCPNIFTY") -> "NSE:MIDCPNIFTY"
        symbol.contains("SENSEX") -> "BSE:SENSEX"
        symbol.contains("BANKEX") -> "BSE:BANKEX"
        else -> "BSE:$symbol" // Fallback
    }

    val htmlData = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <style>
                body, html { margin: 0; padding: 0; height: 100%; width: 100%; background-color: #131722; }
                #tv_chart_container { height: 100%; width: 100%; }
            </style>
        </head>
        <body>
            <div id="tv_chart_container"></div>
            <script type="text/javascript" src="https://s3.tradingview.com/tv.js"></script>
            <script type="text/javascript">
                new TradingView.widget({
                    "autosize": true,
                    "symbol": "$mappedSymbol",
                    "interval": "5",
                    "timezone": "Asia/Kolkata",
                    "theme": "dark",
                    "style": "1",
                    "locale": "in",
                    "enable_publishing": false,
                    "backgroundColor": "#131722",
                    "gridColor": "#1f2933",
                    "hide_top_toolbar": false,
                    "hide_legend": false,
                    "save_image": false,
                    "container_id": "tv_chart_container",
                    "toolbar_bg": "#131722",
                    "studies": ["Volume@tv-basicstudies"]
                });
            </script>
        </body>
        </html>
    """.trimIndent()

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                webViewClient = WebViewClient()
                loadDataWithBaseURL("https://in.tradingview.com/", htmlData, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://in.tradingview.com/", htmlData, "text/html", "UTF-8", null)
        },
        modifier = modifier.fillMaxSize()
    )
}
