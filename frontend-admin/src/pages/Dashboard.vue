<template>
  <div v-loading="loading">
    <el-alert
      v-if="error"
      type="error"
      :closable="false"
      :title="error"
      description="请确认后端已启动，且当前账号角色为机构账号"
      show-icon
    />

    <template v-if="wellness">
      <div class="section-head">
        <h3>全周期运营</h3>
        <span>健康、社区、心理与合作机构的整体水位</span>
      </div>
      <el-row :gutter="14" class="stat-row">
        <el-col v-for="s in wellnessStats" :key="s.label" :xs="12" :sm="8" :md="4">
          <div class="metric">
            <p class="metric-num">{{ s.value }}</p>
            <p class="metric-label">{{ s.label }}</p>
            <p class="metric-hint">{{ s.hint }}</p>
          </div>
        </el-col>
      </el-row>
    </template>

    <template v-if="data">
      <div class="section-head">
        <h3>孕产照护</h3>
        <span>在管家庭的任务与产检完成情况</span>
      </div>
      <el-row :gutter="16">
        <el-col v-for="s in stats" :key="s.label" :xs="12" :sm="12" :md="6">
          <el-card shadow="never" class="stat-card">
            <p class="stat-num">{{ s.value }}</p>
            <p class="stat-label">{{ s.label }}</p>
            <p class="stat-hint">{{ s.hint }}</p>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="charts">
        <el-col :md="9" :sm="24">
          <el-card shadow="never">
            <template #header>
              <div class="card-head">
                <span>在管孕妈孕周分布</span>
                <span class="muted">合计 {{ data.profileCount }} 人已建档</span>
              </div>
            </template>
            <div ref="pieEl" class="chart"></div>
          </el-card>
        </el-col>
        <el-col :md="15" :sm="24">
          <el-card shadow="never">
            <template #header>
              <div class="card-head">
                <span>近 7 天指标记录</span>
                <span class="muted">累计 {{ data.recordCount }} 条</span>
              </div>
            </template>
            <div ref="barEl" class="chart"></div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="charts">
        <el-col :md="12" :sm="24">
          <el-card shadow="never">
            <template #header>
              <div class="card-head">
                <span>家属任务完成率</span>
                <span class="muted">{{ data.taskDone }} / {{ data.taskTotal }} 项</span>
              </div>
            </template>
            <el-progress
              :percentage="data.taskRate"
              :stroke-width="14"
              :color="progressColor(data.taskRate)"
            />
            <p class="progress-hint">
              完成率高说明家属参与度好；低于 40% 可考虑在宣教里强调任务分工。
            </p>
          </el-card>
        </el-col>
        <el-col :md="12" :sm="24">
          <el-card shadow="never">
            <template #header>
              <div class="card-head">
                <span>产检完成率</span>
                <span class="muted">{{ data.checkupDone }} / {{ data.checkupTotal }} 项</span>
              </div>
            </template>
            <el-progress
              :percentage="data.checkupRate"
              :stroke-width="14"
              :color="progressColor(data.checkupRate)"
            />
            <p class="progress-hint">
              按建档用户的全孕期产检项统计，含已完成；跳过项不计入完成。
            </p>
          </el-card>
        </el-col>
      </el-row>

      <p class="note">{{ data.note }}</p>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, shallowRef } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { getData } from '../api'

