import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import Messages from '../views/Messages.vue'
import { getConversations, getReceivedMessages, getSentMessages, getConversation, markAsRead, sendMessage } from '../api/message'
import { getProductDetail } from '../api/product'

const navigation = vi.hoisted(() => ({ query: {}, push: vi.fn(), replace: vi.fn(), setUnreadCount: vi.fn() }))
vi.mock('vue-router', () => ({
  useRoute: () => navigation,
  useRouter: () => ({ push: navigation.push, replace: navigation.replace }),
}))
vi.mock('../stores/notification', () => ({ useNotificationStore: () => ({ setUnreadCount: navigation.setUnreadCount }) }))
vi.mock('../api/message', () => ({
  getConversations: vi.fn(), getReceivedMessages: vi.fn(), getSentMessages: vi.fn(), getConversation: vi.fn(),
  markAsRead: vi.fn(), markAllAsRead: vi.fn(), sendMessage: vi.fn(),
}))
vi.mock('../api/user', () => ({ getUserById: vi.fn() }))
vi.mock('../api/product', () => ({ getProductDetail: vi.fn(), splitImages: () => [] }))
vi.mock('../utils/feedback', () => ({ notify: vi.fn() }))

const incoming = { id: 201, senderId: 2, receiverId: 12, content: '台灯可以在图书馆见面看货',
  createdTime: '2026-10-15T10:20:00', isRead: 0, sender: { id: 2, username: '小林', avatar: '/test-avatar.png' } }
const other = { id: 202, senderId: 12, receiverId: 3, content: '请问自行车还在吗',
  createdTime: '2026-10-15T10:10:00', isRead: 1, receiver: { id: 3, username: '小周' } }
const response = data => ({ code: 200, data })
const deferred = () => {
  let resolve, reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}
let wrapper
async function start(query = {}) {
  navigation.query = query
  wrapper = mount(Messages, { global: { stubs: { RouterLink: { props: ['to'], template: '<a><slot /></a>' } } } })
  await flushPromises()
  return wrapper
}
async function select(name) {
  await wrapper.find(`[aria-label="打开与${name}的会话"]`).trigger('click')
  await flushPromises()
}

describe('paged message contracts', () => {
  it('loads older messages with a cursor and polls only after the latest id', async () => {
    getConversations.mockResolvedValue(response({ items: [{ userId: 2, username: '小林', unreadCount: 0 }], total: 1, unreadTotal: 0 }))
    getConversation.mockResolvedValueOnce(response({ items: [{ ...incoming }], hasMore: true, readIds: [] }))
      .mockResolvedValueOnce(response({ items: [{ ...incoming, id: 190, content: '更早的消息' }], hasMore: false, readIds: [] }))
      .mockResolvedValue(response({ items: [], hasMore: false, readIds: [] }))
    await start(); await select('小林')
    await wrapper.find('.load-older').trigger('click'); await flushPromises()
    expect(getConversation).toHaveBeenCalledWith(2, { before: 201 })
    expect(wrapper.find('.chat-messages').text()).toContain('更早的消息')
    await vi.advanceTimersByTimeAsync(5000); await flushPromises()
    expect(getConversation).toHaveBeenCalledWith(2, { after: 201 })
  })
  it('searches the server rather than filtering only the current conversation page', async () => {
    getConversations.mockResolvedValue(response({ items: [], total: 0, unreadTotal: 0 }))
    await start()
    await wrapper.find('[aria-label="搜索会话"]').setValue('旧商品')
    await vi.advanceTimersByTimeAsync(250); await flushPromises()
    expect(getConversations).toHaveBeenLastCalledWith(expect.objectContaining({ search: '旧商品', page: 1 }))
  })
})

beforeEach(() => {
  vi.clearAllMocks()
  vi.useFakeTimers()
  sessionStorage.clear(); localStorage.clear()
  sessionStorage.setItem('user', JSON.stringify({ id: 12, username: '体验账号' }))
  getConversations.mockImplementation(async () => ({ code: 200, data: [...(await getReceivedMessages()).data, ...(await getSentMessages()).data] }))
  getReceivedMessages.mockResolvedValue(response([{ ...incoming }]))
  getSentMessages.mockResolvedValue(response([{ ...other }]))
  getConversation.mockImplementation(async id => response(id === 2 ? [{ ...incoming }] : [{ ...other }]))
  markAsRead.mockResolvedValue(response(null))
  sendMessage.mockImplementation(async body => response({ id: 300 + sendMessage.mock.calls.length, senderId: 12,
    receiverId: body.receiverId, content: body.content, createdTime: '2026-10-15T11:35:00', isRead: 0 }))
})
afterEach(() => { wrapper?.unmount(); vi.useRealTimers() })

