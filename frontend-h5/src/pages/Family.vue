<template>
  <div class="family">
    <van-nav-bar title="家人" left-arrow @click-left="$router.back()" />

    <!-- 还没加入任何家庭：两条入口 -->
    <template v-if="role === 'NONE'">
      <div class="hero-card">
        <p class="hero-title">这段路，不必一个人走</p>
        <p class="hero-sub">家属只能看到你允许他看到的部分，情绪记录与量表永远不会自动共享。</p>
      </div>

      <div class="card">
        <p class="card-title">我是妈妈</p>
        <p class="hint">生成一个 6 位邀请码，24 小时内有效、只能用一次</p>
        <van-button round block type="primary" :loading="creating" @click="createInvite">
          生成邀请码
        </van-button>
        <div v-if="invite" class="invite-box">
          <p class="invite-code">{{ invite.code }}</p>
          <p class="invite-hint">{{ invite.hint }}</p>
          <van-button size="small" round plain type="primary" @click="copyCode">复制</van-button>
        </div>
      </div>

      <div class="card">
        <p class="card-title">我是家属</p>
        <van-field v-model="code" placeholder="输入 6 位邀请码" maxlength="6" center clearable />
        <div class="save">
          <van-button round block plain type="primary" :loading="joining" @click="join">加入家庭</van-button>
        </div>
      </div>
    </template>

    <!-- 妈妈视图 -->
    <template v-else-if="role === 'OWNER'">
      <div class="card">
        <div class="card-head">
          <p class="card-title">邀请家人</p>
          <van-tag plain type="primary">{{ family?.memberCount ?? 0 }} 位已加入</van-tag>
        </div>
        <div v-if="invite" class="invite-box">
          <p class="invite-code">{{ invite.code }}</p>
          <p class="invite-hint">{{ invite.hint }}</p>
          <van-button size="small" round plain type="primary" @click="copyCode">复制</van-button>
        </div>
        <van-button v-else round block plain type="primary" :loading="creating" @click="createInvite">
          生成新邀请码
        </van-button>
        <p class="hint">邀请码过期或已使用后，重新生成即可</p>
      </div>

      <div class="card">
        <p class="card-title">家人与可见范围</p>
        <p class="hint">默认只能看到任务和状态摘要；勾选后才会看到对应指标</p>
        <van-cell-group :border="false">
          <van-cell
            v-for="m in members"
            :key="m.id"
            :title="m.nickname"
            :label="`${m.relation} · ${m.visibleFields.length ? '已授权 ' + m.visibleFields.length + ' 项指标' : '未授权任何指标'}`"
            is-link
            @click="openPerm(m)"
          />
          <van-cell v-if="!members.length" title="还没有家人加入" label="把上面的邀请码发给他" />
        </van-cell-group>
        <van-cell v-for="m in members" :key="'rm' + m.id" :border="false" class="rm-row">
          <template #title>
            <span class="rm" @click="removeMember(m)">移除 {{ m.nickname }}</span>
          </template>
        </van-cell>
      </div>

      <div class="card">
        <p class="card-title">派个任务包</p>
        <van-radio-group v-model="templateCode">
          <van-cell
            v-for="t in templates"
            :key="t.code"
            :title="t.name"
            :label="`${t.stage} · 含 ${t.items.length} 项`"
            clickable
            @click="templateCode = t.code"
          >
            <template #right-icon><van-radio :name="t.code" /></template>
          </van-cell>
        </van-radio-group>
        <div v-if="members.length" class="assignee">
          <p class="sub-title">派给谁</p>
          <van-radio-group v-model="assignee" direction="horizontal">
            <van-radio v-for="m in members" :key="m.userId" :name="m.userId">{{ m.nickname }}</van-radio>
          </van-radio-group>
        </div>
        <p v-else class="hint">先邀请家人加入，再下发任务</p>
        <div class="save">
          <van-button round block type="primary" :disabled="!canAssign" :loading="assigning" @click="assign">
            下发
          </van-button>
        </div>
      </div>

      <div class="card">
        <div class="card-head">
          <p class="card-title">任务进度</p>
          <span class="summary">{{ doneCount }}/{{ tasks.length }} 已完成</span>
        </div>
        <van-cell-group :border="false">
          <van-cell v-for="t in tasks" :key="t.id" :title="t.title" :label="`${t.assigneeNickname} · ${t.dueDate || ''}`">
            <template #value>
              <van-tag :type="tagType(t)" plain>{{ statusText(t) }}</van-tag>
            </template>
          </van-cell>
          <van-cell v-if="!tasks.length" title="还没有任务" label="选一个任务包派给家人" />
        </van-cell-group>
      </div>
    </template>

    <!-- 家属视图 -->
    <template v-else>
      <div class="card">
        <p class="card-title">{{ summary?.ownerNickname || '她' }} 的今天</p>
        <div v-if="summary?.weeks != null" class="week-line">
          <span class="week-num">{{ summary.weeks }}</span>
          <span class="week-plus">+{{ summary.days }}</span>
          <span class="week-unit">周</span>
          <van-tag color="#4a6b47">{{ summary.stage }}</van-tag>
        </div>
        <p v-else class="hint">她还没有建档</p>
        <div class="stats">
          <div class="stat">
            <p class="stat-num">{{ summary?.checkupDone ?? 0 }}/{{ summary?.checkupTotal ?? 0 }}</p>
            <p class="stat-label">产检完成</p>
          </div>
          <div class="stat">
            <p class="stat-num">{{ summary?.taskDone ?? 0 }}/{{ summary?.taskTotal ?? 0 }}</p>
            <p class="stat-label">我的任务</p>
          </div>
        </div>
        <p v-if="summary?.nextCheckupName" class="next">
          下次产检：{{ summary.nextCheckupName }} · {{ summary.nextCheckupDate }}
        </p>
      </div>

      <div class="card">
        <div class="card-head">
          <p class="card-title">我的任务</p>
          <span class="summary">{{ myDoneCount }}/{{ tasks.length }} 已完成</span>
        </div>
        <div v-for="t in tasks" :key="t.id" class="task">
          <div class="task-head">
            <p class="task-title">{{ t.title }}</p>
            <van-tag :type="tagType(t)" plain>{{ statusText(t) }}</van-tag>
          </div>
          <p v-if="t.detail" class="task-detail">{{ t.detail }}</p>
          <p class="task-meta">
            <span v-if="t.dueDate">建议 {{ t.dueDate }} 前完成</span>
            <span v-if="t.overdue" class="overdue">已逾期</span>
          </p>
          <div class="task-actions">
            <van-button
              v-if="t.status === 'PENDING'"
              size="small"
              round
              plain
              type="primary"
              @click="setStatus(t, 'DOING')"
            >
              我来做
            </van-button>
            <van-button
              v-if="t.status !== 'DONE'"
              size="small"
              round
              type="primary"
              @click="setStatus(t, 'DONE')"
            >
              完成打卡
            </van-button>
            <van-button v-else size="small" round plain @click="setStatus(t, 'PENDING')">撤销</van-button>
          </div>
        </div>
        <p v-if="!tasks.length" class="hint">还没有派给你的任务，等她安排就好</p>
      </div>

      <div class="card">
        <p class="card-title">她允许我看到的</p>
        <van-empty v-if="!sharedTypes.length" image="search" description="她还没有授权任何指标" />
        <template v-else>
          <van-tabs
            :active="sharedType"
            shrink
            @update:active="sharedType = $event"
            @change="onSharedTypeChange"
          >
            <van-tab v-for="t in sharedTypes" :key="t" :title="labelOf(t)" :name="t" />
          </van-tabs>

          <div v-show="hasTrend" ref="trendEl" class="chart"></div>
          <p v-if="!hasTrend" class="hint center">近 30 天还没有数据</p>
          <p v-if="trend" class="disclaimer">{{ trend.disclaimer }}</p>

          <van-cell-group :border="false">
            <van-cell
              v-for="r in currentShared"
              :key="r.id"
              :title="`${r.typeLabel} ${r.value}${r.value2 ? '/' + r.value2 : ''} ${r.unit}`"
              :label="(r.measuredAt || '').replace('T', ' ').slice(0, 16)"
            />
            <van-cell v-if="!currentShared.length" title="这段时间还没有记录" />
          </van-cell-group>
        </template>
        <p class="privacy">{{ summary?.privacyNote || '情绪记录与量表得分不对家属开放。' }}</p>
      </div>
    </template>

    <!-- 授权弹层 -->
    <van-popup :show="showPerm" round position="bottom" @update:show="showPerm = $event">
      <div class="perm">
        <p class="perm-title">允许 {{ editing?.nickname }} 看到</p>
        <p class="hint">不勾选 = 只能看到任务与状态摘要</p>
        <van-checkbox-group v-model="pickedFields">
          <van-cell
            v-for="f in shareFields"
            :key="f.code"
            :title="f.label"
            :label="f.unit"
            clickable
            @click="toggle(f.code)"
          >
            <template #right-icon><van-checkbox :name="f.code" @click.stop /></template>
          </van-cell>
        </van-checkbox-group>
        <p class="perm-hint">情绪记录与量表得分不在可授权范围内</p>
        <van-button round block type="primary" :loading="savingPerm" @click="savePerm">保存</van-button>
      </div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { showConfirmDialog, showSuccessToast, showToast } from 'vant'
