<script setup>
import ListPagination from '../components/ListPagination.vue'
import PageHeading from '../components/PageHeading.vue'
import { useMessages } from '../composables/useMessages'
import { sameDay, dateLabel, clockTime, previewTime } from '../utils/chat'
defineOptions({ name: 'MessagesCenter' })

const {
  inboxPage, inboxTotal, hasOlder, olderLoading, loadOlder, loadInbox, currentUserId, conversations, selectedUserId, currentConversation, currentMessages, visibleConversations,
  search, activeTab, newMessage, unreadCount, inboxLoading, chatLoading, refreshing, markingAll, inboxError, chatError,
  newMessageCount, awayFromBottom, messagesContainer, composer, sending, failedSend, productCard, productImage, ignoreProduct,
  selectConversation, sendMessage, onComposerKeydown, addQuickReply, refreshAll, markAllRead, jumpToLatest,
  onMessagesScroll, backToConversations, retrySend, goProduct, hasDraft,
} = useMessages()
const quickReplies = ['你好，请问还在吗？', '方便什么时候见面？', '可以在图书馆门口交易吗？']
const defaultAvatar = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 40 40'%3E%3Crect width='40' height='40' rx='12' fill='%23ecece8'/%3E%3Ccircle cx='20' cy='15' r='6' fill='%23777'/%3E%3Cpath d='M9 35c0-7 5-11 11-11s11 4 11 11' fill='%23777'/%3E%3C/svg%3E"
function messageProductTitle(message) {
  return message.product?.title || (message.productId ? '查看咨询商品' : '')
}
function price(value) { return Number(value).toFixed(2) }
</script>

