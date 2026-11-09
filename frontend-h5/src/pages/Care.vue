<template>
  <div class="page care-page">
    <van-nav-bar title="关怀" />

    <van-tabs v-model:active="activeTab" class="care-tabs" swipeable>
      <!-- ============ 月子调理 ============ -->
      <van-tab title="月子调理">
        <template v-if="!plan">
          <div class="intro green">
            <p class="intro-title">42 天，不必一个人硬扛</p>
            <p class="intro-text">
              填写分娩日期，我们会按产后天数给你一份可执行的清单：吃什么、伤口怎么观察、
              什么时候开始盆底训练、哪天复查。
            </p>
          </div>
          <div class="card">
            <van-field v-model="deliveryDate" label="分娩日期" type="date" :max="today" />
            <van-field label="分娩方式">
              <template #input>
                <van-radio-group v-model="deliveryMode" direction="horizontal">
                  <van-radio name="NATURAL">顺产</van-radio>
                  <van-radio name="CESAREAN">剖宫产</van-radio>
                </van-radio-group>
              </template>
            </van-field>
            <van-button round block color="#7a9b76" :loading="creating" class="create-btn" @click="createPlan">
              生成我的月子计划
            </van-button>
          </div>
        </template>

        <template v-else>
          <div class="care-hero">
            <div>
              <p class="eyebrow">{{ plan.deliveryModeLabel }} · {{ plan.deliveryDate }}</p>
              <p class="care-day">产后第 <strong>{{ plan.dayIndex }}</strong> 天</p>
              <p class="care-stage">{{ plan.stageLabel }}</p>
            </div>
            <div class="progress-ring">
              <span>{{ plan.progress }}%</span>
            </div>
          </div>

          <div class="section-title" style="margin-top: 20px">
            <h3>今天要做</h3>
            <span class="more">{{ plan.today.length }} 项</span>
          </div>
          <div class="task-list">
            <div
              v-for="t in plan.today"
              :key="t.id"
              class="task"
              :class="{ done: t.status === 'DONE' }"
              @click="toggleTask(t)"
            >
              <span class="check">
                <van-icon :name="t.status === 'DONE' ? 'success' : 'circle'" />
              </span>
              <div class="task-body">
                <p class="task-title">{{ t.title }}</p>
                <p class="task-detail">{{ t.detail }}</p>
                <span class="pill">{{ t.categoryLabel }} · 第 {{ t.dayIndex }} 天</span>
              </div>
            </div>
            <p v-if="!plan.today.length" class="empty">今天没有硬性任务，先照顾好自己的睡眠。</p>
          </div>

          <div class="section" v-if="plan.upcoming.length">
            <div class="section-title"><h3>接下来</h3></div>
            <div class="card tight">
              <div v-for="t in plan.upcoming" :key="t.id" class="upcoming">
                <span class="up-date">{{ t.dueDate.slice(5) }}</span>
                <div>
                  <p class="up-title">{{ t.title }}</p>
                  <p class="muted">{{ t.categoryLabel }} · 产后第 {{ t.dayIndex }} 天</p>
                </div>
              </div>
            </div>
          </div>

          <div class="section">
            <div class="section-title"><h3>42 天全清单</h3></div>
            <van-collapse v-model="openWeeks">
              <van-collapse-item v-for="w in plan.weeks" :key="w.weekNo" :title="w.label" :name="String(w.weekNo)">
                <div v-for="t in w.tasks" :key="t.id" class="week-task" @click="toggleTask(t)">
                  <van-icon
                    :name="t.status === 'DONE' ? 'checked' : 'circle'"
                    :color="t.status === 'DONE' ? '#7a9b76' : '#c8c8c8'"
                  />
                  <span :class="{ struck: t.status === 'DONE' }">{{ t.title }}</span>
                </div>
              </van-collapse-item>
            </van-collapse>
          </div>
          <p class="disclaimer">{{ plan.disclaimer }}</p>
        </template>
      </van-tab>

      <!-- ============ 育儿教育 ============ -->
      <van-tab title="育儿教育">
        <div class="intro blue">
          <p class="intro-title">按月龄，只学这一阶段用得上的</p>
          <p class="intro-text">
            不堆课程，也不制造“别人家孩子”的焦虑。每个阶段 2 节课，5 分钟能看完。
          </p>
        </div>

        <div class="card" v-if="parenting?.babyAgeMonths == null">
          <p class="form-title">宝宝多大了？</p>
          <p class="muted" style="margin-bottom: 12px">填写生日后，课程会按月龄自动匹配。</p>
          <van-field v-model="babyBirthDate" label="宝宝生日" type="date" :max="today" />
          <van-button round block color="#7a9b76" :loading="savingBaby" class="create-btn" @click="saveBaby">
            保存并查看课程
          </van-button>
        </div>

        <div class="month-card" v-else>
          <div>
            <p class="eyebrow">当前月龄</p>
            <p class="month-num">{{ parenting.babyAgeMonths }} <span>个月</span></p>
          </div>
          <p class="month-tip">{{ parenting.monthTip }}</p>
        </div>

        <div class="courses">
          <div v-for="c in parenting?.courses" :key="c.id" class="course" @click="openCourse(c.id)">
            <div class="course-head">
              <span class="pill">{{ c.monthHint }}</span>
              <span class="muted">{{ c.lessonCount }} 节</span>
            </div>
            <h3>{{ c.title }}</h3>
            <p class="course-summary">{{ c.summary }}</p>
            <p class="course-source">{{ c.source }}</p>
          </div>
          <p v-if="parenting && !parenting.courses.length" class="empty">
            当前月龄暂无匹配课程，可以看看其他阶段。
          </p>
        </div>
        <p class="disclaimer">{{ parenting?.disclaimer }}</p>
      </van-tab>

      <!-- ============ 心理康复 ============ -->
      <van-tab title="心理康复">
        <div class="intro pink">
          <p class="intro-title">情绪不是矫情，是需要被看见的信号</p>
          <p class="intro-text">
            先记录，再判断。筛查结果只用于提示风险，不给你贴诊断标签。
            黄档会有陪伴建议，红档建议尽快面对面评估。
          </p>
        </div>

        <!-- 情绪打卡 -->
        <div class="card">
          <p class="form-title">今天感觉怎么样？</p>
          <div class="moods">
            <button
              v-for="m in moodOptions"
              :key="m.value"
              class="mood"
              :class="{ active: checkin.mood === m.value }"
              @click="checkin.mood = m.value"
            >
              <span class="mood-dot" :style="{ background: m.color }"></span>
              <span>{{ m.label }}</span>
            </button>
          </div>
          <div class="chips">
            <button
              v-for="t in moodTags"
              :key="t"
              class="chip"
              :class="{ on: checkin.tags.includes(t) }"
              @click="toggleTag(t)"
            >
              {{ t }}
            </button>
          </div>
          <van-field
            v-model="checkin.note"
            rows="2"
            autosize
            type="textarea"
            maxlength="300"
            placeholder="想写一句就写一句，不写也可以"
            class="note-field"
          />
          <van-button round block color="#7a9b76" :loading="savingMood" class="save-btn" @click="saveMood">
            记下来
          </van-button>
        </div>

        <!-- EPDS -->
        <div class="section-title" style="margin-top: 22px">
          <h3>EPDS 产后情绪筛查</h3>
          <span class="more">10 题 · 约 2 分钟</span>
        </div>

        <div class="card" v-if="!epdsOpen && !epdsResult">
          <p class="muted">
            爱丁堡产后抑郁量表（EPDS）是常用的筛查工具，关注最近 7 天的状态。
            它不能诊断抑郁症，但能帮你知道“是不是该找人聊聊了”。
          </p>
          <van-button round block plain color="#7a9b76" class="save-btn" @click="epdsOpen = true">
            开始筛查
          </van-button>
        </div>

        <div class="card" v-if="epdsOpen && !epdsResult">
          <div v-for="(q, i) in epdsQuestions" :key="i" class="question">
            <p class="q-title">{{ i + 1 }}. {{ q.title }}</p>
            <van-radio-group v-model="epdsAnswers[i]">
              <van-radio v-for="opt in q.options" :key="opt.score" :name="opt.score" class="q-option">
                {{ opt.label }}
              </van-radio>
            </van-radio-group>
          </div>
          <p class="muted" v-if="epdsAnswers.some((a) => a < 0)">请完成全部 10 题后再提交。</p>
          <van-button round block color="#7a9b76" :loading="submittingEpds" class="save-btn" @click="submitEpds">
            查看结果
          </van-button>
        </div>

        <div class="card result" v-if="epdsResult" :class="epdsResult.level.toLowerCase()">
          <div class="result-head">
            <span class="pill">{{ epdsResult.levelLabel }}</span>
            <span class="score">{{ epdsResult.score }} <em>/ 30</em></span>
          </div>
          <p class="interpretation">{{ epdsResult.interpretation }}</p>
          <ul class="suggestions">
            <li v-for="s in epdsResult.suggestions" :key="s">{{ s }}</li>
          </ul>
          <p v-if="epdsResult.safetyNote" class="safety">{{ epdsResult.safetyNote }}</p>
          <p class="muted">{{ epdsResult.disclaimer }}</p>
          <van-button
            v-if="epdsResult.level !== 'GREEN'"
            round
            block
            plain
            color="#7a9b76"
            class="save-btn"
            @click="$router.push('/partners')"
          >
            查看可预约的合作机构
          </van-button>
          <button class="reset" @click="resetEpds">重新测一次</button>
        </div>

        <!-- 历史 -->
        <div class="section-title" style="margin-top: 22px">
          <h3>最近两周</h3>
          <span class="more">{{ mental?.recentCheckins?.length || 0 }} 次记录</span>
        </div>
        <div class="card tight">
          <div v-for="c in mental?.recentCheckins" :key="c.id" class="history">
            <span class="mood-dot small" :style="{ background: moodColor(c.mood) }"></span>
            <div>
              <p class="h-title">{{ c.moodLabel }}<span v-if="c.tags.length"> · {{ c.tags.join(' / ') }}</span></p>
              <p class="muted">{{ c.note || c.timeText }}</p>
            </div>
            <span class="h-time">{{ c.timeText }}</span>
          </div>
          <p v-if="!mental?.recentCheckins?.length" class="empty">还没有记录，今天可以先写一句。</p>
        </div>

        <div class="section" v-if="mental?.tickets?.length">
          <div class="section-title"><h3>跟进工单</h3></div>
          <div class="card tight">
            <div v-for="t in mental.tickets" :key="t.id" class="ticket">
              <span class="pill" :class="t.level === 'RED' ? 'pink' : 'amber'">{{ t.levelLabel }}</span>
              <div>
                <p class="h-title">{{ t.summary }}</p>
                <p class="muted">{{ t.statusLabel }} · {{ t.timeText }}</p>
              </div>
            </div>
          </div>
        </div>

        <p class="disclaimer">{{ mental?.disclaimer }}</p>
      </van-tab>
    </van-tabs>

    <!-- 课程详情 -->
    <van-popup v-model:show="showCourse" position="bottom" round :style="{ maxHeight: '82%' }">
      <div class="course-detail" v-if="courseDetail">
        <div class="form-head">
          <div>
            <span class="pill">{{ courseDetail.course.monthHint }}</span>
            <h3>{{ courseDetail.course.title }}</h3>
          </div>
          <van-icon name="cross" @click="showCourse = false" />
        </div>
        <div v-for="(l, i) in courseDetail.lessons" :key="l.id" class="lesson">
          <p class="lesson-title">{{ i + 1 }}. {{ l.title }} <span>{{ l.durationMin }} 分钟</span></p>
          <p v-for="(line, j) in l.content.split('\n')" :key="j" class="lesson-text">{{ line }}</p>
        </div>
        <p class="muted">{{ courseDetail.disclaimer }}</p>
        <p class="muted">来源：{{ courseDetail.course.source }}</p>
      </div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { showFailToast, showSuccessToast } from 'vant'