import { api } from '../api'
import { useTrendChart, type TrendData } from '../composables/useTrendChart'

interface FamilyVO {
  familyId: number | null
  role: 'NONE' | 'OWNER' | 'MEMBER'
  ownerUserId: number | null
  ownerNickname: string
  memberCount: number
}
interface InvitationVO { code: string; expiresAt: string; hint: string }
interface MemberVO {
  id: number
  userId: number
  nickname: string
  relation: string
  visibleFields: string[]
}
interface ShareFieldVO { code: string; label: string; unit: string }
interface TaskTemplateVO { code: string; stage: string; name: string; description: string; items: string[] }
interface TaskVO {
  id: number
  title: string
  detail: string
  dueDate: string
  status: 'PENDING' | 'DOING' | 'DONE'
  assigneeUserId: number
  assigneeNickname: string
  mine: boolean
  overdue: boolean
}
interface SharedSummaryVO {
  ownerNickname: string
  weeks: number | null
  days: number | null
  stage: string | null
  checkupTotal: number
  checkupDone: number
  nextCheckupName: string | null
  nextCheckupDate: string | null
  taskTotal: number
  taskDone: number
  visibleFields: string[]
  privacyNote: string
}
interface RecordVO {
  id: number
  type: string
  typeLabel: string
  value: number
  value2: number | null
  unit: string
  measuredAt: string
}

