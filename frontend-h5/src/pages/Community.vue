<template>
  <div class="page community">
    <van-nav-bar title="她社区">
      <template #right>
        <span class="write" @click="openForm">写一篇</span>
      </template>
    </van-nav-bar>

    <div class="banner">
      <p>身体的问题不需要羞耻，经验也不必一个人摸索。</p>
      <span>匿名可选 · 内容人工审核</span>
    </div>

    <van-tabs v-model:active="activeCategory" class="cats" @change="reload">
      <van-tab v-for="c in categories" :key="c.key" :title="c.label" :name="c.key" />
    </van-tabs>

    <div class="list">
      <article v-for="post in posts" :key="post.id" class="post" @click="$router.push(`/community/${post.id}`)">
        <div class="post-head">
          <span class="pill pink">{{ post.categoryLabel }}</span>
          <span class="time">{{ post.timeText }}</span>
        </div>
        <h3>{{ post.title }}</h3>
        <p class="content">{{ post.content }}</p>
        <div class="post-foot">
          <span class="author">{{ post.authorName }}</span>
          <div class="ops">
            <button class="op" :class="{ liked: post.liked }" @click.stop="like(post)">
              <van-icon :name="post.liked ? 'like' : 'like-o'" />
              {{ post.likeCount }}
            </button>
            <span class="op"><van-icon name="comment-o" /> {{ post.commentCount }}</span>
          </div>
        </div>
      </article>

      <div v-if="!posts.length && !loading" class="empty">
        <p>这里还很安静。</p>
        <p class="muted">你可以是第一个开口的人。</p>
      </div>

      <div v-if="hasMore" class="more-wrap">
        <van-button size="small" round plain color="#7a9b76" :loading="loading" @click="loadMore">
          加载更多
        </van-button>
      </div>
      <p v-else-if="posts.length" class="end">没有更多了</p>
    </div>

    <!-- 发帖 -->
    <van-popup v-model:show="showForm" position="bottom" round>
      <div class="form">
        <div class="form-head">
          <h3>写点什么</h3>
          <van-icon name="cross" @click="showForm = false" />
        </div>

        <p class="form-label">发到哪个分类</p>
        <div class="cat-picker">
          <button
            v-for="c in categories.slice(1)"
            :key="c.key"
            :class="{ active: form.category === c.key }"
            @click="form.category = c.key"
          >
            {{ c.label }}
          </button>
        </div>

        <van-field v-model="form.title" label="标题" maxlength="80" placeholder="一句话说清你想聊什么" />
        <van-field
          v-model="form.content"
          rows="6"
          autosize
          type="textarea"
          maxlength="2000"
          show-word-limit
          placeholder="可以写具体经历：发生了什么、试过什么、后来怎么样。不要留联系方式。"
        />

        <div class="anon">
          <div>
            <p>匿名发布</p>
            <span class="muted">开启后不展示昵称，也不出现在个人主页</span>
          </div>
          <van-switch v-model="form.anonymous" size="22" active-color="#7a9b76" />
        </div>

        <p class="form-note">涉及自伤、用药交易、导流的内容会进入人工审核。</p>

        <van-button round block color="#7a9b76" :loading="saving" @click="submit">发布</van-button>
      </div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { showSuccessToast, showToast } from 'vant'
import { get, post } from '../api/request'