import { get, post, put } from '../api/request'

interface TaskVO {
  id: number
  dayIndex: number
  dueDate: string
  category: string
  categoryLabel: string
  title: string
  detail: string
  status: string
}

interface PlanVO {
  id: number
  deliveryDate: string
  deliveryMode: string
  deliveryModeLabel: string
  dayIndex: number
  stageLabel: string
  progress: number
  today: TaskVO[]
  upcoming: TaskVO[]
  weeks: { weekNo: number; label: string; tasks: TaskVO[] }[]
  disclaimer: string
}

interface CourseVO {
  id: number
  code: string
  title: string
  summary: string
  minMonth: number
  maxMonth: number
  monthHint: string
  lessonCount: number
  source: string
}

interface ParentingPage {
  babyAgeMonths: number | null
  monthTip: string
  courses: CourseVO[]
  disclaimer: string
}

interface CourseDetail {
  course: CourseVO
  lessons: { id: number; title: string; content: string; durationMin: number }[]
  disclaimer: string
}

interface CheckinVO {
  id: number
  mood: number
  moodLabel: string
  tags: string[]
  note?: string
  timeText: string
}

interface ScaleVO {
  id: number
  score: number
  level: string
  levelLabel: string
}

interface TicketVO {
  id: number
  level: string
  levelLabel: string
  summary: string
  statusLabel: string
  timeText: string
}

