import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import axios from 'axios'
import AdminUsers from '../views/Admin/AdminUsers.vue'
import AdminProducts from '../views/Admin/AdminProducts.vue'
import AdminOrders from '../views/Admin/AdminOrders.vue'
import AdminCategories from '../views/Admin/AdminCategories.vue'
import { confirmAction, notify } from '../utils/feedback'

vi.mock('axios', () => ({ default: { get: vi.fn(), put: vi.fn(), post: vi.fn(), delete: vi.fn() } }))
vi.mock('../utils/feedback', () => ({ notify: vi.fn(), confirmAction: vi.fn() }))
vi.mock('../constants/categories', () => ({ loadCategories: async () => [], categoryLabel: value => value }))

const response = data => ({ data: { code: 200, data } })
const list = (items, total = items.length) => response({ items, total })
let wrapper
async function start(component) {
  wrapper = mount(component)
  await flushPromises()
}
beforeEach(() => {
  vi.clearAllMocks()
  sessionStorage.clear(); localStorage.clear()
  sessionStorage.setItem('token', 'demo-token')
  sessionStorage.setItem('user', JSON.stringify({ id: 1, username: 'admin' }))
  confirmAction.mockResolvedValue(true)
  axios.put.mockResolvedValue(response(null))
  axios.delete.mockResolvedValue(response(null))
})
afterEach(() => wrapper?.unmount())

describe('admin management interactions', () => {
  it('protects the current administrator account and respects cancelled disabling', async () => {
    axios.get.mockResolvedValue(list([{ id: 1, username: 'admin', status: 1 }, { id: 2, username: '学生', status: 1 }]))
    confirmAction.mockResolvedValue(false)
    await start(AdminUsers)
    expect(wrapper.findAll('tbody button')).toHaveLength(1)
    expect(wrapper.find('tbody').text()).toContain('当前账号')
    await wrapper.find('tbody button').trigger('click')
    await flushPromises()
    expect(axios.put).not.toHaveBeenCalled()
  })

  it('prevents repeated account changes while confirmation is pending', async () => {
    let resolve
    confirmAction.mockReturnValue(new Promise(done => { resolve = done }))
    axios.get.mockResolvedValue(list([{ id: 2, username: '学生', status: 1 }]))
    await start(AdminUsers)
    await wrapper.find('tbody button').trigger('click')
    await wrapper.find('tbody button').trigger('click')
    expect(confirmAction).toHaveBeenCalledOnce()
    resolve(true)
    await flushPromises()
    expect(axios.put).toHaveBeenCalledOnce()
    expect(axios.put.mock.calls[0][1]).toBe(0)
  })

  it('keeps the true account status when the server rejects a change', async () => {
    axios.get.mockResolvedValue(list([{ id: 2, username: '学生', status: 0 }]))
    axios.put.mockResolvedValue({ data: { code: 400, message: '账号状态更新失败' } })
    await start(AdminUsers)
    await wrapper.find('tbody button').trigger('click')
    await flushPromises()
    expect(wrapper.find('.admin-status').text()).toBe('已禁用')
    expect(notify).toHaveBeenCalledWith('账号状态更新失败')
    expect(notify).not.toHaveBeenCalledWith('账号已启用')
  })

  it('returns to the first page after the administrator changes the search or page size', async () => {
    axios.get.mockResolvedValue(list([{ id: 2, username: '学生', status: 1 }], 25))
    await start(AdminUsers)
    await wrapper.findAll('.pagination-button')[1].trigger('click')
    await flushPromises()
    expect(axios.get.mock.lastCall[1].params.page).toBe(2)
    await wrapper.find('.search-input').setValue(' 学生 ')
    await wrapper.find('form').trigger('submit')
    await flushPromises()
    expect(axios.get.mock.lastCall[1].params).toMatchObject({ page: 1, search: '学生' })
    await wrapper.find('.admin-pagination select').setValue('20')
    await flushPromises()
    expect(axios.get.mock.lastCall[1].params).toMatchObject({ page: 1, pageSize: 20 })
  })

  it('does not offer status changes for sold or reserved products', async () => {
    axios.get.mockResolvedValue(list([{ id: 2, title: '待见面商品', status: 0, pendingOrder: true }, { id: 3, title: '已成交商品', status: 1 }]))
    await start(AdminProducts)
    expect(wrapper.findAll('tbody button').map(button => button.text())).toEqual(['详情', '详情'])
    expect(wrapper.find('tbody').text()).toContain('交易中')
  })

  it('offers deletion only for cancelled order records', async () => {
    axios.get.mockResolvedValue(list([{ id: 1, status: 0 }, { id: 2, status: 3 }, { id: 3, status: 4 }]))
    confirmAction.mockResolvedValue(false)
    await start(AdminOrders)
    expect(wrapper.findAll('tbody button')).toHaveLength(1)
    await wrapper.find('tbody button').trigger('click')
    await flushPromises()
    expect(axios.delete).not.toHaveBeenCalled()
  })

  it('moves back a page when deleting the final result on the last page', async () => {
    axios.get.mockResolvedValueOnce(list([{ id: 1, status: 4 }], 11))
      .mockResolvedValueOnce(list([{ id: 11, status: 4 }], 11))
      .mockResolvedValueOnce(list([], 10))
      .mockResolvedValueOnce(list([{ id: 1, productTitle: '上一页订单', status: 4 }], 10))
    await start(AdminOrders)
    await wrapper.findAll('.pagination-button')[1].trigger('click')
    await flushPromises()
    await wrapper.find('tbody button').trigger('click')
    await flushPromises()
    expect(axios.get.mock.lastCall[1].params.page).toBe(1)
    expect(wrapper.text()).toContain('上一页订单')
  })

  it('disables deleting a category that still has products', async () => {
    axios.get.mockResolvedValue(response([{ id: 1, name: 'books', description: '教材书籍', productCount: 8 }]))
    await start(AdminCategories)
    const remove = wrapper.findAll('tbody button')[1]
    expect(remove.attributes('disabled')).toBeDefined()
    expect(remove.attributes('title')).toContain('还有商品')
    await remove.trigger('click')
    expect(axios.delete).not.toHaveBeenCalled()
  })
})
