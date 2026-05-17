import { useState, useRef } from 'react'
import * as api from '../api/index.js'
import styles from './FileTree.module.css'

export default function FileTree({ node, code, isReadOnly, onPreview, withRefresh, onError, loading }) {
  return (
    <div className={styles.tree}>
      <TreeNode
        node={node}
        code={code}
        isReadOnly={isReadOnly}
        onPreview={onPreview}
        withRefresh={withRefresh}
        onError={onError}
        depth={0}
        isRoot
      />
      {loading && <div className={styles.loadingBar} />}
    </div>
  )
}

function TreeNode({ node, code, isReadOnly, onPreview, withRefresh, onError, depth, isRoot }) {
  const [expanded, setExpanded] = useState(true)
  const [renaming, setRenaming] = useState(false)
  const [renameName, setRenameName] = useState('')
  const [creatingFolder, setCreatingFolder] = useState(false)
  const [newFolderName, setNewFolderName] = useState('')
  const fileInputRef = useRef(null)
  const folderInputRef = useRef(null)

  const isDir = node.is_directory
  const category = node.file?.category

  function parentPath(nodePath) {
    if (isRoot) return '/'
    const parts = nodePath.split('/').filter(Boolean)
    parts.pop()
    return '/' + parts.join('/')
  }

  // --- Actions ---
  async function handleDelete() {
    if (!window.confirm(`Delete "${node.name}"?`)) return
    await withRefresh(api.deleteEntry)(code, node.path)
  }

  async function handleRename() {
    if (!renameName.trim() || renameName === node.name) { setRenaming(false); return }
    const pp = parentPath(node.path)
    const newPath = (pp === '/' ? '' : pp) + '/' + renameName.trim()
    await withRefresh(api.renameEntry)(code, node.path, newPath)
    setRenaming(false)
  }

  async function handleUploadFile(e) {
    const files = e.target.files
    if (!files.length) return
    for (const file of files) {
      await withRefresh(api.uploadFile)(code, node.path, file.name, file)
    }
    e.target.value = ''
  }

  async function handleUploadFolder(e) {
    const file = e.target.files[0]
    if (!file) return
    await withRefresh(api.uploadDirectory)(code, node.path, file.name.replace(/\.zip$/i, ''), file)
    e.target.value = ''
  }

  async function handleDownload() {
    try {
      const blob = await api.downloadEntry(code, node.path)
      api.triggerDownload(blob, node.name)
    } catch (e) { onError(e.message) }
  }

  async function handleDownloadZip() {
    try {
      const blob = await api.downloadEntry(code, node.path, true)
      api.triggerDownload(blob, node.name + '.zip')
    } catch (e) { onError(e.message) }
  }

  async function handleCreateFolder() {
    if (!newFolderName.trim()) return
    await withRefresh(api.createFolder)(code, node.path, newFolderName.trim())
    setCreatingFolder(false)
    setNewFolderName('')
  }

  async function handleCategoryChange(cat) {
    await withRefresh(api.updateCategory)(code, node.path, cat)
  }

  const indent = depth * 20

  return (
    <div className={styles.nodeWrapper}>
      <div
        className={`${styles.row} ${isRoot ? styles.rootRow : ''}`}
        style={{ paddingLeft: `${12 + indent}px` }}
      >
        {/* Expand toggle for dirs */}
        {isDir ? (
          <button
            className={styles.expandBtn}
            onClick={() => setExpanded(v => !v)}
            aria-label={expanded ? 'Collapse' : 'Expand'}
          >
            <svg
              width="11" height="11" viewBox="0 0 10 10"
              style={{ transform: expanded ? 'rotate(90deg)' : 'rotate(0deg)', transition: 'transform 0.15s' }}
            >
              <path d="M3 2l4 3-4 3" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round" fill="none"/>
            </svg>
          </button>
        ) : (
          <span className={styles.expandSpacer} />
        )}

        {/* Icon */}
        <span className={`${styles.icon} ${isDir ? styles.iconDir : styles.iconFile}`}>
          {isDir ? <IconFolder /> : IconForCategory(category)}
        </span>

        {/* Name / rename */}
        {renaming ? (
          <input
            className={styles.renameInput}
            value={renameName}
            autoFocus
            onChange={e => setRenameName(e.target.value)}
            onBlur={() => setRenaming(false)}
            onKeyDown={e => {
              if (e.key === 'Enter') handleRename()
              if (e.key === 'Escape') setRenaming(false)
            }}
          />
        ) : (
          <button
            className={`${styles.name} ${(!isDir && (category === 'TEXT' || category === 'IMAGE')) ? styles.nameClickable : ''}`}
            onClick={() => !isDir && onPreview(node)}
            title={node.path}
          >
            {node.name}
          </button>
        )}

        {/* Category badge for files */}
        {!isDir && category && (
          <span className={`${styles.cat} ${styles[`cat${category}`]}`}>
            {category}
          </span>
        )}

        {/* File size */}
        {!isDir && node.file?.size != null && (
          <span className={styles.size}>{formatSize(node.file.size)}</span>
        )}

        {/* Actions */}
        <div className={styles.actions}>
          {/* Upload file to folder */}
          {isDir && !isReadOnly && (
            <>
              <input
                type="file" multiple ref={fileInputRef}
                style={{ display: 'none' }} onChange={handleUploadFile}
              />
              <input
                type="file" ref={folderInputRef} accept=".zip"
                style={{ display: 'none' }} onChange={handleUploadFolder}
              />
              <ActionBtn title="Upload file(s)" onClick={() => fileInputRef.current?.click()}>
                <IconUpload />
              </ActionBtn>
              <ActionBtn title="Upload folder (.zip)" onClick={() => folderInputRef.current?.click()}>
                <IconFolderUp />
              </ActionBtn>
              <ActionBtn title="New folder" onClick={() => setCreatingFolder(v => !v)}>
                <IconNewFolder />
              </ActionBtn>
            </>
          )}

          {/* Download */}
          <ActionBtn title="Download" onClick={handleDownload}>
            <IconDownload />
          </ActionBtn>
          <ActionBtn title="Download as ZIP" onClick={handleDownloadZip}>
            <IconZip />
          </ActionBtn>

          {/* Category selector for files */}
          {!isDir && !isReadOnly && (
            <select
              className={styles.catSelect}
              value={category}
              onChange={e => handleCategoryChange(e.target.value)}
              title="Change category"
            >
              <option value="TEXT">TEXT</option>
              <option value="IMAGE">IMAGE</option>
              <option value="OTHER">OTHER</option>
            </select>
          )}

          {/* Rename */}
          {!isRoot && !isReadOnly && (
            <ActionBtn title="Rename" onClick={() => { setRenameName(node.name); setRenaming(true) }}>
              <IconRename />
            </ActionBtn>
          )}

          {/* Delete */}
          {!isRoot && !isReadOnly && (
            <ActionBtn title="Delete" danger onClick={handleDelete}>
              <IconDelete />
            </ActionBtn>
          )}
        </div>
      </div>

      {/* New folder input */}
      {creatingFolder && (
        <div className={styles.newFolderRow} style={{ paddingLeft: `${12 + indent + 28}px` }}>
          <IconFolder />
          <input
            className={styles.renameInput}
            placeholder="Folder name"
            value={newFolderName}
            autoFocus
            onChange={e => setNewFolderName(e.target.value)}
            onKeyDown={e => {
              if (e.key === 'Enter') handleCreateFolder()
              if (e.key === 'Escape') { setCreatingFolder(false); setNewFolderName('') }
            }}
          />
          <button className={styles.confirmCreate} onClick={handleCreateFolder}>Create</button>
          <button className={styles.cancelCreate} onClick={() => { setCreatingFolder(false); setNewFolderName('') }}>✕</button>
        </div>
      )}

      {/* Children */}
      {isDir && expanded && node.children?.length > 0 && (
        <div>
          {node.children.map(child => (
            <TreeNode
              key={child.path}
              node={child}
              code={code}
              isReadOnly={isReadOnly}
              onPreview={onPreview}
              withRefresh={withRefresh}
              onError={onError}
              depth={depth + 1}
              isRoot={false}
            />
          ))}
        </div>
      )}

      {/* Empty folder hint */}
      {isDir && expanded && (!node.children || node.children.length === 0) && !creatingFolder && (
        <div className={styles.emptyHint} style={{ paddingLeft: `${12 + indent + 28}px` }}>
          {isReadOnly ? 'Empty folder' : 'Empty — upload files or create a folder'}
        </div>
      )}
    </div>
  )
}

