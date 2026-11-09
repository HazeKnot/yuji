<template>
  <div class="page-secondary">
    <van-nav-bar title="合作机构" left-arrow @click-left="$router.back()" />

    <div class="head">
      <p class="head-title">把线下的支持，接到线上</p>
      <p class="head-text">
        妇幼保健院、月子中心、产后康复与社区机构。你可以先看服务与位置，再决定要不要预约。
        平台不参与诊疗，也不按医疗收入推荐排名。
      </p>
    </div>

    <div class="filters">
      <button
        v-for="f in filters"
        :key="f.key"
        class="filter"
        :class="{ active: activeType === f.key }"
        @click="selectType(f.key)"
      >
        {{ f.label }}
      </button>
    </div>

    <div class="list">
      <div v-for="org in orgs" :key="org.id" class="org" @click="open(org.id)">
        <div class="org-head">
          <span class="type">{{ org.typeLabel }}</span>
          <span class="muted">{{ org.city }}{{ org.district || '' }}</span>
        </div>
        <h3>{{ org.name }}</h3>
        <p class="intro">{{ org.intro }}</p>
        <div class="org-foot">
          <span class="muted">{{ org.serviceCount }} 项服务</span>
          <span class="detail">查看与预约 ›</span>
        </div>
      </div>

      <p v-if="!orgs.length" class="empty">这个类型下暂时没有合作机构。</p>
    </div>

    <div class="card claim">
      <p class="claim-title">机构合作</p>
      <p class="muted">
        如果你是妇幼保健院、月子中心或社区机构，希望把产后复查、盆底评估、心理筛查接入育己，
        可通过机构端账号入驻，或联系项目团队。
      </p>
    </div>

    <p class="disclaimer">机构信息由合作方提供并经平台审核。具体服务、价格与医疗建议以机构现场为准。</p>

    <!-- 详情 + 预约 -->
    <van-popup v-model:show="showDetail" position="bottom" round :style="{ maxHeight: '88%' }">
      <div class="detail-panel" v-if="detail">
        <div class="detail-head">
          <div>
            <span class="type">{{ detail.org.typeLabel }}</span>
            <h2>{{ detail.org.name }}</h2>
            <p class="muted">{{ detail.org.city }}{{ detail.org.district || '' }} · {{ detail.org.address }}</p>
          </div>
          <van-icon name="cross" @click="showDetail = false" />
        </div>

        <p class="detail-intro">{{ detail.org.intro }}</p>

        <p class="block-title">服务</p>
        <div class="service" v-for="s in detail.services" :key="s.id">
          <div>
            <p class="service-name">{{ s.name }}</p>
            <p class="muted">{{ s.description }}</p>
          </div>
          <span class="price">{{ s.priceText }}</span>
        </div>

        <p class="block-title">预约咨询</p>
        <van-field v-model="lead.name" label="称呼" placeholder="怎么称呼你" maxlength="20" />
        <van-field v-model="lead.phone" label="手机号" type="tel" placeholder="11 位手机号" maxlength="11" />
        <van-field v-model="lead.need" label="想咨询" placeholder="比如产后 42 天复查" maxlength="100" />
        <van-field v-model="lead.preferredDate" label="期望日期" type="date" :min="today" />

        <van-button round block color="#7a9b76" :loading="submitting" class="submit" @click="submitLead">
          提交预约意向
        </van-button>
        <p class="privacy">
          提交后，机构会在工作时间内联系你。平台不会把你的健康档案提供给机构。
        </p>
      </div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { showFailToast, showSuccessToast } from 'vant'
import { get, post } from '../api/request'

interface Org {
  id: number
  name: string
  type: string
  typeLabel: string
  city: string
  district?: string
  address?: string
  intro: string
  serviceCount: number
}

interface Service {
  id: number
  name: string
  priceText?: string
  description?: string
}

interface Detail {
  org: Org
  services: Service[]
}

const filters = [
  { key: '', label: '全部' },
  { key: 'HOSPITAL', label: '妇幼保健院' },
  { key: 'MATERNITY', label: '月子中心' },
  { key: 'POSTPARTUM', label: '产后康复' },
  { key: 'COMMUNITY', label: '社区机构' },
]

