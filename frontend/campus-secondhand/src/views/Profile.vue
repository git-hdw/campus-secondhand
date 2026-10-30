<template>
  <div class="profile-page">
    <main class="main-content workspace-page">
      <PageHeading title="个人中心" description="管理发布、收藏与订单" />
      <div class="profile-header">
        <div class="profile-card">
          <div class="avatar-section">
            <div class="avatar-wrapper">
              <img :src="userInfo.avatar" alt="用户头像" class="avatar" />
              <div class="avatar-ring"></div>
            </div>
            <div class="user-badges">
              <span v-if="userInfo.studentNo" class="badge verified">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                  <polyline points="22 4 12 14.01 9 11.01"/>
                </svg>
                已登记学号
              </span>
              <span v-if="!userInfo.studentNo" class="badge level">未填学号</span>
            </div>
          </div>
          
          <div class="user-info">
            <h2 class="user-name">{{ userInfo.username }}</h2>
            <p class="user-bio">{{ userInfo.bio || '还没有填写简介' }}</p>
            <div class="user-meta">
              <div v-if="userInfo.location" class="meta-item">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>
                  <circle cx="12" cy="10" r="3"/>
                </svg>
                <span>{{ userInfo.location }}</span>
              </div>
              <div class="meta-item">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
                  <line x1="16" y1="2" x2="16" y2="6"/>
                  <line x1="8" y1="2" x2="8" y2="6"/>
                  <line x1="3" y1="10" x2="21" y2="10"/>
                </svg>
                <span>加入于 {{ userInfo.joinDate || '—' }}</span>
              </div>
            </div>
          </div>
          
          <div class="user-stats">
            <div class="stat-item">
              <div class="stat-value">{{ stats.listings }}</div>
              <div class="stat-label">在售</div>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <div class="stat-value">{{ stats.sold }}</div>
              <div class="stat-label">已售</div>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <div class="stat-value">{{ stats.favorites }}</div>
              <div class="stat-label">收藏</div>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <div class="stat-value">{{ stats.views }}</div>
              <div class="stat-label">被浏览</div>
            </div>
          </div>
          
          <div class="profile-actions">
            <button class="action-btn-primary" @click="go('/post')">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="12" y1="5" x2="12" y2="19"/>
                <line x1="5" y1="12" x2="19" y2="12"/>
              </svg>
              发布商品
            </button>
            <button class="action-btn-secondary" @click="editProfile">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
              </svg>
              编辑资料
            </button>
          </div>
          <p v-if="pageNotice" class="page-notice">{{ pageNotice }}</p>
        </div>
      </div>

      <SessionPanel />
      <div class="profile-tabs">
        <button 
          v-for="tab in tabs" 
          :key="tab.id"
          class="tab-btn"
          :class="{ active: activeTab === tab.id }"
          @click="activeTab = tab.id"
        >
          <component :is="tab.icon" />
          <span>{{ tab.name }}</span>
          <span class="tab-count">{{ tab.count }}</span>
        </button>
      </div>

      <div v-if="loading" class="review-loading" role="status">正在加载个人资料与订单…</div>
      <div v-else class="tab-content">
        <div v-if="activeTab === 'listings'" class="listings-grid catalog-grid">
          <div 
            v-for="(item, idx) in myListings" 
            :key="idx" 
            class="listing-card catalog-card"
            @click="viewProduct(item)"
          >
            <div class="listing-image catalog-picture">
              <img :src="item.image" :alt="item.title" />
              <div class="listing-status catalog-status" :class="item.status">{{ item.statusText }}</div>
            </div>
            <div class="listing-info catalog-body">
              <h3 class="listing-title catalog-title">{{ item.title }}</h3>
              <div class="listing-meta">
                <span class="listing-price catalog-price"><span class="currency">¥</span>{{ formatCatalogAmount(item.price) }}</span>
                <span class="listing-views">{{ item.views }} 次浏览</span>
              </div>
              <p class="catalog-location"><Location aria-hidden="true" />{{ item.place || '见面地点待商议' }}</p>
              <div class="listing-actions catalog-actions">
                <button v-if="item.rawStatus === 0 && !item.pendingOrder" class="listing-btn edit" @click.stop="offShelf(item)">下架</button>
                <button v-if="[0, 3].includes(item.rawStatus) && !item.pendingOrder" class="listing-btn edit" aria-label="编辑商品" @click.stop="editProduct(item)">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                  </svg>
                </button>
                <button class="listing-btn delete" aria-label="删除商品" @click.stop="deleteProduct(item)">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="3 6 5 6 21 6"/>
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                  </svg>
                </button>
              </div>
            </div>
          </div>
          <div v-if="myListings.length === 0" class="empty-state">
            <div class="empty-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="9" cy="21" r="1"/>
                <circle cx="20" cy="21" r="1"/>
                <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/>
              </svg>
            </div>
            <h3>还没有发布商品</h3>
            <p>发布后先由管理员审核，通过才会出现在商品列表。</p>
            <button class="empty-btn" @click="go('/post')">去发布</button>
          </div>
        </div>

        <div v-if="activeTab === 'favorites'" class="favorites-grid catalog-grid">
          <div 
            v-for="(item, idx) in myFavorites" 
            :key="idx" 
            class="favorite-card catalog-card"
            @click="viewProduct(item)"
          >
            <div class="favorite-image catalog-picture">
              <img :src="item.image" :alt="item.title" />
            </div>
            <div class="favorite-info catalog-body">
              <h3 class="favorite-title catalog-title">{{ item.title }}</h3>
              <div class="favorite-price catalog-price"><span class="currency">¥</span>{{ formatCatalogAmount(item.price) }}</div>
              <div class="favorite-meta">
                <span class="favorite-seller">{{ item.seller }}</span>
                <span class="favorite-time">{{ item.time }}</span>
              </div>
            </div>
            <button class="favorite-remove" aria-label="取消收藏" @click.stop="removeFavorite(item)">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18"/>
                <line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>
          <div v-if="myFavorites.length === 0" class="empty-state">
            <div class="empty-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78l1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
              </svg>
            </div>
            <h3>还没有收藏商品</h3>
            <p>浏览商品时点击收藏，随时关注心仪物品</p>
            <button class="empty-btn" @click="go('/products')">去逛逛</button>
          </div>
        </div>

        <div v-if="activeTab === 'orders'" class="orders-list">
          <div class="order-filters">
            <button type="button" :class="{ active: orderFilter === 'all' }" @click="orderFilter = 'all'">全部</button>
            <button type="button" :class="{ active: orderFilter === 'pending' }" @click="orderFilter = 'pending'">待见面</button>
            <button type="button" :class="{ active: orderFilter === 'completed' }" @click="orderFilter = 'completed'">已完成</button>
            <button type="button" :class="{ active: orderFilter === 'cancelled' }" @click="orderFilter = 'cancelled'">已取消</button>
          </div>
          <div 
            v-for="(order, idx) in visibleOrders"
            :key="order.id || idx"
            class="order-card"
          >
            <div class="order-header">
              <div class="order-info">
                <span class="order-id">订单号：{{ order.orderNo || order.id }}</span>
                <span class="order-date">{{ order.date }}</span>
              </div>
              <div class="order-status" :class="order.status">{{ order.statusText }}</div>
            </div>
            <div class="order-content">
              <div class="order-image">
                <img :src="order.image || '/sample/phone.svg'" :alt="order.title" />
              </div>
              <div class="order-details">
                <h3 class="order-title">{{ order.title }}</h3>
                <div class="order-meta">
                  <span class="order-seller">{{ order.buyerId === userInfo.id ? '买入' : '卖出' }}</span>
                  <span class="order-seller">{{ order.buyerId === userInfo.id ? '卖家：' + order.seller : '买家：' + order.buyer }}</span>
                </div>
              </div>
              <div class="order-price-section">
                <div class="order-price">{{ order.price }}</div>
              </div>
            </div>
            <div class="order-footer">
              <details v-if="order.status === 'pending' && !order.missing && order.buyerId === userInfo.id" class="receipt-help">
                <summary><svg viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.4" aria-hidden="true"><circle cx="10" cy="10" r="7"/><path d="M10 9v5M10 6v1"/></svg>收货须知<span class="help-chevron" aria-hidden="true">⌄</span></summary>
                <p>请在见面验货并收下商品后，再确认收货。确认只记录你的本人声明，系统不核验位置；完成后无法撤销。</p>
              </details>
              <div class="order-actions">
                <button class="order-btn secondary" type="button" @click="router.push('/orders/' + order.id + '/result')">查看订单</button>
                <button v-if="!order.missing && order.buyerId === userInfo.id" class="order-btn secondary" type="button" @click="contactSeller(order)">联系卖家</button>
                <button v-if="order.status === 'pending'" class="order-btn danger" type="button" :disabled="orderBusy" @click="cancelOrderItem(order)">取消订单</button>
                <button v-if="order.status === 'pending' && !order.missing && order.buyerId === userInfo.id" class="order-btn primary" type="button" :disabled="orderBusy" @click="confirmReceive(order)">确认收货</button>
              </div>
            </div>
          </div>
          <button v-if="hiddenDeletedCount > 0" type="button" class="deleted-toggle" @click="showDeleted = !showDeleted">
            {{ showDeleted ? '收起已删除商品的旧订单' : '还有 ' + hiddenDeletedCount + ' 条已删除商品的旧订单' }}
          </button>
          <div v-if="filteredOrders.length === 0" class="empty-state">
            <div class="empty-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <polyline points="14 2 14 8 20 8"/>
                <line x1="16" y1="13" x2="8" y2="13"/>
                <line x1="16" y1="17" x2="8" y2="17"/>
              </svg>
            </div>
            <h3>还没有订单</h3>
            <p>购买商品后，订单会显示在这里</p>
            <button class="empty-btn" @click="go('/products')">去购物</button>
          </div>
        </div>

        <ListPagination v-if="activeTab === 'listings'" :page="listingPage" :total="listingTotal" :busy="listingBusy" @change="loadMyProducts" />
        <ListPagination v-if="activeTab === 'orders'" :page="orderPage" :total="orderTotal" :busy="orderListBusy || orderBusy" @change="loadOrders" />
        <ListPagination v-if="activeTab === 'favorites'" :page="favoritePage" :total="favoriteTotal" :busy="favoriteBusy" @change="loadFavorites" />
        <div v-if="activeTab === 'history'" class="history-list">
          <div 
            v-for="(item, idx) in browseHistory" 
            :key="idx" 
            class="history-card"
            @click="viewProduct(item)"
          >
            <div class="history-image">
              <img :src="item.image" :alt="item.title" />
            </div>
            <div class="history-info">
              <h3 class="history-title">{{ item.title }}</h3>
              <div class="history-meta">
                <span class="history-price">{{ item.price }}</span>
                <span class="history-time">{{ item.time }}</span>
              </div>
            </div>
          </div>
          <div v-if="browseHistory.length === 0" class="empty-state">
            <div class="empty-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <polyline points="12 6 12 12 16 14"/>
              </svg>
            </div>
            <h3>浏览记录为空</h3>
            <p>浏览商品时会自动记录</p>
            <button class="empty-btn" @click="go('/products')">去逛逛</button>
          </div>
        </div>

        <div v-if="activeTab === 'drafts'" class="drafts-list">
          <div 
            v-for="(draft, idx) in myDrafts" 
            :key="idx" 
            class="draft-card"
            @click="editDraft(draft)"
          >
            <div class="draft-image">
              <img v-if="draft.images && draft.images.length > 0" :src="draft.images[0]" :alt="draft.title" />
              <div v-else class="draft-placeholder">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                  <circle cx="8.5" cy="8.5" r="1.5"/>
                  <polyline points="21 15 16 10 5 21"/>
                </svg>
              </div>
              <span v-if="draft.images && draft.images.length > 1" class="draft-image-count">{{ draft.images.length }}</span>
            </div>
            <div class="draft-info">
              <h3 class="draft-title">{{ draft.title || '未命名商品' }}</h3>
              <div class="draft-meta">
                <span class="draft-category">{{ getCategoryName(draft.category) }}</span>
                <span class="draft-condition">{{ draft.condition }}</span>
              </div>
              <div class="draft-price" v-if="draft.price">¥{{ draft.price }}</div>
              <div class="draft-date">保存于 {{ formatDate(draft.savedAt) }}</div>
            </div>
            <div class="draft-actions">
              <button class="draft-btn edit" @click.stop="editDraft(draft)">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                  <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                </svg>
                编辑
              </button>
              <button class="draft-btn delete" @click.stop="deleteDraft(idx)">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="3 6 5 6 21 6"/>
                  <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                </svg>
                删除
              </button>
            </div>
          </div>
          <div v-if="myDrafts.length === 0" class="empty-state">
            <div class="empty-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <polyline points="14 2 14 8 20 8"/>
                <line x1="16" y1="13" x2="8" y2="13"/>
                <line x1="16" y1="17" x2="8" y2="17"/>
              </svg>
            </div>
            <h3>草稿箱为空</h3>
            <p>草稿只保存在这台浏览器，换电脑或清除站点数据后就没有了</p>
            <button class="empty-btn" @click="go('/post')">去发布</button>
          </div>
        </div>
      </div>
    </main>

    <SiteFooter />

    <Teleport to="body">
      <Transition name="modal">
        <div v-if="showEditModal" class="modal-overlay" @click.self="closeEditModal">
          <div class="edit-modal">
            <div class="modal-header">
              <h2>编辑资料</h2>
              <button class="close-btn" @click="closeEditModal">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="18" y1="6" x2="6" y2="18"/>
                  <line x1="6" y1="6" x2="18" y2="18"/>
                </svg>
              </button>
            </div>
            
            <div class="modal-body">
              <div class="avatar-upload-section">
                <div class="avatar-upload">
                  <img :src="editForm.avatar" alt="头像" class="avatar-preview" />
                  <div class="avatar-edit-overlay">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/>
                      <circle cx="12" cy="13" r="4"/>
                    </svg>
                    <span>更换头像</span>
                  </div>
                  <input type="file" accept="image/*" class="avatar-input" @change="handleAvatarChange" />
                </div>
                <p class="avatar-tip">支持 JPG、PNG 格式，大小不超过 2MB</p>
              </div>
              
              <div class="form-section">
                <div class="form-group">
                  <label>用户名</label>
                  <input 
                    type="text" 
                    v-model="editForm.username" aria-label="用户名"
                    placeholder="请输入用户名"
                    maxlength="20"
                  />
                  <span class="char-count">{{ editForm.username.length }}/20</span>
                </div>
                <div class="form-group">
                  <label>学号</label>
                  <input v-model="editForm.studentNo" aria-label="学号" type="text" maxlength="20" placeholder="6到20位字母或数字，自己填写，不向他人公开" />
                </div>
                
                <div class="form-group">
                  <label>个人简介</label>
                  <textarea 
                    v-model="editForm.bio" aria-label="个人简介"
                    placeholder="介绍一下自己吧..."
                    maxlength="200"
                    rows="3"
                  ></textarea>
                  <span class="char-count">{{ editForm.bio.length }}/200</span>
                </div>
                
                <div class="form-group">
                  <label>所在位置</label>
                  <input 
                    type="text" 
                    v-model="editForm.location" aria-label="所在校区"
                    placeholder="如：XX大学XX校区"
                    maxlength="50"
                  />
                </div>
                
                <div class="form-group">
                  <label>联系QQ</label>
                  <input 
                    type="text" 
                    v-model="editForm.qq" aria-label="QQ"
                    placeholder="请输入QQ号"
                    maxlength="15"
                  />
                </div>
                
                <div class="form-group">
                  <label>联系微信</label>
                  <input 
                    type="text" 
                    v-model="editForm.wechat" aria-label="微信"
                    placeholder="请输入微信号"
                    maxlength="30"
                  />
                </div>
                <div class="form-group">
                  <label>原密码（修改密码时填写）</label>
                  <input v-model="editForm.oldPassword" aria-label="原密码" type="password" placeholder="不修改请留空" />
                </div>
                <div class="form-group">
                  <label>新密码</label>
                  <input v-model="editForm.newPassword" aria-label="新密码" type="password" placeholder="6 到 20 位" />
                </div>
              </div>
            </div>
            
            <div class="modal-footer">
              <button class="btn-cancel" @click="closeEditModal">取消</button>
              <button class="btn-save" @click="saveProfile" :disabled="isSaving">
                <svg v-if="isSaving" class="spinner" viewBox="0 0 24 24">
                  <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="3" fill="none" stroke-dasharray="31.4 31.4" stroke-linecap="round"/>
                </svg>
                <span v-if="!isSaving">保存</span>
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
import ListPagination from '../components/ListPagination.vue'
import SessionPanel from '../components/SessionPanel.vue'
import PageHeading from '../components/PageHeading.vue'
import SiteFooter from '../components/SiteFooter.vue'
import { Location } from '@element-plus/icons-vue'
import { formatCatalogAmount } from '../utils/catalogFormat'
defineOptions({ name: 'UserProfile' })
import { ref, computed, h, onMounted, watch, markRaw } from 'vue'
import { notify, confirmAction } from '../utils/feedback'
import { readDrafts, setEditingDraft, removeDraftAt } from '../utils/drafts'
import { useRouter, useRoute } from 'vue-router'
import { updateProfile, getProfile } from '../api/user'
import { getMyProducts, deleteProduct as deleteProductAPI, offShelfProduct, splitImages } from '../api/product'
import { getOrderList, confirmOrder, cancelOrder } from '../api/order'
import { categoryLabel, loadCategories } from '../constants/categories'

