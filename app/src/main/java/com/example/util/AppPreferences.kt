package com.example.util

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64

data class RiskSettings(
    val maxDailyLoss: Double = 10000.0,
    val maxDailyTrades: Int = 20,
    val maxPositionSize: Int = 1000,
    val maxOrderValue: Double = 200000.0,
    val stopLossRequired: Boolean = false,
    val riskPerTradePercent: Double = 2.0,
    val dailyLossWarningEnabled: Boolean = true,
    val enforceDailyLoss: Boolean = true,
    val enforceMaxTrades: Boolean = true,
    val enforceMaxOrderValue: Boolean = true
)

data class InstrumentOrderPreference(
    val exchange: String = "NSE",
    val instrument: String = "NIFTY",
    val defaultOrderType: String = "LIMIT",
    val defaultProductType: String = "INTRADAY",
    val defaultLots: Int = 1,
    val lotMultiplier: Int = 1,
    val stopLossType: String = "PERCENT",
    val stopLossValue: Double = 2.0,
    val autoSlEnabled: Boolean = false,
    val target1: Double = 5.0,
    val target2: Double = 10.0,
    val target3: Double = 15.0,
    val target4: Double = 20.0,
    val trailingSlEnabled: Boolean = false,
    val trailingSlType: String = "POINTS",
    val trailingSlValue: Double = 5.0
)

data class OrderPreferences(
    val confirmBeforeOrder: Boolean = true,
    val confirmBeforeSquareOff: Boolean = true,
    val confirmBeforeModify: Boolean = false,
    val confirmBeforeCancel: Boolean = false,
    val duplicateOrderProtection: Boolean = true,
    val orderRetryProtection: Boolean = true,
    val applyToAllInstruments: Boolean = false,
    val instrumentPreferences: Map<String, InstrumentOrderPreference> = emptyMap(),
    // Backward compatibility getters
    val defaultOrderType: String = "LIMIT",
    val defaultQuantity: Int = 65,
    val defaultProductType: String = "INTRADAY",
    val defaultStopLossPercent: Double = 2.0
)

data class NotificationSettings(
    val notifyOrderUpdates: Boolean = true,
    val notifyPositionUpdates: Boolean = true,
    val notifyPnlAlerts: Boolean = true,
    val notifyRiskAlerts: Boolean = true,
    val notifyMarketStatus: Boolean = true,
    val notifyBrokerAlerts: Boolean = true
)

data class AlertPreferences(
    val brokerConnected: Boolean = true,
    val brokerDisconnected: Boolean = true,
    val buyCeSignal: Boolean = true,
    val buyPeSignal: Boolean = true,
    val entryPosition: Boolean = true,
    val stopLossHit: Boolean = true,
    val target1Hit: Boolean = true,
    val target2Hit: Boolean = true,
    val target3Hit: Boolean = true,
    val target4Hit: Boolean = true,
    val trailingSlHit: Boolean = true,
    val orderExecuted: Boolean = true,
    val orderRejected: Boolean = true,
    val algoStarted: Boolean = true,
    val algoStopped: Boolean = true,
    val riskLimitReached: Boolean = true,
    val marketNews: Boolean = true
)

data class AiSignalSettings(
    val selectedIndex: String = "NIFTY 50",
    val selectedTimeframe: String = "5 MIN",
    val emaEnabled: Boolean = true,
    val vwapEnabled: Boolean = true,
    val rsiEnabled: Boolean = true,
    val supertrendEnabled: Boolean = true,
    val oiEnabled: Boolean = true,
    val volumeEnabled: Boolean = true,
    val confidenceThreshold: Float = 0.65f
)

data class LotSizeSettings(
    val nifty: Int = 65,
    val banknifty: Int = 30,
    val finnifty: Int = 60,
    val midcpnifty: Int = 120,
    val sensex: Int = 20,
    val bankex: Int = 30,
    val crudeoil: Int = 100,
    val crudeoilm: Int = 10,
    val gold: Int = 100,
    val goldm: Int = 10,
    val silver: Int = 30,
    val silverm: Int = 5,
    val copper: Int = 2500,
    val copperm: Int = 250,
    val naturalgas: Int = 1250
)

