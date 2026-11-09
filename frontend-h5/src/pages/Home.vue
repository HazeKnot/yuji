<template>
  <div class="page home">
    <div class="hero">
      <div class="hero-top">
        <p class="hello">{{ greeting }}，{{ overview?.nickname || '姐妹' }}</p>
        <span class="stage">{{ overview?.lifeStageLabel || '全周期健康' }}</span>
      </div>

      <div class="metric">
        <span class="num">{{ heroNumber }}</span>
        <span class="unit">{{ heroUnit }}</span>
      </div>
      <p class="metric-hint">{{ heroHint }}</p>

      <div class="hero-pills">
        <span v-if="overview?.pregnant" class="hero-pill">距预产期 {{ overview.daysToEdd }} 天</span>
        <span v-else-if="overview?.cycleDay" class="hero-pill">
          下次月经 {{ overview.nextPeriodDate }} · {{ overview.daysToNextPeriod }} 天后
        </span>
        <span v-else-if="overview?.careDayIndex" class="hero-pill">{{ overview.careStageLabel }}</span>
      </div>
    </div>

    <!-- 快捷入口 -->
    <div class="quick">
      <button v-for="q in quicks" :key="q.label" class="quick-item" @click="$router.push(q.to)">
        <span class="quick-icon" :style="{ background: q.tint }">
          <van-icon :name="q.icon" size="20" color="#5d7f5a" />
        </span>
        <span class="quick-label">{{ q.label }}</span>
      </button>
    </div>

    <!-- 今日关怀 -->
    <div class="section">
      <div class="section-title">
        <h3>今天只做这几件事</h3>
        <span class="more" @click="$router.push('/care')">全部 ›</span>
      </div>
      <div class="card tight">
        <van-cell
          v-for="task in todayTasks"
          :key="task.key"
          :title="task.title"
          :label="task.label"
          :value="task.tag"
          is-link
          :to="task.to"
        />
      </div>
    </div>

    <!-- 周期 / 月子 / 孕周 主卡 -->
    <div class="section" v-if="overview?.cycleDay || overview?.careDayIndex">
      <div class="section-title">
        <h3>{{ overview?.careDayIndex ? '月子进度' : '这个周期' }}</h3>
        <span class="more" @click="$router.push(overview?.careDayIndex ? '/care' : '/cycle')">详情 ›</span>
      </div>
      <div class="card insight">
        <div class="insight-row">
          <div>
            <p class="insight-label">{{ overview?.careDayIndex ? '产后第' : '周期第' }}天</p>
            <p class="insight-num">
              {{ overview?.careDayIndex || overview?.cycleDay }}
              <span>天</span>
            </p>
          </div>
          <div class="insight-right">
            <span class="pill">{{ overview?.careStageLabel || overview?.phaseLabel }}</span>
            <p class="muted">
              {{ overview?.careStageLabel ? '按 42 天节奏，一次只处理今天' : overview?.phaseHint }}
            </p>
          </div>
        </div>
        <div class="track">
          <span
            class="track-fill"
            :style="{ width: trackPercent + '%' }"
          ></span>
        </div>
        <p class="track-hint">
          {{ overview?.careDayIndex ? '已完成 ' + trackPercent + '% 的月子任务' : '平均周期 ' + (overview?.avgCycleLength || 28) + ' 天 · 经期 ' + (overview?.avgPeriodLength || 5) + ' 天' }}
        </p>
      </div>
    </div>

    <!-- 社区热帖 -->
    <div class="section">
      <div class="section-title">
        <h3>她社区 · 正在聊</h3>
        <span class="more" @click="$router.push('/community')">进去看看 ›</span>
      </div>
      <div class="card tight">
        <div v-for="post in posts" :key="post.id" class="post" @click="$router.push(`/community/${post.id}`)">
          <span class="pill pink">{{ post.categoryLabel }}</span>
          <p class="post-title">{{ post.title }}</p>
          <p class="post-meta">{{ post.authorName }} · {{ post.timeText }} · {{ post.likeCount }} 人抱抱</p>
        </div>
        <p v-if="!posts.length" class="muted" style="padding: 8px 0">
          还没有帖子。你可以是第一个开口的人。
        </p>
      </div>
    </div>

    <!-- 本周知识 -->
    <div class="section" v-if="articles.length">
      <div class="section-title">
        <h3>为你挑的知识</h3>
        <span class="more" @click="$router.push('/knowledge')">全部 ›</span>
      </div>
      <div class="card tight">
        <van-cell
          v-for="a in articles"
          :key="a.id"
          :title="a.title"
          :label="a.summary"
          is-link
          @click="$router.push(`/knowledge/${a.id}`)"
        />
      </div>
    </div>

    <!-- 合作机构 -->
    <div class="card partner" @click="$router.push('/partners')">
      <div>
        <p class="partner-title">找身边的妇幼保健院 / 月子中心</p>
        <p class="muted">合作机构可预约产后复查、盆底评估、月子套餐</p>
      </div>
      <van-icon name="arrow" color="#8aa886" />
    </div>

    <p class="disclaimer">育己提供健康教育与自我照护工具，不提供诊断与处方。</p>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { get } from '../api/request'

