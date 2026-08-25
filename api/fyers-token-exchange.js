const https = require('https');
const crypto = require('crypto');

module.exports = async (req, res) => {
  // Support both POST and GET
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
    return res.status(400).json({ s: "error", code: 400, message: "Missing authorization code" });
  }

  let appId = (process.env.FYERS_APP_ID || '').trim();
  const secretId = (process.env.FYERS_SECRET_ID || '').trim();

  if (!appId || !secretId) {
    return res.status(500).json({ s: "error", code: 500, message: "Server missing FYERS credentials configuration" });
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
