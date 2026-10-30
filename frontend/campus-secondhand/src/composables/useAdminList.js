import { computed, onMounted, onUnmounted, ref } from 'vue'
import axios from 'axios'
import { getToken } from '../utils/session'

export function adminHeaders() {
  return { Authorization: 'Bearer ' + getToken() }
}

export function requireAdminData(response, fallback = '操作失败，请重试') {
  if (response.data?.code !== 200) throw new Error(response.data?.message || fallback)
  return response.data.data
}

export function adminError(error) {
  return error.response?.data?.message || error.message || '暂时无法加载，请稍后重试'
}

export function useAdminList(endpoint, filters = () => ({})) {
  const items = ref([]), total = ref(0), page = ref(1), pageSize = ref(10)
  const loading = ref(false), error = ref('')
  const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))
  let requestId = 0

  async function load() {
    const request = ++requestId
    loading.value = true
    error.value = ''
    try {
      const response = await axios.get(endpoint, {
        headers: adminHeaders(),
        params: { ...filters(), page: page.value, pageSize: pageSize.value },
      })
      if (request !== requestId) return
      const data = requireAdminData(response, '列表加载失败')
      total.value = data?.total ?? 0
      if (page.value > totalPages.value) {
        page.value = totalPages.value
        return await load()
      }
      items.value = data?.items || []
    } catch (err) {
      if (request !== requestId) return
      items.value = []
      total.value = 0
      error.value = adminError(err)
    } finally {
      if (request === requestId) loading.value = false
    }
  }

  function applyFilters() { page.value = 1; return load() }
  function changePage(next) {
    if (next >= 1 && next <= totalPages.value && next !== page.value && !loading.value) {
      page.value = next
      return load()
    }
  }
  function resizePage(size) { pageSize.value = size; return applyFilters() }
  onMounted(load)
  onUnmounted(() => { requestId++ })
  return { items, total, page, pageSize, totalPages, loading, error, load, applyFilters, changePage, resizePage }
}
