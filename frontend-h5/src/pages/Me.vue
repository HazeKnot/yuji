<template>
  <div class="page me">
    <van-nav-bar title="我的" />

    <div class="profile-card">
      <div class="avatar">她</div>
      <div class="info">
        <p class="name">{{ auth.user?.nickname || '姐妹' }}</p>
        <p class="phone">{{ maskedPhone }}</p>
        <span class="stage">{{ overview?.lifeStageLabel || '全周期健康' }}</span>
      </div>
      <button class="edit" @click="$router.push('/onboarding')">完善</button>
    </div>

    <p v-if="overview?.todayTip" class="tip">{{ overview.todayTip }}</p>

    <div class="section-title" style="margin-top: 20px"><h3>我的健康</h3></div>
    <div class="card tight">
      <van-cell title="经期记录" label="记录周期，查看预测" is-link to="/cycle" />
      <van-cell title="月事餐饮" label="按周期阶段的一周食谱" is-link to="/meal" />
      <van-cell title="私密健康咨询" label="匿名问，不做诊断" is-link to="/consult" />
      <van-cell title="身体指标" label="体重 / 血压 / 血糖 / 胎动" is-link to="/records" />
      <van-cell title="检查报告" label="拍照归档，随时回看" is-link to="/reports" />
      <van-cell title="产检日历" label="全孕期计划" is-link to="/checkups" />
    </div>

    <div class="section-title" style="margin-top: 20px"><h3>陪伴与支持</h3></div>
    <div class="card tight">
      <van-cell title="月子调理" label="42 天饮食、康复与复查清单" is-link to="/care?tab=0" />
      <van-cell title="育儿教育" label="按月龄匹配的 5 分钟课程" is-link to="/care?tab=1" />
      <van-cell title="心理康复" label="情绪打卡与 EPDS 筛查" is-link to="/care?tab=2" />
      <van-cell title="她社区" label="经验分享与匿名树洞" is-link to="/community" />
      <van-cell title="合作机构" label="妇幼保健院 / 月子中心" is-link to="/partners" />
    </div>

    <div class="section-title" style="margin-top: 20px"><h3>家庭与隐私</h3></div>
    <div class="card tight">
      <van-cell title="家人与权限" label="邀请家人、控制可见指标" is-link to="/family" />
      <van-cell title="孕产知识库" label="按孕周推荐，来源可查" is-link to="/knowledge" />
      <van-cell title="隐私与数据" label="情绪与咨询内容默认不对家属开放" />
    </div>

    <div class="logout">
      <van-button round block plain type="danger" @click="logout">退出登录</van-button>
    </div>

    <p class="disclaimer">育己 · 女性全周期健康与家庭育儿支持</p>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { get } from '../api/request'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const overview = ref<{ lifeStageLabel: string; todayTip: string } | null>(null)

const maskedPhone = computed(() => {
  const p = auth.user?.phone || ''
  return p ? p.slice(0, 3) + '****' + p.slice(-4) : ''
})

function logout() {
  auth.logout()
  router.replace('/login')
}

onMounted(async () => {
  overview.value = await get<{ lifeStageLabel: string; todayTip: string }>('/wellness/overview').catch(
    () => null,
  )
})
</script>

<style scoped>
.profile-card {
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 12px 16px 0;
  padding: 20px;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 8px 24px rgba(78, 96, 73, 0.06);
}

.avatar {
  width: 54px;
  height: 54px;
  border-radius: 50%;
  background: linear-gradient(145deg, #8fb189, #6a8d66);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.info {
  flex: 1;
}

.name {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
}

.phone {
  margin: 4px 0 6px;
  color: var(--yuji-muted);
  font-size: 12px;
}

.stage {
  font-size: 10px;
  padding: 3px 9px;
  border-radius: 999px;
  background: #eef4ec;
  color: var(--yuji-primary-deep);
}

.edit {
  border: 1px solid #d7e3d3;
  background: #fff;
  border-radius: 999px;
  padding: 5px 12px;
  font-size: 11px;
  color: var(--yuji-primary-dark);
}

.tip {
  margin: 12px 22px 0;
  font-size: 11px;
  line-height: 1.7;
  color: #9a9f97;
}

.logout {
  margin: 30px 16px 0;
}

.disclaimer {
  margin: 22px 0 10px;
  text-align: center;
  font-size: 10px;
  color: #c4c4c4;
  letter-spacing: 0.5px;
}
</style>
