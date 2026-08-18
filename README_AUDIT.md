# Real Data Audit & Fix Report

## 1. Angel One Broker Integrations
- **Market Data**: Implemented official Angel One SmartAPI WebSocket (`wss://smartapisocket.angelone.in/smart-stream`) to replace the mock JWT-only check. 
- **Instrument Master**: Removed deterministic mock `hashCode()` generation from `InstrumentMapUtil`. Token updates dynamically map via `AngelOneBrokerService` using official SmartAPI endpoints.
- **Option Chain**: Deleted `OptionChainGenerator.kt` entirely. Option Chain data now strictly relies on `AngelOneBrokerService.getOptionChain()` without synthetic fallback.

## 2. Dhan Broker Integrations
- **Authentication**: Intact and unaltered. Real HTTP callbacks securely initialize Dhan access tokens.
- **Order Execution**: Verified real-data logic flow in `DhanBrokerService.placeOrder()` and `cancelOrder()`. Instrument IDs are mapped through `InstrumentMapUtil` and not mocked.
- **Portfolio & Holdings**: Dhan portfolio syncing uses real `api.getHoldings()` and `api.getPositions()` network requests.

## 3. Algo Trading Engine & AI Signals
- **Fake Logic Removal**: Removed simulated calculations (`changePct * 15`) and fixed the `processMarketFeed()` in `AlgoEngine.kt` to explicitly require real Option Chain and Technical Indicators inputs before generating any `AISignalEntity`.
- **Reason Generation**: Removed fake robust fallback reasons (e.g. `EMA 9 > EMA 20`, `Price above VWAP`) generated on the client side in `AISignalsScreen.kt`. Real technicals must be streamed natively from the server/broker to be rendered.
- **Underlying Value**: Removed mathematical fallback calculation (`signal.ltp * 150`) in `AISignalsScreen.kt`. Natively passes `signal.underlyingLtp`.
- **Algo P&L History**: Removed simulated static `PerformanceMetricBox` UI. P&L, Win Rate, trades, and drawdowns are now computed directly from `AlgoEngine.liveTradeHistory` dynamically instead of being hardcoded to 66.67% and ₹1,250.00.

## 4. UI Fixes
- Removed fallback static displays inside `AlgoScreen.kt` (`NO ACTIVE SIGNAL` correctly displays instead of a hardcoded `NIFTY 50 CE 22300`).

STATUS: NO SIMULATED FALLBACK FOUND IN ACTIVE MODULES.
