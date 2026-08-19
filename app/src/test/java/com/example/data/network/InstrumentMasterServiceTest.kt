package com.example.data.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import okhttp3.OkHttpClient

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class InstrumentMasterServiceTest {

    @Test
    fun testInstrumentMasterServiceParsingWithNulls() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val service = InstrumentMasterService(OkHttpClient(), context)
        
        val sampleJson = """
        [
            {
                "token": "26000",
                "symbol": "NIFTY 50",
                "name": "NIFTY",
                "expiry": null,
                "strike": null,
                "lotsize": null,
                "instrumenttype": "AMXIDX",
                "exch_seg": "nse_cm",
                "tick_size": null
            },
            {
                "token": "2885",
                "symbol": "RELIANCE-EQ",
                "name": "RELIANCE",
                "expiry": "",
                "strike": -1.0,
                "lotsize": 1,
                "instrumenttype": "",
                "exch_seg": "NSE",
                "tick_size": 5.0
            },
            {
                "token": "45000",
                "symbol": "NIFTY28AUG2622000CE",
                "name": "NIFTY",
                "expiry": "28AUG2026",
                "strike": 2200000.0,
                "lotsize": 50,
                "instrumenttype": "OPTIDX",
                "exch_seg": "NFO",
                "tick_size": 5.0
            }
        ]
        """.trimIndent()

        val parseMethod = service.javaClass.getDeclaredMethod("parseInputStream", java.io.InputStream::class.java).apply {
            isAccessible = true
        }
        parseMethod.invoke(service, ByteArrayInputStream(sampleJson.toByteArray(Charsets.UTF_8)))

        val niftyToken = service.resolveAngelToken("NIFTY 50", "NSE")
        assertEquals("26000", niftyToken)

        val relianceToken = service.resolveAngelToken("RELIANCE", "NSE")
        assertEquals("2885", relianceToken)

        val opt = service.resolveOptionInstrument("NIFTY", "28AUG2026", 22000.0, "CE")
        assertNotNull(opt)
        assertEquals("45000", opt?.token)
    }
}
