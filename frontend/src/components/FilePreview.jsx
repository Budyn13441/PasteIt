import { useEffect, useState } from 'react'
import styles from './FilePreview.module.css'

export default function FilePreview({ preview, onClose }) {
  const { node, url, type } = preview
  const [textContent, setTextContent] = useState(null)
  const [loading, setLoading] = useState(type === 'TEXT')

  useEffect(() => {
    if (type === 'TEXT') {
      fetch(url)
        .then(r => r.text())
        .then(t => { setTextContent(t); setLoading(false) })
        .catch(() => { setTextContent('Could not load file content.'); setLoading(false) })
    }
  }, [url, type])

  useEffect(() => {
    function onKey(e) { if (e.key === 'Escape') onClose() }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  return (
    <div className={styles.overlay} onClick={e => { if (e.target === e.currentTarget) onClose() }}>
      <div className={styles.modal}>
        <div className={styles.modalHeader}>
          <span className={styles.fileName}>{node.name}</span>
          <div className={styles.headerActions}>
            <a href={url} download={node.name} className={styles.downloadBtn} title="Download">
              <svg width="14" height="14" viewBox="0 0 16 16" fill="none">
                <path d="M8 4v7M5 8.5l3 3 3-3" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                <path d="M2.5 13h11" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
              </svg>
              Download
            </a>
            <button className={styles.closeBtn} onClick={onClose} title="Close (Esc)">✕</button>
          </div>
        </div>

        <div className={styles.body}>
          {loading && (
            <div className={styles.loadingState}>
              <div className={styles.spinner} />
              <span>Loading…</span>
            </div>
          )}

          {type === 'IMAGE' && (
            <div className={styles.imageContainer}>
              <img src={url} alt={node.name} className={styles.image} />
            </div>
          )}

          {type === 'TEXT' && !loading && (
            <pre className={styles.textContent}>{textContent}</pre>
          )}
        </div>
      </div>
    </div>
  )
}