<template>
  <div class="messages-page">
    <main class="messages-main workspace-page">
      <PageHeading title="消息" description="沟通商品细节，约好校内见面" />
      <div class="messages-container" :class="{ 'has-selection': selectedUserId }">
        <aside class="messages-sidebar" aria-label="会话列表">
          <div class="sidebar-header">
            <div class="sidebar-heading"><h2>会话</h2><span v-if="unreadCount" class="unread-total">{{ unreadCount > 99 ? '99+' : unreadCount }}</span></div>
            <button type="button" class="refresh-button" aria-label="刷新消息" :disabled="refreshing" @click="refreshAll(true)">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true"><path d="M20 7v5h-5M4 17v-5h5"/><path d="M6 7a7 7 0 0 1 12-1l2 6M4 12l2 6a7 7 0 0 0 12-1"/></svg>
            </button>
          </div>
          <div class="conversation-search">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true"><circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 4 4"/></svg>
            <input v-model="search" type="search" aria-label="搜索会话" placeholder="搜索联系人、消息或商品" />
            <button v-if="search" type="button" aria-label="清空会话搜索" @click="search = ''">×</button>
          </div>
          <div class="conversation-filters" aria-label="会话筛选">
            <div class="filter-buttons">
              <button type="button" :class="{ active: activeTab === 'all' }" :aria-pressed="activeTab === 'all'" @click="activeTab = 'all'">全部</button>
              <button type="button" :class="{ active: activeTab === 'unread' }" :aria-pressed="activeTab === 'unread'" @click="activeTab = 'unread'">未读<span v-if="unreadCount"> {{ unreadCount }}</span></button>
            </div>
            <button type="button" class="mark-all" :disabled="!unreadCount || markingAll" @click="markAllRead">{{ markingAll ? '标记中…' : '全部已读' }}</button>
          </div>
          <div v-if="inboxError" class="message-error sidebar-error" role="alert">{{ inboxError }}<button type="button" @click="refreshAll(true)">重试</button></div>
          <div class="conversations-list" :aria-busy="inboxLoading">
            <p v-if="inboxLoading" class="panel-state">正在加载会话…</p>
            <div v-else-if="!visibleConversations.length && !inboxError" class="panel-state">
              <p>{{ search ? '没有找到相关会话' : activeTab === 'unread' ? '暂时没有未读消息' : '还没有会话' }}</p>
              <span>{{ search ? '试试联系人名称或消息关键词' : activeTab === 'unread' ? '收到的新消息会显示在这里' : '在商品详情页点击「联系卖家」开始聊天' }}</span>
            </div>
            <button v-for="peer in visibleConversations" :key="peer.userId" type="button" class="conversation-item" :class="{ active: selectedUserId === peer.userId, unread: peer.unreadCount > 0 }" :aria-pressed="selectedUserId === peer.userId" :aria-label="'打开与' + peer.username + '的会话'" @click="selectConversation(peer)">
              <div class="conv-avatar"><img :src="peer.avatar || defaultAvatar" alt="" /><span v-if="peer.unreadCount" class="message-badge">{{ peer.unreadCount > 99 ? '99+' : peer.unreadCount }}</span></div>
              <div class="conv-info">
                <div class="conv-header"><span class="conv-name">{{ peer.username }}</span><time class="conv-time">{{ previewTime(peer.lastMessageTime) }}</time></div>
                <p class="conv-preview"><span v-if="hasDraft(peer.userId) && selectedUserId !== peer.userId" class="draft-label">[草稿] </span>{{ peer.lastSent ? '我：' : '' }}{{ peer.lastMessage || '还没有消息，打个招呼吧' }}</p>
              </div>
            </button>
          </div>
          <ListPagination :page="inboxPage" :total="inboxTotal" :busy="refreshing" @change="loadInbox" />
          <div class="sidebar-footer"><span class="sync-dot" :class="{ syncing: refreshing }"></span>{{ refreshing ? '正在更新消息…' : '消息自动更新' }}<span>{{ conversations.length }} 个会话</span></div>
        </aside>

        <section class="chat-panel" aria-label="聊天窗口">
          <div v-if="!selectedUserId" class="empty-chat">
            <div class="empty-chat-icon"><svg viewBox="0 0 48 48" fill="none" stroke="currentColor" stroke-width="1.7" aria-hidden="true"><path d="M39 29a5 5 0 0 1-5 5H20L9 41V12a5 5 0 0 1 5-5h20a5 5 0 0 1 5 5z"/><path d="M17 18h14M17 25h9"/></svg></div>
            <h2>从一句问候开始</h2><p>选择左侧会话，继续沟通商品细节。<br />也可以从商品页联系卖家，约好见面时间和地点。</p>
            <RouterLink to="/products" class="browse-link">去看看商品 →</RouterLink>
          </div>
          <template v-else>
            <header class="chat-header">
              <button type="button" class="chat-back" aria-label="返回会话列表" @click="backToConversations">←</button>
              <img class="chat-avatar" :src="currentConversation?.avatar || defaultAvatar" alt="" />
              <div class="chat-user-details"><h2>{{ currentConversation?.username || '商品联系人' }}</h2><p>聊清商品细节，约好校内见面</p></div>
              <span class="chat-kind">商品私信</span>
            </header>
            <div v-if="productCard" class="consulting-card">
              <button type="button" class="consulting-product" @click="goProduct(productCard.id)">
                <img v-if="productImage" :src="productImage" alt="" /><span v-else class="product-symbol" aria-hidden="true">▣</span>
                <span class="consulting-info"><span class="consulting-caption">正在咨询</span><strong>{{ productCard.title }}</strong></span>
                <span v-if="productCard.price != null" class="consulting-price">¥{{ price(productCard.price) }}</span><span class="view-product">查看商品 →</span>
              </button>
              <button type="button" class="detach-product" aria-label="取消本次商品关联" title="取消本次商品关联" @click="ignoreProduct = true">×</button>
            </div>
            <div class="chat-history">
              <div ref="messagesContainer" class="chat-messages" role="log" aria-live="polite" aria-label="聊天记录" :aria-busy="chatLoading" @scroll.passive="onMessagesScroll">
                <button v-if="hasOlder" type="button" class="load-older" :disabled="olderLoading" @click="loadOlder">{{ olderLoading ? '加载中…' : '加载更早的消息' }}</button>
                <p v-if="chatLoading" class="panel-state">正在加载聊天记录…</p>
                <div v-else-if="!currentMessages.length && !chatError" class="empty-conversation"><p>还没有消息，打个招呼吧</p><span>可以先询问商品成色、价格和见面地点。</span></div>
                <template v-for="(message, index) in currentMessages" :key="message.id">
                  <div v-if="index === 0 || !sameDay(message.createdTime, currentMessages[index - 1].createdTime)" class="date-divider">{{ dateLabel(message.createdTime) }}</div>
                  <div class="message" :class="{ sent: Number(message.senderId) === currentUserId, received: Number(message.senderId) !== currentUserId }">
                    <img v-if="Number(message.senderId) !== currentUserId" class="message-avatar" :src="currentConversation?.avatar || defaultAvatar" alt="" />
                    <div class="message-content">
                      <button v-if="messageProductTitle(message)" type="button" class="message-product" @click="goProduct(message.product?.id || message.productId)">商品 · {{ messageProductTitle(message) }} ↗</button>
                      <div class="message-bubble"><p>{{ message.content }}</p></div>
                      <div class="message-meta"><time>{{ clockTime(message.createdTime) }}</time><span v-if="Number(message.senderId) === currentUserId" :class="{ 'message-read': Number(message.isRead) === 1 }">{{ Number(message.isRead) === 1 ? '已读' : '未读' }}</span></div>
                    </div>
                  </div>
                </template>
              </div>
              <button v-if="awayFromBottom || newMessageCount" type="button" class="jump-latest" @click="jumpToLatest">{{ newMessageCount ? newMessageCount + ' 条新消息' : '回到最新消息' }} ↓</button>
            </div>
            <div v-if="chatError" class="message-error" role="alert">{{ chatError }}<button type="button" @click="refreshAll(true)">重试</button></div>
            <div v-if="failedSend" class="message-error send-error" role="alert"><span>{{ failedSend.error }}<small>未发送：{{ failedSend.content }}</small></span><button type="button" :disabled="sending" @click="retrySend">重试这条</button></div>
            <div class="chat-composer">
              <div class="quick-replies" aria-label="常用回复"><button v-for="reply in quickReplies" :key="reply" type="button" @click="addQuickReply(reply)">{{ reply }}</button></div>
              <label class="visually-hidden" for="message-content">消息内容</label>
              <textarea id="message-content" ref="composer" v-model="newMessage" maxlength="500" rows="3" placeholder="输入消息，聊聊你想了解的细节…" :disabled="chatLoading" aria-describedby="composer-help" @keydown="onComposerKeydown"></textarea>
              <div class="composer-footer"><p id="composer-help">Enter 发送 · Shift + Enter 换行<span v-if="newMessage"> · 草稿已暂存</span></p><span class="message-count" :class="{ 'near-limit': newMessage.length >= 470 }">{{ newMessage.length }}/500</span><button type="button" class="send-btn" :disabled="chatLoading || !newMessage.trim() || sending" @click="sendMessage">{{ sending ? '发送中…' : '发送' }}<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true"><path d="m5 12 14 0m-6-6 6 6-6 6"/></svg></button></div>
            </div>
          </template>
        </section>
      </div>
    </main>
  </div>
