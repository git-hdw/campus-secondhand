import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import App from '../App.vue'
import Home from '../views/Home.vue'
import { getHomeStats, getProductList } from '../api/product'
import { getUnreadCount } from '../api/message'
import { logout } from '../api/auth'

vi.mock('../api/auth', () => ({ logout: vi.fn().mockResolvedValue({ code: 200 }) }))

vi.mock('../api/product', () => ({ getProductList: vi.fn(), getHomeStats: vi.fn(), splitImages: value => value ? [value] : [] }))
vi.mock('../api/message', () => ({ getUnreadCount: vi.fn() }))
vi.mock('../constants/categories', () => ({ CATEGORY_FALLBACK: [{ value: 'books', label: '教材书籍' }], loadCategories: async () => [{ value: 'books', label: '教材书籍' }] }))
const dummy = { template: '<main><h1>测试页面</h1><button>正文按钮</button></main>' }
let wrapper
let router
async function start({ width = 1440, path = '/', home = false } = {}) {
  window.innerWidth = width
  router = createRouter({ history: createMemoryHistory(), routes: [
    { path: '/', component: home ? Home : dummy },
    ...['/products', '/products/:id', '/browse', '/checkout/:id', '/orders/:id/result', '/post', '/drafts', '/messages', '/profile', '/help', '/login', '/register', '/forgot'].map(path => ({ path, component: dummy })),
    { path: '/admin', component: dummy, meta: { requiresAdmin: true } },
    { path: '/admin/:section', component: dummy, meta: { requiresAdmin: true } },
  ] })
  await router.push(path)
  await router.isReady()
  wrapper = mount(App, { attachTo: document.body, global: { plugins: [router, createPinia()] } })
  await flushPromises()
}
beforeEach(() => {
  vi.clearAllMocks()
  localStorage.clear()
  sessionStorage.clear()
  getUnreadCount.mockResolvedValue({ code: 200, data: { count: 104 } })
  getHomeStats.mockResolvedValue({ code: 200, data: { onSaleProducts: 5, categoryCounts: [{ category: 'books', count: 5 }] } })
  getProductList.mockResolvedValue({ code: 200, data: { items: [{ id: 1, title: '一本教材', price: 12, images: '/book.jpg', status: 0, pendingOrder: true, tradePlace: '图书馆' }] } })
})
afterEach(() => { wrapper?.unmount(); document.body.innerHTML = ''; document.body.style.overflow = '' })

describe('shared sidebar', () => {
  it('defaults to expanded desktop navigation and allows collapsing without an overlay', async () => {
    await start()
    expect(wrapper.find('.app-shell').attributes('style')).toContain('224px')
    expect(wrapper.find('[aria-label="管理"]').exists()).toBe(false)
    expect(wrapper.find('[aria-label="登录"]').exists()).toBe(true)
    await wrapper.find('[aria-label="收起导航"]').trigger('click')
    expect(wrapper.find('.app-shell').attributes('style')).toContain('64px')
    expect(wrapper.find('.navigation-scrim').exists()).toBe(false)
  })
  it('opens the rail as an overlay, isolates content and restores focus on Escape', async () => {
    await start({ width: 640 })
    await wrapper.find('[aria-label="展开导航"]').trigger('click')
    await flushPromises()
    expect(wrapper.find('.app-shell').attributes('style')).toContain('64px')
    expect(wrapper.find('[aria-modal="true"]').exists()).toBe(true)
    expect(wrapper.find('.app-body').attributes('inert')).toBeDefined()
    expect(document.body.style.overflow).toBe('hidden')
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    await flushPromises()
    expect(wrapper.find('.navigation-scrim').exists()).toBe(false)
    expect(document.activeElement.getAttribute('aria-label')).toBe('展开导航')
    expect(document.body.style.overflow).toBe('')
  })
  it('closes the mobile drawer on route change and hides navigation on auth pages', async () => {
    await start({ width: 390 })
    expect(wrapper.find('.app-shell').attributes('style')).toContain('0px')
    expect(wrapper.find('#site-navigation').isVisible()).toBe(false)
    await wrapper.find('[aria-label="打开导航"]').trigger('click')
    await wrapper.find('[aria-label="商品"]').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/products')
    expect(wrapper.find('#site-navigation').isVisible()).toBe(false)
    expect(document.activeElement.id).toBe('page-content')
    await router.push('/login')
    await flushPromises()
    expect(wrapper.find('#site-navigation').exists()).toBe(false)
    expect(wrapper.find('.mobile-brandbar').exists()).toBe(false)
  })
  it('traps drawer keyboard focus and closes cleanly across breakpoints', async () => {
    await start({ width: 640 })
    await wrapper.find('[aria-label="展开导航"]').trigger('click')
    await flushPromises()
    const first = wrapper.find('[aria-label="校园二手首页"]')
    const last = wrapper.find('[aria-label="登录"]')
    first.element.focus()
    await first.trigger('keydown', { key: 'Tab', shiftKey: true })
    expect(document.activeElement).toBe(last.element)
    await last.trigger('keydown', { key: 'Tab' })
    expect(document.activeElement).toBe(first.element)
    window.innerWidth = 1200
    window.dispatchEvent(new Event('resize'))
    await flushPromises()
    expect(wrapper.find('.navigation-scrim').exists()).toBe(false)
    expect(wrapper.find('.app-shell').attributes('style')).toContain('224px')
    expect(document.body.style.overflow).toBe('')
  })
  it('keeps admin access and unread counts in sync with logout', async () => {
    sessionStorage.setItem('token', 'test-token')
    sessionStorage.setItem('user', JSON.stringify({ username: '管理员', role: 1 }))
    await start({ path: '/admin' })
    expect(wrapper.find('[aria-label="管理"]').attributes('aria-current')).toBe('page')
    expect(wrapper.find('.admin-workspace-nav').exists()).toBe(true)
    expect(wrapper.find('.app-body .admin-workspace-nav').exists()).toBe(true)
    expect(wrapper.find('.nav-badge').text()).toBe('99+')
    await wrapper.find('[aria-label="退出登录"]').trigger('click')
    await flushPromises()
    expect(sessionStorage.getItem('token')).toBeNull()
    expect(logout).toHaveBeenCalled()
    expect(router.currentRoute.value.path).toBe('/login')
  })
  it.each([['/browse', '商品'], ['/checkout/1', '商品'], ['/drafts', '发布'], ['/orders/1/result', '我的']])('highlights the parent navigation for %s', async (path, label) => {
    await start({ path })
    expect(wrapper.find('[aria-label="' + label + '"]').attributes('aria-current')).toBe('page')
  })
})