const role = ref<'NONE' | 'OWNER' | 'MEMBER'>('NONE')
const family = ref<FamilyVO | null>(null)
const invite = ref<InvitationVO | null>(null)
const members = ref<MemberVO[]>([])
const shareFields = ref<ShareFieldVO[]>([])
const templates = ref<TaskTemplateVO[]>([])
const tasks = ref<TaskVO[]>([])
const summary = ref<SharedSummaryVO | null>(null)

const code = ref('')
const creating = ref(false)
const joining = ref(false)
const assigning = ref(false)
const savingPerm = ref(false)
const templateCode = ref('')
const assignee = ref<number | null>(null)

const showPerm = ref(false)
const editing = ref<MemberVO | null>(null)
const pickedFields = ref<string[]>([])

const sharedType = ref('')
const sharedMap = ref<Record<string, RecordVO[]>>({})
const trend = ref<TrendData | null>(null)
const trendEl = ref<HTMLElement>()
const { render: renderTrend, resize: resizeChart } = useTrendChart(trendEl)
const hasTrend = computed(() => !!trend.value?.points?.length)

const doneCount = computed(() => tasks.value.filter((t) => t.status === 'DONE').length)
const myDoneCount = computed(() => tasks.value.filter((t) => t.mine && t.status === 'DONE').length)
const canAssign = computed(() => !!templateCode.value && !!assignee.value && members.value.length > 0)
const sharedTypes = computed(() => summary.value?.visibleFields ?? [])
const currentShared = computed(() => sharedMap.value[sharedType.value] ?? [])

function labelOf(type: string) {
  return shareFields.value.find((f) => f.code === type)?.label || type
}

function statusText(t: TaskVO) {
  if (t.status === 'DONE') return '已完成'
  if (t.status === 'DOING') return '进行中'
  return t.overdue ? '待完成（逾期）' : '待完成'
}

function tagType(t: TaskVO) {
  if (t.status === 'DONE') return 'success'
  if (t.status === 'DOING') return 'primary'
  return t.overdue ? 'danger' : 'warning'
}

