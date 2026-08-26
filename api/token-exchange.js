const https = require('https');

// In-memory cache of successful token exchanges (code -> token response) to handle retries seamlessly
const tokenCache = new Map();

function getCachedToken(code) {
  const clean = (code || '').trim();
  if (!clean) return null;
  const entry = tokenCache.get(clean);
  if (!entry) return null;
  // Expire after 10 minutes
  if (Date.now() - entry.timestamp > 10 * 60 * 1000) {
    tokenCache.delete(clean);
    return null;
  }
  return entry.response;
}

function cacheToken(code, response) {
  const clean = (code || '').trim();
  if (clean && response) {
    tokenCache.set(clean, { timestamp: Date.now(), response });
  }
}

function parseRequestBody(req) {
  return new Promise((resolve) => {
    if (req.body && typeof req.body === 'object') {
      return resolve(req.body);
    }
    if (typeof req.body === 'string' && req.body.length > 0) {
      try {
        return resolve(JSON.parse(req.body));
      } catch (_) {
        try {
          const params = new URLSearchParams(req.body);
          const obj = {};
          for (const [k, v] of params.entries()) obj[k] = v;
          return resolve(obj);
        } catch (_) {
          return resolve({});
        }
      }
    }
    let data = '';
    req.on('data', chunk => { data += chunk; });
    req.on('end', () => {
      if (!data) return resolve({});
      try {
        resolve(JSON.parse(data));
      } catch (_) {
        try {
          const params = new URLSearchParams(data);
          const obj = {};
          for (const [k, v] of params.entries()) obj[k] = v;
          resolve(obj);
        } catch (_) {
          resolve({});
        }
      }
    });
    req.on('error', () => resolve({}));
  });
}

module.exports = async (req, res) => {
  // Set CORS headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, Api-Version');

  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  const parsedBody = await parseRequestBody(req);

  const code = (req.query?.code || req.query?.auth_code || parsedBody?.code || parsedBody?.auth_code || '').trim();
  const redirectUri = (req.query?.redirect_uri || parsedBody?.redirect_uri || process.env.UPSTOX_REDIRECT_URI || 'https://application-beige-psi.vercel.app/oauth').trim();
  const clientId = (req.query?.client_id || req.query?.apiKey || parsedBody?.client_id || parsedBody?.apiKey || process.env.UPSTOX_API_KEY || '').trim();
  const clientSecret = (req.query?.client_secret || req.query?.apiSecret || parsedBody?.client_secret || parsedBody?.apiSecret || process.env.UPSTOX_API_SECRET || '').trim();

  if (!code) {
    return res.status(400).json({ status: "error", error: "Missing authorization code" });
  }

  // Return cached token if already exchanged successfully
  const cachedResponse = getCachedToken(code);
  if (cachedResponse) {
    return res.status(200).json(cachedResponse);
  }

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
    redirect_uri: redirectUri,
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

  return new Promise((resolve) => {
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
          if (postRes.statusCode >= 200 && postRes.statusCode < 300 && (jsonResponse.access_token || jsonResponse.data?.access_token)) {
            cacheToken(code, jsonResponse);
          }
          res.status(postRes.statusCode).json(jsonResponse);
        } catch (e) {
          res.status(postRes.statusCode).send(body);
        }
        resolve();
      });
    });

    postReq.on('error', (e) => {
      console.error(`Upstox token exchange network error: ${e.message}`);
      res.status(500).json({ status: "error", error: `Internal connection error: ${e.message}` });
      resolve();
    });

    postReq.write(postData);
    postReq.end();
  });
};

