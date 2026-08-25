module.exports = (req, res) => {
  const queryParams = new URLSearchParams(req.query).toString();
  const appDeepLink = `kingkhan://oauth${queryParams ? '?' + queryParams : ''}`;

  res.writeHead(302, { Location: appDeepLink });
  res.end();
};
