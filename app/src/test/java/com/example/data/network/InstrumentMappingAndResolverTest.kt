package com.example.data.network

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CanonicalInstrument
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import okhttp3.OkHttpClient

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InstrumentMappingAndResolverTest {

    private lateinit var context: Context
    private lateinit var service: InstrumentMasterService

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        service = InstrumentMasterService(OkHttpClient(), context)
    }

    @Test
    fun testExchangeNormalizationNcoToMcx() {
        assertEquals("MCX", InstrumentMasterService.normalizeExchange("NCO"))
        assertEquals("MCX", InstrumentMasterService.normalizeExchange("nco"))
        assertEquals("MCX", InstrumentMasterService.normalizeExchange("MCX"))
        assertEquals("MCX", InstrumentMasterService.normalizeExchange("mcx"))
        assertEquals("MCX", InstrumentMasterService.normalizeExchange("mcx_fo"))
        assertEquals("MCX", InstrumentMasterService.normalizeExchange("mcx_comm"))
        assertEquals("MCX", InstrumentMasterService.normalizeExchange("mcx_opt"))
        assertEquals("MCX", InstrumentMasterService.normalizeExchange("mcx_fut"))
    }

    @Test
    fun testExchangeNormalizationExistingExchanges() {
        assertEquals("NSE", InstrumentMasterService.normalizeExchange("NSE"))
        assertEquals("NSE", InstrumentMasterService.normalizeExchange("nse_cm"))
        assertEquals("NSE", InstrumentMasterService.normalizeExchange("nse-cm"))
        assertEquals("NSE", InstrumentMasterService.normalizeExchange("nse_eq"))

        assertEquals("BSE", InstrumentMasterService.normalizeExchange("BSE"))
        assertEquals("BSE", InstrumentMasterService.normalizeExchange("bse_cm"))
        assertEquals("BSE", InstrumentMasterService.normalizeExchange("bse-cm"))
        assertEquals("BSE", InstrumentMasterService.normalizeExchange("bse_eq"))

        assertEquals("NFO", InstrumentMasterService.normalizeExchange("NFO"))
        assertEquals("NFO", InstrumentMasterService.normalizeExchange("nse_fo"))
        assertEquals("NFO", InstrumentMasterService.normalizeExchange("nse_fno"))

        assertEquals("BFO", InstrumentMasterService.normalizeExchange("BFO"))
        assertEquals("BFO", InstrumentMasterService.normalizeExchange("bse_fo"))

        assertEquals("CDS", InstrumentMasterService.normalizeExchange("CDS"))
        assertEquals("NCDEX", InstrumentMasterService.normalizeExchange("NCDEX"))
    }

    @Test
    fun testCrudeoilAndNcoDerivativesResolution() = runBlocking {
        val sampleJson = """
        [
            {
                "token": "249861",
                "symbol": "CRUDEOIL24NOVFUT",
                "name": "CRUDEOIL",
                "expiry": "18NOV2024",
                "strike": -1.0,
                "lotsize": 100,
                "instrumenttype": "FUTCOM",
                "exch_seg": "mcx_fo",
                "tick_size": 1.0
            },
            {
                "token": "249862",
                "symbol": "CRUDEOIL24NOV6000CE",
                "name": "CRUDEOIL",
                "expiry": "18NOV2024",
                "strike": 600000.0,
                "lotsize": 100,
                "instrumenttype": "OPTCOM",
                "exch_seg": "NCO",
                "tick_size": 0.5
            },
            {
                "token": "249863",
                "symbol": "NATURALGAS24NOV200PE",
                "name": "NATURALGAS",
                "expiry": "25NOV2024",
                "strike": 20000.0,
                "lotsize": 1250,
                "instrumenttype": "OPTCOM",
                "exch_seg": "nco",
                "tick_size": 0.1
            }
        ]
        """.trimIndent()

        val parseMethod = service.javaClass.getDeclaredMethod("parseInputStream", java.io.InputStream::class.java).apply {
            isAccessible = true
        }
        parseMethod.invoke(service, ByteArrayInputStream(sampleJson.toByteArray(Charsets.UTF_8)))

        val crudeFutures = service.getInstrumentByToken("249861", 5) // 5 is MCX
        assertNotNull(crudeFutures)
        assertEquals("CRUDEOIL24NOVFUT", crudeFutures?.symbol)
        assertEquals("MCX", InstrumentMasterService.normalizeExchange(crudeFutures!!.exch_seg))

        val crudeOption = service.getInstrumentByToken("249862", 5)
        assertNotNull(crudeOption)
        assertEquals("CRUDEOIL24NOV6000CE", crudeOption?.symbol)
        // NCO exch_seg MUST normalize to MCX
        assertEquals("MCX", InstrumentMasterService.normalizeExchange(crudeOption!!.exch_seg))

        val ngOption = service.getInstrumentByToken("249863", 5)
        assertNotNull(ngOption)
        assertEquals("NATURALGAS24NOV200PE", ngOption?.symbol)
        assertEquals("MCX", InstrumentMasterService.normalizeExchange(ngOption!!.exch_seg))
    }
}
