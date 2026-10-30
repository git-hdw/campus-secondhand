<template>
  <div class="post-page">
    <main class="main-content workspace-page">
      <PageHeading
        :title="editingId ? '编辑商品' : '发布闲置'"
        description="让闲置物品找到新主人"
      />
      <div class="post-layout">
        <div class="form-container">
          <form @submit.prevent="handleSubmit" class="post-form">
            <div class="form-section">
              <h2 class="section-title">基本信息</h2>

              <div class="form-group">
                <label class="form-label" for="product-title"
                  >商品标题 <span class="required">*</span></label
                >
                <input
                  type="text"
                  v-model="form.title"
                  id="product-title"
                  aria-label="商品标题"
                  class="form-input"
                  placeholder="请输入商品标题，2到60个字"
                  maxlength="60"
                  required
                />
                <span class="char-count">{{ form.title.length }}/60</span>
              </div>

              <div class="form-row">
                <div class="form-group">
                  <label class="form-label" for="product-category"
                    >商品分类 <span class="required">*</span></label
                  >
                  <ElSelect
                    v-model="form.category"
                    id="product-category"
                    aria-label="商品分类"
                    aria-required="true"
                    class="post-select"
                    popper-class="post-select-menu"
                    placeholder="请选择分类"
                  >
                    <ElOption
                      v-for="cat in categories"
                      :key="cat.value"
                      :value="cat.value"
                      :label="cat.label"
                    />
                  </ElSelect>
                </div>

                <div class="form-group">
                  <label class="form-label" for="product-condition"
                    >商品成色 <span class="required">*</span></label
                  >
                  <ElSelect
                    v-model="form.condition"
                    id="product-condition"
                    aria-label="商品成色"
                    aria-required="true"
                    class="post-select"
                    popper-class="post-select-menu"
                    placeholder="请选择成色"
                  >
                    <ElOption
                      v-for="condition in conditions"
                      :key="condition"
                      :value="condition"
                      :label="condition"
                    />
                  </ElSelect>
                </div>
              </div>

              <div class="form-row">
                <div class="form-group">
                  <label class="form-label" for="product-price"
                    >商品价格 <span class="required">*</span></label
                  >
                  <div class="price-input-wrapper">
                    <span class="price-symbol">¥</span>
                    <input
                      type="number"
                      v-model="form.price"
                      id="product-price"
                      aria-label="商品价格"
                      class="form-input price-input"
                      placeholder="0.00"
                      min="0.01"
                      step="0.01"
                      required
                    />
                  </div>
                </div>

                <div class="form-group">
                  <label class="form-label" for="product-original-price">原价（可选）</label>
                  <div class="price-input-wrapper">
                    <span class="price-symbol">¥</span>
                    <input
                      type="number"
                      v-model="form.originalPrice"
                      id="product-original-price"
                      aria-label="原价"
                      class="form-input price-input"
                      placeholder="0.00"
                      min="0"
                      step="0.01"
                    />
                  </div>
                  <p class="form-hint">原价高于售价时，会在详情中展示。</p>
                </div>
              </div>
              <div class="form-group">
                <label class="form-label" for="product-place"
                  >见面地点 <span class="required">*</span></label
                >
                <ElSelect
                  v-model="form.tradePlace"
                  id="product-place"
                  aria-label="见面地点"
                  aria-required="true"
                  class="post-select"
                  popper-class="post-select-menu"
                  placeholder="请选择见面地点"
                >
                  <ElOption
                    v-for="place in tradePlaces"
                    :key="place"
                    :value="place"
                    :label="place"
                  />
                </ElSelect>
              </div>
            </div>

            <div class="form-section">
              <h2 class="section-title" id="product-description-label">
                商品描述 <span class="required">*</span>
              </h2>

              <div class="form-group">
                <textarea
                  v-model="form.description"
                  id="product-description"
                  aria-labelledby="product-description-label"
                  class="form-textarea"
                  placeholder="请详细描述商品的状态、使用时间、转手原因等，让买家更了解商品情况..."
                  rows="6"
                  maxlength="500"
                  required
                ></textarea>
                <span class="char-count">{{ form.description.length }}/500</span>
              </div>
            </div>

            <div class="form-section">
              <h2 class="section-title">商品图片</h2>

              <div class="form-group">
                <div class="image-upload">
                  <div v-for="(img, index) in form.images" :key="index" class="uploaded-image">
                    <img :src="img" :alt="'商品图片' + (index + 1)" />
                    <button
                      type="button"
                      class="remove-image"
                      :aria-label="'移除图片 ' + (index + 1)"
                      @click="removeImage(index)"
                    >
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <line x1="18" y1="6" x2="6" y2="18" />
                        <line x1="6" y1="6" x2="18" y2="18" />
                      </svg>
                    </button>
                  </div>
                  <label class="upload-button" v-if="form.images.length < 9">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                      <polyline points="17 8 12 3 7 8" />
                      <line x1="12" y1="3" x2="12" y2="15" />
                    </svg>
                    <span>上传图片</span>
                    <input
                      type="file"
                      accept="image/*"
                      @change="handleImageUpload"
                      class="upload-input"
                      aria-label="上传商品图片"
                    />
                  </label>
                </div>
                <p class="form-hint">
                  {{
                    compressing
                      ? '正在压缩图片，请稍候…'
                      : '最多 9 张，支持 JPG、PNG、WebP，单张原图不超过 10MB，自动压缩后保存。'
                  }}
                </p>
              </div>
            </div>

            <div class="form-actions">
              <button type="button" class="btn-secondary" @click="saveDraft">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z" />
                  <polyline points="17 21 17 13 7 13 7 21" />
                  <polyline points="7 3 7 8 15 8" />
                </svg>
                保存草稿
              </button>
              <button type="submit" class="btn-primary" :disabled="submitting || compressing">
                <span v-if="!submitting">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <line x1="22" y1="2" x2="11" y2="13" />
                    <polygon points="22 2 15 22 11 13 2 9 22 2" />
                  </svg>
                  {{ editingId ? '保存修改' : '提交审核' }}
                </span>
                <span v-else class="loading">
                  <div class="btn-spinner"></div>
                  发布中...
                </span>
              </button>
            </div>
          </form>
        </div>
        <aside class="form-section tips-section" aria-label="发布须知">
          <h2 class="section-title">发布须知</h2>
          <ul class="tips-list">
            <li>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                <polyline points="22 4 12 14.01 9 11.01" />
              </svg>
              <span>商品信息需真实准确，不得虚假宣传</span>
            </li>
            <li>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                <polyline points="22 4 12 14.01 9 11.01" />
              </svg>
              <span>建议上传实物图片，展示商品真实状态</span>
            </li>
            <li>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                <polyline points="22 4 12 14.01 9 11.01" />
              </svg>
              <span>定价合理，描述详细，更容易成交</span>
            </li>
            <li>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                <polyline points="22 4 12 14.01 9 11.01" />
              </svg>
              <span>发布后先待审核，通过后才会公开。当面交易，平台不收款。</span>
            </li>
          </ul>
        </aside>
      </div>
    </main>

    <SiteFooter />

    <el-dialog
      v-model="showSuccess"
      class="publish-result-dialog"
      :title="editingId ? '修改已保存' : '已提交审核'"
      width="460px"
      align-center
      :show-close="false"
      :close-on-click-modal="false"
    >
      <div class="modal-content">
        <div class="success-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
            <polyline points="22 4 12 14.01 9 11.01" />
          </svg>
        </div>
        <h2>{{ editingId ? '修改已保存' : '已提交审核' }}</h2>
        <p>
          {{
            editingId
              ? '商品内容已更新，可在我的发布中查看。'
              : '管理员审核通过后，商品才会公开展示。你可以在「我的发布」查看状态。'
          }}
        </p>
        <div class="modal-actions">
          <button class="btn-secondary" @click="go('/profile')">查看我的发布</button>
          <button class="btn-primary" @click="continuePost">继续发布</button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import PageHeading from '../components/PageHeading.vue'
