import { request } from './http'

export async function getOrderList(params = {}) {
  return await request('/orders/my?' + new URLSearchParams(params))
}

export async function getOrderDetail(orderId) {
  return await request(`/orders/${orderId}`)
}

export async function confirmOrder(orderId) {
  return await request(`/orders/${orderId}/confirm`, { method: 'PUT' })
}

export async function cancelOrder(orderId) {
  return await request(`/orders/${orderId}/cancel`, { method: 'PUT' })
}

export async function createOrder(productId) {
  return await request('/orders', {
    method: 'POST',
    body: JSON.stringify({ productId })
  })
}