const today = new Date().toISOString().slice(0, 10)
const activeType = ref('')
const orgs = ref<Org[]>([])
const showDetail = ref(false)
const detail = ref<Detail | null>(null)
const submitting = ref(false)

const lead = reactive({
  name: '',
  phone: '',
  need: '',
  preferredDate: '',
})

async function load() {
  orgs.value = await get<Org[]>('/wellness/partners', {
    params: activeType.value ? { type: activeType.value } : {},
  })
}

function selectType(type: string) {
  activeType.value = type
  load()
}

async function open(id: number) {
  detail.value = await get<Detail>(`/wellness/partners/${id}`)
  lead.name = ''
  lead.phone = ''
  lead.need = ''
  lead.preferredDate = ''
  showDetail.value = true
}

async function submitLead() {
  if (!detail.value) return
  if (!lead.name.trim()) {
    showFailToast('请填写称呼')
    return
  }
  if (!/^1\d{10}$/.test(lead.phone)) {
    showFailToast('请填写正确的 11 位手机号')
    return
  }
  if (!lead.need.trim()) {
    showFailToast('请简单描述想咨询的内容')
    return
  }
  submitting.value = true
  try {
    await post(`/wellness/partners/${detail.value.org.id}/leads`, {
      name: lead.name.trim(),
      phone: lead.phone,
      need: lead.need.trim(),
      preferredDate: lead.preferredDate || undefined,
    })
    showDetail.value = false
    showSuccessToast('已提交，机构会联系你')
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.head {
  margin: 12px 16px 0;
  padding: 20px;
  border-radius: 20px;
  background: linear-gradient(145deg, #f1f6ef, #e6efe3);
}

.head-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--yuji-primary-deep);
}

.head-text {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.8;
  color: #66745f;
}

.filters {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding: 16px 16px 4px;
}

.filter {
  flex-shrink: 0;
  border: 1px solid #e8eae4;
  background: #fff;
  border-radius: 999px;
  padding: 6px 13px;
  font-size: 12px;
  color: #6f756d;
}

.filter.active {
  background: #eef4ec;
  border-color: #b9ceb4;
  color: var(--yuji-primary-deep);
}

.list {
  padding: 8px 16px 0;
}

.org {
  background: #fff;
  border-radius: 18px;
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 6px 18px rgba(78, 96, 73, 0.05);
}

.org-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.type {
  font-size: 10px;
  padding: 3px 9px;
  border-radius: 999px;
  background: #eef4ec;
  color: var(--yuji-primary-deep);
}

.org h3 {
  margin: 10px 0 6px;
  font-size: 16px;
}

.intro {
  margin: 0;
  font-size: 12px;
  line-height: 1.7;
  color: var(--yuji-muted);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.org-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}

.detail {
  font-size: 11px;
  color: var(--yuji-primary);
}

.claim {
  margin-top: 20px;
  background: #fdfcf9;
}

.claim-title {
  margin: 0 0 6px;
  font-size: 13px;
  font-weight: 500;
}

.empty {
  text-align: center;
  font-size: 12px;
  color: #b3b3b3;
  padding: 50px 0;
}

.disclaimer {
  margin: 20px 24px 0;
  font-size: 11px;
  color: #b3b3b3;
  line-height: 1.7;
  text-align: center;
}

.detail-panel {
  padding: 22px 18px 30px;
  max-height: 84vh;
  overflow-y: auto;
}

.detail-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}

.detail-head h2 {
  margin: 8px 0 4px;
  font-size: 19px;
}

.detail-head .muted {
  margin: 0;
}

.detail-intro {
  margin: 14px 0 0;
  font-size: 12px;
  line-height: 1.8;
  color: #5c6259;
}

.block-title {
  margin: 20px 0 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--yuji-text);
}

.service {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px solid var(--yuji-line);
}

.service-name {
  margin: 0 0 4px;
  font-size: 13px;
}

.service .muted {
  margin: 0;
}

.price {
  font-size: 11px;
  color: #a4783e;
  flex-shrink: 0;
}

.submit {
  margin-top: 18px;
}

.privacy {
  margin: 12px 0 0;
  font-size: 10px;
  line-height: 1.7;
  color: #b3b3b3;
}
</style>
