<template>
  <div class="consult-page" :class="{ chatting: !!detail }">
    <van-nav-bar
      :title="detail ? detail.session.title : '私密健康咨询'"
      left-arrow
      @click-left="back"
    >
      <template #right v-if="detail">
        <span class="referral-link" @click="referral">转人工</span>
      </template>
    </van-nav-bar>

    <!-- 主题列表 -->
    <template v-if="!detail">
      <div class="intro">
        <p class="intro-title">有些问题，不好意思问出口</p>
        <p class="intro-text">
          分泌物、瘙痒、痛经、乳房硬块、漏尿、避孕……这里可以匿名问。
          我们不做诊断，但会告诉你：可以先做什么、什么时候必须就医、该挂哪个科。
        </p>
        <div class="intro-pills">
          <span>不需要真名</span>
          <span>不进社区</span>
          <span>家属不可见</span>
        </div>
      </div>

      <div class="section-title" style="margin-top: 22px">
        <h3>你想问哪方面</h3>
        <span class="more">共 {{ topics.length }} 个主题</span>
      </div>

      <div class="topics">
        <button v-for="t in topics" :key="t.code" class="topic" @click="start(t)">
          <span class="topic-cat">{{ t.categoryLabel }}</span>
          <strong>{{ t.title }}</strong>
          <em>{{ t.summary }}</em>
          <span class="topic-dept">{{ t.department }} ›</span>
        </button>
      </div>

      <div class="section" v-if="sessions.length">
        <div class="section-title"><h3>之前的咨询</h3></div>
        <div class="card tight">
          <div v-for="s in sessions" :key="s.id" class="session" @click="open(s.id)">
            <div>
              <p class="session-title">{{ s.title }}</p>
              <p class="muted">{{ s.riskLabel }} · {{ statusLabel(s.status) }}</p>
            </div>
            <van-icon name="arrow" color="#c4c4c4" />
          </div>
        </div>
      </div>

      <p class="disclaimer">咨询内容为健康教育与就诊准备信息，不构成诊断或用药建议。</p>
    </template>

    <!-- 会话 -->
    <template v-else>
      <div class="risk-banner" v-if="detail.session.riskLevel === 'RED'">
        你提到的情况可能需要尽快线下就医。如出现大出血、晕厥、剧烈腹痛，请直接拨打 120。
      </div>

      <div class="messages" ref="messageBox">
        <div
          v-for="m in detail.messages"
          :key="m.id"
          class="msg"
          :class="m.role.toLowerCase()"
        >
          <div v-if="m.role !== 'SYSTEM'" class="bubble">{{ m.content }}</div>
          <p v-else class="system-text">{{ m.content }}</p>
        </div>
      </div>

      <div class="composer">
        <van-field
          v-model="draft"
          rows="1"
          autosize
          type="textarea"
          maxlength="1000"
          placeholder="描述你的情况，比如持续多久、有没有疼痛"
          class="input"
        />
        <van-button
          round
          color="#7a9b76"
          size="small"
          :loading="sending"
          :disabled="!draft.trim()"
          @click="send"
        >
          发送
        </van-button>
      </div>
    </template>

    <!-- 转介结果 -->
    <van-popup v-model:show="showReferral" position="bottom" round>
      <div class="referral" v-if="referralResult">
        <h3>{{ referralResult.levelLabel }} · 已提交转介</h3>
        <p class="muted">{{ referralResult.message }}</p>
        <div class="partner" v-for="p in referralResult.partners" :key="p.id">
          <div>
            <p class="partner-name">{{ p.name }}</p>
            <p class="muted">{{ p.typeLabel }} · {{ p.city }}{{ p.district || '' }}</p>
            <p class="muted">{{ p.address }}</p>
          </div>
          <van-icon name="arrow" color="#c4c4c4" @click="$router.push('/partners')" />
        </div>
        <van-button round block color="#7a9b76" @click="showReferral = false">知道了</van-button>
        <p class="disclaimer">转介不等于诊断。工单会由合作机构在工作时间内联系，紧急情况请直接就医。</p>
      </div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import { showFailToast, showSuccessToast } from 'vant'
import { get, post } from '../api/request'

interface Topic {
  id: number
  code: string
  title: string
  category: string
  categoryLabel: string
  summary: string
  department: string
}

interface Session {
  id: number
  title: string
  riskLevel: string
  riskLabel: string
  status: string
}

interface Message {
  id: number
  role: string
  content: string
}

interface Detail {
  session: Session
  messages: Message[]
  disclaimer: string
}

interface Referral {
  ticketId: number
  level: string
  levelLabel: string
  message: string
  partners: {
    id: number
    name: string
    typeLabel: string
    city: string
    district?: string
    address?: string
  }[]
}

const topics = ref<Topic[]>([])
const sessions = ref<Session[]>([])
const detail = ref<Detail | null>(null)
const draft = ref('')
const sending = ref(false)
const messageBox = ref<HTMLElement>()
const showReferral = ref(false)
const referralResult = ref<Referral | null>(null)

function statusLabel(status: string) {
  return { OPEN: '进行中', REFERRED: '已转介', CLOSED: '已结束' }[status] || status
}

async function load() {
  const [t, s] = await Promise.all([
    get<Topic[]>('/wellness/consult/topics'),
    get<Session[]>('/wellness/consult/sessions'),
  ])
  topics.value = t
  sessions.value = s
}

