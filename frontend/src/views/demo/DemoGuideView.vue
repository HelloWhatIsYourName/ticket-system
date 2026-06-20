<template>
  <section class="demo-guide-view">
    <header class="workspace-page-header">
      <div>
        <p>Demo guide</p>
        <h3>答辩演示导览</h3>
      </div>
      <span>10-15 分钟完整闭环</span>
    </header>

    <div class="demo-guide-layout">
      <article class="demo-step">
        <span>01</span>
        <h4>登录与权限恢复</h4>
        <p>使用 admin/user/agent 账号演示 RBAC、菜单差异、刷新后的登录态恢复。</p>
        <RouterLink to="/app/system">查看系统管理</RouterLink>
      </article>

      <article class="demo-step">
        <span>02</span>
        <h4>知识录入</h4>
        <p>录入“密码重置 FAQ”，确认文档解析状态，并用检索测试验证召回片段。</p>
        <RouterLink to="/app/knowledge">进入知识库</RouterLink>
      </article>

      <article class="demo-step">
        <span>03</span>
        <h4>AI 问答</h4>
        <p>询问“忘记密码后应该如何重置？”，展示回答、置信度、引用来源。</p>
        <RouterLink to="/app/ai/chat">进入 AI 问答</RouterLink>
      </article>

      <article class="demo-step">
        <span>04</span>
        <h4>转工单</h4>
        <p>在 AI 会话右侧创建工单，说明 sourceSessionId 与人工流转的关系。</p>
        <RouterLink to="/app/ai/chat">创建转人工工单</RouterLink>
      </article>

      <article class="demo-step">
        <span>05</span>
        <h4>处理工单</h4>
        <p>打开我的工单和详情页，演示回复、内部备注、开始处理、标记解决。</p>
        <RouterLink to="/app/tickets/my">查看我的工单</RouterLink>
      </article>

      <article class="demo-step">
        <span>06</span>
        <h4>统计看板</h4>
        <p>展示工单总量、处理状态、知识命中率、热门问题，连接管理视角。</p>
        <RouterLink to="/app/admin/dashboard">打开统计看板</RouterLink>
      </article>

      <article class="demo-step">
        <span>07</span>
        <h4>系统管理</h4>
        <p>展示用户启停、角色分配、权限模块，说明 RBAC 如何保护业务操作。</p>
        <RouterLink to="/app/system">查看角色权限</RouterLink>
      </article>
    </div>
  </section>
</template>
