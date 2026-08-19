package com.example.data.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class InstrumentMasterServiceTest {

    @Test
    fun testInstrumentMasterService() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val client = OkHttpClient.Builder()
            .readTimeout(120, TimeUnit.SECONDS)
            .build()
        val service = InstrumentMasterService(client, context)
        service.loadMaster()
        
        val map = service.javaClass.getDeclaredField("instrumentMap").apply { isAccessible = true }.get(service) as Map<String, Instrument>
        println("Total instruments loaded: ${map.size}")
        
        println("NIFTY 50 token: ${service.resolveAngelToken("NIFTY 50", "NSE")}")
        println("BANKNIFTY token: ${service.resolveAngelToken("BANKNIFTY", "NSE")}")
        println("RELIANCE token: ${service.resolveAngelToken("RELIANCE-EQ", "NSE") ?: service.resolveAngelToken("RELIANCE", "NSE")}")
        println("TCS token: ${service.resolveAngelToken("TCS-EQ", "NSE") ?: service.resolveAngelToken("TCS", "NSE")}")
        println("INFY token: ${service.resolveAngelToken("INFY-EQ", "NSE") ?: service.resolveAngelToken("INFY", "NSE")}")
        println("SBIN token: ${service.resolveAngelToken("SBIN-EQ", "NSE") ?: service.resolveAngelToken("SBIN", "NSE")}")
        
        val niftyOpts = service.getOptionExpiries("NIFTY")
        if (niftyOpts.isNotEmpty()) {
            val opts = service.getOptionInstruments("NIFTY", niftyOpts[0])
            if (opts.isNotEmpty()) {
                val ce = opts.firstOrNull { it.symbol.endsWith("CE") }
                val pe = opts.firstOrNull { it.symbol.endsWith("PE") }
                println("Sample NIFTY CE: ${ce?.token} - ${ce?.symbol}")
                println("Sample NIFTY PE: ${pe?.token} - ${pe?.symbol}")
            }
        }
    }
}
