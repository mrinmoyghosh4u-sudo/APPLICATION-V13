module.exports = (req, res) => {
  const queryParams = new URLSearchParams(req.query).toString();
  const appDeepLink = `kingkhan://oauth${queryParams ? '?' + queryParams : ''}`;

  res.setHeader('Content-Type', 'text/html');
  res.status(200).send(`
    <!DOCTYPE html>
    <html lang="en">
    <head>
      <meta charset="UTF-8">
      <meta name="viewport" content="width=device-width, initial-scale=1.0">
      <title>OAuth Authentication - KingKhan App</title>
      <style>
        body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: #0F172A; color: #F8FAFC; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; text-align: center; }
        .card { background: #1E293B; border: 1px solid #334155; border-radius: 12px; padding: 32px; max-width: 420px; box-shadow: 0 10px 25px rgba(0,0,0,0.5); }
        .btn { display: inline-block; background: #00BFA5; color: #0F172A; font-weight: bold; padding: 14px 28px; border-radius: 8px; text-decoration: none; margin-top: 20px; font-size: 16px; }
        .status { color: #00BFA5; font-weight: 600; }
      </style>
      <script>
        setTimeout(function() {
          window.location.href = "${appDeepLink}";
        }, 300);
      </script>
    </head>
    <body>
      <div class="card">
        <h2>Authentication Complete!</h2>
        <p class="status">Redirecting to KingKhan App...</p>
        <p>If the app does not open automatically, tap below:</p>
        <a class="btn" href="${appDeepLink}">Open App Manually</a>
      </div>
    </body>
    </html>
  `);
};
