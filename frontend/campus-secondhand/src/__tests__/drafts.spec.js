import { beforeEach, describe, expect, it } from 'vitest'
import { readDrafts, saveDraftData, readEditingDraft, setEditingDraft, removePublishedDraft } from '../utils/drafts'

const signIn = (id) => sessionStorage.setItem('user', JSON.stringify({ id }))
beforeEach(() => { localStorage.clear(); sessionStorage.clear() })

describe('local product drafts', () => {
  it('isolates drafts when two students share a browser', () => {
    signIn(1); saveDraftData({ title:'学生一的草稿' })
    signIn(2); expect(readDrafts()).toEqual([])
    signIn(1); expect(readDrafts()[0].title).toBe('学生一的草稿')
  })
  it('continues the selected draft and updates it instead of adding duplicates', () => {
    signIn(1)
    const draft = saveDraftData({title:'旧标题'})
    setEditingDraft(draft)
    expect(readEditingDraft().title).toBe('旧标题')
    saveDraftData({...draft,title:'新标题'})
    expect(readDrafts()).toHaveLength(1)
    expect(readDrafts()[0].title).toBe('新标题')
    removePublishedDraft(draft.draftId)
    expect(readDrafts()).toEqual([])
    expect(readEditingDraft()).toBeNull()
  })
  it('recovers from corrupted storage without breaking profile rendering', () => {
    signIn(1); localStorage.setItem('product_drafts_1','broken')
    expect(readDrafts()).toEqual([])
  })
})
