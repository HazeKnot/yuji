<template>
  <el-container class="layout">
    <el-aside width="216px" class="aside">
      <div class="brand">
        <p class="brand-name">育己</p>
        <p class="brand-sub">机构协作端</p>
      </div>
      <el-menu :default-active="route.path" router class="menu">
        <el-menu-item index="/">全周期看板</el-menu-item>
        <el-menu-item index="/members">用户管理</el-menu-item>
        <el-menu-item index="/alerts">AI 风控告警</el-menu-item>
        <el-menu-item index="/tickets">风险跟进</el-menu-item>
        <el-menu-item index="/leads">预约线索</el-menu-item>
        <el-menu-item index="/partners">合作机构</el-menu-item>
        <el-menu-item index="/community">社区审核</el-menu-item>
      </el-menu>
      <p class="aside-foot">只展示聚合数据与用户主动提交的预约，不开放健康明细。</p>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div>
          <p class="title">{{ route.meta.title || '全周期看板' }}</p>
          <p class="subtitle">{{ route.meta.subtitle || '' }}</p>
        </div>
        <div class="right">
          <el-tag size="small" effect="plain" type="success">机构账号</el-tag>
          <span class="org">{{ orgName }}</span>
          <el-button text @click="logout">退出</el-button>
        </div>
      </el-header>

      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { currentUser, logout as clearSession } from '../api'

const route = useRoute()
const orgName = computed(() => currentUser().nickname || '妇幼合作办')

function logout() {
  clearSession()
}
</script>

<style scoped>
.layout {
  height: 100vh;
}
.aside {
  background: #fff;
  border-right: 1px solid #eef0ee;
  display: flex;
  flex-direction: column;
}
.brand {
  padding: 24px 20px 16px;
}
.brand-name {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 4px;
  color: #4a6b47;
}
.brand-sub {
  margin: 6px 0 0;
  font-size: 11px;
  color: #a8b0a6;
  letter-spacing: 1px;
}
.menu {
  border-right: none;
  flex: 1;
}
.aside-foot {
  margin: 0;
  padding: 16px 20px 22px;
  font-size: 10px;
  line-height: 1.7;
  color: #c0c6be;
}
.header {
  height: 68px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #eef0ee;
}
.title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.subtitle {
  margin: 4px 0 0;
  font-size: 11px;
  color: #b3b3b3;
}
.right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.org {
  font-size: 13px;
  color: #606266;
}
</style>