async function loadMine() {
  const res = await api.get<never, { data: { data: FamilyVO } }>('/family/mine')
  family.value = res.data.data
  role.value = family.value.role

  if (role.value === 'OWNER') {
    await Promise.all([loadMembers(), loadTemplates(), loadTasks()])
  } else if (role.value === 'MEMBER') {
    await Promise.all([loadTasks(), loadSummary(), loadShareFields()])
    await loadSharedRecords()
    await loadSharedTrend()
  }
}

async function loadMembers() {
  const res = await api.get<never, { data: { data: MemberVO[] } }>('/family/members')
  members.value = res.data.data
  if (!assignee.value && members.value.length) {
    assignee.value = members.value[0].userId
  }
}

async function loadTemplates() {
  const res = await api.get<never, { data: { data: TaskTemplateVO[] } }>('/task-templates')
  templates.value = res.data.data
  if (!templateCode.value && templates.value.length) {
    templateCode.value = templates.value[0].code
  }
}

async function loadTasks() {
  const res = await api.get<never, { data: { data: TaskVO[] } }>('/tasks')
  tasks.value = res.data.data
}

async function loadSummary() {
  const res = await api.get<never, { data: { data: SharedSummaryVO } }>('/family/shared/summary')
  summary.value = res.data.data
}

async function loadShareFields() {
  const res = await api.get<never, { data: { data: ShareFieldVO[] } }>('/family/share-fields')
  shareFields.value = res.data.data
}

async function createInvite() {
  creating.value = true
  try {
    const res = await api.post<never, { data: { data: InvitationVO } }>('/family/invitations')
    invite.value = res.data.data
    if (role.value === 'NONE') {
      role.value = 'OWNER'
      family.value = { familyId: null, role: 'OWNER', ownerUserId: null, ownerNickname: '', memberCount: 0 }
      await Promise.all([loadMembers(), loadTemplates(), loadTasks()])
    }
    showSuccessToast('邀请码已生成')
  } finally {
    creating.value = false
  }
}

async function copyCode() {
  if (!invite.value) return
  try {
    await navigator.clipboard.writeText(invite.value.code)
    showSuccessToast('已复制')
  } catch {
    showToast('请长按邀请码手动复制')
  }
}

async function join() {
  if (!code.value.trim()) {
    showToast('请输入邀请码')
    return
  }
  joining.value = true
  try {
    await api.post(`/family/join/${code.value.trim()}`)
    showSuccessToast('已加入家庭')
    code.value = ''
    await loadMine()
  } finally {
    joining.value = false
  }
}

function openPerm(m: MemberVO) {
  editing.value = m
  pickedFields.value = [...m.visibleFields]
  showPerm.value = true
}

function toggle(field: string) {
  const i = pickedFields.value.indexOf(field)
  if (i >= 0) pickedFields.value.splice(i, 1)
  else pickedFields.value.push(field)
}

async function savePerm() {
  if (!editing.value) return
  savingPerm.value = true
  try {
    await api.put(`/family/members/${editing.value.id}/permissions`, {
      visibleFields: pickedFields.value,
      relation: editing.value.relation,
    })
    showPerm.value = false
    showSuccessToast('已更新可见范围')
    await loadMembers()
  } finally {
    savingPerm.value = false
  }
}

async function removeMember(m: MemberVO) {
  await showConfirmDialog({
    title: '移除家人',
    message: `移除后 ${m.nickname} 将看不到任何内容，之后可以用新邀请码重新加入。`,
  })
  await api.delete(`/family/members/${m.id}`)
  showSuccessToast('已移除')
  await loadMembers()
}

async function assign() {
  if (!canAssign.value) return
  assigning.value = true
  try {
    await api.post('/tasks/assign', {
      templateCode: templateCode.value,
      assigneeUserId: assignee.value,
    })
    showSuccessToast('任务已下发')
    await loadTasks()
  } finally {
    assigning.value = false
  }
}

async function setStatus(t: TaskVO, status: TaskVO['status']) {
  await api.put(`/tasks/${t.id}/status`, { status })
  await loadTasks()
  if (role.value === 'MEMBER') await loadSummary()
  showSuccessToast(status === 'DONE' ? '打卡完成' : '已更新')
}

