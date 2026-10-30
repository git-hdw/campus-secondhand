import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { reactive } from 'vue'
import OrderCheckout from '../views/OrderCheckout.vue'
import ProductDetail from '../views/ProductDetail.vue'
import { getProductDetail, splitImages } from '../api/product'
import { createOrder, getOrderDetail, getOrderList } from '../api/order'

const navigation = vi.hoisted(() => ({ route: null, push: vi.fn(), replace: vi.fn() }))
vi.mock('vue-router', () => ({
  useRoute: () => navigation.route,
  useRouter: () => ({ push: navigation.push, replace: navigation.replace }),
}))
vi.mock('../api/product', () => ({ getProductDetail: vi.fn(), splitImages: vi.fn(() => ['/images/demo-catalog/study-laptop.jpg']),
  getMyFavorites: async () => ({ data: [] }), addFavorite: vi.fn(), removeFavorite: vi.fn() }))
vi.mock('../api/order', () => ({ createOrder: vi.fn(), getOrderDetail: vi.fn(), getOrderList: vi.fn() }))
vi.mock('../api/user', () => ({ getUserById: async () => ({ data: { id: 2, username: '小林' } }) }))
vi.mock('../constants/categories', () => ({ categoryLabel: () => '电子产品', loadCategories: vi.fn() }))
vi.mock('../utils/feedback', () => ({ notify: vi.fn() }))

const item = { id: 128, userId: 2, title: '学习办公笔记本电脑', price: 1680, status: 0, pendingOrder: false,
  condition: '轻微使用痕迹', tradePlace: '图书馆门口', createdTime: '2026-10-15T14:00:00' }
const order = { id: 88, orderNo: 'ORD-DEMO-88', productId: 128, buyerId: 12, sellerId: 2,
  price: 1680, status: 0, createdTime: '2026-10-15T15:00:00' }
const response = data => ({ code: 200, data })
const deferred = () => {
  let resolve
  const promise = new Promise(done => { resolve = done })
  return { promise, resolve }
}
let wrapper
async function start(result = false, component = OrderCheckout) {
  navigation.route = reactive({ name: result ? 'OrderResult' : 'OrderCheckout',
    params: result ? { orderId: '88' } : { productId: '128', id: '128' }, query: {} })
  wrapper = mount(component, { global: { stubs: { RouterLink: {
    props: ['to'], template: '<a :href="typeof to === \'string\' ? to : to.path"><slot /></a>',
  } } } })
  await flushPromises()
}
const confirm = () => wrapper.find('.checkout-total .flow-primary')

beforeEach(() => {
  vi.clearAllMocks()
  localStorage.clear(); sessionStorage.clear()
  sessionStorage.setItem('user', JSON.stringify({ id: 12, username: '体验账号' }))
  getProductDetail.mockResolvedValue(response({ ...item }))
  getOrderDetail.mockResolvedValue(response({ ...order }))
  getOrderList.mockResolvedValue(response([]))
  createOrder.mockResolvedValue(response({ ...order }))
})
afterEach(() => wrapper?.unmount())

