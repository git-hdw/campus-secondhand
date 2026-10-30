import { readUser } from './session'

const key = () => `product_drafts_${readUser().id || 'guest'}`
const editingKey = () => `product_draft_${readUser().id || 'guest'}`
function parse(raw, fallback) {
  try { return JSON.parse(raw) ?? fallback } catch { return fallback }
}
export function readDrafts() {
  const rows = parse(localStorage.getItem(key()), [])
  return Array.isArray(rows) ? rows : []
}
export function saveDraftData(data) {
  const rows = readDrafts()
  const draft = { ...data, draftId: data.draftId || crypto.randomUUID(), savedAt: Date.now() }
  const next = [draft, ...rows.filter(row => row.draftId !== draft.draftId)]
  localStorage.setItem(key(), JSON.stringify(next))
  return draft
}
export function removeDraftAt(index) {
  const rows = readDrafts(); rows.splice(index, 1)
  localStorage.setItem(key(), JSON.stringify(rows))
}
export function removePublishedDraft(draftId) {
  if (draftId) localStorage.setItem(key(), JSON.stringify(readDrafts().filter(row => row.draftId !== draftId)))
  localStorage.removeItem(editingKey())
}
export function clearDrafts() { localStorage.removeItem(key()) }
export function setEditingDraft(draft) { localStorage.setItem(editingKey(), JSON.stringify(draft)) }
export function readEditingDraft() { return parse(localStorage.getItem(editingKey()), null) }
