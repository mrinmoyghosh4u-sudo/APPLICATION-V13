module.exports = (req, res) => {
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
};