const router = useRouter()
const route = useRoute()
function go(path) { router.push(path) }

const activeTab = ref('listings')
watch(activeTab, (tab) => {
  if (route.query.tab !== tab) router.replace({ path: '/profile', query: { tab } })
})
const loading = ref(true)
const orderBusy = ref(false)
watch(() => route.query.tab, (tab) => { if (tab) activeTab.value = String(tab) })

const userInfo = ref({
  id: null,
  username: localStorage.getItem('username') || '用户',
  avatar: 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40"%3E%3Ccircle cx="20" cy="20" r="20" fill="%23333"/%3E%3Ccircle cx="20" cy="16" r="6" fill="%23666"/%3E%3Cpath d="M8 36c0-6.6 5.4-12 12-12s12 5.4 12 12" fill="%23666"/%3E%3C/svg%3E',
  bio: '',
  location: '',
  joinDate: '',
  studentNo: ''
})

const stats = ref({
  listings: 0,
  sold: 0,
  favorites: 0,
  views: 0
})

const ShoppingBag = {
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
      h('path', { d: 'M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z' }),
      h('line', { x1: '3', y1: '6', x2: '21', y2: '6' }),
      h('path', { d: 'M16 10a4 4 0 0 1-8 0' })
    ])
  }
}

const Heart = {
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
      h('path', { d: 'M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78l1.06-1.06a5.5 5.5 0 0 0 0-7.78z' })
    ])
  }
}

