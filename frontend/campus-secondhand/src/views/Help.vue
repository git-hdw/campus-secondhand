<template>
  <div class="help-page">
    <main>
      <h1>使用说明</h1>
      <section id="about">
        <h2>这个系统做什么</h2>
        <p>同学之间当面看货、当面交易。平台不收款，不发货，也不担保。</p>
      </section>
      <section id="trade">
        <h2>交易说明</h2>
        <ul>
          <li>新商品先由管理员审核，通过后才公开。</li>
          <li>下单后商品锁定，同一件商品同时只能有一笔待见面订单。这时页面显示「待见面」，不再显示「在售」。</li>
          <li>见面地点在图书馆、教学楼、食堂、宿舍楼下、操场中选择。</li>
          <li>买家点「我已见面收货」只表示本人声明已经当面看过并收下。系统不核验位置。</li>
          <li>商品已删除时，待见面订单只能取消，不能确认收货。</li>
        </ul>
      </section>
      <section id="faq">
        <h2>常见问题</h2>
        <p>学号由本人填写，不是学校认证。填了显示「已登记学号」，没填显示「未填学号」。登录可以用用户名或学号。</p>
        <p>草稿只保存在这台浏览器。忘记密码没有邮件重置，登录后在个人中心修改密码。</p>
        <p>卖家没有上传图片时，页面显示「暂无图片」。不要凭空图片判断成色，先在消息里问清楚。</p>
      </section>
      <section id="contact">
        <h2>联系</h2>
        <p>约见和提问都在商品页的「联系卖家」里完成。没有客服热线和社交账号。</p>
      </section>
      <section id="privacy">
        <h2>资料</h2>
        <p>他人只能看到你的用户名和头像，以及你是否填写了学号。邮箱、手机和学号本身不公开。</p>
      </section>
    </main>
  </div>
</template>

<style scoped>
.help-page {
  min-height: 100vh;
  background: var(--color-background);
  color: var(--color-foreground);
}
.help-bar {
  display: flex;
  gap: 20px;
  padding: 18px 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.help-bar a { color: #93c5fd; text-decoration: none; }
main { max-width: 720px; margin: 0 auto; padding: 32px 20px 64px; }
h1 { margin: 0 0 24px; font-size: 28px; }
section { margin-bottom: 28px; }
h2 { margin: 0 0 8px; font-size: 18px; }
p, li { line-height: 1.7; color: var(--color-muted-foreground); }
</style>
