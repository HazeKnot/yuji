<template>
  <div class="page">
    <van-nav-bar title="健康" />

    <div class="stage-hero">
      <div>
        <p class="stage-label">当前阶段</p>
        <p class="stage-name">{{ overview?.lifeStageLabel || '全周期健康' }}</p>
        <p class="stage-tip">{{ overview?.todayTip || '记录身体，也记录情绪。' }}</p>
      </div>
      <button class="edit" @click="$router.push('/onboarding')">调整</button>
    </div>

    <!-- 周期 -->
    <div class="card cycle-card">
      <div class="card-head">
        <div>
          <p class="eyebrow">月经周期</p>
          <p class="cycle-num">
            <template v-if="summary?.cycleDay">
              第 <strong>{{ summary.cycleDay }}</strong> 天
              <span class="pill">{{ summary.phaseLabel }}</span>
            </template>
            <template v-else>
              <span class="empty">还没有足够的周期记录</span>
            </template>
          </p>
        </div>
        <van-icon name="calendar-o" size="26" color="#a8c3a4" />
      </div>

      <p v-if="summary?.cycleDay" class="cycle-hint">{{ summary.phaseHint }}</p>
      <p v-else class="cycle-hint">记下这次月经第一天，就能开始预测和按阶段安排饮食。</p>

      <div class="cycle-meta" v-if="summary?.cycleDay">
        <div>
          <span class="meta-num">{{ summary.avgCycleLength }}</span>
          <span class="meta-label">平均周期</span>
        </div>
        <div>
          <span class="meta-num">{{ summary.avgPeriodLength }}</span>
          <span class="meta-label">平均经期</span>
        </div>
        <div>
          <span class="meta-num">{{ summary.daysToNextPeriod }}</span>
          <span class="meta-label">距下次</span>
        </div>
      </div>

      <div class="card-actions">
        <van-button size="small" round color="#7a9b76" @click="$router.push('/cycle')">记录月经</van-button>
        <van-button size="small" round plain color="#7a9b76" @click="$router.push('/cycle')">周期日历</van-button>
      </div>
    </div>

    <!-- 功能入口 -->
    <div class="section-title" style="margin-top: 22px">
      <h3>身体与私密</h3>
    </div>
    <div class="card tight menu">
      <div class="menu-item" @click="$router.push('/meal')">
        <span class="icon" style="background: #fbf1e4"><van-icon name="shopping-cart-o" color="#b58243" /></span>
        <div class="menu-text">
          <strong>月事餐饮规划</strong>
          <em>按经期 / 卵泡期 / 排卵期 / 黄体期生成一周食谱</em>
        </div>
        <van-icon name="arrow" color="#c4c4c4" />
      </div>
      <div class="menu-item" @click="$router.push('/consult')">
        <span class="icon" style="background: #f7ecee"><van-icon name="chat-o" color="#a25f68" /></span>
        <div class="menu-text">
          <strong>私密健康咨询</strong>
          <em>分泌物、瘙痒、痛经、乳房、盆底、避孕，匿名问</em>
        </div>
        <van-icon name="arrow" color="#c4c4c4" />
      </div>
      <div class="menu-item" @click="$router.push('/records')">
        <span class="icon" style="background: #eef4ec"><van-icon name="chart-trending-o" color="#5d7f5a" /></span>
        <div class="menu-text">
          <strong>身体指标</strong>
          <em>体重、血压、血糖、胎动与趋势</em>
        </div>
        <van-icon name="arrow" color="#c4c4c4" />
      </div>
    </div>

    <div class="section-title" style="margin-top: 22px">
      <h3>孕产与档案</h3>
    </div>
    <div class="card tight menu">
      <div class="menu-item" @click="$router.push('/checkups')">
        <span class="icon" style="background: #eef4ec"><van-icon name="todo-list-o" color="#5d7f5a" /></span>
        <div class="menu-text">
          <strong>产检日历</strong>
          <em>按孕周生成的全孕期计划</em>
        </div>
        <van-icon name="arrow" color="#c4c4c4" />
      </div>
      <div class="menu-item" @click="$router.push('/reports')">
        <span class="icon" style="background: #eef2f6"><van-icon name="description-o" color="#5c7f9a" /></span>
        <div class="menu-text">
          <strong>检查报告</strong>
          <em>拍照归档，随时回看</em>
        </div>
        <van-icon name="arrow" color="#c4c4c4" />
      </div>
      <div class="menu-item" @click="$router.push('/family')">
        <span class="icon" style="background: #f3f0e8"><van-icon name="friends-o" color="#9a8148" /></span>
        <div class="menu-text">
          <strong>家人协同</strong>
          <em>任务分工与字段级隐私授权</em>
        </div>
        <van-icon name="arrow" color="#c4c4c4" />
      </div>
      <div class="menu-item" @click="$router.push('/partners')">
        <span class="icon" style="background: #eef4ec"><van-icon name="shop-o" color="#5d7f5a" /></span>
        <div class="menu-text">
          <strong>合作机构</strong>
          <em>妇幼保健院、月子中心、产后康复</em>
        </div>
        <van-icon name="arrow" color="#c4c4c4" />
      </div>
    </div>

    <p class="disclaimer">健康数据只用于生成你的建议。预测与筛查都不替代医生面诊。</p>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { get } from '../api/request'