async function start(topic: Topic) {
  const d = await post<Detail>('/wellness/consult/sessions', { topicCode: topic.code })
  detail.value = d
  scrollBottom()
}

async function open(id: number) {
  detail.value = await get<Detail>(`/wellness/consult/sessions/${id}`)
  scrollBottom()
}

function back() {
  if (detail.value) {
    detail.value = null
    load()
  } else {
    history.back()
  }
}

async function send() {
  const content = draft.value.trim()
  if (!content || !detail.value) return
  sending.value = true
  try {
    const added = await post<Message[]>(
      `/wellness/consult/sessions/${detail.value.session.id}/messages`,
      { content },
    )
    detail.value.messages.push(...added)
    draft.value = ''
    if (added.some((m) => m.role === 'ASSISTANT' && m.content.includes('尽快就医'))) {
      detail.value.session.riskLevel = 'RED'
    }
    scrollBottom()
  } finally {
    sending.value = false
  }
}

async function referral() {
  if (!detail.value) return
  if (detail.value.session.status === 'REFERRED') {
    showFailToast('已经提交过转介')
    return
  }
  referralResult.value = await post<Referral>(
    `/wellness/consult/sessions/${detail.value.session.id}/referral`,
  )
  detail.value.session.status = 'REFERRED'
  showReferral.value = true
  showSuccessToast('已提交，机构会联系你')
}

function scrollBottom() {
  nextTick(() => {
    if (messageBox.value) {
      messageBox.value.scrollTop = messageBox.value.scrollHeight
    }
  })
}

onMounted(load)
</script>

<style scoped>
.consult-page {
  min-height: 100vh;
  padding-bottom: 30px;
}

.consult-page.chatting {
  padding-bottom: 84px;
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}

.intro {
  margin: 12px 16px 0;
  padding: 22px;
  border-radius: 22px;
  background: linear-gradient(145deg, #f8eef0, #f3e3e6);
}

.intro-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #7c4c54;
  letter-spacing: 0.3px;
}

.intro-text {
  margin: 10px 0 0;
  font-size: 12px;
  line-height: 1.8;
  color: #86646a;
}

.intro-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.intro-pills span {
  font-size: 10px;
  padding: 4px 9px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.65);
  color: #8d6169;
}

.topics {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin: 0 16px;
}

.topic {
  border: none;
  text-align: left;
  background: #fff;
  border-radius: 16px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  box-shadow: 0 5px 16px rgba(78, 96, 73, 0.05);
  min-height: 142px;
}

.topic-cat {
  font-size: 10px;
  color: #a25f68;
  background: #f7ecee;
  border-radius: 999px;
  padding: 3px 8px;
  align-self: flex-start;
}

.topic strong {
  font-size: 14px;
  color: var(--yuji-ink);
  line-height: 1.4;
}

.topic em {
  font-style: normal;
  font-size: 11px;
  line-height: 1.6;
  color: var(--yuji-muted);
  flex: 1;
}

.topic-dept {
  font-size: 10px;
  color: var(--yuji-primary);
}

.session {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid var(--yuji-line);
}

.session:last-child {
  border-bottom: none;
}

.session-title {
  margin: 0 0 4px;
  font-size: 13px;
}

.session .muted {
  margin: 0;
}

.disclaimer {
  margin: 22px 24px 0;
  font-size: 11px;
  color: #b3b3b3;
  line-height: 1.7;
  text-align: center;
}

.referral-link {
  font-size: 12px;
  color: #7a9b76;
}

.risk-banner {
  margin: 8px 16px 0;
  padding: 10px 14px;
  border-radius: 12px;
  background: #fdeeee;
  color: #b04a4a;
  font-size: 11px;
  line-height: 1.6;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 14px 16px 10px;
  background: #f7f6f2;
  margin-top: 8px;
}

.msg {
  display: flex;
  margin-bottom: 14px;
}

.msg.user {
  justify-content: flex-end;
}

.msg.assistant {
  justify-content: flex-start;
}

.bubble {
  max-width: 84%;
  padding: 12px 14px;
  border-radius: 16px;
  background: #fff;
  font-size: 13px;
  line-height: 1.75;
  color: var(--yuji-text);
  white-space: pre-wrap;
  word-break: break-word;
  box-shadow: 0 3px 10px rgba(78, 96, 73, 0.05);
}

.msg.user .bubble {
  background: #7a9b76;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.msg.assistant .bubble {
  border-bottom-left-radius: 4px;
}

.system-text {
  margin: 0 auto;
  max-width: 88%;
  text-align: center;
  font-size: 11px;
  line-height: 1.6;
  color: #999;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 999px;
  padding: 6px 12px;
}

.composer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 10px 14px calc(10px + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1px solid var(--yuji-line);
  z-index: 20;
}

.input {
  flex: 1;
  background: #f7f7f4;
  border-radius: 18px;
  padding: 6px 12px;
}

.referral {
  padding: 24px 20px 30px;
}

.referral h3 {
  margin: 0 0 8px;
  font-size: 17px;
}

.partner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  padding: 14px;
  margin: 12px 0;
  border-radius: 14px;
  background: #fafaf7;
}

.partner-name {
  margin: 0 0 4px;
  font-size: 13px;
  font-weight: 500;
}

.partner .muted {
  margin: 2px 0;
}
</style>
