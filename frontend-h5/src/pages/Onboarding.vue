<template>
  <div class="onboarding">
    <van-nav-bar
      v-if="step === 1"
      title="补充一点信息"
      left-arrow
      @click-left="step = 0"
    />
    <div v-else class="brand">
      <button class="brand-back" @click="$router.back()">返回</button>
      <p class="brand-name">育己</p>
      <p class="brand-slogan">育儿之前，先育己</p>
    </div>

    <van-steps :active="step" active-color="#7a9b76" class="steps">
      <van-step>你现在在哪个阶段</van-step>
      <van-step>让建议更贴近你</van-step>
    </van-steps>

    <!-- 第一步：阶段 -->
    <div v-if="step === 0" class="panel fade-in">
      <h2>先告诉我们，你现在最需要什么</h2>
      <p class="hint">不收集真名，健康数据只用于给你生成建议。之后可以随时修改。</p>
      <div class="stages">
        <button
          v-for="s in stages"
          :key="s.key"
          class="stage-card"
          :class="{ active: form.stage === s.key }"
          @click="form.stage = s.key"
        >
          <span class="stage-icon" :style="{ background: s.tint }">
            <van-icon :name="s.icon" size="20" color="#5d7f5a" />
          </span>
          <span class="stage-text">
            <strong>{{ s.title }}</strong>
            <em>{{ s.desc }}</em>
          </span>
          <van-icon v-if="form.stage === s.key" name="success" color="#7a9b76" />
        </button>
      </div>
      <van-button round block color="#7a9b76" class="next" :disabled="!form.stage" @click="step = 1">
        下一步
      </van-button>
    </div>

    <!-- 第二步：按阶段填写 -->
    <div v-else class="panel fade-in">
      <van-field
        v-model="form.nickname"
        label="怎么称呼你"
        maxlength="20"
        placeholder="昵称即可，不用真名"
      />
      <div class="field-gap"></div>

      <template v-if="isPregnancy">
        <h2>{{ form.stage === 'PREPARE' ? '备孕从记住周期开始' : '宝宝大概什么时候来？' }}</h2>
        <p class="hint">填一个就行，记不清也没关系。孕期会同时生成产检计划。</p>
        <van-field v-model="form.lmpDate" label="末次月经" type="date" :max="today" placeholder="第一天" />
        <div class="or">或者</div>
        <van-field v-model="form.edd" label="预产期" type="date" placeholder="医生说的预产期" />
      </template>

      <template v-else-if="form.stage === 'POSTPARTUM'">
        <h2>月子期，先把节奏放慢</h2>
        <p class="hint">填写分娩日期后，会生成 42 天调理清单（饮食、伤口、盆底、情绪、复查）。</p>
        <van-field v-model="form.deliveryDate" label="分娩日期" type="date" :max="today" placeholder="宝宝出生那天" />
        <van-field label="分娩方式">
          <template #input>
            <van-radio-group v-model="form.deliveryMode" direction="horizontal">
              <van-radio name="NATURAL">顺产</van-radio>
              <van-radio name="CESAREAN">剖宫产</van-radio>
            </van-radio-group>
          </template>
        </van-field>
        <van-field v-model="form.babyBirthDate" label="宝宝生日" type="date" :max="today" placeholder="选填，用于育儿课程" />
      </template>

      <template v-else-if="form.stage === 'PARENTING'">
        <h2>宝宝多大了？</h2>
        <p class="hint">课程会按月龄命中：0-3 月看护理，6 月看辅食，两岁看如厕。</p>
        <van-field v-model="form.babyBirthDate" label="宝宝生日" type="date" :max="today" placeholder="选择日期" />
      </template>

      <template v-else>
        <h2>{{ form.stage === 'MENOPAUSE' ? '先记录一周，再去门诊聊' : '记下最近一次月经' }}</h2>
        <p class="hint">有起点才能预测。记不清可以留空，之后在「健康 - 经期记录」补上。</p>
        <van-field v-model="form.lmpDate" label="最近一次月经" type="date" :max="today" placeholder="第一天，可留空" />
        <van-field label="平均周期">
          <template #input>
            <van-stepper v-model="form.cycleLength" :min="21" :max="45" integer />
            <span class="unit">天</span>
          </template>
        </van-field>
        <van-field label="平均经期">
          <template #input>
            <van-stepper v-model="form.periodLength" :min="2" :max="10" integer />
            <span class="unit">天</span>
          </template>
        </van-field>
      </template>

      <div class="concerns-block">
        <p class="concerns-title">最近最想解决什么？<span>选 1-3 个</span></p>
        <van-checkbox-group v-model="form.concerns" direction="horizontal" class="concerns">
          <van-checkbox v-for="c in concernOptions" :key="c" :name="c" shape="square">{{ c }}</van-checkbox>
        </van-checkbox-group>
      </div>

      <p class="privacy">
        健康数据仅用于生成你的建议。情绪与私密咨询内容默认不对家属开放。
      </p>

      <van-button round block color="#7a9b76" :loading="loading" @click="submit">
        开始使用
      </van-button>
      <button class="skip" @click="submit(true)">先跳过，直接看看</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showSuccessToast } from 'vant'
