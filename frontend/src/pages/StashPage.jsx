import { useEffect, useRef, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { useStash } from '../hooks/useStash.js'
import FileTree from '../components/FileTree.jsx'
import StashHeader from '../components/StashHeader.jsx'
import FilePreview from '../components/FilePreview.jsx'
import * as api from '../api/index.js'
import styles from './StashPage.module.css'

export default function StashPage() {
  const { code } = useParams()
  const { stash, loading, error, refresh, withRefresh, setError } = useStash(code)
  const [preview, setPreview] = useState(null) // { node, url, type }
  const initialized = useRef(false)

  useEffect(() => {
    if (!initialized.current) {
      initialized.current = true
      refresh()
    }
  }, [refresh])

  const isReadOnly = stash?.is_read_only ?? false

  async function handlePreview(node) {
    if (node.is_directory) return
    const cat = node.file?.category
    if (cat !== 'TEXT' && cat !== 'IMAGE') return
    try {
      const blob = await api.downloadEntry(code, node.path)
      const url = URL.createObjectURL(blob)
      setPreview({ node, url, type: cat })
    } catch (e) {
      setError(e.message)
    }
  }

  function closePreview() {
    if (preview?.url) URL.revokeObjectURL(preview.url)
    setPreview(null)
  }

  if (loading && !stash) {
    return (
      <div className={styles.centered}>
        <div className={styles.spinner} />
        <span>Loading stash…</span>
      </div>
    )
  }

  if (error && !stash) {
    return (
      <div className={styles.centered}>
        <div className={styles.errorBox}>
          <div className={styles.errorIcon}>⚠</div>
          <h2>Stash not found</h2>
          <p>{error}</p>
          <Link to="/" className={styles.backLink}>← Back to home</Link>
        </div>
      </div>
    )
  }

  return (
    <div className={styles.page}>
      <nav className={styles.topbar}>
        <Link to="/" className={styles.logoLink}>
          <span className={styles.logoIcon}>⌁</span>
          <span>PasteIt</span>
        </Link>
        <div className={styles.codeChip}>
          <span className={styles.codeLabel}>Code</span>
          <code className={styles.codeValue}>{code}</code>
          <button
            className={styles.copyBtn}
            title="Copy link"
            onClick={() => navigator.clipboard.writeText(window.location.href)}
          >
            <IconCopy />
          </button>
        </div>
      </nav>

      <main className={styles.main}>
        {stash && (
          <StashHeader
            stash={stash}
            code={code}
            onRefresh={refresh}
            withRefresh={withRefresh}
          />
        )}

        {error && (
          <div className={styles.errorBanner}>
            {error}
            <button onClick={() => setError(null)} className={styles.dismissBtn}>✕</button>
          </div>
        )}

        {stash && (
          <div className={styles.treeCard}>
            <FileTree
              node={stash.file_tree}
              code={code}
              isReadOnly={isReadOnly}
              onPreview={handlePreview}
              withRefresh={withRefresh}
              onError={setError}
              loading={loading}
            />
          </div>
        )}
      </main>

      {preview && (
        <FilePreview preview={preview} onClose={closePreview} />
      )}
    </div>
  )
}

function IconCopy() {
  return (
    <svg width="14" height="14" viewBox="0 0 16 16" fill="none">
      <rect x="5" y="5" width="9" height="9" rx="1.5" stroke="currentColor" strokeWidth="1.5"/>
      <path d="M11 5V3.5A1.5 1.5 0 009.5 2h-6A1.5 1.5 0 002 3.5v6A1.5 1.5 0 003.5 11H5" stroke="currentColor" strokeWidth="1.5"/>
    </svg>
  )
}