interface MentalOverview {
  recentCheckins: CheckinVO[]
  scales: ScaleVO[]
  tickets: TicketVO[]
  tip: string
  disclaimer: string
}

interface EpdsResult {
  score: number
  level: string
  levelLabel: string
  interpretation: string
  suggestions: string[]
  safetyNote?: string
  ticketId?: number
  disclaimer: string
}

const route = useRoute()
const today = new Date().toISOString().slice(0, 10)
const activeTab = ref(Number(route.query.tab) || 0)

const plan = ref<PlanVO | null>(null)
const deliveryDate = ref('')
const deliveryMode = ref('NATURAL')
const creating = ref(false)
const openWeeks = ref<string[]>([])

const parenting = ref<ParentingPage | null>(null)
const babyBirthDate = ref('')
const savingBaby = ref(false)
const showCourse = ref(false)
const courseDetail = ref<CourseDetail | null>(null)

const mental = ref<MentalOverview | null>(null)
const moodOptions = [
  { value: 1, label: '很糟', color: '#c98b93' },
  { value: 2, label: '有点低', color: '#d9a58b' },
  { value: 3, label: '还行', color: '#d9c08b' },
  { value: 4, label: '不错', color: '#a8c3a4' },
  { value: 5, label: '很好', color: '#7a9b76' },
]
const moodTags = ['疲惫', '焦虑', '委屈', '被理解', '睡不好', '有力量']
const checkin = reactive({ mood: 3, tags: [] as string[], note: '' })
const savingMood = ref(false)

