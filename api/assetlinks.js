module.exports = (req, res) => {
  res.setHeader('Content-Type', 'application/json');
  res.status(200).json([
    {
      "relation": ["delegate_permission/common.handle_all_urls"],
      "target": {
        "namespace": "android_app",
        "package_name": "com.aistudio.kingkhanaitradepro.vtrqpx",
        "sha256_cert_fingerprints": [
          "60:83:FD:11:58:E7:DA:78:D7:06:75:1F:BE:79:B4:E7:93:18:E5:F0:93:C6:53:8A:EA:91:35:27:F9:B7:10:F5"
        ]
      }
    }
  ]);
};
