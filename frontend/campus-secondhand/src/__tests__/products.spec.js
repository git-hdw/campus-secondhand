import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import Products from '../views/Products.vue'
import { getProductList } from '../api/product'

vi.mock('../api/product', () => ({ getProductList: vi.fn(), splitImages: () => [] }))
vi.mock('../constants/categories', () => ({
  loadCategories: async () => [{value:'books',label:'教材书籍'}],
  categoryLabel: value => value,
}))
vi.mock('vue-router', () => ({
  useRoute: () => ({query:{},fullPath:'/products'}),
  useRouter: () => ({push:vi.fn()}),
}))
const result = (title) => ({code:200,data:{items:[{id:1,title,price:20,status:0}],total:1,page:1}})
const deferred = () => {
  let resolve
  const promise = new Promise(done => { resolve = done })
  return { promise, resolve }
}
let wrapper
beforeEach(() => { vi.clearAllMocks(); vi.useFakeTimers(); localStorage.clear() })
afterEach(() => { wrapper?.unmount(); vi.useRealTimers() })

describe('product browsing', () => {
  it('ignores an older response after the student changes category', async () => {
    const older = deferred(), newer = deferred()
    getProductList.mockReturnValueOnce(older.promise).mockReturnValueOnce(newer.promise)
    wrapper = mount(Products, { global: { stubs: { RouterLink: true } } })
    await flushPromises()
    const category = wrapper.findAll('button.filter-tab').find(button => button.text() === '教材书籍')
    await category.trigger('click')
    await vi.advanceTimersByTimeAsync(200)
    older.resolve(result('过期的生活用品'))
    await flushPromises()
    expect(wrapper.text()).not.toContain('过期的生活用品')
    newer.resolve(result('当前教材'))
    await flushPromises()
    expect(wrapper.text()).toContain('当前教材')
    expect(getProductList.mock.calls[1][0].category).toBe('books')
  })
  it('shows a retry action for a failed request instead of claiming the list is empty', async () => {
    getProductList.mockRejectedValueOnce(new Error('服务器暂时不可用')).mockResolvedValueOnce(result('恢复后的商品'))
    wrapper = mount(Products, { global: { stubs: { RouterLink: true } } })
    await flushPromises()
    expect(wrapper.text()).toContain('服务器暂时不可用')
    await wrapper.find('button.retry-btn').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('恢复后的商品')
  })
})