const epdsOpen = ref(false)
const epdsAnswers = ref<number[]>(Array(10).fill(-1))
const submittingEpds = ref(false)
const epdsResult = ref<EpdsResult | null>(null)

const epdsQuestions = [
  {
    title: '我能看到事物有趣的一面，并笑得开心',
    options: [
      { score: 0, label: '同以前一样' },
      { score: 1, label: '没有以前那么多' },
      { score: 2, label: '肯定比以前少' },
      { score: 3, label: '完全不能' },
    ],
  },
  {
    title: '我欣然期待未来的一切',
    options: [
      { score: 0, label: '同以前一样' },
      { score: 1, label: '没有以前那么多' },
      { score: 2, label: '肯定比以前少' },
      { score: 3, label: '完全不能' },
    ],
  },
  {
    title: '当事情出错时，我会不必要地责备自己',
    options: [
      { score: 0, label: '没有这样' },
      { score: 1, label: '不经常这样' },
      { score: 2, label: '有时会这样' },
      { score: 3, label: '大部分时候会这样' },
    ],
  },
  {
    title: '我无缘无故感到焦虑和担心',
    options: [
      { score: 0, label: '一点也没有' },
      { score: 1, label: '极少这样' },
      { score: 2, label: '有时候这样' },
      { score: 3, label: '经常这样' },
    ],
  },
  {
    title: '我无缘无故感到害怕和惊慌',
    options: [
      { score: 0, label: '一点也没有' },
      { score: 1, label: '不经常这样' },
      { score: 2, label: '有时这样' },
      { score: 3, label: '相当多时候这样' },
    ],
  },
  {
    title: '很多事情冲着我来，使我透不过气',
    options: [
      { score: 0, label: '一直应付得很好' },
      { score: 1, label: '大部分时候应付得好' },
      { score: 2, label: '有时候不能应付' },
      { score: 3, label: '大多数时候不能应付' },
    ],
  },
  {
    title: '我很不开心，以致失眠',
    options: [
      { score: 0, label: '一点也没有' },
      { score: 1, label: '不经常这样' },
      { score: 2, label: '有时候这样' },
      { score: 3, label: '大部分时间这样' },
    ],
  },
  {
    title: '我感到难过和悲伤',
    options: [
      { score: 0, label: '没有这样' },
      { score: 1, label: '不经常这样' },
      { score: 2, label: '相当时候这样' },
      { score: 3, label: '大部分时候这样' },
    ],
  },
  {
    title: '我不开心到哭',
    options: [
      { score: 0, label: '从来没有' },
      { score: 1, label: '只是偶尔这样' },
      { score: 2, label: '有时候这样' },
      { score: 3, label: '大部分时间这样' },
    ],
  },
  {
    title: '我想过要伤害自己',
    options: [
      { score: 0, label: '没有这样' },
      { score: 1, label: '很少这样' },
      { score: 2, label: '有时候这样' },
      { score: 3, label: '经常这样' },
    ],
  },
]

