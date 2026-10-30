import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import axios from 'axios'
import AdminDashboard from '../views/Admin/AdminDashboard.vue'
import AdminProducts from '../views/Admin/AdminProducts.vue'

vi.mock('axios', () => ({ default: { get: vi.fn() } }))
vi.mock('../utils/feedback', () => ({ notify: vi.fn() }))
vi.mock('../constants/categories', () => ({
  loadCategories: async () => [{ value: 'books', label: '教材书籍' }],
  categoryLabel: value => value,
}))

const response = data => ({ data: { code: 200, data } })
const productList = (title = '测试商品', total = 65) => response({ items: [{ id: 1, title, status: 0, price: 20 }], total })
const deferred = () => {
  let resolve
  const promise = new Promise(done => { resolve = done })
  return { promise, resolve }
}
let wrapper
async function start(component) {
  wrapper = mount(component, { global: { stubs: { RouterLink: true } } })
  await flushPromises()
}

beforeEach(() => {
  vi.clearAllMocks()
  localStorage.clear()
  sessionStorage.clear()
  sessionStorage.setItem('token', 'demo-admin-token')
  sessionStorage.setItem('user', JSON.stringify({ username: 'admin', role: 1 }))
})
afterEach(() => wrapper?.unmount())

describe('admin overview', () => {
  it('loads actual statistics including zero values', async () => {
    axios.get.mockResolvedValue(response({ userCount: 12, productCount: 65, orderCount: 81, messageCount: 0 }))
    await start(AdminDashboard)
    expect(wrapper.findAll('.stat-number').map(node => node.text())).toEqual(['12', '65', '81', '0'])
    expect(axios.get).toHaveBeenCalledWith('/api/admin/stats', { headers: { Authorization: 'Bearer demo-admin-token' } })
  })

  it('shows a failed request and reloads statistics without leaving permanent loading placeholders', async () => {
    axios.get.mockRejectedValueOnce({ response: { data: { message: '统计服务暂时不可用' } } })
      .mockResolvedValueOnce(response({ userCount: 12, productCount: 65, orderCount: 81, messageCount: 38 }))
    await start(AdminDashboard)
    expect(wrapper.find('[role="alert"]').text()).toContain('统计服务暂时不可用')
    expect(wrapper.find('.stat-number').text()).toBe('—')
    await wrapper.find('.stats-error button').trigger('click')
    await flushPromises()
    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
    expect(wrapper.find('.stat-number').text()).toBe('12')
  })

  it('does not treat a business error as an empty successful result', async () => {
    axios.get.mockResolvedValue({ data: { code: 403, message: '权限不足' } })
    await start(AdminDashboard)
    expect(wrapper.find('[role="alert"]').text()).toContain('权限不足')
    expect(wrapper.find('.refresh-stats').attributes('disabled')).toBeUndefined()
  })

  it('blocks repeated refresh requests while loading', async () => {
    const pending = deferred()
    axios.get.mockReturnValue(pending.promise)
    await start(AdminDashboard)
    expect(wrapper.find('.refresh-stats').attributes('disabled')).toBeDefined()
    await wrapper.find('.refresh-stats').trigger('click')
    expect(axios.get).toHaveBeenCalledOnce()
    pending.resolve(response({ userCount: 12 }))
    await flushPromises()
    expect(wrapper.find('.refresh-stats').attributes('disabled')).toBeUndefined()
  })
})

describe('admin product filtering', () => {
  it.each(['search', 'category', 'status'])('resets to the first page when changing %s', async kind => {
    axios.get.mockResolvedValue(productList())
    await start(AdminProducts)
    await wrapper.findAll('.pagination-button').find(button => button.text() === '下一页').trigger('click')
    await flushPromises()
    expect(axios.get.mock.lastCall[1].params.page).toBe(2)
    if (kind === 'search') {
      await wrapper.find('.search-input').setValue('教材')
      await wrapper.find('form.admin-toolbar').trigger('submit')
    } else if (kind === 'category') {
      await wrapper.findAll('select')[0].setValue('books')
    } else {
      await wrapper.findAll('select')[1].setValue('3')
    }
    await flushPromises()
    expect(axios.get.mock.lastCall[1].params.page).toBe(1)
  })

  it('ignores an older response when filters change quickly', async () => {
    const older = deferred(), newer = deferred()
    axios.get.mockResolvedValueOnce(productList()).mockReturnValueOnce(older.promise).mockReturnValueOnce(newer.promise)
    await start(AdminProducts)
    await wrapper.findAll('select')[0].setValue('books')
    await wrapper.findAll('select')[1].setValue('3')
    newer.resolve(productList('当前筛选结果'))
    await flushPromises()
    older.resolve(productList('过期筛选结果'))
    await flushPromises()
    expect(wrapper.text()).toContain('当前筛选结果')
    expect(wrapper.text()).not.toContain('过期筛选结果')
  })

  it('distinguishes an empty result from a server failure and supports retry', async () => {
    axios.get.mockResolvedValueOnce(response({ items: [], total: 0 }))
      .mockRejectedValueOnce({ response: { data: { message: '登录已过期' } } })
      .mockResolvedValueOnce(productList('重新加载的商品'))
    await start(AdminProducts)
    expect(wrapper.find('[role="status"]').text()).toContain('没有找到符合条件的商品')
    await wrapper.find('form.admin-toolbar').trigger('submit')
    await flushPromises()
    expect(wrapper.find('.error-container').text()).toContain('登录已过期')
    await wrapper.find('.retry-button').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('重新加载的商品')
  })
})
