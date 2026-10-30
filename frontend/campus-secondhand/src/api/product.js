import { request } from './http'

export async function getProductList(params = {}) {
  const q = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value) !== '') q.set(key, value)
  })
  const suffix = q.toString() ? `?${q}` : ''
  return await request('/products/list' + suffix)
}

export async function getHomeStats() {
  return await request('/stats')
}

export async function getMyProducts(params = {}) {
  return await request('/products/my?' + new URLSearchParams(params))
}

export async function getProductsByCategory(category) {
  return await request(`/products/category/${encodeURIComponent(category)}`)
}

export async function searchProducts(keyword) {
  return await request(`/products/search?keyword=${encodeURIComponent(keyword)}`)
}

export async function getProductDetail(id) {
  return await request(`/products/${id}`)
}

export async function createProduct(params, requestKey) {
  return await request('/products/create', {
    method: 'POST',
    headers: requestKey ? { 'Idempotency-Key': requestKey } : {},
    body: JSON.stringify(params)
  })
}

export async function updateProduct(id, params) {
  return await request(`/products/update/${id}`, {
    method: 'PUT',
    body: JSON.stringify(params)
  })
}

export async function offShelfProduct(id) {
  return await request(`/products/off-shelf/${id}`, { method: 'PUT' })
}

export async function deleteProduct(id) {
  return await request(`/products/delete/${id}`, {
    method: 'DELETE'
  })
}

export async function markAsSold(id) {
  return await request(`/products/${id}/sold`, {
    method: 'PUT'
  })
}

export function splitImages(images) {
  if (!images) return []
  const out = []
  let rest = images
  while (rest) {
    if (rest.startsWith('data:')) {
      const meta = rest.indexOf(',')
      if (meta < 0) {
        out.push(rest)
        break
      }
      const sep = rest.indexOf(',', meta + 1)
      if (sep < 0) {
        out.push(rest)
        break
      }
      out.push(rest.slice(0, sep))
      rest = rest.slice(sep + 1)
    } else {
      const sep = rest.indexOf(',')
      if (sep < 0) {
        out.push(rest)
        break
      }
      out.push(rest.slice(0, sep))
      rest = rest.slice(sep + 1)
    }
  }
  return out.map(item => item.trim()).filter(Boolean)
}

export async function getMyFavorites() {
  return await request('/favorites/my')
}

export async function addFavorite(productId) {
  return await request('/favorites', {
    method: 'POST',
    body: JSON.stringify({ productId })
  })
}

export async function removeFavorite(id) {
  return await request(`/favorites/${id}`, { method: 'DELETE' })
}

export default { 
  getProductList, 
  getMyProducts, 
  getProductsByCategory, 
  searchProducts, 
  getProductDetail,
  createProduct,
  updateProduct,
  deleteProduct,
  markAsSold
}
