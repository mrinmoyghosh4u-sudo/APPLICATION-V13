module.exports = (req, res) => {
  const queryParams = new URLSearchParams(req.query).toString();
  const appDeepLink = `kingkhan://oauth${queryParams ? '?' + queryParams : ''}`;

  const hasState = Boolean(req.query?.state);
  const hasCode = Boolean(req.query?.code || req.query?.auth_code || req.query?.token);
  console.log(`[OAuth Callback] Processing redirect: hasState=${hasState}, hasCode=${hasCode}`);

  res.setHeader('Content-Type', 'text/html; charset=utf-8');
  res.status(200).send(`
    <!DOCTYPE html>
    <html lang="en">
    <head>
      <meta charset="UTF-8">
      <meta name="viewport" content="width=device-width, initial-scale=1.0">
      <title>Connecting to KingKhan App</title>
      <style>
        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #0B0E14; color: #F8FAFC; display: flex; justify-content: center; align-items: center; min-height: 100vh; margin: 0; text-align: center; }
        .card { background: #151C28; border: 1px solid #233044; border-radius: 16px; padding: 36px 24px; max-width: 400px; width: 90%; box-shadow: 0 12px 30px rgba(0,0,0,0.6); }
        .spinner { width: 48px; height: 48px; border: 4px solid #233044; border-top: 4px solid #00E676; border-radius: 50%; animation: spin 0.8s linear infinite; margin: 0 auto 20px; }
        @keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
        h2 { font-size: 20px; margin: 0 0 8px; color: #FFFFFF; font-weight: 700; }
        p { margin: 6px 0; font-size: 13px; color: #94A3B8; }
        .status { color: #00E676; font-weight: 600; font-size: 14px; margin-bottom: 16px; }
        .btn { display: inline-block; background: #00E676; color: #0B0E14; font-weight: 700; padding: 14px 28px; border-radius: 10px; text-decoration: none; margin-top: 18px; font-size: 14px; letter-spacing: 0.5px; }
      </style>
      <script>
        function openApp() {
          try {
            window.location.replace("${appDeepLink}");
          } catch(e) {
            window.location.href = "${appDeepLink}";
          }
          setTimeout(function() {
            window.location.href = "intent://oauth?${queryParams}#Intent;scheme=kingkhan;package=com.aistudio.kingkhanaitradepro.vtrqpx;end";
          }, 800);
        }
        window.onload = function() {
          openApp();
        };
      </script>
    </head>
    <body>
      <div class="card">
        <div class="spinner"></div>
        <h2>Authentication Complete!</h2>
        <div class="status">✓ Redirecting to KingKhan App...</div>
        <p>Your session has been verified and authorized.</p>
        <p>If the app does not open automatically, tap below:</p>
        <a class="btn" href="${appDeepLink}">OPEN KINGKHAN APP</a>
      </div>
    </body>
    </html>
  `);
};