const List = {
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
      h('line', { x1: '8', y1: '6', x2: '21', y2: '6' }),
      h('line', { x1: '8', y1: '12', x2: '21', y2: '12' }),
      h('line', { x1: '8', y1: '18', x2: '21', y2: '18' }),
      h('line', { x1: '3', y1: '6', x2: '3.01', y2: '6' }),
      h('line', { x1: '3', y1: '12', x2: '3.01', y2: '12' }),
      h('line', { x1: '3', y1: '18', x2: '3.01', y2: '18' })
    ])
  }
}

const Clock = {
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
      h('circle', { cx: '12', cy: '12', r: '10' }),
      h('polyline', { points: '12 6 12 12 16 14' })
    ])
  }
}

const File = {
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' }, [
      h('path', { d: 'M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z' }),
      h('polyline', { points: '14 2 14 8 20 8' }),
      h('line', { x1: '16', y1: '13', x2: '8', y2: '13' }),
      h('line', { x1: '16', y1: '17', x2: '8', y2: '17' })
    ])
  }
}

const tabs = ref([
  { id: 'listings', name: '我的发布', count: 0, icon: markRaw(ShoppingBag) },
  { id: 'favorites', name: '我的收藏', count: 0, icon: markRaw(Heart) },
  { id: 'orders', name: '我的订单', count: 0, icon: markRaw(List) },
  { id: 'history', name: '浏览历史', count: 0, icon: markRaw(Clock) },
  { id: 'drafts', name: '我的草稿', count: 0, icon: markRaw(File) }
])

