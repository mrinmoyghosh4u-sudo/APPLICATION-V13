const express = require('express');
const cors = require('cors');

const app = express();

app.use(cors());
app.use(express.json());

// Android App Links Verification Endpoint
app.get('/.well-known/assetlinks.json', (req, res) => {
  res.setHeader('Content-Type', 'application/json');
  res.json([
    {
      "relation": ["delegate_permission/common.handle_all_urls"],
      "target": {
        "namespace": "android_app",
        "package_name": "com.example",
        "sha256_cert_fingerprints": [
          "12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF:12:34:56:78:90:AB:CD:EF"
        ]
      }
    }
  ]);
});

// OAuth Callback Endpoint
app.get('/oauth', (req, res) => {
  const queryParams = new URLSearchParams(req.query).toString();
  const appDeepLink = `kingkhan://oauth${queryParams ? '?' + queryParams : ''}`;

  console.log(`OAuth Callback query received: ${queryParams}`);
  console.log(`Deep link redirect URL: ${appDeepLink}`);

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
});

const https = require('https');

// Token Exchange Endpoint
const handleTokenExchange = (req, res) => {
  let code = '';
  let redirectUri = '';

  if (req.method === 'POST') {
    code = req.body?.code || '';
    redirectUri = req.body?.redirect_uri || '';
  } else {
    code = req.query?.code || '';
    redirectUri = req.query?.redirect_uri || '';
  }

  code = (code || '').trim();
  redirectUri = (redirectUri || '').trim();

  if (!code) {
    return res.status(400).json({ error: "Missing authorization code" });
  }

  const clientId = (process.env.UPSTOX_API_KEY || '').trim();
  const clientSecret = (process.env.UPSTOX_API_SECRET || '').trim();
  const defaultRedirectUri = (process.env.UPSTOX_REDIRECT_URI || '').trim();
  
  const finalRedirectUri = redirectUri || defaultRedirectUri || "https://application-beige-psi.vercel.app/oauth";

  if (!clientId || !clientSecret) {
    return res.status(500).json({ error: "Server missing Upstox credentials configuration" });
  }

  const postData = new URLSearchParams({
    code: code,
    client_id: clientId,
    client_secret: clientSecret,
    redirect_uri: finalRedirectUri,
    grant_type: 'authorization_code'
  }).toString();

  const options = {
    hostname: 'api.upstox.com',
    port: 443,
    path: '/v2/login/authorization/token',
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      'Api-Version': '2.0',
      'Accept': 'application/json',
      'Content-Length': Buffer.byteLength(postData)
    }
  };

  const postReq = https.request(options, (postRes) => {
    let body = '';
    postRes.setEncoding('utf8');
    postRes.on('data', (chunk) => {
      body += chunk;
    });
    postRes.on('end', () => {
      res.setHeader('Content-Type', 'application/json');
      try {
        const jsonResponse = JSON.parse(body);
        res.status(postRes.statusCode).json(jsonResponse);
      } catch (e) {
        res.status(postRes.statusCode).send(body);
      }
    });
  });

  postReq.on('error', (e) => {
    console.error(`Token exchange error: ${e.message}`);
    res.status(500).json({ error: `Internal connection error: ${e.message}` });
  });

  postReq.write(postData);
  postReq.end();
};

app.all('/api/token-exchange', handleTokenExchange);
app.all('/token-exchange', handleTokenExchange);

app.get('/', (req, res) => {
  res.setHeader('Content-Type', 'text/html');
  res.status(200).send(`
    <!DOCTYPE html>
    <html>
    <head><title>KingKhan OAuth Server</title></head>
    <body style="font-family: sans-serif; background: #0F172A; color: #FFF; padding: 40px; text-align: center;">
      <h1>KingKhan OAuth Callback Server</h1>
      <p style="color: #00BFA5;">Status: Active & Ready</p>
      <p>Callback Endpoint: <code>https://application-beige-psi.vercel.app/oauth</code></p>
    </body>
    </html>
  `);
});

module.exports = app;

if (require.main === module) {
  const PORT = process.env.PORT || 3000;
  app.listen(PORT, () => {
    console.log(`Server listening on port ${PORT}`);
  });
}
