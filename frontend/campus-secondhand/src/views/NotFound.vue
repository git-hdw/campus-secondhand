<template>
  <main class="not-found">
    <p class="code">404</p>
    <h1>这个页面没有找到</h1>
    <p>链接可能有误，可以返回商品列表继续浏览。</p>
    <RouterLink to="/products">浏览商品 →</RouterLink>
  </main>
</template>
<style scoped>
.not-found { max-width: 600px; padding: 80px 24px; margin: auto; text-align: center; }
.code { font-size: 52px; font-weight: 800; margin: 0; color: #8a7a00; }
h1 { font-size: 26px; } p { color: #666; } a { display: inline-block; padding: 10px 24px; border-radius: 8px; background: var(--color-brand); color: #1a1a1a; text-decoration: none; }
</style>
