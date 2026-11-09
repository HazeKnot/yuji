<template>
  <div class="detail-page">
    <van-nav-bar title="帖子" left-arrow @click-left="$router.back()" />

    <template v-if="post">
      <article class="post">
        <div class="post-head">
          <span class="pill pink">{{ post.categoryLabel }}</span>
          <span class="time">{{ post.timeText }}</span>
        </div>
        <h1>{{ post.title }}</h1>
        <p class="author">{{ post.authorName }}</p>
        <div class="content">
          <p v-for="(line, i) in post.content.split('\n')" :key="i">{{ line }}</p>
        </div>

        <div class="post-ops">
          <button class="op" :class="{ liked: post.liked }" @click="like">
            <van-icon :name="post.liked ? 'like' : 'like-o'" size="18" />
            {{ post.liked ? '已抱抱' : '抱抱她' }} · {{ post.likeCount }}
          </button>
          <span class="op"><van-icon name="comment-o" size="16" /> {{ post.commentCount }}</span>
        </div>
      </article>

      <div class="section-title" style="margin-top: 20px">
        <h3>评论 {{ comments.length }}</h3>
      </div>

      <div class="comments">
        <div v-for="c in comments" :key="c.id" class="comment">
          <p class="c-author">{{ c.authorName }}<span>{{ c.timeText }}</span></p>
          <p class="c-content">{{ c.content }}</p>
        </div>
        <p v-if="!comments.length" class="empty">还没有评论。说一句“我也是”，对她就是支持。</p>
      </div>

      <div class="composer">
        <van-field
          v-model="draft"
          rows="1"
          autosize
          type="textarea"
          maxlength="1000"
          placeholder="友善一点，她可能正在很难的时候"
          class="input"
        />
        <button class="anon-btn" :class="{ on: anonymous }" @click="anonymous = !anonymous">匿名</button>
        <van-button
          round
          size="small"
          color="#7a9b76"
          :loading="sending"
          :disabled="!draft.trim()"
          @click="send"
        >
          发送
        </van-button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { showSuccessToast } from 'vant'
import { get, post as create } from '../api/request'

interface PostVO {
  id: number
  title: string
  content: string
  categoryLabel: string
  authorName: string
  liked: boolean
  likeCount: number
  commentCount: number
  timeText: string
}

interface CommentVO {
  id: number
  authorName: string
  content: string
  timeText: string
}

const route = useRoute()
const id = Number(route.params.id)
const post = ref<PostVO | null>(null)
const comments = ref<CommentVO[]>([])
const draft = ref('')
const anonymous = ref(false)
const sending = ref(false)

async function load() {
  const [p, c] = await Promise.all([
    get<PostVO>(`/wellness/community/posts/${id}`),
    get<CommentVO[]>(`/wellness/community/posts/${id}/comments`),
  ])
  post.value = p
  comments.value = c
}

async function like() {
  if (!post.value) return
  const res = await create<{ liked: boolean; likeCount: number }>(
    `/wellness/community/posts/${id}/like`,
  )
  post.value.liked = res.liked
  post.value.likeCount = res.likeCount
}

async function send() {
  if (!post.value || !draft.value.trim()) return
  sending.value = true
  try {
    const c = await create<CommentVO>(`/wellness/community/posts/${id}/comments`, {
      content: draft.value.trim(),
      anonymous: anonymous.value,
    })
    comments.value.push(c)
    post.value.commentCount += 1
    draft.value = ''
    showSuccessToast('已回复')
  } finally {
    sending.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  padding-bottom: 90px;
}

.post {
  margin: 12px 16px 0;
  padding: 20px;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 8px 24px rgba(78, 96, 73, 0.06);
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

.post h1 {
  margin: 14px 0 8px;
  font-size: 20px;
  line-height: 1.5;
  letter-spacing: 0.2px;
}

.author {
  margin: 0 0 16px;
  font-size: 11px;
  color: #9a9f97;
}

.content {
  font-size: 14px;
  line-height: 1.95;
  color: #4a5048;
}

.content p {
  margin: 0 0 10px;
  white-space: pre-wrap;
}

.post-ops {
  display: flex;
  gap: 22px;
  align-items: center;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid var(--yuji-line);
}

.op {
  border: none;
  background: transparent;
  font-size: 12px;
  color: #9a9f97;
  display: flex;
  align-items: center;
  gap: 5px;
}

.op.liked {
  color: #c98b93;
}

.comments {
  margin: 0 16px;
}

.comment {
  background: #fff;
  border-radius: 14px;
  padding: 14px;
  margin-bottom: 10px;
}

.c-author {
  margin: 0 0 6px;
  font-size: 11px;
  color: var(--yuji-primary-dark);
  display: flex;
  justify-content: space-between;
}

.c-author span {
  color: #b3b3b3;
}

.c-content {
  margin: 0;
  font-size: 13px;
  line-height: 1.8;
  color: #4a5048;
  white-space: pre-wrap;
}

.empty {
  text-align: center;
  font-size: 12px;
  color: #b3b3b3;
  padding: 30px 10px;
  line-height: 1.7;
}

.composer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: flex-end;
  gap: 8px;
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

.anon-btn {
  border: none;
  background: transparent;
  font-size: 11px;
  color: #9a9f97;
  padding: 8px 2px;
}

.anon-btn.on {
  color: var(--yuji-primary-dark);
  font-weight: 600;
}
</style>
