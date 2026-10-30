import { afterEach, describe, expect, it, vi } from 'vitest'
import { createProduct } from '../api/product'
import { sendMessage } from '../api/message'

afterEach(() => vi.unstubAllGlobals())

describe('retry identifiers reach the API', () => {
  it('sends the same product identifier on a repeated submission', async () => {
    const fetch = vi.fn().mockResolvedValue({ ok: true, status: 200, json: async () => ({ code: 200, data: { id: 1 } }) })
    vi.stubGlobal('fetch', fetch)
    const payload = { title: '教材', price: '20.00' }
    await createProduct(payload, 'product-retry-key-001')
    await createProduct(payload, 'product-retry-key-001')
    for (const [, options] of fetch.mock.calls) {
      expect(options.headers['Idempotency-Key']).toBe('product-retry-key-001')
      expect(JSON.parse(options.body)).toEqual(payload)
    }
  })

  it('sends the message identifier as a header, without changing business fields', async () => {
    const fetch = vi.fn().mockResolvedValue({ ok: true, status: 200, json: async () => ({ code: 200 }) })
    vi.stubGlobal('fetch', fetch)
    await sendMessage({ receiverId: 2, content: '你好', requestKey: 'message-retry-key-001' })
    const options = fetch.mock.calls[0][1]
    expect(options.headers['Idempotency-Key']).toBe('message-retry-key-001')
    expect(JSON.parse(options.body)).toEqual({ receiverId: 2, content: '你好' })
  })
})