describe('market homepage', () => {
  it('shows actual category counts, pending labels, prices and compatible search routes', async () => {
    await start({ home: true })
    expect(wrapper.find('.market-count').text()).toContain('5 件')
    expect(wrapper.find('.market-category small').text()).toBe('5')
    expect(wrapper.find('.market-pending').text()).toBe('待见面')
    expect(wrapper.find('.market-item-price').text()).toBe('¥12')
    expect(wrapper.find('.market-category[href="/products?category=books"]').exists()).toBe(true)
    await wrapper.find('[aria-label="搜索商品"]').setValue('  教材  ')
    await wrapper.find('.market-search').trigger('submit')
    await flushPromises()
    expect(router.currentRoute.value.query).toEqual({ search: '教材' })
  })
  it('does not invent zero counts if statistics fail, while showing the product empty state', async () => {
    getHomeStats.mockRejectedValue(new Error('offline'))
    getProductList.mockResolvedValue({ code: 200, data: { items: [] } })
    await start({ home: true })
    expect(wrapper.find('.market-count').exists()).toBe(false)
    expect(wrapper.find('.market-category small').exists()).toBe(false)
    expect(wrapper.find('.market-empty a').attributes('href')).toBe('/post')
  })
  it('shows skeletons during loading, exposes failures and supports retry', async () => {
    let reject
    getProductList.mockImplementationOnce(() => new Promise((_, fail) => { reject = fail }))
    await start({ home: true })
    expect(wrapper.findAll('.market-skeleton')).toHaveLength(8)
    reject(new Error('暂时无法加载'))
    await flushPromises()
    expect(wrapper.find('[role="alert"]').text()).toContain('暂时无法加载')
    await wrapper.find('.market-state button').trigger('click')
    await flushPromises()
    expect(wrapper.findAll('.market-item')).toHaveLength(1)
    expect(wrapper.find('[role="alert"]').exists()).toBe(false)
  })
  it('handles broken and absent images without losing item links or titles', async () => {
    const title = '这是一件有很长标题的校园闲置商品'.repeat(4)
    getProductList.mockResolvedValue({ code: 200, data: { items: [
      { id: 1, title: '损坏图片', price: 12, images: '/broken.jpg' },
      { id: 2, title, price: 0, images: '', tradePlace: '' },
    ] } })
    await start({ home: true })
    await wrapper.find('.market-item-image img').trigger('error')
    expect(wrapper.find('.market-image-placeholder').text()).toBe('暂无图片')
    const cards = wrapper.findAll('.market-item')[1]
    expect(cards.find('h3').attributes('title')).toBe(title)
    expect(cards.attributes('href')).toBe('/products/2')
    expect(cards.find('.market-item-price').text()).toBe('¥0')
    expect(cards.find('.market-item-place').text()).toBe('见面地点待商议')
  })
})