function moodColor(mood: number) {
  return moodOptions.find((m) => m.value === mood)?.color || '#c8c8c8'
}

async function loadCare() {
  plan.value = await get<PlanVO | null>('/wellness/care/plan')
  if (plan.value?.weeks?.length) {
    openWeeks.value = [String(plan.value.weeks[0].weekNo)]
  }
}

async function loadParenting() {
  parenting.value = await get<ParentingPage>('/wellness/parenting/courses')
}

async function loadMental() {
  mental.value = await get<MentalOverview>('/wellness/mental/overview')
}

async function createPlan() {
  if (!deliveryDate.value) {
    showFailToast('请选择分娩日期')
    return
  }
  creating.value = true
  try {
    plan.value = await post<PlanVO>('/wellness/care/plans', {
      deliveryDate: deliveryDate.value,
      deliveryMode: deliveryMode.value,
    })
    showSuccessToast('计划已生成')
  } finally {
    creating.value = false
  }
}

async function toggleTask(task: TaskVO) {
  const status = task.status === 'DONE' ? 'PENDING' : 'DONE'
  await put(`/wellness/care/tasks/${task.id}`, { status })
  await loadCare()
}

async function saveBaby() {
  if (!babyBirthDate.value) {
    showFailToast('请选择宝宝生日')
    return
  }
  savingBaby.value = true
  try {
    await post('/wellness/profiles', { babyBirthDate: babyBirthDate.value })
    showSuccessToast('已保存')
    await loadParenting()
  } finally {
    savingBaby.value = false
  }
}

async function openCourse(id: number) {
  courseDetail.value = await get<CourseDetail>(`/wellness/parenting/courses/${id}`)
  showCourse.value = true
}

function toggleTag(tag: string) {
  if (checkin.tags.includes(tag)) {
    checkin.tags = checkin.tags.filter((t) => t !== tag)
  } else if (checkin.tags.length < 3) {
    checkin.tags.push(tag)
  }
}

async function saveMood() {
  savingMood.value = true
  try {
    await post('/wellness/mental/checkins', {
      mood: checkin.mood,
      tags: checkin.tags,
      note: checkin.note || undefined,
    })
    checkin.note = ''
    checkin.tags = []
    showSuccessToast('记下了')
    await loadMental()
  } finally {
    savingMood.value = false
  }
}

async function submitEpds() {
  if (epdsAnswers.value.some((a) => a < 0)) {
    showFailToast('请完成全部 10 题')
    return
  }
  submittingEpds.value = true
  try {
    epdsResult.value = await post<EpdsResult>('/wellness/mental/scales/epds', {
      answers: epdsAnswers.value,
    })
    epdsOpen.value = false
    await loadMental()
  } finally {
    submittingEpds.value = false
  }
}

function resetEpds() {
  epdsResult.value = null
  epdsAnswers.value = Array(10).fill(-1)
  epdsOpen.value = false
}

onMounted(async () => {
  await Promise.all([loadCare(), loadParenting(), loadMental()])
})
</script>

