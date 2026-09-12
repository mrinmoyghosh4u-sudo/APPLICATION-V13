const https = require('https');

function parseRequestBody(req) {
  return new Promise((resolve) => {
    if (req.body && typeof req.body === 'object') {
      return resolve(req.body);
    }
    let data = '';
    req.on('data', chunk => { data += chunk; });
    req.on('end', () => {
      if (!data) return resolve({});
      try {
        resolve(JSON.parse(data));
      } catch (_) {
        resolve({});
      }
    });
    req.on('error', () => resolve({}));
  });
}

function makeHttpsRequest(options, postData) {
  return new Promise((resolve, reject) => {
    const req = https.request(options, (res) => {
      let body = '';
      res.setEncoding('utf8');
      res.on('data', chunk => { body += chunk; });
      res.on('end', () => {
        try {
          resolve({
            statusCode: res.statusCode,
            data: JSON.parse(body)
          });
        } catch (e) {
          resolve({
            statusCode: res.statusCode,
            data: body
          });
        }
      });
    });
    
    req.on('error', (e) => reject(e));
    if (postData) {
      req.write(postData);
    }
    req.end();
  });
}

module.exports = async (req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (req.method === 'OPTIONS') {
    return res.status(200).end();
  }

  const clientId = (process.env.DHAN_CLIENT_ID || '').trim();
  const apiKey = (process.env.DHAN_API_KEY || '').trim();
  const clientSecret = (process.env.DHAN_CLIENT_SECRET || '').trim();

  if (!clientId || !apiKey || !clientSecret) {
    return res.status(500).json({ errorMessage: "Server missing DHAN credentials configuration (DHAN_CLIENT_ID / DHAN_API_KEY / DHAN_CLIENT_SECRET)" });
  }

  const parsedBody = await parseRequestBody(req);
  const action = parsedBody.action || req.query.action;

  if (action === 'generate-consent') {
    try {
      const options = {
        hostname: 'auth.dhan.co',
        port: 443,
        path: `/app/generate-consent?client_id=${encodeURIComponent(clientId)}`,
        method: 'POST',
        headers: {
          'app_id': apiKey,
          'app_secret': clientSecret,
          'Content-Length': 0
        }
      };
      const response = await makeHttpsRequest(options, null);
      return res.status(response.statusCode).json(response.data);
    } catch (e) {
      console.error(`Dhan generate-consent error: ${e.message}`);
      return res.status(500).json({ errorMessage: `Internal connection error: ${e.message}` });
    }
  } else if (action === 'consume-consent') {
    const tokenId = (parsedBody.tokenId || req.query.tokenId || '').trim();
    if (!tokenId) {
      return res.status(400).json({ errorMessage: "Missing tokenId" });
    }

    try {
      const options = {
        hostname: 'auth.dhan.co',
        port: 443,
        path: `/app/consumeApp-consent?tokenId=${encodeURIComponent(tokenId)}`,
        method: 'POST',
        headers: {
          'app_id': apiKey,
          'app_secret': clientSecret,
          'Content-Length': 0
        }
      };
      const response = await makeHttpsRequest(options, null);
      return res.status(response.statusCode).json(response.data);
    } catch (e) {
      console.error(`Dhan consume-consent error: ${e.message}`);
      return res.status(500).json({ errorMessage: `Internal connection error: ${e.message}` });
    }
  } else {
    return res.status(400).json({ errorMessage: "Invalid action. Must be generate-consent or consume-consent" });
  }
};
