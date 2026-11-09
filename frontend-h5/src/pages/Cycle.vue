<template>
  <div class="page-secondary">
    <van-nav-bar title="经期记录" left-arrow @click-left="$router.back()" />

    <div class="summary">
      <div class="summary-top">
        <div>
          <p class="eyebrow">现在</p>
          <p class="day" v-if="summary?.cycleDay">
            周期第 <strong>{{ summary.cycleDay }}</strong> 天
          </p>
          <p class="day" v-else>还没有预测</p>
          <p class="phase" v-if="summary?.phaseLabel">{{ summary.phaseLabel }} · {{ summary.phaseHint }}</p>
          <p class="phase" v-else>记录一次月经，就能看到预测。</p>
        </div>
        <div class="ring">
          <span>{{ summary?.cycleDay || '—' }}</span>
          <em>/ {{ summary?.avgCycleLength || 28 }} 天</em>
        </div>
      </div>

      <div class="prediction" v-if="summary?.nextPeriodDate">
        <div>
          <p class="pred-label">下次预计</p>
          <p class="pred-date">{{ summary.nextPeriodDate }}</p>
        </div>
        <span class="pill">{{ summary.daysToNextPeriod }} 天后</span>
      </div>
    </div>

    <div class="actions">
      <van-button round block color="#7a9b76" @click="openForm">记录这次月经</van-button>
    </div>

    <div class="section" v-if="summary?.predictions?.length">
      <div class="section-title"><h3>接下来三次</h3></div>
      <div class="pred-list">
        <div v-for="p in summary.predictions" :key="p.startDate" class="pred-item">
          <span>{{ p.startDate }}</span>
          <em>预计 {{ p.endDate }}</em>
        </div>
      </div>
    </div>

    <div class="section">
      <div class="section-title">
        <h3>历史记录</h3>
        <span class="more">{{ summary?.records?.length || 0 }} 次</span>
      </div>

      <div v-if="!summary?.records?.length" class="card empty">
        <p>还没有记录。</p>
        <p class="muted">第一次只需要填开始日期，之后周期会越来越准。</p>
      </div>

      <van-swipe-cell v-for="r in summary?.records" :key="r.id" class="record-wrap">
        <div class="record">
          <div class="record-left">
            <p class="record-date">{{ r.startDate }}<span v-if="r.endDate"> → {{ r.endDate }}</span></p>
            <p class="record-meta">
              {{ r.flowLabel }}<template v-if="r.periodDays"> · {{ r.periodDays }} 天</template>
            </p>
            <div class="tags" v-if="r.symptoms?.length">
              <span v-for="s in r.symptoms" :key="s" class="tag">{{ s }}</span>
            </div>
            <p v-if="r.note" class="record-note">{{ r.note }}</p>
          </div>
          <span class="mood" v-if="r.mood">{{ moodLabel(r.mood) }}</span>
        </div>
        <template #right>
          <van-button square type="danger" text="删除" class="del" @click="remove(r.id)" />
        </template>
      </van-swipe-cell>
    </div>

    <p class="disclaimer">{{ summary?.disclaimer || '预测基于历史记录，仅供参考。' }}</p>

    <!-- 记录表单 -->
    <van-popup v-model:show="showForm" position="bottom" round :style="{ paddingBottom: '24px' }">
      <div class="form">
        <div class="form-head">
          <h3>记录一次月经</h3>
          <van-icon name="cross" @click="showForm = false" />
        </div>

        <van-field v-model="form.startDate" label="开始日期" type="date" :max="today" />
        <van-field v-model="form.endDate" label="结束日期" type="date" :min="form.startDate" :max="today" placeholder="还没结束可以留空" />
        <van-field label="经量">
          <template #input>
            <van-radio-group v-model="form.flow" direction="horizontal">
              <van-radio name="LIGHT">少量</van-radio>
              <van-radio name="NORMAL">中等</van-radio>
              <van-radio name="HEAVY">较多</van-radio>
            </van-radio-group>
          </template>
        </van-field>
        <van-field label="心情">
          <template #input>
            <van-radio-group v-model="form.mood" direction="horizontal">
              <van-radio v-for="m in moods" :key="m.key" :name="m.key">{{ m.label }}</van-radio>
            </van-radio-group>
          </template>
        </van-field>

        <div class="form-block">
          <p class="form-label">身体感受</p>
          <van-checkbox-group v-model="form.symptoms" direction="horizontal" class="symptoms">
            <van-checkbox v-for="s in symptomOptions" :key="s" :name="s" shape="square">{{ s }}</van-checkbox>
          </van-checkbox-group>
        </div>

        <van-field
          v-model="form.note"
          rows="2"
          autosize
          type="textarea"
          maxlength="300"
          show-word-limit
          label="备注"
          placeholder="想记什么都可以，比如今天特别累"
        />

        <van-button round block color="#7a9b76" :loading="saving" class="save" @click="save">保存</van-button>
      </div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { showConfirmDialog, showFailToast, showSuccessToast } from 'vant'
import { del, get, post } from '../api/request'

interface RecordVO {
  id: number
  startDate: string
  endDate?: string
  periodDays?: number
  flow: string
  flowLabel: string
  symptoms: string[]
  mood?: string
  note?: string
}

