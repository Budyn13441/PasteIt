import { useState } from 'react'
import * as api from '../api/index.js'
import styles from './StashHeader.module.css'

export default function StashHeader({ stash, code, onRefresh, withRefresh }) {
  const [showProlong, setShowProlong] = useState(false)
  const [newDate, setNewDate] = useState('')
  const [confirming, setConfirming] = useState(false)

  const validUntil = new Date(stash.valid_until)
  const isReadOnly = stash.is_read_only

  function formatDate(d) {
    return d.toLocaleString('en-GB', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit',
    })
  }

  const now = new Date()
  const msLeft = validUntil - now
  const hoursLeft = Math.floor(msLeft / 3600000)
  const minutesLeft = Math.floor((msLeft % 3600000) / 60000)
  const isExpiringSoon = msLeft < 3600000 && msLeft > 0

  async function handleProlong() {
    if (!newDate) return
    await withRefresh(api.prolongStash)(code, new Date(newDate).toISOString())
    setShowProlong(false)
    setNewDate('')
  }

  async function handleReadOnly() {
    setConfirming(false)
    await withRefresh(api.makeReadOnly)(code)
  }

  // Compute min datetime-local value (now)
  const minDate = new Date(Date.now() + 60000).toISOString().slice(0, 16)

  return (
    <div className={styles.header}>
      <div className={styles.left}>
        <div className={`${styles.expiry} ${isExpiringSoon ? styles.expirySoon : ''}`}>
          <svg width="14" height="14" viewBox="0 0 16 16" fill="none">
            <circle cx="8" cy="8" r="6.5" stroke="currentColor" strokeWidth="1.4"/>
            <path d="M8 5v3.5l2 1.5" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round"/>
          </svg>
          <span>
            {msLeft <= 0
              ? 'Expired'
              : isExpiringSoon
              ? `Expires in ${hoursLeft}h ${minutesLeft}m`
              : `Valid until ${formatDate(validUntil)}`}
          </span>
        </div>

        {isReadOnly && (
          <div className={styles.badge}>
            <svg width="11" height="11" viewBox="0 0 16 16" fill="none">
              <rect x="3" y="7" width="10" height="7" rx="1.5" stroke="currentColor" strokeWidth="1.4"/>
              <path d="M5.5 7V5a2.5 2.5 0 015 0v2" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round"/>
            </svg>
            Read-only
          </div>
        )}
      </div>

      {!isReadOnly && (
        <div className={styles.actions}>
          <button className={styles.actionBtn} onClick={() => setShowProlong(v => !v)}>
            <svg width="13" height="13" viewBox="0 0 16 16" fill="none">
              <circle cx="8" cy="8" r="6.5" stroke="currentColor" strokeWidth="1.4"/>
              <path d="M8 5v3.5l2 1.5M12.5 3.5l1 1" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round"/>
            </svg>
            Extend
          </button>

          {!confirming ? (
            <button className={`${styles.actionBtn} ${styles.actionBtnDanger}`} onClick={() => setConfirming(true)}>
              <svg width="13" height="13" viewBox="0 0 16 16" fill="none">
                <rect x="3" y="7" width="10" height="7" rx="1.5" stroke="currentColor" strokeWidth="1.4"/>
                <path d="M5.5 7V5a2.5 2.5 0 015 0v2" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round"/>
              </svg>
              Make read-only
            </button>
          ) : (
            <div className={styles.confirmRow}>
              <span className={styles.confirmText}>This is permanent. Sure?</span>
              <button className={styles.confirmYes} onClick={handleReadOnly}>Yes</button>
              <button className={styles.actionBtn} onClick={() => setConfirming(false)}>Cancel</button>
            </div>
          )}
        </div>
      )}

      {showProlong && (
        <div className={styles.prolongPanel}>
          <label className={styles.prolongLabel}>New expiry date &amp; time</label>
          <div className={styles.prolongRow}>
            <input
              type="datetime-local"
              className={styles.dateInput}
              value={newDate}
              min={minDate}
              onChange={e => setNewDate(e.target.value)}
            />
            <button
              className={styles.prolongSave}
              onClick={handleProlong}
              disabled={!newDate}
            >
              Save
            </button>
            <button className={styles.actionBtn} onClick={() => setShowProlong(false)}>
              Cancel
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
