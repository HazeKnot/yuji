<template>
  <div class="reports">
    <van-nav-bar :title="title" left-arrow @click-left="router.back()" />

    <div class="upload">
      <van-uploader :after-read="onRead" accept="image/*,application/pdf" :max-count="1" :disabled="uploading">
        <van-button round block type="primary" plain icon="photograph" :loading="uploading">
          拍照 / 上传报告
        </van-button>
      </van-uploader>
      <p class="hint">支持 JPG、PNG、WEBP、PDF，单个不超过 10MB；只有你与授权家属可见，可随时删除。</p>
    </div>

    <van-cell-group inset>
      <van-swipe-cell v-for="r in reports" :key="r.id">
        <van-cell
          :title="r.fileName"
          :label="`${sizeText(r.size)} · ${(r.createdAt || '').replace('T', ' ').slice(0, 16)}`"
          is-link
          @click="preview(r)"
        />
        <template #right>
          <van-button square type="danger" text="删除" class="del" @click="remove(r)" />
        </template>
      </van-swipe-cell>
      <van-cell v-if="!reports.length" title="还没有报告" label="产检完拍一张，自动归档到对应产检项" />
    </van-cell-group>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showConfirmDialog, showImagePreview, showSuccessToast, showToast } from 'vant'
import { api } from '../api'

interface ReportVO {
  id: number
  fileName: string
  contentType: string
  size: number
  downloadUrl: string
  createdAt: string
}

const route = useRoute()
const router = useRouter()
const checkupId = computed(() => (route.query.checkupId ? Number(route.query.checkupId) : null))
const title = computed(() => (route.query.name ? `报告 · ${route.query.name}` : '我的报告'))

const reports = ref<ReportVO[]>([])
const uploading = ref(false)

function sizeText(size: number) {
  if (size >= 1024 * 1024) return `${(size / 1024 / 1024).toFixed(1)}MB`
  return `${Math.max(1, Math.round(size / 1024))}KB`
}

async function load() {
  const url = checkupId.value ? `/checkups/${checkupId.value}/reports` : '/reports'
  const res = await api.get<never, { data: { data: ReportVO[] } }>(url)
  reports.value = res.data.data
}

async function onRead(item: any) {
  const file: File = item.file
  if (file.size > 10 * 1024 * 1024) {
    showToast('文件不能超过 10MB')
    return
  }
  if (!checkupId.value) {
    showToast('请从产检项进入上传（一期暂不支持自由上传）')
    return
  }
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', file)
    await api.post(`/checkups/${checkupId.value}/reports`, fd)
    showSuccessToast('已上传')
    await load()
  } finally {
    uploading.value = false
  }
}

async function preview(r: ReportVO) {
  const res = await api.get<never, { data: Blob }>(`/reports/${r.id}/download`, { responseType: 'blob' })
  const url = URL.createObjectURL(res.data)
  if (r.contentType.startsWith('image/')) {
    showImagePreview({ images: [url], closeable: true })
  } else {
    window.open(url, '_blank')
  }
  // 给预览/新窗口留出加载时间再回收
  setTimeout(() => URL.revokeObjectURL(url), 60_000)
}

async function remove(r: ReportVO) {
  await showConfirmDialog({ title: '删除这份报告？', message: '文件会一并删除，不可恢复' })
  await api.delete(`/reports/${r.id}`)
  showSuccessToast('已删除')
  await load()
}

onMounted(load)
</script>

<style scoped>
.reports {
  min-height: 100vh;
  padding-bottom: 80px;
}
.upload {
  margin: 16px;
}
.hint {
  margin: 10px 4px 0;
  color: #b8b8b8;
  font-size: 11px;
  line-height: 1.7;
}
.del {
  height: 100%;
}
</style>
