<template>
  <div>
    <div class="toolbar">
      <div class="filters">
        <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 150px" @change="load">
          <el-option v-for="s in statuses" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-select v-model="orgFilter" placeholder="全部机构" clearable filterable style="width: 220px" @change="load">
          <el-option v-for="o in orgs" :key="o.id" :label="o.name" :value="o.id" />
        </el-select>
      </div>
      <span class="count">共 {{ leads.length }} 条，待联系 {{ newCount }} 条</span>
    </div>

    <el-table :data="leads" v-loading="loading" stripe>
      <el-table-column label="用户" width="130">
        <template #default="{ row }">
          <p class="name">{{ row.name }}</p>
          <p class="sub">{{ row.phone }}</p>
        </template>
      </el-table-column>
      <el-table-column prop="orgName" label="机构" min-width="180" />
      <el-table-column prop="need" label="咨询需求" min-width="220" show-overflow-tooltip />
      <el-table-column label="期望日期" width="120">
        <template #default="{ row }">{{ row.preferredDate || '未指定' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ row.statusLabel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="timeText" label="提交时间" width="120" />
      <el-table-column label="备注" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">{{ row.note || '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="open(row)">跟进</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" title="跟进预约线索" width="480px">
      <div v-if="current" class="lead-info">
        <p><strong>{{ current.name }}</strong> · {{ current.phone }}</p>
        <p class="sub">{{ current.orgName }} · {{ current.need }}</p>
        <p class="sub" v-if="current.preferredDate">期望日期：{{ current.preferredDate }}</p>
      </div>
      <el-form label-width="80px">
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="s in statuses" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="跟进备注">
          <el-input v-model="form.note" type="textarea" :rows="3" maxlength="300" show-word-limit />
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

interface LeadVO {
  id: number
  orgId: number
  orgName: string
  name: string
  phone: string
  need: string
  preferredDate?: string
  status: string
  statusLabel: string
  note?: string
  timeText: string
}

interface OrgVO {
  id: number
  name: string
}

const statuses = [
  { value: 'NEW', label: '待联系' },
  { value: 'CONTACTED', label: '已联系' },
  { value: 'BOOKED', label: '已预约' },
  { value: 'CLOSED', label: '已关闭' },
]

const leads = ref<LeadVO[]>([])
const orgs = ref<OrgVO[]>([])
const loading = ref(false)
const saving = ref(false)
const dialog = ref(false)
const statusFilter = ref('')
const orgFilter = ref<number | ''>('')
const current = ref<LeadVO | null>(null)
const form = reactive({ status: 'NEW', note: '' })

const newCount = computed(() => leads.value.filter((l) => l.status === 'NEW').length)

function statusType(status: string) {
  return { NEW: 'warning', CONTACTED: 'primary', BOOKED: 'success', CLOSED: 'info' }[status] || 'info'
}

async function load() {
  loading.value = true
  try {
    leads.value = await getData<LeadVO[]>('/admin/wellness/leads', {
      status: statusFilter.value || undefined,
      orgId: orgFilter.value || undefined,
    })
  } finally {
    loading.value = false
  }
}

function open(row: LeadVO) {
  current.value = row
  form.status = row.status
  form.note = row.note || ''
  dialog.value = true
}

async function save() {
  if (!current.value) return
  saving.value = true
  try {
    await putData(`/admin/wellness/leads/${current.value.id}`, {
      status: form.status,
      note: form.note || undefined,
    })
    ElMessage.success('已更新')
    dialog.value = false
    await load()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  orgs.value = await getData<OrgVO[]>('/admin/wellness/partners').catch(() => [])
  await load()
})
</script>

<style scoped>
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
.name {
  margin: 0 0 3px;
  font-weight: 500;
}
.sub {
  margin: 0;
  font-size: 11px;
  color: #b3b3b3;
  line-height: 1.7;
}
.lead-info {
  padding: 12px 14px;
  border-radius: 10px;
  background: #f7f9f6;
  margin-bottom: 16px;
}
.lead-info p {
  margin: 0 0 4px;
  font-size: 13px;
}
</style>