<style scoped>
.care-page {
  background: var(--yuji-bg);
}

.care-tabs {
  margin-top: 4px;
}

.intro {
  margin: 12px 16px 0;
  padding: 20px;
  border-radius: 20px;
}

.intro.green {
  background: linear-gradient(140deg, #f0f6ed, #e4efe0);
}

.intro.blue {
  background: linear-gradient(140deg, #eef3f7, #e3ecf3);
}

.intro.pink {
  background: linear-gradient(140deg, #f9eef0, #f3e2e6);
}

.intro-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--yuji-primary-deep);
}

.intro.blue .intro-title {
  color: #4c6d84;
}

.intro.pink .intro-title {
  color: #7c4c54;
}

.intro-text {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.8;
  color: #6c7568;
}

.intro.blue .intro-text {
  color: #62798a;
}

.intro.pink .intro-text {
  color: #86646a;
}

.create-btn {
  margin-top: 18px;
}

.care-hero {
  margin: 12px 16px 0;
  padding: 20px;
  border-radius: 20px;
  background: linear-gradient(145deg, #f0f6ed, #e2eddf);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.eyebrow {
  margin: 0;
  font-size: 11px;
  color: #7f8f7a;
}

.care-day {
  margin: 8px 0 6px;
  font-size: 15px;
  color: var(--yuji-text);
}

.care-day strong {
  font-size: 32px;
  color: var(--yuji-primary-deep);
  line-height: 1;
}

.care-stage {
  margin: 0;
  font-size: 12px;
  color: #66745f;
}

.progress-ring {
  width: 70px;
  height: 70px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.7);
  border: 2px solid #cfe0ca;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 600;
  color: var(--yuji-primary-deep);
}

.task-list {
  margin: 0 16px;
}

.task {
  display: flex;
  gap: 12px;
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  margin-bottom: 10px;
  box-shadow: 0 5px 16px rgba(78, 96, 73, 0.05);
}

.task.done {
  opacity: 0.62;
}

.check {
  color: var(--yuji-primary);
  font-size: 19px;
  line-height: 1.2;
  flex-shrink: 0;
}

.task-body {
  flex: 1;
}

.task-title {
  margin: 0 0 6px;
  font-size: 14px;
  font-weight: 500;
  color: var(--yuji-ink);
}

.task.done .task-title {
  text-decoration: line-through;
}

.task-detail {
  margin: 0 0 8px;
  font-size: 11px;
  line-height: 1.7;
  color: var(--yuji-muted);
}

.upcoming {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 11px 0;
  border-bottom: 1px solid var(--yuji-line);
}

.upcoming:last-child {
  border-bottom: none;
}

.up-date {
  font-size: 11px;
  color: var(--yuji-primary);
  background: #f0f5ee;
  border-radius: 8px;
  padding: 3px 7px;
  flex-shrink: 0;
}

.up-title {
  margin: 0 0 3px;
  font-size: 13px;
}

.week-task {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 2px;
  font-size: 12px;
  color: var(--yuji-text);
  border-bottom: 1px dashed #f0f0ec;
}

.week-task:last-child {
  border-bottom: none;
}

.struck {
  text-decoration: line-through;
  color: #b3b3b3;
}

.form-title {
  margin: 0 0 4px;
  font-size: 14px;
  font-weight: 500;
}

.month-card {
  margin: 12px 16px 0;
  padding: 18px;
  border-radius: 18px;
  background: linear-gradient(140deg, #eef3f7, #e5edf3);
}

.month-num {
  margin: 6px 0 0;
  font-size: 30px;
  font-weight: 700;
  color: #4c6d84;
  line-height: 1;
}

.month-num span {
  font-size: 12px;
  font-weight: 400;
  color: #7b93a4;
}

.month-tip {
  margin: 12px 0 0;
  font-size: 12px;
  line-height: 1.8;
  color: #62798a;
}

.courses {
  margin: 12px 16px 0;
}

.course {
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  margin-bottom: 10px;
  box-shadow: 0 5px 16px rgba(78, 96, 73, 0.05);
}

.course-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.course h3 {
  margin: 10px 0 6px;
  font-size: 15px;
}

.course-summary {
  margin: 0;
  font-size: 12px;
  line-height: 1.7;
  color: var(--yuji-muted);
}

.course-source {
  margin: 8px 0 0;
  font-size: 10px;
  color: #c0c0c0;
}

.form-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 8px;
}

.form-head h3 {
  margin: 8px 0 0;
  font-size: 17px;
}

.course-detail {
  padding: 22px 18px 30px;
  max-height: 80vh;
  overflow-y: auto;
}

.lesson {
  padding: 14px 0;
  border-bottom: 1px solid var(--yuji-line);
}

.lesson-title {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 600;
}

.lesson-title span {
  font-size: 10px;
  font-weight: 400;
  color: var(--yuji-muted);
  margin-left: 6px;
}

.lesson-text {
  margin: 0 0 6px;
  font-size: 12px;
  line-height: 1.9;
  color: #5c6259;
}

.moods {
  display: flex;
  gap: 6px;
  margin: 12px 0;
}

.mood {
  flex: 1;
  border: 1px solid #eef0ea;
  background: #fff;
  border-radius: 12px;
  padding: 10px 2px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  font-size: 10px;
  color: #7b817a;
}

.mood.active {
  border-color: #b9ceb4;
  background: #f6faf4;
  color: var(--yuji-primary-deep);
}

.mood-dot {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  display: block;
}

.mood-dot.small {
  width: 9px;
  height: 9px;
  margin-top: 5px;
  flex-shrink: 0;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin: 10px 0 4px;
}

.chip {
  border: 1px solid #eef0ea;
  background: #fff;
  border-radius: 999px;
  padding: 5px 11px;
  font-size: 11px;
  color: #7b817a;
}

.chip.on {
  background: #eef4ec;
  border-color: #b9ceb4;
  color: var(--yuji-primary-deep);
}

.note-field {
  background: #fafaf7;
  border-radius: 12px;
  margin-top: 10px;
}

.save-btn {
  margin-top: 14px;
}

.question {
  padding: 12px 0;
  border-bottom: 1px solid var(--yuji-line);
}

.question:last-of-type {
  border-bottom: none;
}

.q-title {
  margin: 0 0 8px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--yuji-text);
}

