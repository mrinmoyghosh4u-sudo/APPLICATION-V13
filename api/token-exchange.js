const https = require('https');

module.exports = async (req, res) => {
  // Support both POST and GET for flexibility
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
      console.error(`Token exchange error: ${e.message}`);
      res.status(500).json({ error: `Internal connection error: ${e.message}` });
      resolve();
    });

    postReq.write(postData);
    postReq.end();
  });
};
