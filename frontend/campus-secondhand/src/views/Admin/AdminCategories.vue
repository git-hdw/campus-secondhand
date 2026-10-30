<script setup>
import { computed, onMounted, ref } from 'vue'
import axios from 'axios'
import { ElDialog } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import AdminListState from '../../components/AdminListState.vue'
import { adminHeaders, requireAdminData, adminError } from '../../composables/useAdminList'
import { notify, confirmAction } from '../../utils/feedback'

const categories = ref([]), loading = ref(false), error = ref(''), busy = ref(false)
const newCategory = ref({ name: '', description: '' }), editingCategory = ref(null), showEdit = ref(false)
const productCount = computed(() => categories.value.reduce((count, item) => count + (item.productCount || 0), 0))
async function load() {
  loading.value = true
  error.value = ''
  try { categories.value = requireAdminData(await axios.get('/api/admin/categories', { headers: adminHeaders() })) || [] }
  catch (err) { error.value = adminError(err) }
  finally { loading.value = false }
}
function validateName(name) { return name.trim().length >= 1 && name.trim().length <= 20 }
async function addCategory() {
  if (busy.value) return
  const name = newCategory.value.name.trim(), description = newCategory.value.description.trim()
  if (!/^[a-z0-9]{2,20}$/.test(name)) { notify('分类编码需为 2～20 位小写字母或数字'); return }
  if (!validateName(description)) { notify('显示名称需为 1～20 个字'); return }
  busy.value = true
  try {
    requireAdminData(await axios.post('/api/admin/categories', { name, description }, { headers: adminHeaders() }))
    newCategory.value = { name: '', description: '' }
    notify('分类已添加')
    await load()
  } catch (err) { notify(adminError(err)) }
  finally { busy.value = false }
}
function editCategory(category) { editingCategory.value = { ...category, description: category.description || category.name }; showEdit.value = true }
async function saveCategory() {
  if (busy.value || !editingCategory.value) return
  const description = editingCategory.value.description.trim()
  if (!validateName(description)) { notify('显示名称需为 1～20 个字'); return }
  busy.value = true
  try {
    requireAdminData(await axios.put('/api/admin/categories/' + editingCategory.value.id, { description }, { headers: adminHeaders() }))
    showEdit.value = false
    notify('分类已保存')
    await load()
  } catch (err) { notify(adminError(err)) }
  finally { busy.value = false }
}
async function deleteCategory(category) {
  if (busy.value || category.productCount > 0) return
  busy.value = true
  try {
    if (!await confirmAction('确认删除「' + (category.description || category.name) + '」吗？删除后无法恢复。', '删除分类', '确认删除')) return
    requireAdminData(await axios.delete('/api/admin/categories/' + category.id, { headers: adminHeaders() }))
    notify('分类已删除')
    await load()
  } catch (err) { notify(adminError(err)) }
  finally { busy.value = false }
}
onMounted(load)
</script>

<template>
  <main class="admin-categories admin-page">
    <div class="admin-page-heading"><div><h1>分类管理</h1><p class="admin-page-description">维护学生浏览商品时看到的分类。</p></div><span class="admin-total">{{ categories.length }} 个分类 · {{ productCount }} 件商品</span></div>
    <div class="admin-categories-grid">
      <section class="admin-panel">
        <AdminListState :loading="loading" :error="error" :empty="!categories.length" empty-text="还没有商品分类" @retry="load">
          <div class="admin-table-scroll"><table class="admin-table">
            <thead><tr><th scope="col">分类名称</th><th scope="col">商品数量</th><th scope="col">操作</th></tr></thead>
            <tbody><tr v-for="category in categories" :key="category.id" class="category-item">
              <td><span class="admin-cell-title">{{ category.description || category.name }}</span><span class="admin-cell-sub">{{ category.name }}</span></td>
              <td><span class="admin-category-count">{{ category.onSaleCount || 0 }}<small>件在售</small></span><span class="admin-cell-sub">共 {{ category.productCount || 0 }} 件商品</span></td>
              <td><div class="admin-row-actions"><button class="admin-btn small" :disabled="busy" @click="editCategory(category)">编辑</button><button class="admin-btn small danger" :disabled="busy || category.productCount > 0" :title="category.productCount > 0 ? '分类下还有商品，不能删除' : '删除空分类'" @click="deleteCategory(category)">删除</button></div></td>
            </tr></tbody>
          </table></div>
        </AdminListState>
      </section>
      <form class="admin-panel admin-category-form" @submit.prevent="addCategory">
        <h2>添加分类</h2>
        <div class="admin-form-field"><label class="admin-form-label" for="category-label">显示名称</label><input id="category-label" v-model="newCategory.description" class="admin-form-input" maxlength="20" placeholder="例如：生活用品" required /><p class="admin-form-hint">学生在商品列表中看到的名称。</p></div>
        <div class="admin-form-field"><label class="admin-form-label" for="category-code">分类编码</label><input id="category-code" v-model="newCategory.name" class="admin-form-input" maxlength="20" placeholder="例如：living" pattern="[a-z0-9]{2,20}" required /><p class="admin-form-hint">2～20 位小写字母或数字，创建后不能修改。</p></div>
        <button class="admin-btn primary" type="submit" :disabled="busy"><Plus aria-hidden="true" />{{ busy ? '处理中…' : '添加分类' }}</button>
      </form>
    </div>
    <ElDialog v-model="showEdit" title="编辑分类" width="460px" class="admin-detail-dialog" :close-on-click-modal="false" align-center>
      <form v-if="editingCategory" id="edit-category-form" @submit.prevent="saveCategory">
        <p class="admin-form-hint">分类编码：{{ editingCategory.name }}</p>
        <div class="admin-form-field"><label class="admin-form-label" for="edit-category-label">显示名称</label><input id="edit-category-label" v-model="editingCategory.description" class="admin-form-input" maxlength="20" required /></div>
      </form>
      <template #footer><button class="admin-btn" :disabled="busy" @click="showEdit = false">取消</button><button class="admin-btn primary" style="margin-left: 10px" type="submit" form="edit-category-form" :disabled="busy">{{ busy ? '保存中…' : '保存修改' }}</button></template>
    </ElDialog>
  </main>
</template>
