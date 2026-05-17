import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { createStash } from '../api/index.js'
import styles from './HomePage.module.css'

export default function HomePage() {
  const navigate = useNavigate()
  const [code, setCode] = useState('')
  const [creating, setCreating] = useState(false)
  const [error, setError] = useState('')

  async function handleNew() {
    setCreating(true)
    setError('')
    try {
      const data = await createStash()
      navigate(`/${data.code}`)
    } catch (e) {
      setError(e.message)
      setCreating(false)
    }
  }

  function handleFind(e) {
    e.preventDefault()
    const trimmed = code.trim()
    if (trimmed) navigate(`/${trimmed}`)
  }

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <div className={styles.logo}>
          <span className={styles.logoIcon}>⌁</span>
          <span className={styles.logoText}>PasteIt</span>
        </div>
        <p className={styles.tagline}>Share files &amp; folders with a single link</p>
      </header>

      <main className={styles.main}>
        <div className={styles.card}>
          <div className={styles.half}>
            <h2 className={styles.sectionTitle}>Open existing</h2>
            <p className={styles.sectionDesc}>Enter a code to access a stash</p>
            <form onSubmit={handleFind} className={styles.findForm}>
              <input
                className={styles.input}
                value={code}
                onChange={e => setCode(e.target.value)}
                placeholder="e.g. aB3xK7qZ"
                spellCheck={false}
                autoFocus
              />
              <button type="submit" className={styles.btnSecondary} disabled={!code.trim()}>
                Open
              </button>
            </form>
          </div>

          <div className={styles.divider} />

          <div className={styles.half}>
            <h2 className={styles.sectionTitle}>Create new</h2>
            <p className={styles.sectionDesc}>Get a fresh link to upload your files</p>
            <button
              className={styles.btnPrimary}
              onClick={handleNew}
              disabled={creating}
            >
              {creating ? (
                <span className={styles.spinner} />
              ) : (
                <>
                  <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                    <path d="M8 3v10M3 8h10" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"/>
                  </svg>
                  New stash
                </>
              )}
            </button>
          </div>
        </div>

        {error && <div className={styles.errorBanner}>{error}</div>}

        <footer className={styles.footer}>
          Files are stored for 24 hours by default &middot; No account required
        </footer>
      </main>
    </div>
  )
}