import { post } from '../api/request'

const router = useRouter()
const step = ref(0)
const loading = ref(false)
const today = new Date().toISOString().slice(0, 10)

const stages = [
  { key: 'CYCLE', title: '经期管理', desc: '周期记录、预测与经期饮食', icon: 'calendar-o', tint: '#eef4ec' },
  { key: 'PREPARE', title: '备孕', desc: '周期、叶酸与孕前准备', icon: 'flower-o', tint: '#f7ecee' },
  { key: 'PREGNANT', title: '孕期', desc: '孕周、产检日历与报告', icon: 'like-o', tint: '#f3f0e8' },
  { key: 'POSTPARTUM', title: '产后 / 月子', desc: '42 天调理、盆底与情绪', icon: 'smile-o', tint: '#eef4ec' },
  { key: 'PARENTING', title: '育儿', desc: '按月龄的课程与照护', icon: 'guide-o', tint: '#eef2f6' },
  { key: 'MENOPAUSE', title: '更年期', desc: '潮热、睡眠与情绪管理', icon: 'clock-o', tint: '#fbf1e4' },
]

const concernOptions = ['痛经', '经前不适', '睡眠', '情绪', '备孕', '产后恢复', '盆底', '育儿']

const savedNickname = (() => {
  try {
    return JSON.parse(localStorage.getItem('user') || '{}').nickname || ''
  } catch {
    return ''
  }
})()

const form = reactive({
  stage: '',
  nickname: savedNickname,
  lmpDate: '',
  edd: '',
  cycleLength: 28,
  periodLength: 5,
  deliveryDate: '',
  deliveryMode: 'NATURAL',
  babyBirthDate: '',
  concerns: [] as string[],
})

const isPregnancy = computed(() => form.stage === 'PREPARE' || form.stage === 'PREGNANT')

