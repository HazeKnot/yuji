<template>
  <div>
    <el-alert
      type="warning"
      :closable="false"
      show-icon
      title="AI 风控引擎自动扫描社区帖子与情绪打卡。"
      description="危机表达 → 红档告警（内容已进待审核，建议 24 小时内联系）；消极情绪 → 黄档预警（内容正常发布，建议安排陪伴随访）。只展示风险摘要，不展示内容全文。"
      class="notice"
    />

    <div class="toolbar">
      <div class="filters">
        <el-select v-model="sourceFilter" placeholder="全部来源" clearable style="width: 150px" @change="load">
          <el-option label="社区风控" value="COMMUNITY" />
          <el-option label="情绪打卡" value="EMOTION" />
        </el-select>
        <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 140px" @change="load">
          <el-option label="待跟进" value="OPEN" />
          <el-option label="跟进中" value="FOLLOWING" />
          <el-option label="已关闭" value="CLOSED" />
        </el-select>
        <el-select v-model="levelFilter" placeholder="全部等级" clearable style="width: 130px" @change="load">
          <el-option label="红档" value="RED" />
          <el-option label="黄档" value="YELLOW" />
        </el-select>
      </div>
      <span class="count">待跟进 {{ openCount }} 个 · 红档 {{ redCount }} 个</span>
    </div>

    <el-table :data="filtered" v-loading="loading" stripe>
      <el-table-column label="等级" width="90">
        <template #default="{ row }">
          <el-tag :type="row.level === 'RED' ? 'danger' : 'warning'" size="small">{{ row.levelLabel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="来源" width="120">
        <template #default="{ row }">{{ sourceLabel(row.source) }}</template>
      </el-table-column>
      <el-table-column prop="summary" label="告警摘要" min-width="280" show-overflow-tooltip />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small" effect="plain">{{ row.statusLabel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="跟进备注" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">{{ row.handlerNote || '—' }}</template>
      </el-table-column>
      <el-table-column prop="timeText" label="时间" width="130" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="open(row)">跟进</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" title="AI 风控告警跟进" width="500px">
      <div v-if="current" class="ticket-info">
        <div class="row">
          <el-tag :type="current.level === 'RED' ? 'danger' : 'warning'" size="small">{{ current.levelLabel }}</el-tag>
          <span class="sub">{{ sourceLabel(current.source) }} · {{ current.timeText }}</span>
        </div>
        <p class="summary">{{ current.summary }}</p>
      </div>

      <el-form label-width="84px">
        <el-form-item label="处理状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="待跟进" value="OPEN" />
            <el-option label="跟进中" value="FOLLOWING" />
            <el-option label="已关闭" value="CLOSED" />
          </el-select>
        </el-form-item>
        <el-form-item label="跟进记录">
          <el-input
            v-model="form.handlerNote"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="记录联系与陪伴安排。不要记录不必要的隐私细节。"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getData, putData } from '../api'

interface TicketVO {
  id: number
  source: string
  level: string
  levelLabel: string
  summary: string
  status: string
  statusLabel: string
  handlerNote?: string
  timeText: string
}

const tickets = ref<TicketVO[]>([])
const loading = ref(false)
const saving = ref(false)
const dialog = ref(false)
const sourceFilter = ref('')
const statusFilter = ref('')
const levelFilter = ref('')
const current = ref<TicketVO | null>(null)
const form = reactive({ status: 'OPEN', handlerNote: '' })

const filtered = computed(() =>
  sourceFilter.value ? tickets.value.filter((t) => t.source === sourceFilter.value) : tickets.value,
)
const openCount = computed(() => filtered.value.filter((t) => t.status !== 'CLOSED').length)
const redCount = computed(() => filtered.value.filter((t) => t.level === 'RED' && t.status !== 'CLOSED').length)

function sourceLabel(source: string) {
  return { COMMUNITY: '社区风控', EMOTION: '情绪打卡' }[source] || source
}

function statusType(status: string) {
  return { OPEN: 'warning', FOLLOWING: 'primary', CLOSED: 'info' }[status] || 'info'
}

async function load() {
  loading.value = true
  try {
    tickets.value = await getData<TicketVO[]>('/admin/wellness/alerts', {
      status: statusFilter.value || undefined,
      level: levelFilter.value || undefined,
    })
  } finally {
    loading.value = false
  }
}

function open(row: TicketVO) {
  current.value = row
  form.status = row.status
  form.handlerNote = row.handlerNote || ''
  dialog.value = true
}

async function save() {
  if (!current.value) return
  saving.value = true
  try {
    await putData(`/admin/wellness/tickets/${current.value.id}`, {
      status: form.status,
      handlerNote: form.handlerNote || undefined,
    })
    ElMessage.success('已更新')
    dialog.value = false
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.notice {
  margin-bottom: 16px;
  border-radius: 10px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.filters {
  display: flex;
  gap: 10px;
}
.count {
  font-size: 12px;
  color: #b3b3b3;
}
.ticket-info {
  padding: 12px 14px;
  border-radius: 10px;
  background: #faf7f7;
  margin-bottom: 16px;
}
.ticket-info .row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.summary {
  margin: 10px 0 0;
  font-size: 13px;
  line-height: 1.7;
}
.sub {
  font-size: 11px;
  color: #b3b3b3;
}
</style>
