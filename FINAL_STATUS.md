BUILD VERIFIED: PASS

REAL BROKER RUNTIME TEST: REQUIRES REAL DEVICE + VALID ANGEL ONE CREDENTIALS

To perform the real broker runtime test on a physical Android device:
1. Build the APK (`gradle :app:assembleDebug`) and install it on an Android device.
2. Launch the app and log in to Angel One securely using the SmartAPI settings.
3. Verify that the Dashboard connects and retrieves the Feed Token.
4. Navigate to Profile > Settings & Preferences > RUN LIVE DATA TEST (DIAGNOSTICS).
5. Ensure all diagnostics report "PASS" or "RESOLVED", and "First Real Tick" shows "YES".
6. Return to the Home screen and verify that NIFTY 50, BANKNIFTY, FINNIFTY, and MIDCPNIFTY display real-time, tick-by-tick prices, showing that the SmartAPI WebSocket V2 is securely connected and processing actual market data. 
