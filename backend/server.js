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
const crypto = require('crypto');

// In-memory set of consumed auth codes to guarantee idempotency and prevent duplicate upstream exchange
const consumedCodes = new Map();

function isCodeConsumed(code) {
  const clean = (code || '').trim();
  if (!clean) return true;
  const entry = consumedCodes.get(clean);
  if (!entry) return false;
  // Expire after 10 minutes
  if (Date.now() - entry > 10 * 60 * 1000) {
    consumedCodes.delete(clean);
    return false;
  }
  return true;
}

function markCodeConsumed(code) {
  const clean = (code || '').trim();
  if (clean) {
    consumedCodes.set(clean, Date.now());
  }
}

// Token Exchange Endpoint
const handleTokenExchange = (req, res) => {
  let code = '';
  let redirectUri = '';

  if (req.method === 'POST') {
    code = req.body?.code || req.body?.auth_code || '';
    redirectUri = req.body?.redirect_uri || '';
  } else {
    code = req.query?.code || req.query?.auth_code || '';
    redirectUri = req.query?.redirect_uri || '';
  }

  code = (code || '').trim();
  redirectUri = (redirectUri || '').trim();

  if (!code) {
    return res.status(400).json({ status: "error", error: "Missing authorization code" });
  }

  if (isCodeConsumed(code)) {
    return res.status(400).json({
      status: "error",
      error: "AUTH_CODE_ALREADY_USED: This authorization code has already been exchanged or expired. A new login is required."
    });
  }

  markCodeConsumed(code);

  const clientId = (req.query?.client_id || req.query?.apiKey || req.body?.client_id || req.body?.apiKey || process.env.UPSTOX_API_KEY || '').trim();
  const clientSecret = (req.query?.client_secret || req.query?.apiSecret || req.body?.client_secret || req.body?.apiSecret || process.env.UPSTOX_API_SECRET || '').trim();
  const defaultRedirectUri = (process.env.UPSTOX_REDIRECT_URI || '').trim();
  
  const finalRedirectUri = redirectUri || defaultRedirectUri || "https://application-beige-psi.vercel.app/oauth";

  if (!clientId || !clientSecret) {
    return res.status(500).json({
      status: "error",
      error: "Server missing Upstox credentials configuration. UPSTOX_API_KEY and UPSTOX_API_SECRET must be configured."
    });
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
    res.status(500).json({ status: "error", error: `Internal connection error: ${e.message}` });
  });

  postReq.write(postData);
  postReq.end();
};

app.all('/api/token-exchange', handleTokenExchange);
app.all('/token-exchange', handleTokenExchange);

// FYERS Token Exchange Endpoint
const handleFyersTokenExchange = (req, res) => {
  let code = '';
  let redirectUri = '';

  if (req.method === 'POST') {
    code = req.body?.code || req.body?.auth_code || '';
    redirectUri = req.body?.redirect_uri || '';
  } else {
    code = req.query?.code || req.query?.auth_code || '';
    redirectUri = req.query?.redirect_uri || '';
  }

  code = (code || '').trim();
  redirectUri = (redirectUri || '').trim();

  if (!code) {
    return res.status(400).json({ s: "error", code: 400, message: "Missing authorization code" });
  }

  if (isCodeConsumed(code)) {
    return res.status(400).json({
      s: "error",
      code: 400,
      message: "AUTH_CODE_ALREADY_USED: This authorization code has already been exchanged or expired. A new login is required."
    });
  }

  markCodeConsumed(code);

  let appId = (req.query?.app_id || req.query?.appId || req.query?.client_id || req.body?.app_id || req.body?.appId || req.body?.client_id || process.env.FYERS_APP_ID || '').trim();
  const secretId = (req.query?.secret_id || req.query?.secretId || req.query?.client_secret || req.body?.secret_id || req.body?.secretId || req.body?.client_secret || process.env.FYERS_SECRET_ID || '').trim();

  if (!appId || !secretId) {
    return res.status(500).json({ s: "error", code: 500, message: "Server missing FYERS credentials configuration (FYERS_APP_ID / FYERS_SECRET_ID)" });
  }

  if (!appId.endsWith('-100')) {
    appId = `${appId}-100`;
  }

  const hashInput = `${appId}:${secretId}`;
  const appIdHash = crypto.createHash('sha256').update(hashInput).digest('hex');

  const postPayload = JSON.stringify({
    grant_type: 'authorization_code',
    appIdHash: appIdHash,
    code: code
  });

  const options = {
    hostname: 'api-t1.fyers.in',
    port: 443,
    path: '/api/v3/validate-authcode',
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      'Content-Length': Buffer.byteLength(postPayload)
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
    console.error(`FYERS Token exchange error: ${e.message}`);
    res.status(500).json({ s: "error", code: 500, message: `Internal connection error: ${e.message}` });
  });

  postReq.write(postPayload);
  postReq.end();
};

app.all('/api/fyers-token-exchange', handleFyersTokenExchange);
app.all('/fyers-token-exchange', handleFyersTokenExchange);

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
