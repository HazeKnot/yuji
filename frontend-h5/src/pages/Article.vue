<template>
  <div class="article">
    <van-nav-bar title="孕产知识" left-arrow @click-left="$router.back()" />

    <template v-if="data">
      <div class="head">
        <h1>{{ data.title }}</h1>
        <div class="meta">
          <van-tag plain size="small">{{ data.categoryLabel }}</van-tag>
          <span class="week">{{ data.weekHint }}</span>
        </div>
        <p class="summary">{{ data.summary }}</p>
      </div>

      <div class="body">
        <p v-for="(p, i) in paragraphs" :key="i">{{ p }}</p>
      </div>

      <div class="source">
        <p class="source-label">内容来源</p>
        <p class="source-text">{{ data.source }}</p>
        <p v-if="data.reviewer" class="source-reviewer">审核：{{ data.reviewer }}</p>
      </div>

      <p class="disclaimer">{{ data.disclaimer }}</p>
    </template>

    <van-empty v-else-if="!loading" image="search" description="内容不存在或已下线" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../api'

interface ArticleVO {
  id: number
  code: string
  title: string
  category: string
  categoryLabel: string
  minWeek: number
  maxWeek: number
  weekHint: string
  summary: string
  body: string
  source: string
  reviewer: string | null
  disclaimer: string
}

const route = useRoute()
const data = ref<ArticleVO | null>(null)
const loading = ref(true)

const paragraphs = computed(() =>
  (data.value?.body || '')
    .split(/\n{2,}|\n/)
    .map((s) => s.trim())
    .filter(Boolean),
)

onMounted(async () => {
  try {
    const res = await api.get<never, { data: { data: ArticleVO } }>(`/articles/${route.params.id}`)
    data.value = res.data.data
  } catch {
    data.value = null
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.article {
  min-height: 100vh;
  background: #fff;
  padding-bottom: 40px;
}
.head {
  padding: 20px 20px 4px;
}
.head h1 {
  margin: 0;
  font-size: 21px;
  line-height: 1.4;
  color: #262626;
}
.meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
}
.week {
  font-size: 12px;
  color: #b3b3b3;
}
.summary {
  margin: 12px 0 0;
  font-size: 13px;
  color: #7a9b76;
  line-height: 1.6;
}
.body {
  padding: 8px 20px 0;
}
.body p {
  margin: 14px 0;
  font-size: 15px;
  line-height: 1.85;
  color: #3a3a3a;
}
.source {
  margin: 22px 20px 0;
  padding: 12px 14px;
  background: #f6f7f4;
  border-radius: 10px;
}
.source-label {
  margin: 0 0 4px;
  font-size: 11px;
  color: #a8a8a8;
}
.source-text {
  margin: 0;
  font-size: 12px;
  color: #606266;
  line-height: 1.6;
}
.source-reviewer {
  margin: 6px 0 0;
  font-size: 11px;
  color: #b3b3b3;
}
.disclaimer {
  margin: 18px 20px 0;
  font-size: 11px;
  color: #c0c0c0;
  line-height: 1.6;
}
</style>
