# OAuth Callback Server Setup Guide

## DNS & Realm Status
`https://kingkhan.app` is currently **unregistered/unresolvable on public DNS** (`NXDOMAIN`). Because of this:
- Opening `https://kingkhan.app/oauth` in a web browser returns `DNS_PROBE_FINISHED_NXDOMAIN`.
- Dhan and Angel One OAuth servers cannot reach `kingkhan.app` unless the domain is registered and pointed to an active HTTPS host.

## Options to Resolve

### Option A: Use Custom Scheme (`kingkhan://oauth`) - Recommended for Direct Mobile Testing
1. In the Dhan HQ Developer Portal or SmartAPI portal, set your **Redirect URI** to:
   `kingkhan://oauth`
2. In the app's Dhan connect screen, leave the Redirect URI field as `kingkhan://oauth`.
3. When Dhan completes login, Android will intercept `kingkhan://oauth` directly on the device without requiring any web domain or server.

### Option B: Deploy this Backend to Vercel/Render/Heroku/AWS
If your broker portal strictly enforces an `https://` prefix for Redirect URIs:
1. Deploy this `/backend` folder to a free cloud host like Vercel, Render, or Railway (e.g. `https://my-oauth-server.vercel.app`).
2. Register `https://my-oauth-server.vercel.app/oauth` as your Redirect URI in Dhan HQ / SmartAPI.
3. Update the app's Redirect URI field to match `https://my-oauth-server.vercel.app/oauth`.
4. The server will receive the code and automatically forward the user to `kingkhan://oauth` on the phone.

### Option C: Purchase & Host `kingkhan.app`
1. Purchase `kingkhan.app` on Google Domains / Namecheap / Cloudflare.
2. Add an A / CNAME record pointing to your backend host.
3. Update `server.js` with your app's release SHA-256 fingerprint in `assetlinks.json`.
