<template>
  <div class="page-secondary">
    <van-nav-bar title="月事餐饮" left-arrow @click-left="$router.back()" />

    <div class="phase-card">
      <div class="phase-head">
        <div>
          <p class="eyebrow">本周处于</p>
          <p class="phase-name">{{ plan?.phaseLabel || '均衡饮食' }}</p>
        </div>
        <span class="phase-chip">四餐搭配</span>
      </div>
      <p class="phase-hint">{{ plan?.phaseHint }}</p>
      <div class="week-nav">
        <button @click="shiftWeek(-7)"><van-icon name="arrow-left" /> 上一周</button>
        <span>{{ plan?.weekStart }} — {{ plan?.weekEnd }}</span>
        <button @click="shiftWeek(7)">下一周 <van-icon name="arrow" /></button>
      </div>
    </div>

    <van-tabs v-model:active="activeDay" class="day-tabs" swipeable animated>
      <van-tab v-for="(day, i) in plan?.days || []" :key="day.date" :title="day.weekday">
        <div class="day-panel">
          <div class="day-head">
            <span>{{ day.date }}</span>
            <span class="pill">{{ day.nutrients }}</span>
          </div>

          <div class="meal" v-for="m in day.meals" :key="m.type">
            <span class="meal-type" :class="m.type.toLowerCase()">{{ m.typeLabel }}</span>
            <p class="meal-title">{{ m.title }}</p>
          </div>

          <p class="day-tip">{{ day.tips }}</p>
        </div>
      </van-tab>
    </van-tabs>

    <div class="section">
      <div class="section-title">
        <h3>本周购物清单</h3>
        <span class="more">{{ plan?.shoppingList?.length || 0 }} 样</span>
      </div>
      <div class="card">
        <div class="shopping">
          <span v-for="item in plan?.shoppingList" :key="item" class="shop-item">{{ item }}</span>
        </div>
      </div>
    </div>

    <div class="card note-card">
      <p class="note-title">怎么用这份食谱</p>
      <p class="muted">
        按自己的胃口和过敏情况替换同类食材即可，不必严格照做。贫血、妊娠糖尿病、食物过敏等情况，请以医生或营养师的方案为准。
      </p>
    </div>

    <p class="disclaimer">{{ plan?.disclaimer }}</p>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { get } from '../api/request'

interface MealVO {
  type: string
  typeLabel: string
  title: string
}

interface DayVO {
  date: string
  weekday: string
  dayIndex: number
  meals: MealVO[]
  nutrients: string
  tips: string
}

interface PlanVO {
  phase: string
  phaseLabel: string
  phaseHint: string
  weekStart: string
  weekEnd: string
  days: DayVO[]
  shoppingList: string[]
  disclaimer: string
}

const plan = ref<PlanVO | null>(null)
const activeDay = ref(0)
const week = ref('')

function shiftWeek(days: number) {
  const base = week.value ? new Date(week.value) : new Date()
  base.setDate(base.getDate() + days)
  week.value = base.toISOString().slice(0, 10)
  load()
}

async function load() {
  plan.value = await get<PlanVO>('/wellness/meal-plan', {
    params: week.value ? { week: week.value } : {},
  })
  const todayIndex = plan.value?.days?.findIndex((d) => d.date === new Date().toISOString().slice(0, 10))
  activeDay.value = todayIndex != null && todayIndex >= 0 ? todayIndex : 0
}

onMounted(load)
</script>

<style scoped>
.phase-card {
  margin: 12px 16px 0;
  padding: 20px;
  border-radius: 22px;
  background: linear-gradient(145deg, #fdf6ea, #f7ecdb);
}

.phase-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.eyebrow {
  margin: 0;
  font-size: 11px;
  color: #a88b62;
  letter-spacing: 1px;
}

.phase-name {
  margin: 6px 0 0;
  font-size: 22px;
  font-weight: 600;
  color: #7b5a2e;
}

.phase-chip {
  font-size: 10px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.6);
  color: #8a6a3c;
}

.phase-hint {
  margin: 12px 0 0;
  font-size: 12px;
  line-height: 1.7;
  color: #7c6749;
}

.week-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px dashed #e7d7bd;
  font-size: 11px;
  color: #8a7658;
}

.week-nav button {
  border: none;
  background: transparent;
  font-size: 11px;
  color: #8a7658;
  display: flex;
  align-items: center;
  gap: 3px;
}

.day-tabs {
  margin-top: 16px;
}

.day-panel {
  margin: 12px 16px 0;
  padding: 18px;
  background: #fff;
  border-radius: 18px;
  box-shadow: 0 6px 18px rgba(78, 96, 73, 0.05);
}

.day-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--yuji-muted);
}

.day-head .pill {
  flex-shrink: 0;
  max-width: 70%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meal {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 14px 0;
  border-bottom: 1px solid var(--yuji-line);
}

.meal:last-of-type {
  border-bottom: none;
}

.meal-type {
  flex-shrink: 0;
  width: 44px;
  text-align: center;
  font-size: 10px;
  padding: 4px 0;
  border-radius: 8px;
  background: #f2f6f0;
  color: #5d7f5a;
}

.meal-type.lunch {
  background: #fbf4e8;
  color: #a4783e;
}

.meal-type.dinner {
  background: #eef2f6;
  color: #5c7f9a;
}

.meal-type.snack {
  background: #f7ecee;
  color: #a25f68;
}

.meal-title {
  margin: 3px 0 0;
  font-size: 14px;
  line-height: 1.6;
  color: var(--yuji-ink);
}

.day-tip {
  margin: 14px 0 0;
  padding: 10px 12px;
  border-radius: 12px;
  background: #fbfaf7;
  font-size: 11px;
  line-height: 1.7;
  color: #8a8f86;
}

.shopping {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.shop-item {
  font-size: 12px;
  padding: 5px 11px;
  border-radius: 999px;
  background: #f4f7f2;
  color: #5f6d5b;
}

.note-card {
  background: #fdfcf9;
}

.note-title {
  margin: 0 0 6px;
  font-size: 13px;
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
