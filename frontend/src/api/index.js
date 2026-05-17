const BASE = '/api/v1'

async function handleResponse(res) {
  if (res.ok) {
    // 204 No Content
    if (res.status === 204) return null
    const ct = res.headers.get('content-type') || ''
    if (ct.includes('application/json')) return res.json()
    return res.blob()
  }
  let err
  try { err = await res.json() } catch { err = { message: `HTTP ${res.status}` } }
  throw Object.assign(new Error(err.message || 'Request failed'), { status: res.status, data: err })
}

export async function createStash() {
  const res = await fetch(`${BASE}/new`, { method: 'POST' })
  return handleResponse(res) // { code }
}

export async function viewStash(code) {
  const res = await fetch(`${BASE}/view/${code}`)
  return handleResponse(res)
}

export async function makeReadOnly(code) {
  const res = await fetch(`${BASE}/make-readonly/${code}`, { method: 'POST' })
  return handleResponse(res)
}

export async function prolongStash(code, newExpirationDate) {
  const res = await fetch(`${BASE}/prolong/${code}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ new_expiration_date: newExpirationDate }),
  })
  return handleResponse(res)
}

export async function uploadFile(code, parentPath, name, file) {
  const fd = new FormData()
  fd.append('is_directory', 'false')
  fd.append('parent_path', parentPath)
  fd.append('name', name)
  fd.append('data', file)
  const res = await fetch(`${BASE}/upload/${code}`, { method: 'POST', body: fd })
  return handleResponse(res)
}

export async function uploadDirectory(code, parentPath, name, zipFile) {
  const fd = new FormData()
  fd.append('is_directory', 'true')
  fd.append('parent_path', parentPath)
  fd.append('name', name)
  fd.append('data', zipFile)
  const res = await fetch(`${BASE}/upload/${code}`, { method: 'POST', body: fd })
  return handleResponse(res)
}

export async function downloadEntry(code, path, asZip = false) {
  const params = new URLSearchParams({ path })
  if (asZip) params.set('format', 'zip')
  const res = await fetch(`${BASE}/download/${code}?${params}`)
  if (!res.ok) return handleResponse(res)
  return res.blob()
}

export async function deleteEntry(code, path) {
  const params = new URLSearchParams({ path })
  const res = await fetch(`${BASE}/delete/${code}?${params}`, { method: 'DELETE' })
  return handleResponse(res)
}

export async function renameEntry(code, oldPath, newPath) {
  const res = await fetch(`${BASE}/rename/${code}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ old_path: oldPath, new_path: newPath }),
  })
  return handleResponse(res)
}

export async function createFolder(code, parentPath, name) {
  const res = await fetch(`${BASE}/new-folder/${code}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ parent_path: parentPath, name }),
  })
  return handleResponse(res)
}

export async function updateCategory(code, filePath, category) {
  const res = await fetch(`${BASE}/update-category/${code}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ file_path: filePath, category }),
  })
  return handleResponse(res)
}

export function triggerDownload(blob, filename) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}
