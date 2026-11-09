<template>
  <div class="records">
    <van-nav-bar title="指标记录" left-arrow @click-left="$router.back()" />

    <van-tabs v-model:active="active" shrink>
      <van-tab v-for="t in types" :key="t.code" :title="t.label" :name="t.code" />
    </van-tabs>

    <!-- 快速录入：只填一个数（血压两个），默认"现在" -->
    <div class="card">
      <p class="card-title">记一笔</p>
      <template v-if="active === 'BLOOD_PRESSURE'">
        <van-field v-model="value" type="number" label="收缩压" placeholder="如 120" :right-icon="unit" />
        <van-field v-model="value2" type="number" label="舒张压" placeholder="如 80" :right-icon="unit" />
      </template>
      <van-field
        v-else
        v-model="value"
        type="number"
        :label="currentType.label"
        :placeholder="currentType.placeholder"
        :right-icon="unit"
      />
      <div class="save">
        <van-button round block type="primary" :loading="saving" @click="save">保存</van-button>
      </div>
    </div>

    <!-- 趋势 -->
    <div class="card">
      <div class="card-head">
        <p class="card-title">近 30 天趋势</p>
        <span v-if="trend?.summary" class="summary">
          最新 {{ trend.summary.latest }} {{ unit }} · 平均 {{ trend.summary.avg }}
        </span>
      </div>
      <div v-show="hasPoints" ref="chartEl" class="chart"></div>
      <p v-if="!hasPoints" class="empty">还没有数据，记两笔就能看到曲线</p>
      <p v-if="trend" class="disclaimer">{{ trend.disclaimer }}</p>
    </div>

    <!-- 最近记录 -->
    <van-cell-group inset class="list">
      <van-swipe-cell v-for="r in records" :key="r.id">
        <van-cell
          :title="`${r.typeLabel} ${r.value}${r.value2 ? '/' + r.value2 : ''} ${r.unit}`"
          :label="(r.measuredAt || '').replace('T', ' ').slice(0, 16)"
        />
        <template #right>
          <van-button square type="danger" text="删除" class="del" @click="remove(r)" />
        </template>
      </van-swipe-cell>
      <van-cell v-if="!records.length" title="暂无记录" />
    </van-cell-group>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { showConfirmDialog, showSuccessToast, showToast } from 'vant'
import { api } from '../api'
import { useTrendChart, type TrendData } from '../composables/useTrendChart'

interface TypeMeta {
  code: string
  label: string
  unit: string
  placeholder: string
}

const types: TypeMeta[] = [
  { code: 'WEIGHT', label: '体重', unit: 'kg', placeholder: '如 62.5' },
  { code: 'BLOOD_PRESSURE', label: '血压', unit: 'mmHg', placeholder: '' },
  { code: 'BLOOD_GLUCOSE', label: '血糖', unit: 'mmol/L', placeholder: '如 5.2' },
  { code: 'FETAL_MOVEMENT', label: '胎动', unit: '次/小时', placeholder: '如 5' },
]

interface RecordVO {
  id: number
  typeLabel: string
  value: number
  value2?: number | null
  unit: string
  measuredAt: string
}

const active = ref('WEIGHT')
const value = ref('')
const value2 = ref('')
const records = ref<RecordVO[]>([])
const trend = ref<TrendData | null>(null)
const saving = ref(false)
const chartEl = ref<HTMLElement>()
const { render: renderTrend, resize: resizeChart } = useTrendChart(chartEl)

const currentType = computed(() => types.find((t) => t.code === active.value)!)
const unit = computed(() => trend.value?.unit || currentType.value.unit)
const hasPoints = computed(() => !!trend.value?.points?.length)

async function loadRecords() {
  const res = await api.get<never, { data: { data: RecordVO[] } }>('/health-records', {
    params: { type: active.value },
  })
  records.value = res.data.data
}

async function loadTrend() {
  const res = await api.get<never, { data: { data: TrendData } }>('/health-records/trend', {
    params: { type: active.value, days: 30 },
  })
  trend.value = res.data.data
  await renderTrend(trend.value)
}

async function save() {
  const v = Number(value.value)
  if (!value.value || Number.isNaN(v)) return showToast('请填写数值')
  const body: Record<string, unknown> = { type: active.value, value: v }
  if (active.value === 'BLOOD_PRESSURE') {
    const v2 = Number(value2.value)
    if (!value2.value || Number.isNaN(v2)) return showToast('请填写舒张压')
    body.value2 = v2
  }
  saving.value = true
  try {
    await api.post('/health-records', body)
    value.value = ''
    value2.value = ''
    showSuccessToast('已记录')
    await Promise.all([loadTrend(), loadRecords()])
  } finally {
    saving.value = false
  }
}

async function remove(r: RecordVO) {
  await showConfirmDialog({ title: '删除这条记录？', message: '删除后不可恢复' })
  await api.delete(`/health-records/${r.id}`)
  showSuccessToast('已删除')
  await Promise.all([loadTrend(), loadRecords()])
}

function onResize() {
  resizeChart()
}

watch(active, async () => {
  await Promise.all([loadTrend(), loadRecords()])
})

onMounted(async () => {
  await Promise.all([loadTrend(), loadRecords()])
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
})
</script>

<style scoped>
.records {
  min-height: 100vh;
  padding-bottom: 80px;
}
.card {
  margin: 12px 16px;
  padding: 16px;
  background: #fff;
  border-radius: 16px;
}
.card-title {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 600;
}
.card-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}
.summary {
  color: #7a9b76;
  font-size: 12px;
}
.chart {
  width: 100%;
  height: 200px;
}
.empty {
  margin: 24px 0;
  text-align: center;
  color: #bbb;
  font-size: 13px;
}
.disclaimer {
  margin: 8px 0 0;
  color: #b8b8b8;
  font-size: 11px;
  line-height: 1.6;
}
.save {
  margin-top: 12px;
}
.list {
  margin-bottom: 20px;
}
.del {
  height: 100%;
}
</style>