function ActionBtn({ children, title, onClick, danger }) {
  return (
    <button
      className={`${styles.actionBtn} ${danger ? styles.actionBtnDanger : ''}`}
      title={title}
      onClick={onClick}
    >
      {children}
    </button>
  )
}

function IconForCategory(cat) {
  if (cat === 'IMAGE') return <IconImage />
  if (cat === 'TEXT') return <IconText />
  return <IconFile />
}

function formatSize(bytes) {
  if (bytes === 0) return '0 B'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1048576) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1048576).toFixed(1)} MB`
}

// ---- Icons ----
function IconFolder() {
  return (
    <svg width="14" height="14" viewBox="0 0 16 16" fill="none">
      <path d="M1.5 4.5A1.5 1.5 0 013 3h3.25l1.5 1.5H13A1.5 1.5 0 0114.5 6v6A1.5 1.5 0 0113 13.5H3A1.5 1.5 0 011.5 12V4.5z" stroke="currentColor" strokeWidth="1.3" fill="none"/>
    </svg>
  )
}
function IconFile() {
  return (
    <svg width="13" height="13" viewBox="0 0 16 16" fill="none">
      <path d="M9.5 2H4a1.5 1.5 0 00-1.5 1.5v9A1.5 1.5 0 004 14h8a1.5 1.5 0 001.5-1.5V6L9.5 2z" stroke="currentColor" strokeWidth="1.3"/>
      <path d="M9.5 2v4H13.5" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round"/>
    </svg>
  )
}
function IconText() {
  return (
    <svg width="13" height="13" viewBox="0 0 16 16" fill="none">
      <path d="M9.5 2H4a1.5 1.5 0 00-1.5 1.5v9A1.5 1.5 0 004 14h8a1.5 1.5 0 001.5-1.5V6L9.5 2z" stroke="currentColor" strokeWidth="1.3"/>
      <path d="M5 9h6M5 11.5h4" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round"/>
    </svg>
  )
}
function IconImage() {
  return (
    <svg width="13" height="13" viewBox="0 0 16 16" fill="none">
      <rect x="2" y="3" width="12" height="10" rx="1.5" stroke="currentColor" strokeWidth="1.3"/>
      <circle cx="5.5" cy="6.5" r="1.2" stroke="currentColor" strokeWidth="1.1"/>
      <path d="M2 11l3.5-3 2.5 2 2-1.5L14 11" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" strokeLinejoin="round"/>
    </svg>
  )
}
function IconUpload() {
  return (
    <svg width="13" height="13" viewBox="0 0 16 16" fill="none">
      <path d="M8 11V4M5 7l3-3 3 3" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round"/>
      <path d="M2.5 13h11" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round"/>
    </svg>
  )
}
function IconFolderUp() {
  return (
    <svg width="14" height="14" viewBox="0 0 16 16" fill="none">
      <path d="M1.5 4.5A1.5 1.5 0 013 3h3.25l1.5 1.5H13A1.5 1.5 0 0114.5 6v6A1.5 1.5 0 0113 13.5H3A1.5 1.5 0 011.5 12V4.5z" stroke="currentColor" strokeWidth="1.3"/>
      <path d="M8 11V7M6 9l2-2 2 2" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" strokeLinejoin="round"/>
    </svg>
  )
}
function IconNewFolder() {
  return (
    <svg width="14" height="14" viewBox="0 0 16 16" fill="none">
      <path d="M1.5 4.5A1.5 1.5 0 013 3h3.25l1.5 1.5H13A1.5 1.5 0 0114.5 6v6A1.5 1.5 0 0113 13.5H3A1.5 1.5 0 011.5 12V4.5z" stroke="currentColor" strokeWidth="1.3"/>
      <path d="M8 7.5v4M6 9.5h4" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round"/>
    </svg>
  )
}
function IconDownload() {
  return (
    <svg width="13" height="13" viewBox="0 0 16 16" fill="none">
      <path d="M8 4v7M5 8.5l3 3 3-3" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round"/>
      <path d="M2.5 13h11" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round"/>
    </svg>
  )
}
function IconZip() {
  return (
    <svg width="13" height="13" viewBox="0 0 16 16" fill="none">
      <path d="M9.5 2H4a1.5 1.5 0 00-1.5 1.5v9A1.5 1.5 0 004 14h8a1.5 1.5 0 001.5-1.5V6L9.5 2z" stroke="currentColor" strokeWidth="1.3"/>
      <path d="M7 2v3.5M9 2v1.5M7 5.5h2M7 7h2M7 8.5h2M7.5 10H9l-2 2h2" stroke="currentColor" strokeWidth="1.1" strokeLinecap="round"/>
    </svg>
  )
}
function IconRename() {
  return (
    <svg width="13" height="13" viewBox="0 0 16 16" fill="none">
      <path d="M11.5 2.5l2 2-7 7H4.5v-2l7-7z" stroke="currentColor" strokeWidth="1.3" strokeLinejoin="round"/>
      <path d="M2 13.5h12" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round"/>
    </svg>
  )
}
function IconDelete() {
  return (
    <svg width="13" height="13" viewBox="0 0 16 16" fill="none">
      <path d="M2.5 4.5h11M6 4.5V3h4v1.5" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round"/>
      <path d="M4 4.5l.75 8.5h6.5L12 4.5" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" strokeLinejoin="round"/>
    </svg>
  )
}
