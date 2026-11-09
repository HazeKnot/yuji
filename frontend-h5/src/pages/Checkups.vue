<template>
  <div class="checkups">
    <van-nav-bar title="产检日历" left-arrow @click-left="$router.back()" />

    <van-cell-group inset>
      <van-swipe-cell v-for="c in checkups" :key="c.id">
        <van-cell
          :title="c.name"
          :label="`${c.windowText || ''}${c.dueDate ? ' · 建议 ' + c.dueDate : ''}`"
          :value="statusText(c.status)"
          :class="{ overdue: c.overdue }"
          is-link
          @click="onClick(c)"
        />
        <template #right>
          <van-button square type="primary" text="完成" class="btn" @click="mark(c, 'DONE')" />
          <van-button square type="default" text="跳过" class="btn" @click="mark(c, 'SKIPPED')" />
        </template>
      </van-swipe-cell>
      <van-cell v-if="checkups.length === 0" title="暂无产检计划" />
    </van-cell-group>

    <p class="note">
      以上时间表依据《孕前和孕期保健指南（2018）》整理，仅供参考提醒，<br />
      具体检查项目与时间请以你的产检医院安排为准。
    </p>

    <van-action-sheet
      v-model:show="showSheet"
      :actions="sheetActions"
      cancel-text="取消"
      @select="onSheetSelect"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast } from 'vant'
import { api } from '../api'

const router = useRouter()

interface CheckupVO {
  id: number
  code: string
  name: string
  suggestedWeek: number
  windowText: string
  dueDate: string
  status: string
  overdue: boolean
}

const checkups = ref<CheckupVO[]>([])
const showSheet = ref(false)
const current = ref<CheckupVO | null>(null)

const sheetActions = [
  { name: '查看 / 上传报告', value: 'REPORT' },
  { name: '已完成', value: 'DONE' },
  { name: '这次先跳过', value: 'SKIPPED' },
  { name: '恢复待办', value: 'PLANNED' },
]

function statusText(status: string) {
  return status === 'DONE' ? '已完成' : status === 'SKIPPED' ? '已跳过' : '待办'
}

function onClick(c: CheckupVO) {
  current.value = c
  showSheet.value = true
}

function onSheetSelect(action: any) {
  showSheet.value = false
  if (!current.value) return
  if (action.value === 'REPORT') {
    router.push({
      path: '/reports',
      query: { checkupId: String(current.value.id), name: current.value.name },
    })
    return
  }
  mark(current.value, action.value)
}

async function mark(c: CheckupVO, status: string) {
  await api.put(`/checkups/${c.id}`, { status })
  c.status = status
  c.overdue = false
  showSuccessToast('已更新')
}

onMounted(async () => {
  const res = await api.get<never, { data: { data: CheckupVO[] } }>('/checkups')
  checkups.value = res.data.data
})
</script>

<style scoped>
.checkups {
  padding-bottom: 80px;
  min-height: 100vh;
}
.overdue :deep(.van-cell__value) {
  color: #ee7d7d;
}
.btn {
  height: 100%;
}
.note {
  text-align: center;
  color: #b8b8b8;
  font-size: 12px;
  line-height: 1.8;
  margin-top: 20px;
  padding: 0 24px;
}
</style>
