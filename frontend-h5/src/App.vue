<template>
  <router-view />
  <van-tabbar v-if="showTabbar" route safe-area-inset-bottom active-color="#5d7f5a" inactive-color="#9aa09a">
    <van-tabbar-item replace to="/" icon="home-o">首页</van-tabbar-item>
    <van-tabbar-item replace to="/health" icon="chart-trending-o">健康</van-tabbar-item>
    <van-tabbar-item replace to="/community" icon="chat-o">社区</van-tabbar-item>
    <van-tabbar-item replace to="/care" icon="like-o">关怀</van-tabbar-item>
    <van-tabbar-item replace to="/me" icon="user-o">我的</van-tabbar-item>
  </van-tabbar>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
// 只有五个一级页面显示底部导航；二级页面用 nav-bar 返回，避免误触
const tabs = ['/', '/health', '/community', '/care', '/me']
const showTabbar = computed(() => tabs.includes(route.path))
</script>

<style>
.van-tabbar {
  border-top: 1px solid rgba(236, 235, 230, 0.9);
  box-shadow: 0 -6px 20px rgba(78, 96, 73, 0.05);
}
.van-tabbar-item__text {
  font-size: 11px;
}
</style>