const myListings = ref([])
const listingPage = ref(1), listingTotal = ref(0), listingBusy = ref(false)
let listingVersion = 0
const myFavorites = ref([])
const myOrders = ref([])
const orderPage = ref(1), orderTotal = ref(0), favoritePage = ref(1), favoriteTotal = ref(0)
const orderListBusy = ref(false), favoriteBusy = ref(false)
let orderLoadVersion = 0, favoriteLoadVersion = 0
const orderFilter = ref('all')
const showDeleted = ref(false)
watch(orderFilter, () => { orderPage.value = 1; void loadOrders() })
const filteredOrders = computed(() => myOrders.value.filter(order => {
  if (orderFilter.value === 'pending') return order.status === 'pending'
  if (orderFilter.value === 'completed') return order.status === 'completed'
  if (orderFilter.value === 'cancelled') return order.status === 'cancelled'
  return true
}))
const hiddenDeletedCount = computed(() => filteredOrders.value.filter(order => order.missing && order.status !== 'pending').length)
const visibleOrders = computed(() => filteredOrders.value.filter(order => showDeleted.value || !order.missing || order.status === 'pending'))
const browseHistory = ref([])

function formatJoinDate(value) {
  const [year, month] = String(value || '').split('-')
  if (!year || !month) return ''
  return `${year}年${Number(month)}月`
}

function loadBrowseHistory() {
  try {
    const list = JSON.parse(localStorage.getItem('browseHistory') || '[]')
    browseHistory.value = Array.isArray(list) ? list : []
  } catch {
    browseHistory.value = []
  }
  const tab = tabs.value.find(t => t.id === 'history')
  if (tab) tab.count = browseHistory.value.length
}
const myDrafts = ref([])

async function loadUserProfile() {
  try {
    const res = await getProfile()
    if (res.code === 200 && res.data) {
      const data = res.data
      userInfo.value.id = data.id
      userInfo.value.username = data.username || userInfo.value.username
      userInfo.value.avatar = data.avatar || userInfo.value.avatar || userInfo.value.avatar
      userInfo.value.bio = data.bio || ''
      userInfo.value.location = data.location || ''
      userInfo.value.qq = data.qq || ''
      userInfo.value.wechat = data.wechat || ''
      userInfo.value.joinDate = formatJoinDate(data.createdTime)
      userInfo.value.studentNo = data.studentNo || ''
    }
  } catch (e) {
    console.error('Failed to load user profile:', e)
  }
}

async function loadMyProducts(page = listingPage.value) {
  const version = ++listingVersion
  listingBusy.value = true
  try {
    const res = await getMyProducts({ page, pageSize: 20 })
    if (version !== listingVersion) return
    if (res.code === 200 && res.data) {
      const products = res.data.items || res.data
      listingPage.value = page
      listingTotal.value = res.data.total ?? products.length
      myListings.value = products.map(p => ({
        id: p.id,
        title: p.title,
        price: `¥${p.price}`,
        image: splitImages(p.images)[0] || '/sample/phone.svg',
        place: p.tradePlace || '',
        rawStatus: p.status,
        pendingOrder: !!p.pendingOrder,
        status: p.status === 0 && p.pendingOrder ? 'pending' : p.status === 0 ? 'selling' : p.status === 3 ? 'pending' : 'sold',
        statusText: p.status === 0 && p.pendingOrder ? '待见面' : p.status === 0 ? '在售' : p.status === 1 ? '已售' : p.status === 2 ? '已下架' : p.status === 3 ? '待审核' : '其他',
        views: p.viewCount || 0
      }))
      
      const listingsCount = products.filter(p => p.status === 0).length
      const soldCount = products.filter(p => p.status === 1).length
      const totalViews = products.reduce((sum, p) => sum + (p.viewCount || 0), 0)
      
      stats.value.listings = res.data.stats?.listings ?? listingsCount
      stats.value.sold = res.data.stats?.sold ?? soldCount
      stats.value.views = res.data.stats?.views ?? totalViews
      
      tabs.value.find(t => t.id === 'listings').count = listingTotal.value
    }
  } catch (e) {
    if (version === listingVersion) pageNotice.value = e.message || '发布列表加载失败'
  } finally { if (version === listingVersion) listingBusy.value = false }
}

function loadDrafts() {
  const drafts = readDrafts()
  myDrafts.value = drafts
  const draftsTab = tabs.value.find(t => t.id === 'drafts')
  if (draftsTab) draftsTab.count = drafts.length
}