describe('separate order confirmation and result pages', () => {
  it('shows the purchased snapshot without requesting changed or deleted product data', async () => {
    getOrderDetail.mockResolvedValue(response({ ...order, snapshotJson: JSON.stringify({ ...item, title: '下单时的商品', tradePlace: '原约定地点' }) }))
    await start(true)
    expect(wrapper.text()).toContain('下单时的商品')
    expect(wrapper.text()).toContain('原约定地点')
    expect(getProductDetail).not.toHaveBeenCalled()
    expect(createOrder).not.toHaveBeenCalled()
  })
  it('opens a dedicated confirmation route instead of replacing the product detail buttons', async () => {
    await start(false, ProductDetail)
    await wrapper.find('.action-btn.buy').trigger('click')
    expect(navigation.push).toHaveBeenCalledWith('/checkout/128')
    expect(wrapper.text()).not.toContain('确认下单')
    expect(createOrder).not.toHaveBeenCalled()
  })

  it('shows product, meeting instructions and amount before creating any order', async () => {
    await start()
    expect(wrapper.find('h1').text()).toBe('确认这件好物')
    expect(wrapper.text()).toContain('图书馆门口')
    expect(wrapper.find('.amount-total').text()).toContain('1680.00')
    expect(wrapper.text()).toContain('平台不收款')
    expect(createOrder).not.toHaveBeenCalled()
    await wrapper.find('.back-to-product').trigger('click')
    expect(navigation.push).toHaveBeenCalledWith('/products/128')
  })

  it('guards repeated clicks and navigates to the real created order', async () => {
    await start()
    const pending = deferred()
    createOrder.mockReturnValueOnce(pending.promise)
    await confirm().trigger('click')
    await flushPromises()
    expect(confirm().element.disabled).toBe(true)
    await confirm().trigger('click')
    expect(createOrder).toHaveBeenCalledOnce()
    pending.resolve(response({ ...order }))
    await flushPromises()
    expect(navigation.replace).toHaveBeenCalledWith({ name: 'OrderResult', params: { orderId: '88' } })
  })

  it('requires reconfirmation when the product price changes before submitting', async () => {
    await start()
    getProductDetail.mockResolvedValue(response({ ...item, price: 1500 }))
    await confirm().trigger('click')
    await flushPromises()
    expect(createOrder).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('商品价格已更新')
    expect(wrapper.find('.amount-total').text()).toContain('1500.00')
    await confirm().trigger('click')
    await flushPromises()
    expect(createOrder).toHaveBeenCalledOnce()
  })

  it('blocks a newly reserved item instead of displaying a false success', async () => {
    await start()
    getProductDetail.mockResolvedValue(response({ ...item, pendingOrder: true }))
    await confirm().trigger('click')
    await flushPromises()
    expect(createOrder).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('已有待见面订单')
    expect(navigation.replace).not.toHaveBeenCalled()
    expect(confirm().element.disabled).toBe(true)
  })

  it('recovers an order created successfully when the original response times out', async () => {
    await start()
    createOrder.mockRejectedValueOnce(new Error('请求超时'))
    getOrderList.mockResolvedValue(response([{ ...order }]))
    await confirm().trigger('click')
    await flushPromises()
    expect(createOrder).toHaveBeenCalledOnce()
    expect(navigation.replace).toHaveBeenCalledWith({ name: 'OrderResult', params: { orderId: '88' } })
  })

  it('keeps an unverified failure visible without claiming success', async () => {
    await start()
    createOrder.mockRejectedValueOnce(new Error('服务器暂时不可用'))
    await confirm().trigger('click')
    await flushPromises()
    expect(wrapper.find('[role="alert"]').text()).toContain('服务器暂时不可用')
    expect(navigation.replace).not.toHaveBeenCalled()
  })

  it('uses the stored order price and reloads the result without submitting a second order', async () => {
    getProductDetail.mockResolvedValue(response({ ...item, price: 1999 }))
    await start(true)
    expect(wrapper.find('h1').text()).toContain('下单成功')
    expect(wrapper.find('.result-price').text()).toBe('¥1680.00')
    expect(wrapper.text()).toContain('ORD-DEMO-88')
    expect(createOrder).not.toHaveBeenCalled()
    await wrapper.find('.result-actions button').trigger('click')
    expect(navigation.push.mock.calls[0][0].query).toMatchObject({ sellerId: 2, productId: 128 })
    wrapper.unmount()
    await start(true)
    expect(getOrderDetail).toHaveBeenCalledTimes(2)
    expect(createOrder).not.toHaveBeenCalled()
  })

  it('does not show success or private order data when access is denied', async () => {
    getOrderDetail.mockRejectedValueOnce(new Error('无权访问此订单'))
    await start(true)
    expect(wrapper.find('[role="alert"]').text()).toContain('无权访问此订单')
    expect(wrapper.find('.order-result').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('ORD-DEMO-88')
    expect(createOrder).not.toHaveBeenCalled()
  })

  it('shows the actual cancelled state when an old success URL is reopened', async () => {
    getOrderDetail.mockResolvedValue(response({ ...order, status: 4 }))
    await start(true)
    expect(wrapper.find('h1').text()).toBe('订单已取消')
    expect(wrapper.find('.result-actions button').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('下单成功')
  })
})


describe('compact product detail permissions and gallery', () => {
  it('shows one reservation explanation while keeping contact and favorite available', async () => {
    getProductDetail.mockResolvedValue(response({ ...item, pendingOrder: true }))
    await start(false, ProductDetail)
    expect(wrapper.find('.action-btn.buy').exists()).toBe(false)
    expect(wrapper.findAll('.pending-hint')).toHaveLength(1)
    expect(wrapper.find('.contact-seller-btn').exists()).toBe(true)
    expect(wrapper.find('.favorite').exists()).toBe(true)
    expect(wrapper.find('.product-status-badge').text()).toBe('待见面')
  })
  it.each([{ status: 1, label: '已售' }, { status: 2, label: '下架' }])('prevents buying $label products', async ({ status, label }) => {
    getProductDetail.mockResolvedValue(response({ ...item, status }))
    await start(false, ProductDetail)
    expect(wrapper.find('.action-btn.buy').exists()).toBe(false)
    expect(wrapper.find('.product-status-badge').text()).toBe(label)
    expect(wrapper.find('.contact-seller-btn').exists()).toBe(true)
  })
  it('prevents self purchase and self contact', async () => {
    getProductDetail.mockResolvedValue(response({ ...item, userId: 12 }))
    await start(false, ProductDetail)
    expect(wrapper.find('.action-btn.buy').exists()).toBe(false)
    expect(wrapper.find('.contact-seller-btn').exists()).toBe(false)
    expect(wrapper.text()).toContain('这是你发布的商品')
  })
  it('preserves guest login redirects', async () => {
    sessionStorage.clear()
    await start(false, ProductDetail)
    await wrapper.find('.favorite').trigger('click')
    expect(navigation.push).toHaveBeenLastCalledWith({ path: '/login', query: { redirect: '/products/128' } })
    await wrapper.find('.contact-seller-btn').trigger('click')
    expect(navigation.push.mock.lastCall[0].query.redirect).toContain('/messages?')
  })
  it('preserves full text and switches accessible thumbnail buttons with error fallback', async () => {
    splitImages.mockReturnValue(['/first.jpg', '/second.jpg'])
    const title = '很长的商品标题'.repeat(8), description = '详细商品描述。'.repeat(80)
    getProductDetail.mockResolvedValue(response({ ...item, title, description }))
    await start(false, ProductDetail)
    expect(wrapper.find('h1').text()).toBe(title)
    expect(wrapper.find('.product-description').text()).toBe(description)
    const thumbnails = wrapper.findAll('button.thumbnail')
    expect(thumbnails).toHaveLength(2)
    await thumbnails[1].trigger('click')
    expect(thumbnails[1].attributes('aria-pressed')).toBe('true')
    expect(wrapper.find('.main-image img').attributes('src')).toBe('/second.jpg')
    await wrapper.find('.main-image img').trigger('error')
    expect(wrapper.find('[aria-label="暂无图片"]').exists()).toBe(true)
    splitImages.mockReturnValue(['/images/demo-catalog/study-laptop.jpg'])
  })
  it('shows an honest placeholder when the product has no images', async () => {
    splitImages.mockReturnValue([])
    await start(false, ProductDetail)
    expect(wrapper.find('[aria-label="暂无图片"]').exists()).toBe(true)
    expect(wrapper.find('.main-image img').exists()).toBe(false)
    splitImages.mockReturnValue(['/images/demo-catalog/study-laptop.jpg'])
  })
})