class AppPreferences(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("king_khan_app_prefs", Context.MODE_PRIVATE)

    init {
        INSTANCE = this
    }

    // Disclaimer / Terms Acceptance
    fun isDisclaimerAccepted(): Boolean = prefs.getBoolean("disclaimer_accepted", false)
    fun setDisclaimerAccepted(accepted: Boolean) {
        prefs.edit().putBoolean("disclaimer_accepted", accepted).apply()
    }

    // Security & Biometric
    fun isBiometricEnabled(): Boolean = prefs.getBoolean("biometric_enabled", false)
    fun setBiometricEnabled(enabled: Boolean) {
        val editor = prefs.edit().putBoolean("biometric_enabled", enabled)
        if (enabled) {
            editor.putBoolean("app_lock_enabled", true)
        }
        editor.apply()
    }

    fun hasPasscode(): Boolean = prefs.getString("passcode_hash", "").orEmpty().isNotBlank()

    fun setPasscode(pin: String): Boolean {
        if (pin.length < 4 || pin.length > 6) return false
        val salt = generateSalt()
        val hash = hashPin(pin, salt)
        prefs.edit()
            .putString("passcode_salt", salt)
            .putString("passcode_hash", hash)
            .putBoolean("app_lock_enabled", true)
            .apply()
        return true
    }

    fun verifyPasscode(pin: String): Boolean {
        val salt = prefs.getString("passcode_salt", "") ?: return false
        val storedHash = prefs.getString("passcode_hash", "") ?: return false
        if (salt.isBlank() || storedHash.isBlank()) return false
        val hash = hashPin(pin, salt)
        return hash == storedHash
    }

    fun disablePasscode() {
        prefs.edit()
            .remove("passcode_salt")
            .remove("passcode_hash")
            .putBoolean("biometric_enabled", false)
            .putBoolean("app_lock_enabled", false)
            .apply()
    }

    fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("app_lock_enabled", enabled).apply()
    }

    fun isAppLockEnabled(): Boolean {
        val explicitLock = prefs.getBoolean("app_lock_enabled", true)
        return explicitLock && (hasPasscode() || isBiometricEnabled())
    }

    private fun generateSalt(): String {
        val random = SecureRandom()
        val saltBytes = ByteArray(16)
        random.nextBytes(saltBytes)
        return Base64.encodeToString(saltBytes, Base64.NO_WRAP)
    }

    private fun hashPin(pin: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val combined = "$salt:$pin".toByteArray(Charsets.UTF_8)
        val digest = md.digest(combined)
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }

    // Risk Settings
    fun getRiskSettings(): RiskSettings {
        return RiskSettings(
            maxDailyLoss = prefs.getFloat("risk_max_daily_loss", 10000.0f).toDouble(),
            maxDailyTrades = prefs.getInt("risk_max_daily_trades", 20),
            maxPositionSize = prefs.getInt("risk_max_position_size", 1000),
            maxOrderValue = prefs.getFloat("risk_max_order_value", 200000.0f).toDouble(),
            stopLossRequired = prefs.getBoolean("risk_sl_required", false),
            riskPerTradePercent = prefs.getFloat("risk_per_trade_pct", 2.0f).toDouble(),
            dailyLossWarningEnabled = prefs.getBoolean("risk_loss_warning_enabled", true),
            enforceDailyLoss = prefs.getBoolean("risk_enforce_daily_loss", true),
            enforceMaxTrades = prefs.getBoolean("risk_enforce_max_trades", true),
            enforceMaxOrderValue = prefs.getBoolean("risk_enforce_max_order_val", true)
        )
    }

    fun saveRiskSettings(settings: RiskSettings) {
        prefs.edit()
            .putFloat("risk_max_daily_loss", settings.maxDailyLoss.toFloat())
            .putInt("risk_max_daily_trades", settings.maxDailyTrades)
            .putInt("risk_max_position_size", settings.maxPositionSize)
            .putFloat("risk_max_order_value", settings.maxOrderValue.toFloat())
            .putBoolean("risk_sl_required", settings.stopLossRequired)
            .putFloat("risk_per_trade_pct", settings.riskPerTradePercent.toFloat())
            .putBoolean("risk_loss_warning_enabled", settings.dailyLossWarningEnabled)
            .putBoolean("risk_enforce_daily_loss", settings.enforceDailyLoss)
            .putBoolean("risk_enforce_max_trades", settings.enforceMaxTrades)
            .putBoolean("risk_enforce_max_order_val", settings.enforceMaxOrderValue)
            .apply()
    }

    // Order Preferences
    val supportedInstrumentsList = listOf(
        Pair("NSE", "NIFTY"),
        Pair("NSE", "BANKNIFTY"),
        Pair("NSE", "FINNIFTY"),
        Pair("NSE", "MIDCPNIFTY"),
        Pair("BSE", "SENSEX"),
        Pair("BSE", "BANKEX"),
        Pair("MCX", "CRUDEOIL"),
        Pair("MCX", "CRUDEOILM")
    )

    fun getInstrumentPreference(instrumentKey: String): InstrumentOrderPreference {
        val normalized = normalizeInstrumentKey(instrumentKey)
        val exch = getExchangeForInstrument(normalized)
        
        val type = prefs.getString("ord_pref_${normalized}_type", "LIMIT") ?: "LIMIT"
        val prod = prefs.getString("ord_pref_${normalized}_prod", "INTRADAY") ?: "INTRADAY"
        val lots = prefs.getInt("ord_pref_${normalized}_lots", 1)
        val mult = prefs.getInt("ord_pref_${normalized}_mult", 1)
        val slType = prefs.getString("ord_pref_${normalized}_sl_type", "PERCENT") ?: "PERCENT"
        val slVal = prefs.getFloat("ord_pref_${normalized}_sl_val", 2.0f).toDouble()
        val autoSl = prefs.getBoolean("ord_pref_${normalized}_auto_sl", false)
        val t1 = prefs.getFloat("ord_pref_${normalized}_t1", 5.0f).toDouble()
        val t2 = prefs.getFloat("ord_pref_${normalized}_t2", 10.0f).toDouble()
        val t3 = prefs.getFloat("ord_pref_${normalized}_t3", 15.0f).toDouble()
        val t4 = prefs.getFloat("ord_pref_${normalized}_t4", 20.0f).toDouble()
        val tslEnabled = prefs.getBoolean("ord_pref_${normalized}_tsl_enabled", false)
        val tslType = prefs.getString("ord_pref_${normalized}_tsl_type", "POINTS") ?: "POINTS"
        val tslVal = prefs.getFloat("ord_pref_${normalized}_tsl_val", 5.0f).toDouble()

        return InstrumentOrderPreference(
            exchange = exch,
            instrument = normalized,
            defaultOrderType = type,
            defaultProductType = prod,
            defaultLots = lots,
            lotMultiplier = mult,
            stopLossType = slType,
            stopLossValue = slVal,
            autoSlEnabled = autoSl,
            target1 = t1,
            target2 = t2,
            target3 = t3,
            target4 = t4,
            trailingSlEnabled = tslEnabled,
            trailingSlType = tslType,
            trailingSlValue = tslVal
        )
    }

    fun saveInstrumentPreference(pref: InstrumentOrderPreference) {
        val key = normalizeInstrumentKey(pref.instrument)
        prefs.edit()
            .putString("ord_pref_${key}_type", pref.defaultOrderType)
            .putString("ord_pref_${key}_prod", pref.defaultProductType)
            .putInt("ord_pref_${key}_lots", pref.defaultLots)
            .putInt("ord_pref_${key}_mult", pref.lotMultiplier)
            .putString("ord_pref_${key}_sl_type", pref.stopLossType)
            .putFloat("ord_pref_${key}_sl_val", pref.stopLossValue.toFloat())
            .putBoolean("ord_pref_${key}_auto_sl", pref.autoSlEnabled)
            .putFloat("ord_pref_${key}_t1", pref.target1.toFloat())
            .putFloat("ord_pref_${key}_t2", pref.target2.toFloat())
            .putFloat("ord_pref_${key}_t3", pref.target3.toFloat())
            .putFloat("ord_pref_${key}_t4", pref.target4.toFloat())
            .putBoolean("ord_pref_${key}_tsl_enabled", pref.trailingSlEnabled)
            .putString("ord_pref_${key}_tsl_type", pref.trailingSlType)
            .putFloat("ord_pref_${key}_tsl_val", pref.trailingSlValue.toFloat())
            .apply()
    }

    fun getOrderPreferences(): OrderPreferences {
        val map = supportedInstrumentsList.associate { (_, instr) ->
            instr to getInstrumentPreference(instr)
        }
        val niftyPref = map["NIFTY"] ?: InstrumentOrderPreference()

        return OrderPreferences(
            confirmBeforeOrder = prefs.getBoolean("order_pref_confirm_order", true),
            confirmBeforeSquareOff = prefs.getBoolean("order_pref_confirm_squareoff", true),
            confirmBeforeModify = prefs.getBoolean("order_pref_confirm_modify", false),
            confirmBeforeCancel = prefs.getBoolean("order_pref_confirm_cancel", false),
            duplicateOrderProtection = prefs.getBoolean("order_pref_duplicate_protection", true),
            orderRetryProtection = prefs.getBoolean("order_pref_retry_protection", true),
            applyToAllInstruments = prefs.getBoolean("order_pref_apply_to_all", false),
            instrumentPreferences = map,
            defaultOrderType = niftyPref.defaultOrderType,
            defaultQuantity = getLotSizeForSymbol("NIFTY") * niftyPref.defaultLots * niftyPref.lotMultiplier,
            defaultProductType = niftyPref.defaultProductType,
            defaultStopLossPercent = niftyPref.stopLossValue
        )
    }

    fun saveOrderPreferences(prefsObj: OrderPreferences) {
        val editor = prefs.edit()
            .putBoolean("order_pref_confirm_order", prefsObj.confirmBeforeOrder)
            .putBoolean("order_pref_confirm_squareoff", prefsObj.confirmBeforeSquareOff)
            .putBoolean("order_pref_confirm_modify", prefsObj.confirmBeforeModify)
            .putBoolean("order_pref_confirm_cancel", prefsObj.confirmBeforeCancel)
            .putBoolean("order_pref_duplicate_protection", prefsObj.duplicateOrderProtection)
            .putBoolean("order_pref_retry_protection", prefsObj.orderRetryProtection)
            .putBoolean("order_pref_apply_to_all", prefsObj.applyToAllInstruments)
        
        editor.apply()

        prefsObj.instrumentPreferences.values.forEach { instPref ->
            saveInstrumentPreference(instPref)
        }
    }

    private fun normalizeInstrumentKey(raw: String): String {
        val upper = raw.uppercase()
        return when {
            upper.contains("BANKNIFTY") -> "BANKNIFTY"
            upper.contains("FINNIFTY") -> "FINNIFTY"
            upper.contains("MIDCPNIFTY") || upper.contains("MIDCAP") -> "MIDCPNIFTY"
            upper.contains("NIFTY") -> "NIFTY"
            upper.contains("BANKEX") -> "BANKEX"
            upper.contains("SENSEX") -> "SENSEX"
            upper.contains("CRUDEOILM") || upper.contains("CRUDE OIL M") -> "CRUDEOILM"
            upper.contains("CRUDEOIL") || upper.contains("CRUDE OIL") -> "CRUDEOIL"
            else -> "NIFTY"
        }
    }

    fun getExchangeForInstrument(instrumentKey: String): String {
        val key = normalizeInstrumentKey(instrumentKey)
        return when (key) {
            "SENSEX", "BANKEX" -> "BSE"
            "CRUDEOIL", "CRUDEOILM" -> "MCX"
            else -> "NSE"
        }
    }

    fun isTelegramEnabled(): Boolean = prefs.getBoolean("telegram_alerts_enabled", true)
    fun setTelegramEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("telegram_alerts_enabled", enabled).apply()
    }

    // Notification Settings
    fun getNotificationSettings(): NotificationSettings {
        return NotificationSettings(
            notifyOrderUpdates = prefs.getBoolean("notif_order_updates", true),
            notifyPositionUpdates = prefs.getBoolean("notif_position_updates", true),
            notifyPnlAlerts = prefs.getBoolean("notif_pnl_alerts", true),
            notifyRiskAlerts = prefs.getBoolean("notif_risk_alerts", true),
            notifyMarketStatus = prefs.getBoolean("notif_market_status", true),
            notifyBrokerAlerts = prefs.getBoolean("notif_broker_alerts", true)
        )
    }

    fun saveNotificationSettings(settings: NotificationSettings) {
        prefs.edit()
            .putBoolean("notif_order_updates", settings.notifyOrderUpdates)
            .putBoolean("notif_position_updates", settings.notifyPositionUpdates)
            .putBoolean("notif_pnl_alerts", settings.notifyPnlAlerts)
            .putBoolean("notif_risk_alerts", settings.notifyRiskAlerts)
            .putBoolean("notif_market_status", settings.notifyMarketStatus)
            .putBoolean("notif_broker_alerts", settings.notifyBrokerAlerts)
            .apply()
    }

    // Alert Preferences
    fun getAlertPreferences(): AlertPreferences {
        return AlertPreferences(
            brokerConnected = prefs.getBoolean("alert_broker_connected", true),
            brokerDisconnected = prefs.getBoolean("alert_broker_disconnected", true),
            buyCeSignal = prefs.getBoolean("alert_buy_ce", true),
            buyPeSignal = prefs.getBoolean("alert_buy_pe", true),
            entryPosition = prefs.getBoolean("alert_entry_pos", true),
            stopLossHit = prefs.getBoolean("alert_sl_hit", true),
            target1Hit = prefs.getBoolean("alert_t1_hit", true),
            target2Hit = prefs.getBoolean("alert_t2_hit", true),
            target3Hit = prefs.getBoolean("alert_t3_hit", true),
            target4Hit = prefs.getBoolean("alert_t4_hit", true),
            trailingSlHit = prefs.getBoolean("alert_tsl_hit", true),
            orderExecuted = prefs.getBoolean("alert_order_executed", true),
            orderRejected = prefs.getBoolean("alert_order_rejected", true),
            algoStarted = prefs.getBoolean("alert_algo_started", true),
            algoStopped = prefs.getBoolean("alert_algo_stopped", true),
            riskLimitReached = prefs.getBoolean("alert_risk_reached", true)
        )
    }

    fun saveAlertPreferences(settings: AlertPreferences) {
        prefs.edit()
            .putBoolean("alert_broker_connected", settings.brokerConnected)
            .putBoolean("alert_broker_disconnected", settings.brokerDisconnected)
            .putBoolean("alert_buy_ce", settings.buyCeSignal)
            .putBoolean("alert_buy_pe", settings.buyPeSignal)
            .putBoolean("alert_entry_pos", settings.entryPosition)
            .putBoolean("alert_sl_hit", settings.stopLossHit)
            .putBoolean("alert_t1_hit", settings.target1Hit)
            .putBoolean("alert_t2_hit", settings.target2Hit)
            .putBoolean("alert_t3_hit", settings.target3Hit)
            .putBoolean("alert_t4_hit", settings.target4Hit)
            .putBoolean("alert_tsl_hit", settings.trailingSlHit)
            .putBoolean("alert_order_executed", settings.orderExecuted)
            .putBoolean("alert_order_rejected", settings.orderRejected)
            .putBoolean("alert_algo_started", settings.algoStarted)
            .putBoolean("alert_algo_stopped", settings.algoStopped)
            .putBoolean("alert_risk_reached", settings.riskLimitReached)
            .apply()
    }

    // SMS Alert Settings
    fun isSmsAlertsEnabled(): Boolean = prefs.getBoolean("sms_alerts_enabled", false)
    fun setSmsAlertsEnabled(enabled: Boolean) = prefs.edit().putBoolean("sms_alerts_enabled", enabled).apply()

    fun getSmsAlertPhone(): String = prefs.getString("sms_alert_phone", "") ?: ""
    fun setSmsAlertPhone(phone: String) = prefs.edit().putString("sms_alert_phone", phone.trim()).apply()

    fun getSmsGatewayUrl(): String = prefs.getString("sms_gateway_url", "") ?: ""
    fun setSmsGatewayUrl(url: String) = prefs.edit().putString("sms_gateway_url", url.trim()).apply()

    fun getSmsApiKey(): String = prefs.getString("sms_api_key", "") ?: ""
    fun setSmsApiKey(apiKey: String) = prefs.edit().putString("sms_api_key", apiKey.trim()).apply()

    // AI Signal Settings
    fun getAiSignalSettings(): AiSignalSettings {
        return AiSignalSettings(
            selectedIndex = prefs.getString("ai_index", "NIFTY 50") ?: "NIFTY 50",
            selectedTimeframe = prefs.getString("ai_timeframe", "5 MIN") ?: "5 MIN",
            emaEnabled = prefs.getBoolean("ai_ema", true),
            vwapEnabled = prefs.getBoolean("ai_vwap", true),
            rsiEnabled = prefs.getBoolean("ai_rsi", true),
            supertrendEnabled = prefs.getBoolean("ai_supertrend", true),
            oiEnabled = prefs.getBoolean("ai_oi", true),
            volumeEnabled = prefs.getBoolean("ai_volume", true),
            confidenceThreshold = prefs.getFloat("ai_confidence", 0.65f)
        )
    }

    fun saveAiSignalSettings(settings: AiSignalSettings) {
        prefs.edit()
            .putString("ai_index", settings.selectedIndex)
            .putString("ai_timeframe", settings.selectedTimeframe)
            .putBoolean("ai_ema", settings.emaEnabled)
            .putBoolean("ai_vwap", settings.vwapEnabled)
            .putBoolean("ai_rsi", settings.rsiEnabled)
            .putBoolean("ai_supertrend", settings.supertrendEnabled)
            .putBoolean("ai_oi", settings.oiEnabled)
            .putBoolean("ai_volume", settings.volumeEnabled)
            .putFloat("ai_confidence", settings.confidenceThreshold)
            .apply()
    }

    // Lot Size Settings
    fun getLotSizeSettings(): LotSizeSettings {
        return LotSizeSettings(
            nifty = prefs.getInt("lot_nifty", 65),
            banknifty = prefs.getInt("lot_banknifty", 30),
            finnifty = prefs.getInt("lot_finnifty", 60),
            midcpnifty = prefs.getInt("lot_midcpnifty", 120),
            sensex = prefs.getInt("lot_sensex", 20),
            bankex = prefs.getInt("lot_bankex", 30),
            crudeoil = prefs.getInt("lot_crudeoil", 100),
            crudeoilm = prefs.getInt("lot_crudeoilm", 10),
            gold = prefs.getInt("lot_gold", 100),
            goldm = prefs.getInt("lot_goldm", 10),
            silver = prefs.getInt("lot_silver", 30),
            silverm = prefs.getInt("lot_silverm", 5),
            copper = prefs.getInt("lot_copper", 2500),
            copperm = prefs.getInt("lot_copperm", 250),
            naturalgas = prefs.getInt("lot_naturalgas", 1250)
        )
    }

    fun saveLotSizeSettings(settings: LotSizeSettings) {
        prefs.edit()
            .putInt("lot_nifty", settings.nifty)
            .putInt("lot_banknifty", settings.banknifty)
            .putInt("lot_finnifty", settings.finnifty)
            .putInt("lot_midcpnifty", settings.midcpnifty)
            .putInt("lot_sensex", settings.sensex)
            .putInt("lot_bankex", settings.bankex)
            .putInt("lot_crudeoil", settings.crudeoil)
            .putInt("lot_crudeoilm", settings.crudeoilm)
            .putInt("lot_gold", settings.gold)
            .putInt("lot_goldm", settings.goldm)
            .putInt("lot_silver", settings.silver)
            .putInt("lot_silverm", settings.silverm)
            .putInt("lot_copper", settings.copper)
            .putInt("lot_copperm", settings.copperm)
            .putInt("lot_naturalgas", settings.naturalgas)
            .apply()
    }

    fun getLotSizeForSymbol(symbol: String): Int {
        val upper = symbol.uppercase()
        val settings = getLotSizeSettings()
        return when {
            upper.contains("BANKNIFTY") -> settings.banknifty
            upper.contains("FINNIFTY") -> settings.finnifty
            upper.contains("MIDCPNIFTY") || upper.contains("MIDCAP") -> settings.midcpnifty
            upper.contains("NIFTY") -> settings.nifty
            upper.contains("BANKEX") -> settings.bankex
            upper.contains("SENSEX") -> settings.sensex
            upper.contains("CRUDEOILM") || upper.contains("CRUDE OIL M") -> settings.crudeoilm
            upper.contains("CRUDEOIL") || upper.contains("CRUDE OIL") -> settings.crudeoil
            upper.contains("GOLDM") || upper.contains("GOLD M") -> settings.goldm
            upper.contains("GOLD") -> settings.gold
            upper.contains("SILVERM") || upper.contains("SILVER M") || upper.contains("SILVERMIC") -> settings.silverm
            upper.contains("SILVER") -> settings.silver
            upper.contains("COPPERM") || upper.contains("COPPER M") -> settings.copperm
            upper.contains("COPPER") -> settings.copper
            upper.contains("NATURALGAS") || upper.contains("NATURAL GAS") -> settings.naturalgas
            else -> 1
        }
    }

    fun isAutoCheckUpdateEnabled(): Boolean {
        return prefs.getBoolean("auto_check_update_enabled", true)
    }

    fun setAutoCheckUpdateEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_check_update_enabled", enabled).apply()
    }

    fun getLastInstalledVersion(): String {
        return prefs.getString("last_installed_version_name", "").orEmpty()
    }

    fun setLastInstalledVersion(version: String) {
        prefs.edit().putString("last_installed_version_name", version).apply()
    }

    fun getPendingUpdateVersion(): String {
        return prefs.getString("pending_update_version_name", "").orEmpty()
    }

    fun setPendingUpdateVersion(version: String) {
        prefs.edit().putString("pending_update_version_name", version).apply()
    }

    fun clearPendingUpdateVersion() {
        prefs.edit().remove("pending_update_version_name").apply()
    }

    fun getGithubTokenRaw(): String {
        return prefs.getString("github_pat_token", "").orEmpty()
    }

    fun clearLegacyGithubToken() {
        prefs.edit().remove("github_pat_token").apply()
    }

    fun getGithubToken(): String {
        return SecureTokenManager.getInstance(context).getGithubToken()
    }

    fun setGithubToken(token: String) {
        SecureTokenManager.getInstance(context).saveGithubToken(token)
    }

    fun clearGithubToken() {
        SecureTokenManager.getInstance(context).clearGithubToken()
    }

    companion object {
        @Volatile
        private var INSTANCE: AppPreferences? = null

        fun getInstance(context: Context): AppPreferences {
            return INSTANCE ?: synchronized(this) {
                val instance = AppPreferences(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }

        fun getGlobalLotSize(symbol: String): Int {
            val instMasterLot = com.example.data.network.InstrumentMasterService.instance?.getLotSizeForSymbol(symbol) ?: 0
            if (instMasterLot > 0) return instMasterLot
            return INSTANCE?.getLotSizeForSymbol(symbol) ?: getFallbackLotSize(symbol)
        }

        fun getFallbackLotSize(symbol: String): Int {
            val instMasterLot = com.example.data.network.InstrumentMasterService.instance?.getLotSizeForSymbol(symbol) ?: 0
            if (instMasterLot > 0) return instMasterLot
            val upper = symbol.uppercase()
            return when {
                upper.contains("BANKNIFTY") -> 30
                upper.contains("FINNIFTY") -> 60
                upper.contains("MIDCPNIFTY") || upper.contains("MIDCAP") -> 120
                upper.contains("NIFTY") -> 65
                upper.contains("BANKEX") -> 30
                upper.contains("SENSEX") -> 20
                upper.contains("CRUDEOILM") || upper.contains("CRUDEOIL M") || upper.contains("CRUDE OIL M") -> 10
                upper.contains("CRUDEOIL") || upper.contains("CRUDE OIL") -> 100
                upper.contains("NATURALGASM") || upper.contains("NATURALGAS M") || upper.contains("NATURAL GAS M") -> 250
                upper.contains("NATURALGAS") || upper.contains("NATURAL GAS") -> 1250
                upper.contains("GOLD GUINEA") -> 1
                upper.contains("GOLD PETAL") -> 1
                upper.contains("GOLDM") || upper.contains("GOLD M") -> 10
                upper.contains("GOLD") -> 100
                upper.contains("SILVER MIC") -> 1
                upper.contains("SILVERM") || upper.contains("SILVER M") -> 5
                upper.contains("SILVER") -> 30
                upper.contains("COPPERM") || upper.contains("COPPER M") -> 250
                upper.contains("COPPER") -> 2500
                upper.contains("ZINCM") || upper.contains("ZINC M") -> 1000
                upper.contains("ZINC") -> 5000
                upper.contains("ALUMINIUMM") || upper.contains("ALUMINIUM M") -> 1000
                upper.contains("ALUMINIUM") -> 5000
                upper.contains("LEADM") || upper.contains("LEAD M") -> 1000
                upper.contains("LEAD") -> 5000
                upper.contains("NICKEL") -> 1500
                upper.contains("BULLDEX") -> 50
                upper.contains("METLDEX") -> 50
                upper.contains("ENRGDEX") -> 125
                else -> 1
            }
        }
    }
}