interface PostVO {
  id: number
  category: string
  categoryLabel: string
  title: string
  content: string
  authorName: string
  anonymous: boolean
  likeCount: number
  commentCount: number
  liked: boolean
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

const categories = [
  { key: 'ALL', label: '全部' },
  { key: 'EXPERIENCE', label: '经验' },
  { key: 'SECRET', label: '难言之隐' },
  { key: 'CONFINEMENT', label: '月子' },
  { key: 'PARENTING', label: '育儿' },
  { key: 'MOOD', label: '树洞' },
]

const activeCategory = ref('ALL')
const posts = ref<PostVO[]>([])
const page = ref(1)
const hasMore = ref(false)
const loading = ref(false)
const showForm = ref(false)
const saving = ref(false)

const form = reactive({
  category: 'EXPERIENCE',
  title: '',
  content: '',
  anonymous: false,
})

async function load(reset = false) {
  loading.value = true
  try {
    if (reset) {
      page.value = 1
      posts.value = []
    }
    const data = await get<PageVO<PostVO>>('/wellness/community/posts', {
      params: {
        page: page.value,
        size: 10,
        category: activeCategory.value === 'ALL' ? undefined : activeCategory.value,
      },
    })
    posts.value = reset ? data.records : [...posts.value, ...data.records]
    hasMore.value = data.hasMore
  } finally {
    loading.value = false
  }
}

function reload() {
  load(true)
}

function loadMore() {
  page.value += 1
  load()
}

async function like(item: PostVO) {
  const res = await post<{ liked: boolean; likeCount: number }>(
    `/wellness/community/posts/${item.id}/like`,
  )
  item.liked = res.liked
  item.likeCount = res.likeCount
}

function openForm() {
  form.category = 'EXPERIENCE'
  form.title = ''
  form.content = ''
  form.anonymous = false
  showForm.value = true
}

async function submit() {
  if (form.title.trim().length < 4) {
    showToast('标题至少 4 个字')
    return
  }
  if (form.content.trim().length < 10) {
    showToast('内容至少 10 个字')
    return
  }
  saving.value = true
  try {
    const created = await post<PostVO>('/wellness/community/posts', {
      category: form.category,
      title: form.title.trim(),
      content: form.content.trim(),
      anonymous: form.anonymous,
    })
    showForm.value = false
    if (created.status === 'PENDING') {
      showToast('已提交，审核通过后展示')
    } else {
      showSuccessToast('发布成功')
      reload()
    }
  } finally {
    saving.value = false
  }
}

onMounted(() => load(true))
</script>

<style scoped>
.banner {
  margin: 10px 16px 0;
  padding: 16px 18px;
  border-radius: 16px;
  background: linear-gradient(140deg, #f7ecee, #f3e4e7);
}

.banner p {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  color: #7c4c54;
}

.banner span {
  display: block;
  margin-top: 6px;
  font-size: 10px;
  color: #a8838a;
}

.write {
  font-size: 12px;
  color: var(--yuji-primary-dark);
}

.cats {
  margin-top: 10px;
}

.list {
  padding: 6px 16px 20px;
}

.post {
  background: #fff;
  border-radius: 18px;
  padding: 16px;
  margin-top: 12px;
  box-shadow: 0 6px 18px rgba(78, 96, 73, 0.05);
}

.post-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.time {
  font-size: 10px;
  color: #b3b3b3;
}

.post h3 {
  margin: 10px 0 6px;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.5;
  color: var(--yuji-ink);
}

.content {
  margin: 0;
  font-size: 12px;
  line-height: 1.75;
  color: #6f756d;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}

.author {
  font-size: 11px;
  color: #9a9f97;
}

.ops {
  display: flex;
  gap: 14px;
  align-items: center;
}

.op {
  border: none;
  background: transparent;
  font-size: 11px;
  color: #9a9f97;
  display: flex;
  align-items: center;
  gap: 4px;
}

.op.liked {
  color: #c98b93;
}

.empty {
  text-align: center;
  padding: 60px 0;
}

.empty p {
  margin: 0 0 6px;
  font-size: 14px;
}

.more-wrap {
  text-align: center;
  margin-top: 18px;
}

.end {
  text-align: center;
  font-size: 10px;
  color: #c4c4c4;
  margin-top: 18px;
}

.form {
  padding: 20px 16px 30px;
}

.form-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.form-head h3 {
  margin: 0;
  font-size: 16px;
}

.form-label {
  margin: 0 16px 8px;
  font-size: 12px;
  color: var(--yuji-muted);
}

.cat-picker {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 0 16px 12px;
}

.cat-picker button {
  border: 1px solid #e8eae4;
  background: #fff;
  border-radius: 999px;
  padding: 6px 12px;
  font-size: 12px;
  color: #6f756d;
}

.cat-picker button.active {
  background: #eef4ec;
  border-color: #b9ceb4;
  color: var(--yuji-primary-deep);
}

.anon {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
}

.anon p {
  margin: 0 0 4px;
  font-size: 13px;
}

.form-note {
  margin: 4px 16px 18px;
  font-size: 10px;
  color: #b3b3b3;
  line-height: 1.6;
}
</style>