</template>

<style scoped>
.load-older { display: block; margin: 0 auto 16px; border: 1px solid #ddd; border-radius: 8px; background: white; padding: 8px 14px; cursor: pointer; }
.messages-page { min-height: calc(100dvh - var(--shell-top, 0px)); }
.messages-main { max-width: 1240px; margin: auto; padding: 24px 28px; }
.messages-container { display: grid; grid-template-columns: minmax(240px, 30%) minmax(0, 1fr); height: min(850px, calc(100dvh - var(--shell-top, 0px) - 56px)); min-height: 500px; overflow: hidden; border: 1px solid #e1e1de; border-radius: 16px; background: white; }
.messages-sidebar { display: flex; flex-direction: column; min-height: 0; min-width: 0; background: #fcfcfb; border-right: 1px solid #e8e8e4; }
.sidebar-header { display: flex; justify-content: space-between; align-items: center; padding: 22px 20px 16px; }
.sidebar-heading { display: flex; align-items: center; gap: 10px; }
.sidebar-heading h1 { margin: 0; font-size: 22px; letter-spacing: -.5px; }
.unread-total { padding: 2px 7px; border-radius: 12px; background: var(--color-brand); font-size: 12px; font-weight: 700; }
.refresh-button { display: grid; place-items: center; width: 30px; height: 30px; background: transparent; border: 1px solid #e5e5e0; border-radius: 8px; }
.refresh-button svg { width: 16px; height: 16px; }
.conversation-search { display: flex; align-items: center; gap: 8px; padding: 0 10px; margin: 0 16px 14px; min-height: 40px; border: 1px solid #e7e7e1; border-radius: 9px; background: white; }
.conversation-search:focus-within { outline: 2px solid #aaa; outline-offset: 1px; }
.conversation-search svg { width: 17px; height: 17px; flex: none; }
.conversation-search input { width: 100%; min-width: 0; padding: 7px 0; border: 0; outline: 0; font-size: 13px; background: transparent; }
.conversation-search input::-webkit-search-cancel-button { display: none; }
.conversation-search button { border: 0; background: transparent; font-size: 18px; }
.conversation-filters { display: flex; align-items: center; justify-content: space-between; padding: 0 16px 14px; border-bottom: 1px solid #ebebe7; }
.filter-buttons { display: flex; gap: 4px; }
.filter-buttons button { padding: 6px 11px; border: 0; border-radius: 7px; background: transparent; font-size: 12px; }
.filter-buttons button.active { background: #fff2a0; font-weight: 650; }
.mark-all { border: 0; background: transparent; font-size: 12px; padding: 6px 0; }
.conversations-list { flex: 1; min-height: 0; overflow-y: auto; padding: 8px; }
.conversation-item { display: flex; width: 100%; align-items: center; gap: 12px; text-align: left; padding: 15px 12px; border: 1px solid transparent; border-radius: 10px; background: transparent; }
.conversation-item:hover { background: #f2f2ed; }
.conversation-item.active { background: #fff8cd; border-color: #eee1a3; }
.conv-avatar { width: 43px; height: 43px; flex: none; position: relative; }
.conv-avatar img { width: 100%; height: 100%; border-radius: 12px; object-fit: cover; }
.message-badge { position: absolute; top: -5px; right: -5px; padding: 1px 5px; border: 2px solid #fcfcfb; border-radius: 12px; background: #1a1a1a; color: white !important; font-size: 10px; }
.conv-info { flex: 1; min-width: 0; }
.conv-header { display: flex; align-items: center; gap: 8px; justify-content: space-between; margin-bottom: 6px; }
.conv-name { font-size: 14px; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.conv-time { font-size: 10px; white-space: nowrap; }
.conv-preview { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin: 0; font-size: 12px; }
.draft-label { color: #ac4a0b !important; }
.unread .conv-preview { font-weight: 600; }
.sidebar-footer { display: flex; align-items: center; gap: 6px; padding: 12px 18px; border-top: 1px solid #ebebe7; font-size: 11px; }
.sidebar-footer > span:last-child { margin-left: auto; }
.sync-dot { width: 5px; height: 5px; border-radius: 50%; background: #93968c; }
.sync-dot.syncing { background: #c6ac00; }
.chat-panel { display: flex; flex-direction: column; min-width: 0; min-height: 0; overflow: hidden; }
.chat-header { display: flex; flex: none; align-items: center; gap: 12px; min-height: 79px; padding: 17px 26px; border-bottom: 1px solid #eaeae5; }
.chat-avatar { width: 40px; height: 40px; border-radius: 12px; object-fit: cover; }
.chat-user-details { min-width: 0; }
.chat-user-details h2 { margin: 0 0 4px; font-size: 17px; }
.chat-user-details p { margin: 0; font-size: 11px; }
.chat-kind { margin-left: auto; flex: none; border: 1px solid #e8e8e1; border-radius: 6px; padding: 4px 7px; font-size: 11px; }
.chat-back { display: none; }
.consulting-card { display: flex; align-items: center; gap: 10px; padding: 10px 22px; border-bottom: 1px solid #ebebe5; background: #fffdf2; flex: none; }
.consulting-product { flex: 1; min-width: 0; display: flex; align-items: center; gap: 12px; padding: 0; border: 0; background: transparent; text-align: left; }
.consulting-product > img, .product-symbol { width: 44px; height: 44px; border-radius: 7px; object-fit: cover; flex: none; }
.product-symbol { display: grid; place-items: center; background: #eeeede; font-size: 22px; }
.consulting-info { flex: 1; display: flex; min-width: 0; flex-direction: column; gap: 3px; }
.consulting-caption { font-size: 10px; }
.consulting-info strong { font-size: 13px; font-weight: 550; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.consulting-price { color: var(--color-accent) !important; font-size: 14px; font-weight: 650; }
.view-product { font-size: 11px; margin-left: 6px; white-space: nowrap; }
.detach-product { width: 24px; height: 24px; border: 0; border-radius: 5px; background: transparent; font-size: 18px; }
.detach-product:hover { background: #f0eddb; }
.chat-history { position: relative; flex: 1; min-height: 0; }
.chat-messages { height: 100%; overflow-y: auto; padding: 18px 26px; background: #f7f7f3; scroll-behavior: auto; }
.date-divider { text-align: center; font-size: 11px; color: #89897f !important; margin: 3px 0 19px; }
.message { display: flex; align-items: flex-start; gap: 10px; margin-bottom: 18px; }
.message.sent { justify-content: flex-end; }
.message-avatar { flex: none; width: 31px; height: 31px; border-radius: 9px; object-fit: cover; }
.message-content { max-width: min(78%, 540px); min-width: 0; }
.message.sent .message-content { display: flex; flex-direction: column; align-items: flex-end; }
.message-product { display: block; max-width: 100%; padding: 0 0 6px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; border: 0; background: transparent; font-size: 10px; }
.message-bubble { padding: 11px 14px; border: 1px solid #e7e7df; border-radius: 3px 12px 12px; background: white; }
.message.sent .message-bubble { background: #fff0a5; border-color: #eee1a2; border-radius: 12px 3px 12px 12px; }
.message-bubble p { margin: 0; white-space: pre-wrap; overflow-wrap: anywhere; font-size: 14px; line-height: 1.7; }
.message-meta { display: flex; gap: 8px; margin-top: 5px; font-size: 10px; }
.message-meta .message-read { color: #75806a !important; }
.jump-latest { position: absolute; bottom: 12px; left: 50%; transform: translateX(-50%); padding: 8px 14px; border: 1px solid #e2e2d7; border-radius: 20px; background: white; box-shadow: 0 3px 12px #0000000a; font-size: 12px; white-space: nowrap; }
.chat-composer { flex: none; padding: 12px 22px 14px; border-top: 1px solid #e6e6df; background: white; }
.quick-replies { display: flex; gap: 6px; overflow-x: auto; margin-bottom: 8px; }
.quick-replies button { border: 1px solid #e6e6df; border-radius: 6px; padding: 4px 8px; background: #fafaf7; white-space: nowrap; font-size: 11px; }
.chat-composer textarea { display: block; width: 100%; height: 66px; padding: 5px 0; resize: none; border: 0; outline: 0; font-size: 14px; line-height: 1.6; }
.chat-composer textarea:focus-visible { outline: 0; }
.chat-composer:focus-within { box-shadow: inset 0 2px 0 #dbcd5c; }
.composer-footer { display: flex; align-items: center; gap: 12px; padding-top: 7px; }
.composer-footer p { font-size: 10px; margin: 0; flex: 1; }
.message-count { font-size: 10px; white-space: nowrap; }
.message-count.near-limit { color: #b45309 !important; }
.send-btn { display: flex; align-items: center; justify-content: center; gap: 8px; min-width: 86px; min-height: 34px; padding: 6px 15px; font-size: 13px; font-weight: 600; }
.send-btn svg { width: 16px; height: 16px; }
.message-error { display: flex; justify-content: space-between; align-items: center; gap: 12px; padding: 10px 20px; font-size: 12px; background: #fff1ed; color: #a5381d !important; border-top: 1px solid #f2d8d1; }
.message-error button { border: 0; background: transparent; text-decoration: underline; font-size: 12px; color: #a5381d !important; flex: none; }
.message-error small { display: block; font-size: 11px; max-width: 400px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #a5381d !important; margin-top: 4px; }
.panel-state, .empty-conversation { padding: 28px 14px; text-align: center; font-size: 13px; line-height: 1.7; }
.panel-state span, .empty-conversation span { font-size: 11px; }
.empty-conversation { padding: 44px 20px; }
.empty-chat { flex: 1; display: flex; align-items: center; justify-content: center; flex-direction: column; padding: 40px; text-align: center; background: #fafaf7; }
.empty-chat-icon { display: grid; place-items: center; width: 88px; height: 88px; border-radius: 24px; background: #efefe6; margin-bottom: 18px; }
.empty-chat-icon svg { width: 42px; height: 42px; }
.empty-chat h2 { font-size: 22px; margin: 0 0 10px; }
.empty-chat p { font-size: 13px; line-height: 1.9; margin: 0 0 24px; }
.browse-link { font-size: 13px; padding: 9px 15px; border-radius: 8px; background: var(--color-brand); text-decoration: none; }
.visually-hidden { position: absolute; width: 1px; height: 1px; padding: 0; overflow: hidden; clip-path: inset(50%); white-space: nowrap; }
.messages-page .conv-time, .messages-page .conv-preview, .messages-page .sidebar-footer, .messages-page .sidebar-footer span, .messages-page .chat-user-details p, .messages-page .chat-kind, .messages-page .consulting-caption, .messages-page .view-product, .messages-page .message-meta, .messages-page .message-meta time, .messages-page .message-meta span, .messages-page .composer-footer p, .messages-page .composer-footer p span, .messages-page .message-count, .messages-page .message-product, .messages-page .panel-state, .messages-page .panel-state span, .messages-page .empty-conversation, .messages-page .empty-conversation span, .messages-page .empty-chat p { color: #77786d !important; }
@media (max-width: 760px) {
  .messages-main { padding: 16px; }
  .messages-container { grid-template-columns: 1fr; height: calc(100dvh - var(--shell-top, 0px) - 40px); min-height: 460px; }
  .chat-panel { display: none; }
  .has-selection .messages-sidebar { display: none; }
  .has-selection .chat-panel { display: flex; }
  .chat-back { display: block; border: 0; background: transparent; font-size: 20px; padding: 4px; }
  .chat-header, .consulting-card, .chat-composer { padding-left: 14px; padding-right: 14px; }
  .chat-messages { padding: 16px 14px; }
  .chat-kind, .view-product { display: none; }
  .composer-footer p { max-width: 190px; }
}
</style>
