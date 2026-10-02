// Safe wrappers around localStorage. Storage can be missing, full or blocked (private mode, site
// data disabled, sandboxed previews) and JSON in it can be corrupt — none of that may crash the app.

let backend = null;
try {
  backend = globalThis.localStorage || null;
  if (backend) {
    const probe = '__qw_probe__';
    backend.setItem(probe, '1');
    backend.removeItem(probe);
  }
} catch {
  backend = null;
}

/** Replace the storage backend (used by tests). */
export function setStorageBackend(b) {
  backend = b;
}

export function storageAvailable() {
  return backend !== null;
}

export function readString(key, fallback = null) {
  try {
    const v = backend ? backend.getItem(key) : null;
    return v === null || v === undefined ? fallback : v;
  } catch {
    return fallback;
  }
}

export function writeString(key, value) {
  try {
    if (!backend) return false;
    backend.setItem(key, String(value));
    return true;
  } catch {
    return false;
  }
}

export function readJSON(key, fallback = null) {
  const raw = readString(key, null);
  if (raw === null) return fallback;
  try {
    return JSON.parse(raw);
  } catch {
    return fallback;
  }
}

export function writeJSON(key, value) {
  try {
    return writeString(key, JSON.stringify(value));
  } catch {
    return false;
  }
}

export function remove(key) {
  try {
    if (backend) backend.removeItem(key);
  } catch {
    /* ignore */
  }
}