async function loadSharedRecords() {
  const type = sharedType.value || sharedTypes.value[0]
  if (!type) return
  sharedType.value = type
  if (sharedMap.value[type]) return
  const res = await api.get<never, { data: { data: RecordVO[] } }>('/family/shared/records', {
    params: { type },
  })
  sharedMap.value = { ...sharedMap.value, [type]: res.data.data }
}

async function loadSharedTrend() {
  const type = sharedType.value
  if (!type) return
  const res = await api.get<never, { data: { data: TrendData } }>('/family/shared/trend', {
    params: { type, days: 30 },
  })
  trend.value = res.data.data
  await renderTrend(trend.value)
}

/** 切换指标：明细与趋势图一起刷新 */
async function onSharedTypeChange() {
  await Promise.all([loadSharedRecords(), loadSharedTrend()])
}

function onResize() {
  resizeChart()
}

onMounted(() => {
  loadMine()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
})
</script>

<style scoped>
.family {
  min-height: 100vh;
  padding-bottom: 88px;
}
.hero-card {
  margin: 16px;
  padding: 24px 20px;
  border-radius: 18px;
  color: #fff;
  background: linear-gradient(160deg, #7a9b76 0%, #5d7f5a 100%);
}
.hero-title {
  margin: 0 0 8px;
  font-size: 19px;
  font-weight: 600;
}
.hero-sub {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  opacity: 0.9;
}
.card {
  margin: 16px;
  padding: 16px;
  background: #fff;
  border-radius: 16px;
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.card-title {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 600;
}
.hint {
  margin: 0 0 12px;
  color: #999;
  font-size: 12px;
  line-height: 1.6;
}
.invite-box {
  margin-top: 12px;
  padding: 16px;
  border-radius: 12px;
  background: #f4f8f3;
  text-align: center;
}
.invite-code {
  margin: 0;
  font-size: 30px;
  font-weight: 700;
  letter-spacing: 6px;
  color: #4a6b47;
}
.invite-hint {
  margin: 6px 0 10px;
  color: #8a8a8a;
  font-size: 12px;
}
.save {
  margin-top: 12px;
}
.sub-title {
  margin: 14px 0 8px;
  font-size: 13px;
  color: #666;
}
.assignee {
  margin-top: 4px;
}
.rm-row {
  padding: 0;
}
.rm {
  color: #c4553f;
  font-size: 13px;
}
.week-line {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin: 8px 0 4px;
}
.week-num {
  font-size: 40px;
  font-weight: 700;
  line-height: 1;
  color: #4a6b47;
}
.week-plus {
  font-size: 16px;
  color: #4a6b47;
}
.week-unit {
  margin-right: 8px;
  color: #666;
}
.stats {
  display: flex;
  gap: 12px;
  margin-top: 14px;
}
.stat {
  flex: 1;
  padding: 12px;
  border-radius: 12px;
  background: #f7f9f6;
  text-align: center;
}
.stat-num {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #4a6b47;
}
.stat-label {
  margin: 4px 0 0;
  color: #999;
  font-size: 12px;
}
.next {
  margin: 12px 0 0;
  font-size: 13px;
  color: #666;
}
.task {
  padding: 12px 0;
  border-bottom: 1px solid #f2f2f2;
}
.task-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.task-title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
}
.task-detail {
  margin: 6px 0 0;
  color: #777;
  font-size: 12px;
  line-height: 1.6;
}
.task-meta {
  display: flex;
  gap: 10px;
  margin: 6px 0 0;
  color: #aaa;
  font-size: 11px;
}
.overdue {
  color: #c4553f;
}
.task-actions {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}
.summary {
  color: #999;
  font-size: 12px;
}
.chart {
  width: 100%;
  height: 200px;
  margin-top: 8px;
}
.disclaimer {
  margin: 4px 0 10px;
  color: #bbb;
  font-size: 11px;
  line-height: 1.5;
}
.center {
  text-align: center;
}
.privacy {
  margin: 14px 0 0;
  padding: 10px 12px;
  border-radius: 10px;
  background: #fdf6ec;
  color: #a06a2c;
  font-size: 12px;
  line-height: 1.6;
}
.perm {
  padding: 20px 16px 28px;
}
.perm-title {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 600;
}
.perm-hint {
  margin: 12px 0;
  color: #a06a2c;
  font-size: 12px;
}
</style>
