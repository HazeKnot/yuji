<template>
  <div class="knowledge">
    <van-nav-bar title="孕产知识" left-arrow @click-left="$router.back()" />

    <div class="intro">
      <p class="intro-title">{{ profile ? `你现在孕 ${profile.weeks} 周 ${profile.days} 天` : '按孕周推荐' }}</p>
      <p class="intro-sub">内容按孕周区间命中，每篇都标注来源；只讲能做什么，不给诊断结论。</p>
    </div>

    <van-tabs :active="active" shrink @update:active="onTab">
      <van-tab title="为你推荐" name="FOR_ME" />
      <van-tab v-for="c in categories" :key="c.value" :title="c.label" :name="c.value" />
    </van-tabs>

    <div v-if="active === 'FOR_ME' && !loading && !rows.length" class="empty">
      当前孕周还没有对应内容，换到分类看看
    </div>

    <van-cell-group :border="false" class="list">
      <van-cell
        v-for="a in rows"
        :key="a.id"
        :title="a.title"
        :label="a.summary"
        is-link
        @click="open(a.id)"
      >
        <template #icon>
          <span class="dot" :class="a.category"></span>
        </template>
        <template #value>
          <span class="week">{{ a.weekHint }}</span>
        </template>
      </van-cell>
      <van-cell v-if="active !== 'FOR_ME' && !loading && !rows.length" title="这个分类还没有内容" />
    </van-cell-group>

    <p class="footer">内容来源在各篇正文底部标注，不替代医生诊断。</p>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'

interface ProfileVO {
  weeks: number
  days: number
}
interface ArticleBriefVO {
  id: number
  code: string
  title: string
  category: string
  categoryLabel: string
  minWeek: number
  maxWeek: number
  weekHint: string
  summary: string
  source: string
}

const router = useRouter()
const categories = [
  { value: 'NUTRITION', label: '营养' },
  { value: 'CHECKUP', label: '产检' },
  { value: 'BODY', label: '身体' },
  { value: 'MIND', label: '情绪' },
  { value: 'PREPARE', label: '待产' },
]

const active = ref('FOR_ME')
const rows = ref<ArticleBriefVO[]>([])
const profile = ref<ProfileVO | null>(null)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    if (active.value === 'FOR_ME') {
      const res = await api.get<never, { data: { data: ArticleBriefVO[] } }>('/articles/for-me', {
        params: { limit: 20 },
      })
      rows.value = res.data.data
    } else {
      const res = await api.get<never, { data: { data: ArticleBriefVO[] } }>('/articles', {
        params: { category: active.value },
      })
      rows.value = res.data.data
    }
  } finally {
    loading.value = false
  }
}

async function onTab(name: string | number) {
  active.value = String(name)
  await load()
}

function open(id: number) {
  router.push(`/knowledge/${id}`)
}

onMounted(async () => {
  const me = await api
    .get<never, { data: { data: ProfileVO } }>('/profiles/mine')
    .catch(() => null)
  profile.value = me?.data?.data ?? null
  await load()
})
</script>

<style scoped>
.knowledge {
  min-height: 100vh;
  padding-bottom: 32px;
  background: #f7f8f6;
}
.intro {
  padding: 16px;
  background: #fff;
}
.intro-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.intro-sub {
  margin: 6px 0 0;
  font-size: 12px;
  color: #a8a8a8;
  line-height: 1.6;
}
.list {
  margin-top: 8px;
}
.dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-right: 8px;
  background: #7a9b76;
}
.dot.CHECKUP {
  background: #5b8db8;
}
.dot.BODY {
  background: #e0a96d;
}
.dot.MIND {
  background: #b48ead;
}
.dot.PREPARE {
  background: #8a8a8a;
}
.week {
  font-size: 11px;
  color: #b3b3b3;
}
.empty {
  padding: 28px 16px;
  text-align: center;
  color: #bbb;
  font-size: 13px;
}
.footer {
  margin: 20px 16px 0;
  font-size: 11px;
  color: #c4c4c4;
  text-align: center;
}
</style>