interface TaskVO {
  id: number
  title: string
  detail: string
  status: string
}

interface Overview {
  hasProfile: boolean
  nickname: string
  lifeStage: string
  lifeStageLabel: string
  cycleDay?: number
  phase?: string
  phaseLabel?: string
  phaseHint?: string
  nextPeriodDate?: string
  daysToNextPeriod?: number
  avgCycleLength?: number
  avgPeriodLength?: number
  pregnant?: boolean
  weeks?: number
  days?: number
  daysToEdd?: number
  pregnancyStage?: string
  postpartum?: boolean
  babyAgeMonths?: number
  careDayIndex?: number
  careStageLabel?: string
  todayCareTasks: TaskVO[]
  todayTip: string
}

interface PostVO {
  id: number
  title: string
  categoryLabel: string
  authorName: string
  timeText: string
  likeCount: number
}

interface ArticleBrief {
  id: number
  title: string
  summary: string
}

const router = useRouter()
const overview = ref<Overview | null>(null)
const posts = ref<PostVO[]>([])
const articles = ref<ArticleBrief[]>([])

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 11) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const quicks = [
  { icon: 'calendar-o', label: '经期记录', to: '/cycle', tint: '#eef4ec' },
  { icon: 'shopping-cart-o', label: '月事餐饮', to: '/meal', tint: '#fbf1e4' },
  { icon: 'chat-o', label: '私密咨询', to: '/consult', tint: '#f7ecee' },
  { icon: 'like-o', label: '月子调理', to: '/care?tab=0', tint: '#eef4ec' },
  { icon: 'guide-o', label: '育儿课堂', to: '/care?tab=1', tint: '#eef2f6' },
  { icon: 'smile-o', label: '心理康复', to: '/care?tab=2', tint: '#f7ecee' },
  { icon: 'chart-trending-o', label: '身体指标', to: '/records', tint: '#eef4ec' },
  { icon: 'todo-list-o', label: '产检日历', to: '/checkups', tint: '#f3f0e8' },
]

const heroNumber = computed(() => {
  const o = overview.value
  if (!o) return '·'
  if (o.pregnant && o.weeks != null) return o.weeks
  if (o.careDayIndex && o.careDayIndex <= 42) return o.careDayIndex
  if (o.cycleDay) return o.cycleDay
  if (o.babyAgeMonths != null) return o.babyAgeMonths
  return '她'
})

const heroUnit = computed(() => {
  const o = overview.value
  if (!o) return ''
  if (o.pregnant && o.weeks != null) return '周 + ' + (o.days ?? 0) + ' 天'
  if (o.careDayIndex && o.careDayIndex <= 42) return '产后第 ' + o.careDayIndex + ' 天'
  if (o.cycleDay) return '周期第 ' + o.cycleDay + ' 天'
  if (o.babyAgeMonths != null) return '宝宝月龄'
  return o.lifeStageLabel
})

const heroHint = computed(() => {
  const o = overview.value
  if (!o) return '正在整理今天的建议'
  if (o.pregnant) return o.pregnancyStage || '孕期'
  if (o.careDayIndex && o.careDayIndex <= 42) return o.careStageLabel || '月子期'
  if (o.cycleDay) return o.phaseLabel + ' · ' + o.phaseHint
  if (o.babyAgeMonths != null) return '养育不是考试，按孩子的节奏来'
  return o.todayTip
})

const trackPercent = computed(() => {
  const o = overview.value
  if (!o) return 0
  if (o.careDayIndex) return Math.min(100, Math.round((o.careDayIndex / 42) * 100))
  if (o.cycleDay && o.avgCycleLength) {
    return Math.min(100, Math.round((o.cycleDay / o.avgCycleLength) * 100))
  }
  return 0
})