.q-option {
  padding: 6px 0;
  font-size: 12px;
}

.result.green {
  background: #f7fbf5;
}

.result.yellow {
  background: #fef9f0;
}

.result.red {
  background: #fdf1f1;
}

.result-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.score {
  font-size: 22px;
  font-weight: 700;
  color: var(--yuji-ink);
}

.score em {
  font-style: normal;
  font-size: 11px;
  font-weight: 400;
  color: var(--yuji-muted);
}

.interpretation {
  margin: 12px 0 8px;
  font-size: 12px;
  line-height: 1.8;
  color: var(--yuji-text);
}

.suggestions {
  margin: 0;
  padding-left: 18px;
}

.suggestions li {
  font-size: 12px;
  line-height: 1.9;
  color: #5c6259;
}

.safety {
  margin: 12px 0;
  padding: 10px 12px;
  border-radius: 10px;
  background: #fdeaea;
  color: #a94a4a;
  font-size: 11px;
  line-height: 1.7;
}

.reset {
  display: block;
  width: 100%;
  border: none;
  background: transparent;
  color: var(--yuji-muted);
  font-size: 11px;
  padding: 14px 0 2px;
}

.history {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  padding: 11px 0;
  border-bottom: 1px solid var(--yuji-line);
}

.history:last-child {
  border-bottom: none;
}

.h-title {
  margin: 0 0 3px;
  font-size: 13px;
  color: var(--yuji-text);
}

.history .muted {
  margin: 0;
}

.h-time {
  margin-left: auto;
  font-size: 10px;
  color: #c0c0c0;
  flex-shrink: 0;
}

.ticket {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  padding: 10px 0;
  border-bottom: 1px solid var(--yuji-line);
}

.ticket:last-child {
  border-bottom: none;
}

.empty {
  text-align: center;
  font-size: 12px;
  color: #b3b3b3;
  padding: 26px 12px;
  line-height: 1.7;
}

.disclaimer {
  margin: 22px 24px 0;
  font-size: 11px;
  color: #b3b3b3;
  line-height: 1.7;
  text-align: center;
}
</style>