async function submit(skip = false) {
  if (!skip) {
    if (isPregnancy.value && !form.lmpDate && !form.edd) {
      showSuccessToast('请填写末次月经或预产期')
      return
    }
    if (form.stage === 'POSTPARTUM' && !form.deliveryDate) {
      showSuccessToast('请选择分娩日期')
      return
    }
    if (form.stage === 'PARENTING' && !form.babyBirthDate) {
      showSuccessToast('请选择宝宝生日')
      return
    }
  }

  loading.value = true
  try {
    if (isPregnancy.value) {
      await post('/profiles', {
        lmpDate: form.lmpDate || undefined,
        edd: form.edd || undefined,
        nickname: form.nickname || undefined,
        concerns: form.concerns,
      })
      // 全周期档案做兜底，失败不影响孕期主流程
      await post('/wellness/profiles', {
        lifeStage: form.stage,
        lastPeriodDate: form.lmpDate || undefined,
        concerns: form.concerns,
      }).catch(() => null)
    } else {
      await post('/wellness/profiles', {
        lifeStage: form.stage,
        nickname: form.nickname || undefined,
        lastPeriodDate: skip ? undefined : form.lmpDate || undefined,
        cycleLength: form.cycleLength,
        periodLength: form.periodLength,
        babyBirthDate: skip ? undefined : form.babyBirthDate || undefined,
        deliveryDate: skip ? undefined : form.deliveryDate || undefined,
        deliveryMode: form.stage === 'POSTPARTUM' ? form.deliveryMode : undefined,
        concerns: form.concerns,
      })
      if (!skip && form.stage === 'POSTPARTUM' && form.deliveryDate) {
        await post('/wellness/care/plans', {
          deliveryDate: form.deliveryDate,
          deliveryMode: form.deliveryMode,
        }).catch(() => null)
      }
    }
    showSuccessToast('设置完成')
    router.replace('/')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.onboarding {
  min-height: 100vh;
  padding: 8px 0 40px;
  background: radial-gradient(120% 60% at 50% 0%, #eef4ec 0%, #faf8f5 55%);
}

.brand {
  position: relative;
  text-align: center;
  padding: 22px 0 6px;
}

.brand-back {
  position: absolute;
  left: 16px;
  top: 18px;
  border: none;
  background: transparent;
  font-size: 12px;
  color: var(--yuji-muted);
  padding: 4px 8px;
}

.brand-name {
  margin: 0;
  font-size: 30px;
  letter-spacing: 8px;
  font-weight: 700;
  color: var(--yuji-primary-deep);
}

.brand-slogan {
  margin: 6px 0 0;
  font-size: 12px;
  letter-spacing: 3px;
  color: var(--yuji-muted);
}

.steps {
  margin-top: 10px;
}

.panel {
  margin: 18px 16px;
  padding: 22px 20px;
  background: #fff;
  border-radius: 22px;
  box-shadow: 0 10px 30px rgba(78, 96, 73, 0.06);
}

.panel h2 {
  margin: 0 0 8px;
  font-size: 19px;
  letter-spacing: 0.3px;
}

.hint {
  color: var(--yuji-muted);
  font-size: 12px;
  line-height: 1.7;
  margin: 0 0 16px;
}

.stages {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.stage-card {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px;
  border-radius: 16px;
  border: 1px solid #eef0ea;
  background: #fff;
  text-align: left;
  min-height: 92px;
}

.stage-card.active {
  border-color: #9dbb98;
  background: #f7fbf5;
  box-shadow: 0 4px 14px rgba(122, 155, 118, 0.12);
}

.stage-icon {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stage-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}

.stage-text strong {
  font-size: 14px;
  color: var(--yuji-ink);
}

.stage-text em {
  font-style: normal;
  font-size: 11px;
  line-height: 1.5;
  color: var(--yuji-muted);
}

.next {
  margin-top: 22px;
}

.or {
  text-align: center;
  color: #bbb;
  margin: 10px 0;
  font-size: 12px;
}

.field-gap {
  height: 12px;
}

.unit {
  margin-left: 8px;
  font-size: 12px;
  color: var(--yuji-muted);
}

.concerns-block {
  margin-top: 20px;
}

.concerns-title {
  font-size: 13px;
  margin: 0 0 10px;
  color: var(--yuji-text);
}

.concerns-title span {
  font-size: 11px;
  color: var(--yuji-muted);
  margin-left: 6px;
}

.concerns {
  row-gap: 10px;
}

.privacy {
  margin: 18px 0;
  font-size: 11px;
  line-height: 1.7;
  color: #b3b3b3;
  background: #fbfaf7;
  border-radius: 12px;
  padding: 10px 12px;
}

.skip {
  display: block;
  width: 100%;
  border: none;
  background: transparent;
  color: var(--yuji-muted);
  font-size: 12px;
  padding: 14px 0 2px;
}
</style>