const todayTasks = computed(() => {
  const o = overview.value
  if (!o) return []
  if (o.todayCareTasks?.length) {
    return o.todayCareTasks.slice(0, 3).map((t) => ({
      key: 'care-' + t.id,
      title: t.title,
      label: t.detail,
      tag: t.status === 'DONE' ? '已完成' : '待打卡',
      to: '/care?tab=0',
    }))
  }
  if (o.pregnant) {
    return [
      { key: 'p1', title: '喝一杯温水', label: '少量多次，比一次灌一大杯更舒服', tag: '现在', to: '/' },
      { key: 'p2', title: '数一次胎动', label: '安静坐 10 分钟，感受宝宝的节奏', tag: '今天', to: '/records' },
      { key: 'p3', title: '把一件家务交出去', label: '照顾自己不是自私，是长期作战的必需品', tag: '委托', to: '/family' },
    ]
  }
  if (o.cycleDay) {
    return [
      { key: 'c1', title: '记录今天的身体感受', label: '疼、胀、累，都值得被记下来', tag: '30 秒', to: '/cycle' },
      { key: 'c2', title: '看一眼本周食谱', label: '按周期阶段安排，不用每天想吃什么', tag: '饮食', to: '/meal' },
      { key: 'c3', title: '散步 10 分钟', label: '不追求步数，先把身体唤醒', tag: '轻活动', to: '/' },
    ]
  }
  return [
    { key: 'd1', title: '记下这次月经第一天', label: '有了起点，预测和饮食建议才会准', tag: '记录', to: '/cycle' },
    { key: 'd2', title: '做一次情绪打卡', label: '不需要理由，想起来就写一句', tag: '30 秒', to: '/care?tab=2' },
    { key: 'd3', title: '看看别人怎么说', label: '身体的困惑不需要一个人扛', tag: '社区', to: '/community' },
  ]
})

onMounted(async () => {
  try {
    const data = await get<Overview>('/wellness/overview')
    overview.value = data
    if (!data.hasProfile) {
      router.replace('/onboarding')
      return
    }
  } catch {
    router.replace('/onboarding')
    return
  }

  // 孕产知识按孕周命中，未孕用户没有孕周，不请求以免弹出 40401 提示
  const articleTask = overview.value?.pregnant
    ? get<ArticleBrief[]>('/articles/for-me', { params: { limit: 2 } }).catch(() => null)
    : Promise.resolve(null)
  const [postPage, articleList] = await Promise.all([
    get<{ records: PostVO[] }>('/wellness/community/posts', { params: { page: 1, size: 3 } }).catch(() => null),
    articleTask,
  ])
  posts.value = postPage?.records ?? []
  articles.value = articleList ?? []
})
</script>

<style scoped>
.hero {
  margin: 12px 16px 0;
  padding: 24px 22px 22px;
  border-radius: 24px;
  background: linear-gradient(150deg, #86a680 0%, #6a8d66 55%, #557850 100%);
  color: #fff;
  position: relative;
  overflow: hidden;
}

.hero::after {
  content: '';
  position: absolute;
  width: 180px;
  height: 180px;
  right: -60px;
  top: -70px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.09);
}

.hero-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
  z-index: 1;
}

.hello {
  margin: 0;
  font-size: 14px;
  opacity: 0.92;
}

.stage {
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
}

.metric {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-top: 22px;
}

.metric .num {
  font-size: 52px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: -1px;
}

.metric .unit {
  font-size: 15px;
  opacity: 0.88;
}

.metric-hint {
  margin: 10px 0 0;
  font-size: 13px;
  line-height: 1.7;
  opacity: 0.9;
  max-width: 88%;
}

.hero-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.hero-pill {
  font-size: 11px;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  line-height: 1.5;
}

.quick {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 4px;
  margin: 20px 12px 0;
}

.quick-item {
  border: none;
  background: transparent;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 8px 2px;
}

.quick-icon {
  width: 46px;
  height: 46px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.quick-label {
  font-size: 12px;
  color: var(--yuji-text);
}

.insight-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.insight-label {
  margin: 0;
  font-size: 12px;
  color: var(--yuji-muted);
}

.insight-num {
  margin: 6px 0 0;
  font-size: 34px;
  font-weight: 700;
  color: var(--yuji-primary-deep);
  line-height: 1;
}

.insight-num span {
  font-size: 13px;
  font-weight: 500;
  color: var(--yuji-muted);
}

.insight-right {
  text-align: right;
  max-width: 58%;
}

.insight-right .muted {
  margin: 6px 0 0;
}

.track {
  height: 7px;
  border-radius: 999px;
  background: #f0f2ee;
  margin-top: 18px;
  overflow: hidden;
}

.track-fill {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #a8c3a4, #6f936c);
  transition: width 0.5s ease;
}

.track-hint {
  margin: 8px 0 0;
  font-size: 11px;
  color: var(--yuji-muted);
}

.post {
  padding: 10px 0;
  border-bottom: 1px solid var(--yuji-line);
}

.post:last-child {
  border-bottom: none;
}

.post-title {
  margin: 8px 0 4px;
  font-size: 14px;
  font-weight: 500;
  color: var(--yuji-ink);
}

.post-meta {
  margin: 0;
  font-size: 11px;
  color: var(--yuji-muted);
}

.partner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.partner-title {
  margin: 0 0 4px;
  font-size: 14px;
  font-weight: 500;
}

.disclaimer {
  margin: 18px 24px 0;
  font-size: 11px;
  color: #b3b3b3;
  line-height: 1.7;
  text-align: center;
}
</style>