import SiteFooter from '../components/SiteFooter.vue'
defineOptions({ name: 'PostMarketplaceProduct' })
import { ref, reactive, computed, onMounted } from 'vue'
import { ElSelect, ElOption } from 'element-plus'
import { notify } from '../utils/feedback'
import { readEditingDraft, saveDraftData, removePublishedDraft } from '../utils/drafts'
import { useRouter, useRoute } from 'vue-router'
import { createProduct, updateProduct, getProductDetail, splitImages } from '../api/product'
import { loadCategories } from '../constants/categories'

const router = useRouter()
const categories = ref([])
const route = useRoute()
const editingId = ref(null)
const submitting = ref(false)
const showSuccess = ref(false)
const draftId = ref(null)
const compressing = ref(false)
const conditions = ['全新', '几乎全新', '轻微使用痕迹', '明显使用痕迹', '有磨损']
const presetTradePlaces = ['图书馆', '教学楼', '食堂', '宿舍楼下', '操场']
const originalTradePlace = ref('')
const tradePlaces = computed(() =>
  originalTradePlace.value && !presetTradePlaces.includes(originalTradePlace.value)
    ? [originalTradePlace.value, ...presetTradePlaces]
    : presetTradePlaces,
)

const form = reactive({
  title: '',
  category: '',
  condition: '',
  price: '',
  originalPrice: '',
  tradePlace: '',
  description: '',
  images: [],
})

function go(path) {
  router.push(path)
}

