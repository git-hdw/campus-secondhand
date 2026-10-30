import { ref } from 'vue'

export const CATEGORY_FALLBACK = [
  { value: 'books', label: '教材书籍' },
  { value: 'electronics', label: '电子产品' },
  { value: 'transport', label: '出行工具' },
  { value: 'gaming', label: '游戏数码' },
  { value: 'clothing', label: '服饰穿搭' },
  { value: 'living', label: '生活用品' },
  { value: 'other', label: '其他' }
]

export const categoryOptions = ref(CATEGORY_FALLBACK.slice())

export function categoryLabel(value) {
  const row = categoryOptions.value.find(item => item.value === value)
  return row ? row.label : (value || '未分类')
}

export async function loadCategories() {
  try {
    const res = await fetch('/api/categories')
    const data = await res.json()
    const rows = Array.isArray(data.data) ? data.data : []
    const list = rows
      .filter(row => row && row.name && row.status !== 0)
      .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
      .map(row => ({ value: row.name, label: row.description || row.name }))
    if (list.length) categoryOptions.value = list
  } catch (e) {
    categoryOptions.value = CATEGORY_FALLBACK.slice()
  }
  return categoryOptions.value.slice()
}
