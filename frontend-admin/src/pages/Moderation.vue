<template>
  <div>
    <div class="toolbar">
      <div class="filters">
        <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 150px" @change="reload">
          <el-option label="已发布" value="PUBLISHED" />
          <el-option label="待审核" value="PENDING" />
          <el-option label="已隐藏" value="HIDDEN" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜索标题" clearable style="width: 200px" />
      </div>
      <span class="count">共 {{ total }} 篇</span>
    </div>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="含危机词或导流内容的帖子会自动进入待审核。"
      description="审核时优先保护求助者：情绪危机帖不直接删除，转为可跟进；广告导流帖直接隐藏。"
      class="notice"
    />

    <el-table :data="filtered" v-loading="loading" stripe>
      <el-table-column label="分类" width="110">
        <template #default="{ row }">
          <el-tag size="small" effect="plain">{{ row.categoryLabel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="标题 / 摘要" min-width="320">
        <template #default="{ row }">
          <p class="title" @click="preview(row)">{{ row.title }}</p>
          <p class="sub">{{ row.content.slice(0, 70) }}…</p>
        </template>
      </el-table-column>
      <el-table-column prop="authorName" label="作者" width="120" />
      <el-table-column label="互动" width="120">
        <template #default="{ row }">{{ row.likeCount }} 赞 / {{ row.commentCount }} 评</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="timeText" label="时间" width="120" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status !== 'PUBLISHED'" link type="primary" @click="setStatus(row, 'PUBLISHED')">
            通过
          </el-button>
          <el-button v-if="row.status !== 'HIDDEN'" link type="danger" @click="setStatus(row, 'HIDDEN')">
            隐藏
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next"
        @current-change="load"
      />
    </div>

    <el-dialog v-model="dialog" title="帖子内容" width="560px">
      <template v-if="active">
        <h3 class="preview-title">{{ active.title }}</h3>
        <p class="preview-meta">{{ active.authorName }} · {{ active.categoryLabel }} · {{ active.timeText }}</p>
        <p class="preview-content">{{ active.content }}</p>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getData, putData } from '../api'

interface PostVO {
  id: number
  categoryLabel: string
  title: string
  content: string
  authorName: string
  likeCount: number
  commentCount: number
  status: string
  timeText: string
}

interface PageVO<T> {
  records: T[]
  total: number
  page: number
  size: number
  hasMore: boolean
}

const posts = ref<PostVO[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const statusFilter = ref('PENDING')
const keyword = ref('')
const dialog = ref(false)
const active = ref<PostVO | null>(null)

const filtered = computed(() => {
  const kw = keyword.value.trim()
  return kw ? posts.value.filter((p) => p.title.includes(kw)) : posts.value
})

function statusLabel(status: string) {
  return { PUBLISHED: '已发布', PENDING: '待审核', HIDDEN: '已隐藏' }[status] || status
}

function statusType(status: string) {
  return status === 'PUBLISHED' ? 'success' : status === 'PENDING' ? 'warning' : 'info'
}

async function load() {
  loading.value = true
  try {
    const data = await getData<PageVO<PostVO>>('/admin/wellness/posts', {
      status: statusFilter.value || undefined,
      page: page.value,
      size: size.value,
    })
    posts.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function reload() {
  page.value = 1
  load()
}

function preview(row: PostVO) {
  active.value = row
  dialog.value = true
}

async function setStatus(row: PostVO, status: string) {
  if (status === 'HIDDEN') {
    await ElMessageBox.confirm('隐藏后用户端将不可见，作者仍可在自己的记录中看到。', '确认隐藏', {
      type: 'warning',
    })
  }
  await putData(`/admin/wellness/posts/${row.id}`, { status })
  ElMessage.success('已更新')
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
}
.filters {
  display: flex;
  gap: 10px;
}
.count {
  font-size: 12px;
  color: #b3b3b3;
}
.notice {
  margin-bottom: 16px;
  border-radius: 10px;
}
.title {
  margin: 0 0 4px;
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  cursor: pointer;
}
.sub {
  margin: 0;
  font-size: 11px;
  color: #b3b3b3;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.preview-title {
  margin: 0 0 6px;
  font-size: 18px;
}
.preview-meta {
  margin: 0 0 14px;
  font-size: 11px;
  color: #b3b3b3;
}
.preview-content {
  margin: 0;
  font-size: 13px;
  line-height: 1.9;
  color: #4a5048;
  white-space: pre-wrap;
}
</style>