async function handleImageUpload(event) {
  const file = event.target.files[0]
  event.target.value = ''
  if (!file || compressing.value) return
  if (!/^image\/(jpeg|png|webp)$/.test(file.type)) {
    notify('请选择 JPG、PNG 或 WebP 图片')
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    notify('单张原图不能超过 10MB')
    return
  }
  if (form.images.length >= 9) {
    notify('最多上传 9 张图片')
    return
  }
  compressing.value = true
  try {
    const bitmap = await createImageBitmap(file)
    const canvas = document.createElement('canvas')
    let size = 800
    let encoded = ''
    for (let attempt = 0; attempt < 5; attempt++) {
      const scale = Math.min(1, size / Math.max(bitmap.width, bitmap.height))
      canvas.width = Math.max(1, Math.round(bitmap.width * scale))
      canvas.height = Math.max(1, Math.round(bitmap.height * scale))
      const ctx = canvas.getContext('2d')
      ctx.fillStyle = '#fff'
      ctx.fillRect(0, 0, canvas.width, canvas.height)
      ctx.drawImage(bitmap, 0, 0, canvas.width, canvas.height)
      encoded = canvas.toDataURL('image/jpeg', 0.7 - attempt * 0.08)
      if (encoded.length <= 21000) break
      size = Math.round(size * 0.75)
    }
    bitmap.close()
    if (encoded.length > 21000 || form.images.join(',').length + encoded.length + 1 > 200000) {
      notify('图片内容仍然过大，请选择更简单或更小的图片')
      return
    }
    form.images.push(encoded)
  } catch {
    notify('图片读取失败，请重新选择')
  } finally {
    compressing.value = false
  }
}

function removeImage(index) {
  form.images.splice(index, 1)
}

let publishAttempt = null

async function handleSubmit() {
  if (submitting.value || compressing.value) return
  if (!form.title.trim() || form.title.trim().length < 2 || !form.description.trim()) {
    notify('标题至少 2 个字，描述不能为空')
    return
  }
  if (!Number.isFinite(Number(form.price)) || Number(form.price) <= 0) {
    notify('价格必须大于 0')
    return
  }
  if (
    !form.title ||
    !form.category ||
    !form.condition ||
    !form.price ||
    !form.tradePlace ||
    !form.description
  ) {
    notify('请填写所有必填项')
    return
  }

  submitting.value = true

  try {
    const payload = {
      title: form.title,
      category: form.category,
      condition: form.condition,
      price: parseFloat(form.price),
      originalPrice: form.originalPrice ? parseFloat(form.originalPrice) : null,
      tradePlace: form.tradePlace,
      description: form.description,
      images: form.images.join(','),
    }
    const fingerprint = JSON.stringify(payload)
    if (!publishAttempt || publishAttempt.fingerprint !== fingerprint) {
      publishAttempt = { fingerprint, key: crypto.randomUUID() }
    }
    const res = editingId.value
      ? await updateProduct(editingId.value, payload)
      : await createProduct(payload, publishAttempt.key)

    if (res.code === 200) {
      publishAttempt = null
      removePublishedDraft(draftId.value)
      draftId.value = null
      showSuccess.value = true
      form.title = ''
      form.category = ''
      form.condition = ''
      form.price = ''
      form.originalPrice = ''
      form.tradePlace = ''
      form.description = ''
      form.images = []
    } else {
      notify(res.message || '发布失败，请重试')
    }
  } catch (e) {
    notify(e.message || '发布失败，请重试')
  } finally {
    submitting.value = false
  }
}

function saveDraft() {
  try {
    const draft = saveDraftData({ ...form, images: [...form.images], draftId: draftId.value })
    draftId.value = draft.draftId
    notify('草稿已保存，仅此浏览器中的当前账号可见')
  } catch {
    notify('浏览器空间不足，草稿未保存。请减少图片后重试')
  }
}

function continuePost() {
  originalTradePlace.value = ''
  draftId.value = null
  editingId.value = null
  router.replace('/post')
  showSuccess.value = false
  form.title = ''
  form.category = ''
  form.condition = ''
  form.price = ''
  form.originalPrice = ''
  form.tradePlace = ''
  form.description = ''
  form.images = []
}

onMounted(async () => {
  categories.value = await loadCategories()
  if (route.query.id) {
    editingId.value = route.query.id
    try {
      const res = await getProductDetail(route.query.id)
      if (res.code === 200 && res.data) {
        form.title = res.data.title || ''
        form.category = res.data.category || ''
        form.condition = res.data.condition || ''
        form.price = res.data.price ?? ''
        form.originalPrice = res.data.originalPrice ?? ''
        form.tradePlace = res.data.tradePlace || ''
        originalTradePlace.value = form.tradePlace
        form.description = res.data.description || ''
        form.images = splitImages(res.data.images)
        return
      }
    } catch (e) {
      notify(e.message || '读取商品失败')
    }
  }
  const draft = readEditingDraft()
  if (draft) {
    for (const key of Object.keys(form)) {
      if (draft[key] !== undefined) form[key] = draft[key]
    }
    draftId.value = draft.draftId || null
    originalTradePlace.value = form.tradePlace
  }
})
</script>

<style src="../styles/post-product.css"></style>
