import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { ElSelect, ElDialog } from 'element-plus'
import PostProduct from '../views/PostProduct.vue'
import { createProduct, updateProduct, getProductDetail } from '../api/product'
import { notify } from '../utils/feedback'
import { readEditingDraft, saveDraftData } from '../utils/drafts'
const nav = vi.hoisted(() => ({ route: { query: {} }, push: vi.fn(), replace: vi.fn() }))
vi.mock('vue-router', () => ({ useRoute: () => nav.route, useRouter: () => nav }))
vi.mock('../api/product', () => ({ createProduct: vi.fn(), updateProduct: vi.fn(), getProductDetail: vi.fn(), splitImages: () => [] }))
vi.mock('../constants/categories', () => ({ loadCategories: async () => [{ value: 'electronics', label: '电子产品' }] }))
vi.mock('../utils/feedback', () => ({ notify: vi.fn() }))
vi.mock('../utils/drafts', () => ({ readEditingDraft: vi.fn(), saveDraftData: vi.fn(), removePublishedDraft: vi.fn() }))
const product = { title: '白色头戴式无线耳机', category: 'electronics', condition: '轻微使用痕迹', price: 120, originalPrice: 299, tradePlace: '第一教学楼大厅', description: '可见面试听。' }
let wrapper
async function start(realDialog = false) {
  wrapper = mount(PostProduct, { attachTo: document.body, global: { components: { ElDialog }, stubs: { RouterLink: true, ElDialog: !realDialog } } })
  await flushPromises()
}
beforeEach(() => {
  vi.clearAllMocks()
  nav.route = { query: {} }
  readEditingDraft.mockReturnValue(null)
  saveDraftData.mockReturnValue({ draftId: 'draft-1' })
  getProductDetail.mockResolvedValue({ code: 200, data: { ...product } })
  updateProduct.mockResolvedValue({ code: 200 })
  createProduct.mockResolvedValue({ code: 200 })
})
afterEach(() => { wrapper?.unmount(); document.body.innerHTML = '' })
describe('publishing selects and historical meeting places', () => {
  it('retains an existing non-preset place in both displayed options and update payload', async () => {
    nav.route.query.id = '129'
    await start()
    const selects = wrapper.findAllComponents(ElSelect)
    expect(selects).toHaveLength(3)
    expect(selects[2].props('modelValue')).toBe(product.tradePlace)
    expect(document.body.textContent).toContain(product.tradePlace)
    expect(selects[2].props('allowCreate')).toBe(false)
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(updateProduct).toHaveBeenCalledWith('129', expect.objectContaining(product))
  })
  it('restores and saves a draft with its original historical place', async () => {
    readEditingDraft.mockReturnValue({ ...product, draftId: 'draft-1', images: [] })
    await start()
    expect(wrapper.findAllComponents(ElSelect)[2].props('modelValue')).toBe(product.tradePlace)
    await wrapper.find('.btn-secondary').trigger('click')
    expect(saveDraftData).toHaveBeenCalledWith(expect.objectContaining({ tradePlace: product.tradePlace, draftId: 'draft-1' }))
  })
  it.each(['category', 'condition', 'tradePlace'])('requires %s before submitting', async field => {
    readEditingDraft.mockReturnValue({ ...product, [field]: '', images: [] })
    await start()
    await wrapper.find('form').trigger('submit')
    expect(notify).toHaveBeenCalledWith('请填写所有必填项')
    expect(createProduct).not.toHaveBeenCalled()
    expect(updateProduct).not.toHaveBeenCalled()
  })
  it('submits the selected preset values using the existing API fields', async () => {
    readEditingDraft.mockReturnValue({ ...product, images: [] })
    await start()
    const selects = wrapper.findAllComponents(ElSelect)
    selects[1].vm.$emit('update:modelValue', '全新')
    selects[2].vm.$emit('update:modelValue', '图书馆')
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(createProduct).toHaveBeenCalledWith(expect.objectContaining({ category: 'electronics', condition: '全新', tradePlace: '图书馆', price: 120 }), expect.any(String))
  })
})


it('keeps the success dialog actions available after submitting and resets for the next item', async () => {
  readEditingDraft.mockReturnValue({ ...product, images: [] })
  await start(true)
  await wrapper.find('form').trigger('submit')
  await flushPromises()
  const dialog = document.querySelector('[role="dialog"]')
  expect(dialog.getAttribute('aria-label')).toBe('已提交审核')
  expect(dialog.textContent).toContain('管理员审核通过后')
  const buttons = [...dialog.querySelectorAll('button')]
  buttons.find(button => button.textContent.includes('查看我的发布')).click()
  expect(nav.push).toHaveBeenCalledWith('/profile')
  buttons.find(button => button.textContent.includes('继续发布')).click()
  await flushPromises()
  expect(nav.replace).toHaveBeenCalledWith('/post')
  expect(wrapper.find('#product-title').element.value).toBe('')
  expect(wrapper.findAllComponents(ElSelect)[2].props('modelValue')).toBe('')
  expect(createProduct).toHaveBeenCalledOnce()
})