async function loadOrders(page = orderPage.value) {
  const version = ++orderLoadVersion
  orderListBusy.value = true
  try {
    const params = { page, pageSize: 20 }
    if (orderFilter.value !== 'all') params.status = { pending: 0, completed: 3, cancelled: 4 }[orderFilter.value]
    const res = await getOrderList(params)
    if (version !== orderLoadVersion) return
    if (res.code === 200 && res.data) {
      orderPage.value = page
      const rows = res.data.items || res.data
      orderTotal.value = res.data.total ?? rows.length
      myOrders.value = rows.map(order => ({
        id: order.id,
        orderNo: order.orderNo,
        sellerId: order.sellerId,
        buyerId: order.buyerId,
        productId: order.productId,
        title: order.productTitle,
        price: `¥${order.price}`,
        image: order.productImage || '/sample/phone.svg',
        status: order.status === 0 ? 'pending' : order.status === 3 ? 'completed' : 'cancelled',
        statusText: order.status === 0 ? '待见面' : order.status === 3 ? '已完成' : order.status === 4 ? '已取消' : '其他',
        seller: order.sellerName,
        buyer: order.buyerName,
        missing: order.productTitle === '商品已删除',
        date: new Date(order.createdAt).toLocaleString('zh-CN')
      }))
      
      const ordersTab = tabs.value.find(t => t.id === 'orders')
      if (ordersTab && orderFilter.value === 'all') ordersTab.count = orderTotal.value
    } else {
      console.error('订单数据格式错误:', res)
    }
  } catch (e) {
    if (version === orderLoadVersion) pageNotice.value = e.message || '订单加载失败，请重试'
  } finally { if (version === orderLoadVersion) orderListBusy.value = false }
}

async function loadAllData() {
  loading.value = true
  await Promise.all([
    loadUserProfile(),
    loadMyProducts(),
    loadDrafts(),
    loadOrders(),
    loadFavorites()
  ])
  loading.value = false
}

function editDraft(draft) {
  setEditingDraft(draft)
  router.push('/post')
}

async function deleteDraft(index) {
  if (await confirmAction('确定要删除这个草稿吗？', '删除草稿', '删除')) {
    removeDraftAt(index)
    myDrafts.value = readDrafts()
    const draftsTab = tabs.value.find(t => t.id === 'drafts')
    if (draftsTab) draftsTab.count = myDrafts.value.length
  }
}

function getCategoryName(category) {
  return categoryLabel(category)
}

function formatDate(timestamp) {
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now - date
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString('zh-CN')
}


function editProfile() {
  openEditModal()
}

function viewProduct(item) {
  router.push(`/products/${item.productId || item.id}`)
}

function editProduct(item) {
  router.push(`/post?id=${item.id}`)
}

async function deleteProduct(item) {
  try {
    await deleteProductAPI(item.id)
    const idx = myListings.value.findIndex(p => p.id === item.id)
    if (idx !== -1) myListings.value.splice(idx, 1)
    await loadMyProducts(Math.max(1, Math.min(listingPage.value, Math.ceil((listingTotal.value - 1) / 20))))
    notify('删除成功！')
  } catch (error) {
    console.error('删除失败:', error)
    notify('删除失败，请稍后重试')
  }
}

async function removeFavorite(item) {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token')
  const res = await fetch('/api/favorites/' + item.id, { method: 'DELETE', headers: { Authorization: 'Bearer ' + token } })
  const data = await res.json()
  if (data.code !== 200) {
    notify(data.message || '取消收藏失败')
    return
  }
  myFavorites.value = myFavorites.value.filter(p => p.id !== item.id)
  favoriteTotal.value = Math.max(0, favoriteTotal.value - 1)
  await loadFavorites(Math.max(1, Math.min(favoritePage.value, Math.ceil(favoriteTotal.value / 20))))
}

async function confirmReceive(order) {
  if (orderBusy.value) return
  if (!await confirmAction('请确认你已经见面验货，并收下这件商品。确认后订单将完成，无法撤销。系统不核验见面位置。', '确认见面收货', '确认已收货')) return
  orderBusy.value = true
  try {
    const res = await confirmOrder(order.id)
    if (res.code === 200) {
      order.status = 'completed'
      order.statusText = '已完成'
      notify('确认收货成功！')
      await loadMyProducts()
    } else {
      notify(res.message || '确认收货失败')
    }
  } catch (error) {
    console.error('确认收货失败:', error)
    notify(error.message || '确认收货失败，请稍后重试')
  } finally {
    orderBusy.value = false
  }
}

async function cancelOrderItem(order) {
  if (orderBusy.value) return
  if (await confirmAction('取消后商品将重新开放给其他同学购买。', '取消订单', '确认取消')) {
    orderBusy.value = true
    try {
      const res = await cancelOrder(order.id)
      if (res.code === 200) {
        order.status = 'cancelled'
        order.statusText = '已取消'
        pageNotice.value = ''
        router.replace({ path: '/profile', query: { tab: 'orders' } })
        notify('取消订单成功！')
      } else {
        notify(res.message || '取消订单失败')
      }
    } catch (error) {
      console.error('取消订单失败:', error)
      notify(error.message || '取消订单失败，请稍后重试')
    } finally {
      orderBusy.value = false
      await loadMyProducts()
    }
  }
}

function contactSeller(order) {
  if (Number(order.sellerId) === Number(userInfo.value.id)) {
    notify('不能咨询自己的商品')
    return
  }
  router.push({
    path: '/messages',
    query: {
      sellerId: order.sellerId,
      seller: order.seller || '',
      productId: order.productId || '',
      productTitle: order.title || ''
    }
  })
}

async function offShelf(item) {
  if (!await confirmAction('下架后其他同学将看不到这件商品。', '下架商品', '确认下架')) return
  try {
    const res = await offShelfProduct(item.id)
    if (res.code === 200) {
      item.rawStatus = 2
      item.statusText = '已下架'
      notify('已下架')
    } else {
      notify(res.message || '下架失败')
    }
  } catch (e) {
    notify(e.message || '下架失败')
  }
}

async function loadFavorites(page = favoritePage.value) {
  const version = ++favoriteLoadVersion
  const token = localStorage.getItem('token') || sessionStorage.getItem('token')
  if (!token) return
  favoriteBusy.value = true
  try {
    const res = await fetch('/api/favorites/my?page=' + page + '&pageSize=20', { headers: { Authorization: 'Bearer ' + token } })
    const data = await res.json()
    if (version !== favoriteLoadVersion) return
    if (!res.ok || data.code !== 200) throw new Error(data.message || '收藏加载失败')
    const rows = data.data.items || data.data
    favoritePage.value = page
    favoriteTotal.value = data.data.total ?? rows.length
    myFavorites.value = rows.map(item => ({
      id: item.id,
      productId: item.productId,
      title: item.title,
      price: item.price === '' ? '' : '¥' + item.price,
      image: item.image || '/sample/phone.svg'
    }))
    const tab = tabs.value.find(t => t.id === 'favorites')
    if (tab) tab.count = favoriteTotal.value
    stats.value.favorites = favoriteTotal.value
  } catch (e) {
    pageNotice.value = e.message || '收藏加载失败，请重试'
  } finally { if (version === favoriteLoadVersion) favoriteBusy.value = false }
}

