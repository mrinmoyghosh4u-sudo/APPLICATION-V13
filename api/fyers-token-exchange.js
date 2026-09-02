const https = require('https');
const crypto = require('crypto');

// In-memory cache of successful token exchanges (code -> response) and consumed codes
// Note: In serverless environments, in-memory caches persist per warm lambda instance.
const tokenCache = new Map();
const consumedCodes = new Map();

function getCachedToken(code) {
  const clean = (code || '').trim();
  if (!clean) return null;
  const entry = tokenCache.get(clean);
  if (!entry) return null;
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
    consumedCodes.set(clean, Date.now());
  }
}

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
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  const parsedBody = await parseRequestBody(req);

  const code = (req.query?.code || req.query?.auth_code || parsedBody?.code || parsedBody?.auth_code || '').trim();

  if (!code) {
    return res.status(400).json({ s: "error", code: 400, message: "Missing authorization code" });
  }

  // Return cached token if already exchanged
  const cachedResponse = getCachedToken(code);
  if (cachedResponse) {
    return res.status(200).json(cachedResponse);
  }

  if (isCodeConsumed(code)) {
    return res.status(400).json({
      s: "error",
      code: 400,
      message: "AUTH_CODE_ALREADY_USED: This authorization code has already been exchanged or expired. A new login is required."
    });
  }

  let appId = (process.env.FYERS_APP_ID || '').trim();
  const secretId = (process.env.FYERS_SECRET_ID || '').trim();

  if (!appId || !secretId) {
    return res.status(500).json({ s: "error", code: 500, message: "Server missing FYERS credentials configuration (FYERS_APP_ID / FYERS_SECRET_ID)" });
  }

  // Ensure -100 suffix on appId for Fyers v3 API
  if (!appId.endsWith('-100')) {
    appId = `${appId}-100`;
  }

  // Calculate SHA-256 appIdHash: SHA-256(appId:secretId)
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
          if (postRes.statusCode >= 200 && postRes.statusCode < 300 && (jsonResponse.access_token || jsonResponse.s === 'ok')) {
            cacheToken(code, jsonResponse);
          } else if (postRes.statusCode === 400 || (jsonResponse.s === 'error' && (jsonResponse.code === 400 || jsonResponse.code === -100))) {
            consumedCodes.set(code, Date.now());
          }
          res.status(postRes.statusCode).json(jsonResponse);
        } catch (e) {
          res.status(postRes.statusCode).send(body);
        }
        resolve();
      });
    });

    postReq.on('error', (e) => {
      console.error(`FYERS Token exchange error: ${e.message}`);
      res.status(500).json({ s: "error", code: 500, message: `Internal connection error: ${e.message}` });
      resolve();
    });

    postReq.write(postPayload);
    postReq.end();
  });
};