interface Summary {
  cycleDay?: number
  phaseLabel?: string
  phaseHint?: string
  avgCycleLength?: number
  avgPeriodLength?: number
  nextPeriodDate?: string
  daysToNextPeriod?: number
  predictions: { startDate: string; endDate: string }[]
  records: RecordVO[]
  disclaimer: string
}

const today = new Date().toISOString().slice(0, 10)
const summary = ref<Summary | null>(null)
const showForm = ref(false)
const saving = ref(false)

const moods = [
  { key: 'CALM', label: '平静' },
  { key: 'LOW', label: '低落' },
  { key: 'IRRITABLE', label: '烦躁' },
  { key: 'ANXIOUS', label: '焦虑' },
]
const symptomOptions = ['痛经', '腰酸', '乳房胀', '情绪低落', '头痛', '水肿', '长痘', '失眠']

const form = reactive({
  startDate: today,
  endDate: '',
  flow: 'NORMAL',
  mood: 'CALM',
  symptoms: [] as string[],
  note: '',
})

function openForm() {
  form.startDate = today
  form.endDate = ''
  form.flow = 'NORMAL'
  form.mood = 'CALM'
  form.symptoms = []
  form.note = ''
  showForm.value = true
}

function moodLabel(mood: string) {
  return moods.find((m) => m.key === mood)?.label || ''
}

async function load() {
  summary.value = await get<Summary>('/wellness/cycles/summary')
}

async function save() {
  if (!form.startDate) {
    showFailToast('请选择开始日期')
    return
  }
  saving.value = true
  try {
    await post('/wellness/cycles', {
      startDate: form.startDate,
      endDate: form.endDate || undefined,
      flow: form.flow,
      mood: form.mood,
      symptoms: form.symptoms,
      note: form.note || undefined,
    })
    showForm.value = false
    showSuccessToast('已记录')
    await load()
  } finally {
    saving.value = false
  }
}

async function remove(id: number) {
  await showConfirmDialog({ title: '删除这条记录？', message: '删除后预测会重新计算。' })
  await del(`/wellness/cycles/${id}`)
  showSuccessToast('已删除')
  await load()
}

onMounted(load)
</script>

<style scoped>
.summary {
  margin: 12px 16px 0;
  padding: 22px;
  border-radius: 22px;
  background: linear-gradient(145deg, #f3f8f0, #e7f0e3);
}

.summary-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.eyebrow {
  margin: 0;
  font-size: 11px;
  color: #7f8f7a;
  letter-spacing: 1px;
}

.day {
  margin: 6px 0 8px;
  font-size: 16px;
  color: var(--yuji-text);
}

.day strong {
  font-size: 36px;
  color: var(--yuji-primary-deep);
  line-height: 1;
}

.phase {
  margin: 0;
  font-size: 12px;
  color: #66745f;
  line-height: 1.6;
  max-width: 210px;
}

.ring {
  width: 84px;
  height: 84px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid #d7e5d2;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ring span {
  font-size: 24px;
  font-weight: 700;
  color: var(--yuji-primary-deep);
  line-height: 1;
}

.ring em {
  font-style: normal;
  font-size: 10px;
  color: var(--yuji-muted);
  margin-top: 4px;
}

.prediction {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px dashed #d3e0ce;
}

.pred-label {
  margin: 0;
  font-size: 11px;
  color: var(--yuji-muted);
}

.pred-date {
  margin: 4px 0 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--yuji-primary-deep);
}

.actions {
  margin: 16px;
}

.pred-list {
  margin: 0 16px;
  display: flex;
  gap: 10px;
}

.pred-item {
  flex: 1;
  background: #fff;
  border-radius: 14px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
  color: var(--yuji-text);
  box-shadow: 0 4px 12px rgba(78, 96, 73, 0.05);
}

.pred-item em {
  font-style: normal;
  font-size: 10px;
  color: var(--yuji-muted);
}

.record-wrap {
  margin: 10px 16px;
  border-radius: 16px;
  overflow: hidden;
  background: #fff;
}

.record {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  padding: 16px;
}

.record-date {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--yuji-ink);
}

.record-date span {
  font-weight: 400;
  color: var(--yuji-muted);
  font-size: 12px;
}

.record-meta {
  margin: 5px 0 0;
  font-size: 11px;
  color: var(--yuji-muted);
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.tag {
  font-size: 10px;
  padding: 3px 8px;
  border-radius: 999px;
  background: #f4f6f2;
  color: #6f7a6c;
}

.record-note {
  margin: 8px 0 0;
  font-size: 11px;
  color: #9a9a9a;
  line-height: 1.6;
}

.mood {
  font-size: 11px;
  color: var(--yuji-primary);
  flex-shrink: 0;
}

.del {
  height: 100%;
}

.empty {
  text-align: center;
  padding: 26px 18px;
}

.empty p {
  margin: 0 0 6px;
  font-size: 14px;
}

.disclaimer {
  margin: 18px 24px 0;
  font-size: 11px;
  color: #b3b3b3;
  line-height: 1.7;
  text-align: center;
}

.form {
  padding: 20px 16px 8px;
}

.form-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.form-head h3 {
  margin: 0;
  font-size: 16px;
}

.form-block {
  padding: 12px 16px;
}

.form-label {
  margin: 0 0 8px;
  font-size: 13px;
  color: #646566;
}

.symptoms {
  row-gap: 10px;
}

.save {
  margin-top: 18px;
}
</style>