echarts.use([BarChart, PieChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

interface DashboardVO {
  momCount: number
  familyCount: number
  memberCount: number
  profileCount: number
  recordCount: number
  reportCount: number
  taskTotal: number
  taskDone: number
  taskRate: number
  checkupTotal: number
  checkupDone: number
  checkupRate: number
  weekBuckets: { label: string; count: number }[]
  recentRecords: { date: string; count: number }[]
  note: string
}

interface WellnessVO {
  userCount: number
  cycleRecords: number
  posts: number
  pendingPosts: number
  openTickets: number
  redTickets: number
  leads: number
  newLeads: number
  partnerOrgs: number
  checkins: number
  scaleResults: number
}

const data = ref<DashboardVO | null>(null)
const wellness = ref<WellnessVO | null>(null)
const loading = ref(false)
const error = ref('')

const pieEl = ref<HTMLElement>()
const barEl = ref<HTMLElement>()
const pie = shallowRef<echarts.ECharts>()
const bar = shallowRef<echarts.ECharts>()

const stats = computed(() => {
  const d = data.value
  if (!d) return []
  return [
    { label: '在管孕妈', value: d.momCount, hint: `已建档 ${d.profileCount} 人` },
    { label: '绑定家庭', value: d.familyCount, hint: `家庭成员 ${d.memberCount} 人` },
    { label: '指标记录', value: d.recordCount, hint: `报告 ${d.reportCount} 份` },
    { label: '家属任务', value: d.taskTotal, hint: `已完成 ${d.taskDone} 项` },
  ]
})

const wellnessStats = computed(() => {
  const w = wellness.value
  if (!w) return []
  return [
    { label: '注册用户', value: w.userCount, hint: '含经期与孕产人群' },
    { label: '周期记录', value: w.cycleRecords, hint: `情绪打卡 ${w.checkins} 次` },
    { label: '社区帖子', value: w.posts, hint: `待审核 ${w.pendingPosts} 篇` },
    { label: '待跟进工单', value: w.openTickets, hint: `红档 ${w.redTickets} 个` },
    { label: '预约线索', value: w.leads, hint: `待联系 ${w.newLeads} 条` },
    { label: '合作机构', value: w.partnerOrgs, hint: `量表 ${w.scaleResults} 份` },
  ]
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [d, w] = await Promise.all([
      getData<DashboardVO>('/admin/dashboard'),
      getData<WellnessVO>('/admin/wellness/overview').catch(() => null),
    ])
    data.value = d
    wellness.value = w
    await nextTick()
    renderCharts()
  } catch (e: any) {
    error.value = e?.message || '看板数据加载失败'
  } finally {
    loading.value = false
  }
}

function renderCharts() {
  const d = data.value
  if (!d) return

  if (pieEl.value) {
    pie.value = pie.value || echarts.init(pieEl.value)
    pie.value.setOption(
      {
        tooltip: { trigger: 'item', formatter: '{b}：{c} 人（{d}%）' },
        legend: { bottom: 0, itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 12 } },
        color: ['#a8c3a4', '#7a9b76', '#4a6b47', '#d9d9d9'],
        series: [
          {
            name: '孕周分布',
            type: 'pie',
            radius: ['42%', '66%'],
            center: ['50%', '44%'],
            avoidLabelOverlap: true,
            itemStyle: { borderColor: '#fff', borderWidth: 2 },
            label: { formatter: '{b}\n{c} 人', fontSize: 11 },
            data: d.weekBuckets.map((b) => ({ name: b.label, value: b.count })),
          },
        ],
      },
      true,
    )
  }

  if (barEl.value) {
    bar.value = bar.value || echarts.init(barEl.value)
    bar.value.setOption(
      {
        grid: { left: 40, right: 16, top: 20, bottom: 40 },
        tooltip: { trigger: 'axis' },
        xAxis: {
          type: 'category',
          data: d.recentRecords.map((r) => r.date.slice(5)),
          axisLabel: { fontSize: 11, color: '#909399' },
          axisLine: { lineStyle: { color: '#e6e6e6' } },
        },
        yAxis: {
          type: 'value',
          minInterval: 1,
          axisLabel: { fontSize: 11, color: '#909399' },
          splitLine: { lineStyle: { color: '#f2f2f2' } },
        },
        series: [
          {
            name: '记录条数',
            type: 'bar',
            barMaxWidth: 28,
            itemStyle: { color: '#7a9b76', borderRadius: [4, 4, 0, 0] },
            data: d.recentRecords.map((r) => r.count),
          },
        ],
      },
      true,
    )
  }
}

function progressColor(rate: number) {
  if (rate >= 70) return '#7a9b76'
  if (rate >= 40) return '#e6a23c'
  return '#f56c6c'
}

function onResize() {
  pie.value?.resize()
  bar.value?.resize()
}

onMounted(() => {
  load()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  pie.value?.dispose()
  bar.value?.dispose()
})
</script>

<style scoped>
.section-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin: 4px 2px 12px;
}
.section-head h3 {
  margin: 0;
  font-size: 15px;
  color: #303133;
}
.section-head span {
  font-size: 12px;
  color: #b3b3b3;
}
.stat-row {
  margin-bottom: 22px;
}
.metric {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 4px 14px rgba(122, 155, 118, 0.06);
}
.metric-num {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  color: #4a6b47;
  line-height: 1.1;
}
.metric-label {
  margin: 8px 0 0;
  font-size: 12px;
  color: #606266;
}
.metric-hint {
  margin: 3px 0 0;
  font-size: 10px;
  color: #b3b3b3;
}
.stat-card {
  margin-bottom: 16px;
  border-radius: 12px;
}
.stat-num {
  font-size: 30px;
  font-weight: 700;
  margin: 0;
  color: #303133;
  line-height: 1.1;
}
.stat-label {
  margin: 8px 0 0;
  color: #606266;
  font-size: 13px;
}
.stat-hint {
  margin: 4px 0 0;
  color: #b3b3b3;
  font-size: 12px;
}
.charts {
  margin-bottom: 16px;
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.muted {
  color: #b3b3b3;
  font-size: 12px;
}
.chart {
  height: 300px;
}
.progress-hint {
  margin: 14px 0 0;
  color: #b3b3b3;
  font-size: 12px;
  line-height: 1.6;
}
.note {
  margin: 4px 2px 8px;
  color: #b3b3b3;
  font-size: 12px;
}
</style>