interface Overview {
  hasProfile: boolean
  lifeStageLabel: string
  todayTip: string
}

interface Summary {
  cycleDay?: number
  phaseLabel?: string
  phaseHint?: string
  avgCycleLength?: number
  avgPeriodLength?: number
  daysToNextPeriod?: number
  nextPeriodDate?: string
}

const overview = ref<Overview | null>(null)
const summary = ref<Summary | null>(null)

onMounted(async () => {
  const [o, s] = await Promise.all([
    get<Overview>('/wellness/overview').catch(() => null),
    get<Summary>('/wellness/cycles/summary').catch(() => null),
  ])
  overview.value = o
  summary.value = s
})
</script>

<style scoped>
.stage-hero {
  margin: 10px 16px 0;
  padding: 20px;
  border-radius: 20px;
  background: linear-gradient(140deg, #f0f6ed, #e6efe3);
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.stage-label {
  margin: 0;
  font-size: 11px;
  color: #7f8f7a;
  letter-spacing: 1px;
}

.stage-name {
  margin: 6px 0 8px;
  font-size: 20px;
  font-weight: 600;
  color: var(--yuji-primary-deep);
}

.stage-tip {
  margin: 0;
  font-size: 12px;
  line-height: 1.7;
  color: #66745f;
}

.edit {
  border: 1px solid #cfdccb;
  background: rgba(255, 255, 255, 0.6);
  color: var(--yuji-primary-deep);
  border-radius: 999px;
  padding: 5px 12px;
  font-size: 11px;
  flex-shrink: 0;
}

.card-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.eyebrow {
  margin: 0;
  font-size: 11px;
  color: var(--yuji-muted);
  letter-spacing: 0.5px;
}

.cycle-num {
  margin: 8px 0 0;
  font-size: 15px;
  color: var(--yuji-text);
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.cycle-num strong {
  font-size: 30px;
  color: var(--yuji-primary-deep);
  line-height: 1;
}

.empty {
  font-size: 13px;
  color: var(--yuji-muted);
}

.cycle-hint {
  margin: 10px 0 0;
  font-size: 12px;
  color: var(--yuji-muted);
  line-height: 1.7;
}

.cycle-meta {
  display: flex;
  gap: 26px;
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px solid var(--yuji-line);
}

.cycle-meta > div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.meta-num {
  font-size: 17px;
  font-weight: 600;
  color: var(--yuji-ink);
}

.meta-label {
  font-size: 11px;
  color: var(--yuji-muted);
}

.card-actions {
  display: flex;
  gap: 10px;
  margin-top: 18px;
}

.menu {
  padding: 4px 16px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid var(--yuji-line);
}

.menu-item:last-child {
  border-bottom: none;
}

.icon {
  width: 38px;
  height: 38px;
  border-radius: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.menu-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.menu-text strong {
  font-size: 14px;
  font-weight: 500;
  color: var(--yuji-ink);
}

.menu-text em {
  font-style: normal;
  font-size: 11px;
  color: var(--yuji-muted);
  line-height: 1.5;
}

.disclaimer {
  margin: 22px 24px 0;
  font-size: 11px;
  color: #b3b3b3;
  line-height: 1.7;
  text-align: center;
}
</style>
