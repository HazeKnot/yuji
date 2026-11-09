<template>
  <div class="login-page">
    <div class="brand">
      <h1>育己</h1>
      <p>育儿之前，先育己</p>
    </div>

    <div class="role-switch">
      <button :class="{ active: role === 'user' }" type="button" @click="switchRole('user')">用户登录</button>
      <button :class="{ active: role === 'org' }" type="button" @click="switchRole('org')">机构登录</button>
    </div>

    <van-form @submit="onSubmit">
      <van-cell-group inset>
        <van-field
          v-model="account"
          name="account"
          :label="role === 'user' ? '手机号' : '机构账号'"
          :placeholder="role === 'user' ? '请输入手机号' : '请输入机构账号'"
          :type="role === 'user' ? 'tel' : 'text'"
          :maxlength="role === 'user' ? 11 : 20"
          :rules="accountRules"
        />
        <van-field
          v-model="password"
          name="password"
          label="密码"
          placeholder="请输入密码"
          type="password"
          :rules="[{ required: true, message: '请填写密码' }]"
        />
        <van-field
          v-if="role === 'user' && mode === 'register'"
          v-model="nickname"
          name="nickname"
          label="昵称"
          placeholder="怎么称呼你（可不填）"
          maxlength="20"
        />
      </van-cell-group>

      <div class="actions">
        <van-button round block type="primary" native-type="submit" :loading="loading" color="#7a9b76">
          {{ role === 'org' ? '登录机构工作台' : mode === 'login' ? '登录' : '注册并开始' }}
        </van-button>
        <van-button v-if="role === 'user'" round block plain type="default" @click="toggleMode">
          {{ mode === 'login' ? '还没账号？去注册' : '已有账号？去登录' }}
        </van-button>
      </div>
    </van-form>

    <p v-if="role === 'org'" class="agreement">
      机构工作台只提供聚合数据与预约线索，不展示用户健康明细<br />
      演示账号：13900000001 / org123456
    </p>
    <p v-else class="agreement">登录即表示同意《用户协议》与《隐私政策》（健康数据将单独征得你的同意后使用）</p>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showFailToast, showSuccessToast } from 'vant'
import { useAuthStore } from '../stores/auth'
import { post } from '../api/request'

const router = useRouter()
const auth = useAuthStore()

const role = ref<'user' | 'org'>('user')
const mode = ref<'login' | 'register'>('login')
const account = ref('')
const password = ref('')
const nickname = ref('')
const loading = ref(false)

interface LoginResp {
  token: string
  user: { id: number; phone: string; nickname: string; role: string }
}

const accountRules = computed(() =>
  role.value === 'user'
    ? [
        { required: true, message: '请填写手机号' },
        { pattern: /^1\d{10}$/, message: '手机号格式不正确' },
      ]
    : [
        { required: true, message: '请填写机构账号' },
        { validator: (v: string) => v.trim().length >= 4, message: '机构账号至少 4 位' },
      ],
)

function switchRole(next: 'user' | 'org') {
  role.value = next
  if (next === 'org') mode.value = 'login'
}

function toggleMode() {
  mode.value = mode.value === 'login' ? 'register' : 'login'
}

// 机构端地址：默认同主机 5174 端口（部署时可整段替换为网关地址）
const ADMIN_BASE = `${location.protocol}//${location.hostname}:5174`

async function onSubmit() {
  loading.value = true
  try {
    if (role.value === 'org') {
      const data = await post<LoginResp>('/auth/login', {
        phone: account.value.trim(),
        password: password.value,
      })
      if (data.user.role !== 'ORG' && data.user.role !== 'ADMIN') {
        showFailToast('该账号不是机构账号，请切换到用户登录')
        return
      }
      localStorage.setItem('org_token', data.token)
      localStorage.setItem('org_user', JSON.stringify(data.user))
      showSuccessToast('正在进入机构工作台')
      window.location.href =
        `${ADMIN_BASE}/login?token=${encodeURIComponent(data.token)}` +
        `&nickname=${encodeURIComponent(data.user.nickname)}` +
        `&role=${data.user.role}`
      return
    }

    if (mode.value === 'login') {
      await auth.login(account.value.trim(), password.value)
    } else {
      await auth.register(account.value.trim(), password.value, nickname.value || undefined)
    }
    if (auth.user && auth.user.role !== 'MOM') {
      // 用户入口误用了机构账号：清掉会话，切到机构登录并保留账号
      auth.logout()
      showFailToast('这是机构账号，请用机构登录')
      role.value = 'org'
      mode.value = 'login'
      return
    }
    showSuccessToast(mode.value === 'login' ? '欢迎回来' : '欢迎加入育己')
    router.replace('/')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  padding: 64px 0 32px;
  background: linear-gradient(180deg, #eef3ec 0%, #faf8f5 40%);
}
.brand {
  text-align: center;
  margin-bottom: 48px;
}
.brand h1 {
  font-size: 40px;
  letter-spacing: 12px;
  margin: 0;
  color: #4a6b47;
}
.brand p {
  color: #8a9a87;
  margin-top: 8px;
}
.role-switch {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-bottom: 24px;
}
.role-switch button {
  border: 1px solid #c8d5c5;
  background: #fff;
  color: #6b7f68;
  border-radius: 999px;
  padding: 8px 24px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}
.role-switch button.active {
  background: #7a9b76;
  border-color: #7a9b76;
  color: #fff;
}
.actions {
  margin: 24px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.agreement {
  text-align: center;
  font-size: 12px;
  color: #b0b0b0;
  padding: 0 32px;
}
</style>
