// api/state-store.js

const crypto = require('crypto');

// In-memory state store for OAuth states. Each state is single-use and expires.
// NOTE: For production, replace with a persistent store (Redis/DB) shared across instances.

const STATE_TTL_MS = 5 * 60 * 1000; // 5 minutes

const states = new Map(); // state -> { createdAt, consumed }

function generateState(prefix = 'upstox_') {
  const id = crypto.randomBytes(16).toString('hex');
  const state = `${prefix}${id}`;
  states.set(state, { createdAt: Date.now(), consumed: false });
  return state;
}

function validateState(state) {
  if (!state || typeof state !== 'string') return false;
  const entry = states.get(state);
  if (!entry) return false;
  if (entry.consumed) return false;
  if (Date.now() - entry.createdAt > STATE_TTL_MS) {
    states.delete(state);
    return false;
  }
  return true;
}

function consumeState(state) {
  if (!validateState(state)) return false;
  const entry = states.get(state);
  if (!entry) return false;
  entry.consumed = true;
  states.set(state, entry);
  // keep key for a short window if needed; we won't remove immediately to help debugging
  return true;
}

module.exports = {
  generateState,
  validateState,
  consumeState,
};
