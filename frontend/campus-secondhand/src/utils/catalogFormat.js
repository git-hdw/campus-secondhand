export function formatCatalogAmount(value) {
  const amount = Number(String(value ?? '').replace(/[¥￥,\s]/g, ''))
  return Number.isFinite(amount) && value !== null && value !== undefined && value !== ''
    ? amount.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
    : '—'
}