const showEditModal = ref(false)
const isSaving = ref(false)

const editForm = ref({
  username: '',
  avatar: '',
  bio: '',
  location: '',
  qq: '',
  wechat: '',
  studentNo: '',
  oldPassword: '',
  newPassword: ''
})

function openEditModal() {
  editForm.value = {
    username: userInfo.value.username,
    avatar: userInfo.value.avatar,
    bio: userInfo.value.bio || '',
    location: userInfo.value.location || '',
    qq: userInfo.value.qq || '',
    wechat: userInfo.value.wechat || '',
    studentNo: userInfo.value.studentNo || '',
    oldPassword: '',
    newPassword: ''
  }
  showEditModal.value = true
}

function closeEditModal() {
  showEditModal.value = false
}

function handleAvatarChange(event) {
  const file = event.target.files[0]
  if (!file) return
  
  if (!file.type.match(/image\/(jpeg|png)/)) {
    notify('请选择 JPG 或 PNG 格式的图片')
    return
  }
  
  if (file.size > 2 * 1024 * 1024) {
    notify('图片大小不能超过 2MB')
    return
  }
  
  const reader = new FileReader()
  reader.onload = (e) => {
    compressImage(e.target.result, 200, 200, 0.8, (compressedDataUrl) => {
      editForm.value.avatar = compressedDataUrl
    })
  }
  reader.readAsDataURL(file)
}

function compressImage(dataUrl, maxWidth, maxHeight, quality, callback) {
  const img = new Image()
  img.onload = () => {
    const canvas = document.createElement('canvas')
    let width = img.width
    let height = img.height
    
    if (width > maxWidth || height > maxHeight) {
      if (width / height > maxWidth / maxHeight) {
        height = Math.round(height * maxWidth / width)
        width = maxWidth
      } else {
        width = Math.round(width * maxHeight / height)
        height = maxHeight
      }
    }
    
    canvas.width = width
    canvas.height = height
    
    const ctx = canvas.getContext('2d')
    ctx.drawImage(img, 0, 0, width, height)
    
    callback(canvas.toDataURL('image/jpeg', quality))
  }
  img.src = dataUrl
}

async function saveProfile() {
  if (isSaving.value) return
  if (!editForm.value.username.trim()) {
    notify('用户名不能为空')
    return
  }
  
  if (editForm.value.username.length < 2) {
    notify('用户名至少2个字符')
    return
  }
  
  isSaving.value = true
  
  try {
    const payload = {
      username: editForm.value.username,
      bio: editForm.value.bio,
      location: editForm.value.location,
      qq: editForm.value.qq,
      wechat: editForm.value.wechat,
      studentNo: editForm.value.studentNo,
      oldPassword: editForm.value.oldPassword,
      newPassword: editForm.value.newPassword
    }
    if (editForm.value.avatar && editForm.value.avatar.length <= 60000) {
      payload.avatar = editForm.value.avatar
    }
    const result = await updateProfile(payload)
    
    if (result.code === 200) {
      const data = result.data
      userInfo.value.username = data.username
      userInfo.value.avatar = data.avatar || userInfo.value.avatar
      userInfo.value.bio = data.bio || ''
      userInfo.value.location = data.location || ''
      userInfo.value.qq = data.qq || ''
      userInfo.value.wechat = data.wechat || ''
      userInfo.value.studentNo = data.studentNo || ''
      
      
      showEditModal.value = false
      notify('保存成功！')
    } else {
      notify(result.message || '保存失败')
    }
  } catch (error) {
    console.error('保存失败:', error)
    notify(error.message || '保存失败，请稍后重试')
  } finally {
    isSaving.value = false
  }
}

const pageNotice = ref('')

onMounted(() => {
  loadCategories()
  if (route.query.tab) activeTab.value = String(route.query.tab)
  if (route.query.notice) pageNotice.value = String(route.query.notice)
  loadBrowseHistory()
  loadAllData()
})
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  background: var(--color-background);
  position: relative;
  overflow-x: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.profile-header {
  margin-bottom: 40px;
}

.profile-card {
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 40px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  display: grid;
  grid-template-columns: 150px 1fr auto auto;
  gap: 40px;
  align-items: center;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.avatar-wrapper {
  position: relative;
  width: 120px;
  height: 120px;
}

.avatar {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  object-fit: cover;
  position: relative;
  z-index: 1;
}

.avatar-ring {
  position: absolute;
  inset: -6px;
  border-radius: 50%;
  background: var(--color-brand);
  animation: rotate 4s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.user-badges {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
}

.badge {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
}

.badge svg {
  width: 14px;
  height: 14px;
}

.badge.verified {
  background: rgba(26, 26, 26, 0.2);
  color: var(--color-foreground);
}

.badge.level {
  background: rgba(245, 158, 11, 0.2);
  color: #f59e0b;
}

.user-info {
  flex: 1;
}

.user-name {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-foreground);
  margin: 0 0 8px 0;
}

.user-bio {
  font-size: 14px;
  color: var(--color-muted-foreground);
  margin: 0 0 16px 0;
}

.user-meta {
  display: flex;
  gap: 24px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--color-muted-foreground);
}

.meta-item svg {
  width: 16px;
  height: 16px;
}

.user-stats {
  display: flex;
  align-items: center;
  gap: 24px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-foreground);
}

.stat-label {
  font-size: 13px;
  color: var(--color-muted-foreground);
  margin-top: 4px;
}

.stat-divider {
  width: 1px;
  height: 50px;
  background: rgba(255, 255, 255, 0.2);
}

.page-notice {
  margin-top: 16px;
  padding: 12px 16px;
  border-radius: 12px;
  background: rgba(26, 26, 26, 0.18);
  color: #d1fae5;
}

.profile-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.action-btn-primary, .action-btn-secondary {
  padding: 14px 24px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.3s ease;
}

.action-btn-primary {
  background: var(--color-brand);
  color: var(--color-foreground);
  border: none;
}