describe('desktop messaging', () => {
  it('searches both contact names and conversation contents', async () => {
    await start()
    await wrapper.find('[aria-label="搜索会话"]').setValue('图书馆')
    expect(wrapper.findAll('.conversation-item')).toHaveLength(1)
    expect(wrapper.find('.conv-name').text()).toBe('小林')
    await wrapper.find('[aria-label="搜索会话"]').setValue('小周')
    expect(wrapper.find('.conv-name').text()).toBe('小周')
    expect(wrapper.find('.conv-avatar img').attributes('src')).toBeTruthy()
  })

  it('keeps drafts separate when switching contacts and restores them after reload', async () => {
    await start()
    await select('小林')
    await wrapper.find('textarea').setValue('和小林约周五见面')
    await select('小周')
    expect(wrapper.find('textarea').element.value).toBe('')
    await wrapper.find('textarea').setValue('和小周约周六见面')
    await select('小林')
    expect(wrapper.find('textarea').element.value).toBe('和小林约周五见面')
    wrapper.unmount()
    await start({ sellerId: '2' })
    expect(wrapper.find('textarea').element.value).toBe('和小林约周五见面')
    expect(sessionStorage.getItem('chat_draft_12_3')).toBe('和小周约周六见面')
  })

  it('does not send on Shift+Enter or while the Chinese input method is composing', async () => {
    await start({ sellerId: '2' })
    const input = wrapper.find('textarea')
    await input.setValue('你好')
    await input.trigger('keydown', { key: 'Enter', shiftKey: true })
    await input.trigger('keydown', { key: 'Enter', isComposing: true })
    expect(sendMessage).not.toHaveBeenCalled()
    await input.trigger('keydown', { key: 'Enter' })
    await flushPromises()
    expect(sendMessage).toHaveBeenCalledOnce()
    expect(wrapper.find('textarea').element.value).toBe('')
    expect(wrapper.find('.message.sent').text()).toContain('11:35')
    expect(wrapper.find('.message.sent').text()).toContain('未读')
  })

  it('refreshes unread messages in other conversations while a chat is open', async () => {
    await start({ sellerId: '2' })
    getReceivedMessages.mockResolvedValue(response([{ ...incoming, isRead: 1 }, {
      id: 203, senderId: 3, receiverId: 12, content: '新消息：明天可以试骑',
      createdTime: '2026-10-15T12:00:00', isRead: 0, sender: { id: 3, username: '小周' },
    }]))
    await vi.advanceTimersByTimeAsync(5000)
    await flushPromises()
    expect(wrapper.find('[aria-label="打开与小周的会话"]').text()).toContain('新消息：明天可以试骑')
    expect(wrapper.find('.chat-user-details h2').text()).toBe('小林')
    expect(wrapper.find('.unread-total').text()).toBe('1')
  })

  it('preserves the new draft when an older send finishes, and offers the failed message for retry', async () => {
    await start({ sellerId: '2' })
    const sending = deferred()
    sendMessage.mockReturnValueOnce(sending.promise)
    await wrapper.find('textarea').setValue('第一条消息')
    await wrapper.find('.send-btn').trigger('click')
    await wrapper.find('textarea').setValue('继续写的第二条消息')
    sending.reject(new Error('暂时无法连接服务器'))
    await flushPromises()
    expect(wrapper.find('textarea').element.value).toBe('继续写的第二条消息')
    expect(wrapper.find('.send-error').text()).toContain('第一条消息')
    await wrapper.find('.send-error button').trigger('click')
    await flushPromises()
    expect(sendMessage.mock.calls[1][0].content).toBe('第一条消息')
    expect(sendMessage.mock.calls[0][0].requestKey).toBeTruthy()
    expect(sendMessage.mock.calls[1][0].requestKey).toBe(sendMessage.mock.calls[0][0].requestKey)
    expect(wrapper.find('textarea').element.value).toBe('继续写的第二条消息')
    expect(wrapper.find('.send-error').exists()).toBe(false)
  })

  it('does not inject a late send into the newly selected chat or clear its draft', async () => {
    await start({ sellerId: '2' })
    const pending = deferred()
    sendMessage.mockReturnValueOnce(pending.promise)
    await wrapper.find('textarea').setValue('只发送给小林')
    await wrapper.find('.send-btn').trigger('click')
    await select('小周')
    await wrapper.find('textarea').setValue('小周的草稿')
    pending.resolve(response({ id: 301, senderId: 12, receiverId: 2, content: '只发送给小林',
      createdTime: '2026-10-15T11:36:00', isRead: 0 }))
    await flushPromises()
    expect(wrapper.find('.chat-messages').text()).not.toContain('只发送给小林')
    expect(wrapper.find('textarea').element.value).toBe('小周的草稿')
    expect(sessionStorage.getItem('chat_draft_12_2')).toBe(null)
  })

  it('keeps an earlier failed message available even when a different message is sent successfully', async () => {
    await start({ sellerId: '2' })
    sendMessage.mockRejectedValueOnce(new Error('网络暂时不可用'))
    await wrapper.find('textarea').setValue('需要稍后重试的内容')
    await wrapper.find('.send-btn').trigger('click')
    await flushPromises()
    await wrapper.find('textarea').setValue('另一条已经准备好的消息')
    await wrapper.find('.send-btn').trigger('click')
    await flushPromises()
    expect(wrapper.find('.send-error').text()).toContain('需要稍后重试的内容')
    expect(wrapper.find('.chat-messages').text()).toContain('另一条已经准备好的消息')
    await wrapper.find('.send-error button').trigger('click')
    await flushPromises()
    expect(wrapper.find('.send-error').exists()).toBe(false)
    expect(wrapper.find('.chat-messages').text()).toContain('需要稍后重试的内容')
  })

  it('attaches the consulted product and supports explicitly ending the association', async () => {
    getProductDetail.mockResolvedValue(response({ id: 128, title: '学习笔记本', price: 1680 }))
    await start({ sellerId: '2', productId: '128', productTitle: '学习笔记本' })
    expect(wrapper.find('.consulting-card').text()).toContain('¥1680.00')
    await wrapper.find('textarea').setValue('咨询这台电脑')
    await wrapper.find('.send-btn').trigger('click')
    await flushPromises()
    expect(sendMessage.mock.calls[0][0].productId).toBe(128)
    await wrapper.find('[aria-label="取消本次商品关联"]').trigger('click')
    await wrapper.find('textarea').setValue('另外聊一件事')
    await wrapper.find('.send-btn').trigger('click')
    await flushPromises()
    expect(sendMessage.mock.calls[1][0].productId).toBe(null)
  })

  it('keeps the reading position and marks arriving messages read only after jumping to the bottom', async () => {
    await start({ sellerId: '2' })
    const panel = wrapper.find('.chat-messages').element
    Object.defineProperties(panel, { scrollHeight: { configurable: true, value: 2000 }, clientHeight: { configurable: true, value: 300 } })
    panel.scrollTop = 0
    await wrapper.find('.chat-messages').trigger('scroll')
    markAsRead.mockClear()
    getConversation.mockResolvedValue(response([{ ...incoming, isRead: 1 }, {
      id: 204, senderId: 2, receiverId: 12, content: '等你看完再回复',
      createdTime: '2026-10-15T12:00:00', isRead: 0,
    }]))
    await vi.advanceTimersByTimeAsync(5000)
    await flushPromises()
    expect(panel.scrollTop).toBe(0)
    expect(markAsRead).not.toHaveBeenCalled()
    expect(wrapper.find('.jump-latest').text()).toContain('1 条新消息')
    await wrapper.find('.jump-latest').trigger('click')
    await flushPromises()
    expect(markAsRead).toHaveBeenCalledWith(204)
    expect(wrapper.find('.jump-latest').exists()).toBe(false)
  })

  it('ignores a conversation response after another contact is selected', async () => {
    const oldResponse = deferred()
    getConversation.mockImplementation(id => id === 2 ? oldResponse.promise : Promise.resolve(response([{ ...other }])))
    await start()
    await wrapper.find('[aria-label="打开与小林的会话"]').trigger('click')
    await select('小周')
    oldResponse.resolve(response([{ ...incoming, content: '不应串到小周的内容' }]))
    await flushPromises()
    expect(wrapper.find('.chat-messages').text()).not.toContain('不应串到小周的内容')
    expect(wrapper.find('.chat-user-details h2').text()).toBe('小周')
  })
})
