<template>
  <div>
    <div class="toolbar">
      <div class="filters">
        <el-input v-model="keyword" placeholder="搜索机构名称 / 城市" clearable style="width: 240px" />
        <el-select v-model="typeFilter" placeholder="机构类型" clearable style="width: 160px">
          <el-option v-for="t in types" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
      </div>
      <el-button type="primary" @click="openCreate">新增合作机构</el-button>
    </div>

    <el-table :data="filtered" v-loading="loading" stripe style="width: 100%">
      <el-table-column prop="name" label="机构" min-width="200">
        <template #default="{ row }">
          <div class="org-cell">
            <p class="name">{{ row.name }}</p>
            <p class="sub">{{ row.city }}{{ row.district || '' }} · {{ row.address || '未填地址' }}</p>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="130">
        <template #default="{ row }">
          <el-tag size="small" effect="plain">{{ row.typeLabel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="服务 / 线索" width="130">
        <template #default="{ row }">
          {{ row.serviceCount }} 项 / {{ row.leadCount }} 条
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="intro" label="简介" min-width="260" show-overflow-tooltip />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row.id)">编辑</el-button>
          <el-button v-if="row.status === 'ACTIVE'" link type="warning" @click="pause(row)">暂停</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="form.id ? '编辑合作机构' : '新增合作机构'" width="640px">
      <el-form :model="form" label-width="92px">
        <el-form-item label="机构名称">
          <el-input v-model="form.name" placeholder="如 杭州市妇幼保健院" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="机构类型">
              <el-select v-model="form.type" style="width: 100%">
                <el-option v-for="t in types" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="合作状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="合作中" value="ACTIVE" />
                <el-option label="已暂停" value="PAUSED" />
                <el-option label="待审核" value="PENDING" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="城市">
              <el-input v-model="form.city" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="区县">
              <el-input v-model="form.district" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="地址">
          <el-input v-model="form.address" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="联系人">
              <el-input v-model="form.contactName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="form.contactPhone" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="简介">
          <el-input v-model="form.intro" type="textarea" :rows="3" maxlength="500" show-word-limit />
        </el-form-item>

        <el-divider content-position="left">服务包</el-divider>
        <div v-for="(s, i) in form.services" :key="i" class="service-row">
          <el-input v-model="s.name" placeholder="服务名称" style="width: 34%" />
          <el-input v-model="s.priceText" placeholder="价格说明" style="width: 26%" />
          <el-input v-model="s.description" placeholder="说明" style="width: 32%" />
          <el-button link type="danger" @click="form.services.splice(i, 1)">删除</el-button>
        </div>
        <el-button link type="primary" @click="addService">+ 添加服务</el-button>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { delData, getData, postData, putData } from '../api'

interface OrgVO {
  id: number
  name: string
  type: string
  typeLabel: string
  city: string
  district?: string
  address?: string
  contactName?: string
  contactPhone?: string
  intro?: string
  status: string
  serviceCount: number
  leadCount: number
}

interface ServiceForm {
  name: string
  priceText: string
  description: string
}

interface OrgForm {
  id?: number
  name: string
  type: string
  city: string
  district: string
  address: string
  contactName: string
  contactPhone: string
  intro: string
  status: string
  services: ServiceForm[]
}

const types = [
  { value: 'HOSPITAL', label: '妇幼保健院' },
  { value: 'MATERNITY', label: '月子中心' },
  { value: 'POSTPARTUM', label: '产后康复' },
  { value: 'COMMUNITY', label: '社区机构' },
]

const orgs = ref<OrgVO[]>([])
const loading = ref(false)
const saving = ref(false)
const dialog = ref(false)
const keyword = ref('')
const typeFilter = ref('')

const form = reactive<OrgForm>({
  name: '',
  type: 'HOSPITAL',
  city: '杭州',
  district: '',
  address: '',
  contactName: '',
  contactPhone: '',
  intro: '',
  status: 'ACTIVE',
  services: [],
})

const filtered = computed(() =>
  orgs.value.filter((o) => {
    const kw = keyword.value.trim()
    const matchKw = !kw || o.name.includes(kw) || o.city.includes(kw)
    const matchType = !typeFilter.value || o.type === typeFilter.value
    return matchKw && matchType
  }),
)

function statusLabel(status: string) {
  return { ACTIVE: '合作中', PAUSED: '已暂停', PENDING: '待审核' }[status] || status
}

function statusType(status: string) {
  return status === 'ACTIVE' ? 'success' : status === 'PENDING' ? 'warning' : 'info'
}

async function load() {
  loading.value = true
  try {
    orgs.value = await getData<OrgVO[]>('/admin/wellness/partners')
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    id: undefined,
    name: '',
    type: 'HOSPITAL',
    city: '杭州',
    district: '',
    address: '',
    contactName: '',
    contactPhone: '',
    intro: '',
    status: 'ACTIVE',
    services: [],
  })
}

function openCreate() {
  resetForm()
  form.services = [{ name: '', priceText: '', description: '' }]
  dialog.value = true
}

async function openEdit(id: number) {
  const detail = await getData<{ org: OrgVO; services: ServiceForm[] }>(
    `/admin/wellness/partners/${id}`,
  )
  resetForm()
  const o = detail.org
  Object.assign(form, {
    id: o.id,
    name: o.name,
    type: o.type,
    city: o.city,
    district: o.district || '',
    address: o.address || '',
    contactName: o.contactName || '',
    contactPhone: o.contactPhone || '',
    intro: o.intro || '',
    status: o.status,
    services: detail.services.map((s) => ({
      name: s.name,
      priceText: s.priceText || '',
      description: s.description || '',
    })),
  })
  dialog.value = true
}

function addService() {
  form.services.push({ name: '', priceText: '', description: '' })
}

async function save() {
  if (!form.name.trim()) {
    ElMessage.warning('请填写机构名称')
    return
  }
  saving.value = true
  try {
    const payload = {
      name: form.name,
      type: form.type,
      city: form.city,
      district: form.district || undefined,
      address: form.address || undefined,
      contactName: form.contactName || undefined,
      contactPhone: form.contactPhone || undefined,
      intro: form.intro || undefined,
      status: form.status,
      services: form.services
        .filter((s) => s.name.trim())
        .map((s) => ({
          name: s.name,
          priceText: s.priceText || undefined,
          description: s.description || undefined,
        })),
    }
    if (form.id) {
      await putData(`/admin/wellness/partners/${form.id}`, payload)
    } else {
      await postData('/admin/wellness/partners', payload)
    }
    ElMessage.success('已保存')
    dialog.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function pause(row: OrgVO) {
  await ElMessageBox.confirm(`暂停后「${row.name}」将不再出现在用户端。`, '确认暂停', {
    type: 'warning',
  })
  await delData(`/admin/wellness/partners/${row.id}`)
  ElMessage.success('已暂停')
  await load()
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 12px;
}
.filters {
  display: flex;
  gap: 10px;
}
.org-cell .name {
  margin: 0 0 4px;
  font-weight: 500;
  color: #303133;
}
.org-cell .sub {
  margin: 0;
  font-size: 11px;
  color: #b3b3b3;
}
.service-row {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 10px;
}
</style>