.action-btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 30px -10px rgba(26, 26, 26, 0.5);
}

.action-btn-secondary {
  background: rgba(255, 255, 255, 0.1);
  color: var(--color-foreground);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.action-btn-secondary:hover {
  background: rgba(255, 255, 255, 0.15);
}

.action-btn-primary svg, .action-btn-secondary svg {
  width: 18px;
  height: 18px;
}

.profile-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  padding: 8px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  overflow-x: auto;
}

.tab-btn {
  flex: 1 1 auto;
  padding: 14px 12px;
  border-radius: 12px;
  border: none;
  background: transparent;
  color: var(--color-muted-foreground);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  white-space: nowrap;
  transition: all 0.3s ease;
}

.tab-btn svg {
  width: 18px;
  height: 18px;
}

.tab-btn:hover {
  color: rgba(255, 255, 255, 0.9);
  background: rgba(255, 255, 255, 0.05);
}

.tab-btn.active {
  background: var(--color-brand);
  color: var(--color-foreground);
}

.tab-count {
  background: rgba(255, 255, 255, 0.2);
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
}

.tab-btn.active .tab-count {
  background: rgba(255, 255, 255, 0.3);
}

.tab-content {
  min-height: 400px;
}

.listings-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.listing-card {
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.1);
  cursor: pointer;
  transition: all 0.3s ease;
}

.listing-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 20px 40px -10px rgba(0, 0, 0, 0.3);
}

.listing-image {
  position: relative;
  height: 180px;
}

.listing-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.listing-status {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 6px 12px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
}

.listing-status.selling {
  background: rgba(26, 26, 26, 0.9);
  color: var(--color-foreground);
}

.listing-status.sold {
  background: rgba(107, 114, 128, 0.9);
  color: var(--color-foreground);
}

.listing-info {
  padding: 16px;
}

.listing-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-foreground);
  margin: 0 0 8px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.listing-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.listing-price {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-foreground);
}

.listing-views {
  font-size: 12px;
  color: var(--color-muted-foreground);
}

.listing-actions {
  display: flex;
  gap: 8px;
}

.listing-btn {
  flex: 1;
  padding: 10px;
  border-radius: 10px;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.listing-btn svg {
  width: 18px;
  height: 18px;
}

.listing-btn.edit {
  background: rgba(59, 130, 246, 0.2);
  color: #3b82f6;
}

.listing-btn.delete {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
}

.listing-btn:hover {
  transform: scale(1.05);
}

.favorites-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.favorite-card {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
}

.favorite-card:hover {
  background: rgba(255, 255, 255, 0.08);
  transform: translateX(5px);
}

.favorite-image {
  width: 80px;
  height: 80px;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
}

.favorite-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.favorite-info {
  flex: 1;
  min-width: 0;
}

.favorite-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-foreground);
  margin: 0 0 6px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.favorite-price {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-foreground);
  margin-bottom: 6px;
}

.favorite-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: var(--color-muted-foreground);
}

.favorite-remove {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.3s ease;
}

.favorite-card:hover .favorite-remove {
  opacity: 1;
}

.favorite-remove svg {
  width: 16px;
  height: 16px;
}

.orders-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-card {
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  padding: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.order-info {
  display: flex;
  gap: 16px;
}

.order-id {
  font-size: 13px;
  color: var(--color-muted-foreground);
}

.order-date {
  font-size: 13px;
  color: var(--color-muted-foreground);
}

.order-status {
  padding: 6px 14px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
}

.order-status.pending {
  background: rgba(245, 158, 11, 0.2);
  color: #f59e0b;
}

.order-status.completed {
  background: rgba(26, 26, 26, 0.2);
  color: var(--color-foreground);
}

.order-content {
  display: flex;
  gap: 16px;
  align-items: center;
}

.order-image {
  width: 80px;
  height: 80px;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
}

.order-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.order-details {
  flex: 1;
  min-width: 0;
}

.order-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-foreground);
  margin: 0 0 8px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.order-filters {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.order-filters button,
.deleted-toggle {
  border: 1px solid rgba(255, 255, 255, 0.15);
  background: transparent;
  color: rgba(255, 255, 255, 0.75);
  border-radius: 999px;
  padding: 6px 14px;
  cursor: pointer;
}

.order-filters button.active {
  background: var(--color-foreground);
  color: var(--color-foreground);
  border-color: var(--color-foreground);
}

.deleted-toggle {
  margin: 8px 0 16px;
}

.order-meta {
  display: flex;
  gap: 8px;
  font-size: 13px;
  color: var(--color-muted-foreground);
}

.order-price-section {
  text-align: right;
}

.order-price {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-foreground);
  margin-bottom: 0;
}

.order-footer { display: flex; align-items: flex-start; justify-content: space-between; flex-wrap: wrap; gap: 14px 20px; margin-top: 18px; padding-top: 16px; border-top: 1px solid #eeeee7; }
.receipt-help { max-width: 410px; }
.receipt-help summary { display: inline-flex; align-items: center; gap: 6px; padding: 9px 0; color: #858579 !important; font-size: 12px; list-style: none; cursor: pointer; }
.receipt-help summary::-webkit-details-marker { display: none; }
.receipt-help summary svg { width: 15px; height: 15px; color: #858579 !important; }
.receipt-help summary .help-chevron { display: inline-block; color: #858579 !important; transition: transform .15s; }
.receipt-help[open] .help-chevron { transform: rotate(180deg); }
.receipt-help p { margin: 4px 0 0; padding: 11px 13px; background: #f7f7f2; border-radius: 8px; font-size: 12px; color: #747568 !important; line-height: 1.8; }

.order-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
  margin-left: auto;
}

.order-btn {
  min-height: 38px;
  min-width: 92px;
  padding: 8px 15px;
  border-radius: 8px;
  border: none;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color .15s, border-color .15s;
}

.order-btn.primary {
  background: var(--color-brand);
  color: var(--color-foreground);
}

.order-btn.secondary {
  background: #fff;
  color: var(--color-foreground);
  border: 1px solid #deded5;
}

.order-btn.danger {
  background: transparent;
  color: #858579 !important;
}

.order-btn.danger:hover { color: #b3412c !important; background: #fff4ef; }
.order-btn.secondary:hover { border-color: #aaa99c; background: #fafaf6; }

.order-btn:hover {
  transform: none;
}

.history-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.history-card {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  cursor: pointer;
  transition: all 0.3s ease;
}

.history-card:hover {
  background: rgba(255, 255, 255, 0.08);
  transform: translateY(-2px);
}

.history-image {
  width: 80px;
  height: 80px;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
}

.history-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.history-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.history-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-foreground);
  margin: 0 0 8px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.history-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.history-price {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-foreground);
}

.history-time {
  font-size: 12px;
  color: var(--color-muted-foreground);
}

.drafts-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.draft-card {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  cursor: pointer;
  transition: all 0.3s ease;
}

.draft-card:hover {
  background: rgba(255, 255, 255, 0.08);
  transform: translateY(-2px);
}

.draft-image {
  width: 100px;
  height: 100px;
  border-radius: 12px;
  overflow: hidden;
  flex-shrink: 0;
  position: relative;
  background: rgba(255, 255, 255, 0.05);
}

.draft-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.draft-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.3);
}

.draft-placeholder svg {
  width: 40px;
  height: 40px;
}

.draft-image-count {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background: rgba(0, 0, 0, 0.6);
  color: var(--color-foreground);
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
}

.draft-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.draft-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-foreground);
  margin: 0 0 8px 0;
}

