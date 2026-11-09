<template>
  <div>
    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="会员 = 向本机构提交过预约意向并绑定平台账号的用户。"
      description="列表只展示基础资料、线索状态与风险分级摘要，不开放健康指标明细。点击「详情」查看该会员的线索与风险跟进记录。"
      class="notice"
    />

    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索昵称 / 手机号"
        clearable
        style="width: 240px"
        @clear="load"
        @keyup.enter="load"
      />
      <el-button type="primary" plain @click="load">搜索</el-button>
      <span class="count">共 {{ members.length }} 位会员</span>
    </div>

    <el-table :data="members" v-loading="loading" stripe>
      <el-table-column prop="nickname" label="会员" min-width="140" />
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column label="预约意向" width="100" align="center">
        <template #default="{ row }">{{ row.leadCount }} 次</template>
      </el-table-column>
      <el-table-column label="最新线索状态" width="120">
        <template #default="{ row }">
          <el-tag size="small" :type="leadType(row.lastLeadStatus)" effect="plain">
            {{ row.lastLeadStatusLabel }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="风险分级" width="110">
        <template #default="{ row }">
          <el-tag v-if="row.lastTicketLevel" :type="row.lastTicketLevel === 'RED' ? 'danger' : 'warning'" size="small">
            {{ row.lastTicketLevelLabel }}
          </el-tag>
          <span v-else class="dim">无记录</span>
        </template>
      </el-table-column>
      <el-table-column label="最近风险摘要" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">{{ row.lastTicketSummary || '—' }}</template>
      </el-table-column>
      <el-table-column prop="joinedText" label="成为会员" width="130" />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-drawer v-model="drawer" :title="current ? `会员详情 · ${current.nickname}` : '会员详情'" size="520px">
      <template v-if="detail">
        <div class="section">
          <p class="section-title">会员资料</p>
          <div class="info-grid">
            <div><span class="label">昵称</span>{{ detail.member.nickname }}</div>
            <div><span class="label">手机号</span>{{ detail.member.phone }}</div>
            <div><span class="label">成为会员</span>{{ detail.member.joinedText }}</div>
            <div>
              <span class="label">风险分级</span>
              <el-tag v-if="detail.member.lastTicketLevel" :type="detail.member.lastTicketLevel === 'RED' ? 'danger' : 'warning'" size="small">
                {{ detail.member.lastTicketLevelLabel }}
              </el-tag>
              <span v-else>暂无记录</span>
            </div>
          </div>
        </div>

        <div class="section">
          <p class="section-title">预约线索（{{ detail.leads.length }}）</p>
          <el-empty v-if="!detail.leads.length" description="暂无线索" :image-size="60" />
          <div v-for="lead in detail.leads" :key="lead.id" class="lead-card">
            <div class="lead-head">
              <el-tag size="small" :type="leadType(lead.status)" effect="plain">{{ lead.statusLabel }}</el-tag>
              <span class="dim">{{ lead.timeText }}</span>
            </div>
            <p class="lead-need">{{ lead.need }}</p>
            <p v-if="lead.preferredDate" class="dim">期望日期：{{ lead.preferredDate }}</p>
            <p v-if="lead.note" class="dim">备注：{{ lead.note }}</p>
          </div>
        </div>

        <div class="section">
          <p class="section-title">风险跟进记录（{{ detail.tickets.length }}）</p>
          <el-alert
            type="warning"
            :closable="false"
            show-icon
            title="只展示风险摘要与分级，不展示咨询与打卡全文。"
            style="margin-bottom: 10px"
          />
          <el-empty v-if="!detail.tickets.length" description="暂无风险记录" :image-size="60" />
          <div v-for="t in detail.tickets" :key="t.id" class="lead-card">
            <div class="lead-head">
              <el-tag size="small" :type="t.level === 'RED' ? 'danger' : 'warning'">{{ t.levelLabel }}</el-tag>
              <span class="dim">{{ sourceLabel(t.source) }} · {{ t.timeText }}</span>
            </div>
            <p class="lead-need">{{ t.summary }}</p>
            <p class="dim">状态：{{ t.statusLabel }}<template v-if="t.handlerNote"> · 备注：{{ t.handlerNote }}</template></p>
          </div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getData } from '../api'

interface MemberVO {
  userId: number
  nickname: string
  phone: string
  leadCount: number
  lastLeadStatus: string
  lastLeadStatusLabel: string
  lastTicketLevel?: string
  lastTicketLevelLabel?: string
  lastTicketSummary?: string
  joinedText: string
}

interface LeadVO {
  id: number
  statusLabel: string
  status: string
  need: string
  preferredDate?: string
  note?: string
  timeText: string
}

interface TicketVO {
  id: number
  source: string
  level: string
  levelLabel: string
  summary: string
  statusLabel: string
  handlerNote?: string
  timeText: string
}

interface MemberDetailVO {
  member: MemberVO
  leads: LeadVO[]
  tickets: TicketVO[]
}

const members = ref<MemberVO[]>([])
const loading = ref(false)
const drawer = ref(false)
const detail = ref<MemberDetailVO | null>(null)
const current = ref<MemberVO | null>(null)
const keyword = ref('')

function leadType(status: string) {
  return { NEW: 'warning', CONTACTED: 'primary', BOOKED: 'success', CLOSED: 'info' }[status] || 'info'
}

function sourceLabel(source: string) {
  return { EPDS: 'EPDS 筛查', CONSULT: '私密咨询', EMOTION: '情绪打卡', COMMUNITY: '社区风控' }[source] || source
}

async function load() {
  loading.value = true
  try {
    members.value = await getData<MemberVO[]>('/admin/wellness/members', {
      keyword: keyword.value || undefined,
    })
  } finally {
    loading.value = false
  }
}

async function openDetail(row: MemberVO) {
  current.value = row
  detail.value = await getData<MemberDetailVO>(`/admin/wellness/members/${row.userId}`)
  drawer.value = true
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
  gap: 10px;
  align-items: center;
  margin-bottom: 16px;
}
.count {
  font-size: 12px;
  color: #b3b3b3;
  margin-left: auto;
}
.dim {
  font-size: 11px;
  color: #b3b3b3;
}
.section {
  margin-bottom: 22px;
}
.section-title {
  margin: 0 0 10px;
  font-size: 13px;
  font-weight: 600;
  color: #4a6b47;
}
.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  font-size: 13px;
  color: #606266;
}
.info-grid .label {
  color: #b3b3b3;
  font-size: 11px;
  margin-right: 8px;
}
.lead-card {
  border: 1px solid #eef0ee;
  border-radius: 10px;
  padding: 10px 14px;
  margin-bottom: 10px;
}
.lead-head {
  display: flex;
  align-items: center;
  gap: 8px;
}
.lead-need {
  margin: 8px 0 4px;
  font-size: 13px;
  line-height: 1.6;
}
</style>
