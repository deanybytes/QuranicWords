/**
 * QuranicWords — High Performance Client Database & Cache Engine
 * Uses IndexedDB for instant 0ms offline storage & fast asynchronous bootstrap.
 */

const DB_NAME = 'QuranicWords_DB_v1';
const DB_VERSION = 1;
const STORE_DATA = 'datasets';

class QuranicDB {
  constructor() {
    this.db = null;
    this.isReady = this.init();
  }

  async init() {
    return new Promise((resolve, reject) => {
      if (!window.indexedDB) {
        console.warn('IndexedDB not supported, falling back to network fetch');
        resolve(null);
        return;
      }

      const request = indexedDB.open(DB_NAME, DB_VERSION);

      request.onupgradeneeded = (e) => {
        const db = e.target.result;
        if (!db.objectStoreNames.contains(STORE_DATA)) {
          db.createObjectStore(STORE_DATA, { keyPath: 'key' });
        }
      };

      request.onsuccess = (e) => {
        this.db = e.target.result;
        resolve(this.db);
      };

      request.onerror = (e) => {
        console.warn('IndexedDB error:', e.target.error);
        resolve(null);
      };
    });
  }

  async get(key) {
    await this.isReady;
    if (!this.db) return null;

    return new Promise((resolve) => {
      try {
        const tx = this.db.transaction(STORE_DATA, 'readonly');
        const store = tx.objectStore(STORE_DATA);
        const req = store.get(key);
        req.onsuccess = () => resolve(req.result ? req.result.data : null);
        req.onerror = () => resolve(null);
      } catch (err) {
        resolve(null);
      }
    });
  }

  async set(key, data) {
    await this.isReady;
    if (!this.db) return;

    try {
      const tx = this.db.transaction(STORE_DATA, 'readwrite');
      const store = tx.objectStore(STORE_DATA);
      store.put({ key, data, timestamp: Date.now() });
    } catch (err) {
      console.warn('Failed to cache in IndexedDB:', err);
    }
  }

  /**
   * Fast Data Fetcher with Stale-While-Revalidate / Cache-First strategy
   */
  async fetchCached(url, cacheKey) {
    // 1. Try IndexedDB cache first for instant boot
    const cached = await this.get(cacheKey);
    if (cached) {
      // Revalidate in background if older than 1 day
      setTimeout(() => this.revalidate(url, cacheKey), 1000);
      return cached;
    }

    // 2. Fetch from network
    try {
      const res = await fetch(url);
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = await res.json();
      this.set(cacheKey, data);
      return data;
    } catch (err) {
      console.error(`Fetch failed for ${url}:`, err);
      throw err;
    }
  }

  async revalidate(url, cacheKey) {
    try {
      const res = await fetch(url);
      if (res.ok) {
        const data = await res.json();
        this.set(cacheKey, data);
      }
    } catch (e) {
      // Ignore background revalidation errors
    }
  }
}

export const dbService = new QuranicDB();