.draft-meta {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.draft-category, .draft-condition {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.1);
  color: var(--color-muted-foreground);
}

.draft-price {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-foreground);
  margin-bottom: 4px;
}

.draft-date {
  font-size: 12px;
  color: var(--color-muted-foreground);
}

.draft-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  justify-content: center;
}

.draft-btn {
  padding: 8px 16px;
  border-radius: 8px;
  border: none;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.3s ease;
}

.draft-btn.edit {
  background: rgba(59, 130, 246, 0.2);
  color: #3b82f6;
}

.draft-btn.delete {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
}

.draft-btn:hover {
  transform: scale(1.05);
}

.draft-btn svg {
  width: 16px;
  height: 16px;
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
}

.empty-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 24px;
  color: rgba(255, 255, 255, 0.2);
}

.empty-icon svg {
  width: 100%;
  height: 100%;
}

.empty-state h3 {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-foreground);
  margin: 0 0 8px 0;
}

.empty-state p {
  font-size: 14px;
  color: var(--color-muted-foreground);
  margin: 0 0 24px 0;
}

.empty-btn {
  padding: 12px 28px;
  background: var(--color-brand);
  color: var(--color-foreground);
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.empty-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 30px -10px rgba(26, 26, 26, 0.5);
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(10px);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.edit-modal {
  background: rgba(26, 26, 62, 0.95);
  backdrop-filter: blur(30px);
  border-radius: 24px;
  width: 100%;
  max-width: 520px;
  max-height: 90vh;
  overflow-y: auto;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.modal-header h2 {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-foreground);
  margin: 0;
}

.close-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  color: var(--color-muted-foreground);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.15);
  color: var(--color-foreground);
}

.close-btn svg {
  width: 20px;
  height: 20px;
}

.modal-body {
  padding: 24px;
}

.avatar-upload-section {
  text-align: center;
  margin-bottom: 32px;
}

.avatar-upload {
  position: relative;
  width: 120px;
  height: 120px;
  margin: 0 auto 12px;
  cursor: pointer;
}

.avatar-preview {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-edit-overlay {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.3s ease;
  color: var(--color-foreground);
}

.avatar-upload:hover .avatar-edit-overlay {
  opacity: 1;
}

.avatar-edit-overlay svg {
  width: 32px;
  height: 32px;
  margin-bottom: 4px;
}

.avatar-edit-overlay span {
  font-size: 12px;
}

.avatar-input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.avatar-tip {
  font-size: 12px;
  color: var(--color-muted-foreground);
  margin: 0;
}

.form-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  position: relative;
}

.form-group label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-muted-foreground);
  margin-bottom: 8px;
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  color: var(--color-foreground);
  font-size: 15px;
  outline: none;
  transition: all 0.3s ease;
  box-sizing: border-box;
}

.form-group input:focus,
.form-group textarea:focus {
  border-color: var(--color-foreground);
  background: rgba(26, 26, 26, 0.05);
}

.form-group textarea {
  resize: none;
}

.char-count {
  position: absolute;
  right: 12px;
  bottom: 10px;
  font-size: 12px;
  color: var(--color-muted-foreground);
}

.modal-footer {
  display: flex;
  gap: 12px;
  padding: 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.btn-cancel,
.btn-save {
  flex: 1;
  padding: 14px 24px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-cancel {
  background: rgba(255, 255, 255, 0.1);
  color: var(--color-muted-foreground);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.btn-cancel:hover {
  background: rgba(255, 255, 255, 0.15);
}

.btn-save {
  background: var(--color-brand);
  color: var(--color-foreground);
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.btn-save:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 10px 30px -10px rgba(26, 26, 26, 0.5);
}

.btn-save:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.spinner {
  width: 20px;
  height: 20px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.modal-enter-active,
.modal-leave-active {
  transition: all 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .edit-modal,
.modal-leave-to .edit-modal {
  transform: scale(0.95);
  opacity: 0;
}

@media (max-width: 1024px) {
  .profile-card {
    grid-template-columns: 120px 1fr;
    gap: 24px;
  }

  .user-stats {
    grid-column: 1 / -1;
    justify-content: space-around;
  }

  .profile-actions {
    grid-column: 1 / -1;
    flex-direction: row;
    justify-content: center;
  }
}

@media (max-width: 768px) {

  .profile-card {
    padding: 24px;
    grid-template-columns: 1fr;
    text-align: center;
  }

  .avatar-section {
    align-items: center;
  }

  .user-meta {
    justify-content: center;
    flex-wrap: wrap;
  }

  .user-stats {
    flex-wrap: wrap;
    justify-content: center;
    gap: 16px;
  }

  .stat-divider {
    display: none;
  }

  .profile-tabs {
    flex-wrap: wrap;
  }

  .tab-btn {
    padding: 10px 16px;
    font-size: 13px;
  }

  .listings-grid,
  .favorites-grid,
  .history-list,
  .drafts-list {
    grid-template-columns: 1fr;
  }

  .order-content {
    flex-direction: column;
    align-items: flex-start;
  }

  .order-price-section {
    width: 100%;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .draft-card {
    flex-direction: column;
  }

  .draft-image {
    width: 100%;
    height: 160px;
  }

  .draft-actions {
    flex-direction: row;
  }
}
</style>
