import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import Profile from '../views/Profile.vue'
import { getOrderList } from '../api/order'
import { getMyProducts } from '../api/product'
vi.mock('vue-router', () => ({ useRoute: () => ({ query: { tab: 'orders' } }), useRouter: () => ({ push: vi.fn(), replace: vi.fn() }) }))
vi.mock('../api/user', () => ({ getProfile: async () => ({ code: 200, data: { id: 2, username: '测试用户' } }), updateProfile: vi.fn() }))
vi.mock('../api/product', () => ({ getMyProducts: vi.fn(), deleteProduct: vi.fn(), offShelfProduct: vi.fn(), splitImages: () => [] }))
vi.mock('../api/order', () => ({ getOrderList: vi.fn(), confirmOrder: vi.fn(), cancelOrder: vi.fn() }))
vi.mock('../constants/categories', () => ({ loadCategories: vi.fn(), categoryLabel: value => value }))
vi.mock('../utils/feedback', () => ({ notify: vi.fn(), confirmAction: vi.fn() }))
let wrapper
beforeEach(() => {
  getMyProducts.mockResolvedValue({ code: 200, data: [] })
  sessionStorage.setItem('token', 'test-token')
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, json: async () => ({ code: 200, data: { items: [], total: 0 } }) }))
  getOrderList.mockImplementation(async ({ page }) => ({ code: 200, data: { total: 21, items: [{ id: page, productId: 900, productTitle: `第${page}页商品`, price: 20, status: 4 }] } }))
})
it('pages published products while keeping global statistics', async () => {
  getMyProducts.mockImplementation(async ({ page }) => ({ code: 200, data: {
    total: 21, stats: { listings: 18, sold: 3, views: 432 },
    items: [{ id: page, title: `发布第${page}页`, price: 20, status: 0, viewCount: 1 }],
  } }))
  wrapper = mount(Profile, { global: { stubs: { SessionPanel: true, RouterLink: true } } })
  await flushPromises()
  await wrapper.findAll('.tab-btn').find(button => button.text().includes('我的发布')).trigger('click')
  await wrapper.findAll('.list-pagination button')[1].trigger('click'); await flushPromises()
  expect(getMyProducts).toHaveBeenLastCalledWith({ page: 2, pageSize: 20 })
  expect(wrapper.text()).toContain('发布第2页')
  expect(wrapper.findAll('.stat-value').map(node => node.text())).toEqual(['18', '3', '0', '432'])
})
afterEach(() => { wrapper?.unmount(); vi.unstubAllGlobals(); sessionStorage.clear() })
it('requests the next server page and resets paging when filtering orders', async () => {
  wrapper = mount(Profile, { global: { stubs: { SessionPanel: true, RouterLink: true } } })
  await flushPromises()
  expect(wrapper.text()).toContain('第1页商品')
  await wrapper.findAll('.list-pagination button')[1].trigger('click'); await flushPromises()
  expect(getOrderList).toHaveBeenLastCalledWith({ page: 2, pageSize: 20 })
  expect(wrapper.text()).toContain('第2页商品')
  await wrapper.findAll('button').find(button => button.text() === '待见面').trigger('click'); await flushPromises()
  expect(getOrderList).toHaveBeenLastCalledWith({ page: 1, pageSize: 20, status: 0 })
})
