<template>
  <div class="forum-page">
    <div class="forum-bg forum-bg-left" />
    <div class="forum-bg forum-bg-right" />

    <div class="forum-container">
      <section class="forum-hero">
        <div>
          <p class="hero-tag">Animal Succour Community</p>
          <h2>社区论坛</h2>
          <p class="hero-subtitle">分享救助故事、交流领养经验，让每一次善意都被看见。</p>
        </div>
        <div class="hero-stats">
          <div class="stat-item">
            <div class="stat-value">{{ posts.length }}</div>
            <div class="stat-label">帖子总数</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ totalComments }}</div>
            <div class="stat-label">评论互动</div>
          </div>
        </div>
      </section>

      <el-card class="post-editor" shadow="never">
        <template #header>
          <div class="editor-title">
            <span>发布新帖子</span>
            <el-tag type="success" effect="plain">支持图文分享</el-tag>
          </div>
        </template>

        <el-input
          v-model="newPost.title"
          placeholder="请输入帖子标题"
          maxlength="60"
          show-word-limit
          class="editor-input"
        />
        <el-input
          v-model="newPost.content"
          type="textarea"
          :rows="5"
          maxlength="1000"
          show-word-limit
          placeholder="说说你的故事、问题或经验..."
          class="editor-input"
        />
        <div class="image-editor">
          <span class="upload-label">上传配图（最多 6 张）</span>
          <image-upload v-model="newPost.images" :limit="6" />
        </div>
        <div class="editor-action">
          <el-button type="primary" @click="createPost">发布帖子</el-button>
        </div>
      </el-card>

      <div v-if="posts.length" class="post-list">
        <el-card v-for="post in posts" :key="post.id" class="post-item" shadow="hover">
          <div class="post-top">
            <div>
              <h3>{{ post.title }}</h3>
              <div class="post-meta">{{ post.authorName }} · {{ formatTime(post.createdAt) }}</div>
            </div>
            <el-button text type="primary" class="like-btn" @click="toggleLikePost(post)">
              {{ hasLikedPost(post) ? '已点赞' : '点赞' }} ({{ post.likes || 0 }})
            </el-button>
          </div>

          <div class="post-content">{{ post.content }}</div>

          <div v-if="post.images?.length" class="post-gallery">
            <el-image
              v-for="(img, index) in post.images"
              :key="`${post.id}_${index}`"
              :src="getImgUrl(img)"
              :preview-src-list="post.images.map(item => getImgUrl(item))"
              fit="cover"
              preview-teleported
            />
          </div>

          <div class="comment-block">
            <div class="comment-title">评论区 ({{ post.comments?.length || 0 }})</div>
            <div v-if="post.comments?.length" class="comment-list">
              <div v-for="comment in post.comments" :key="comment.id" class="comment-item">
                <div>
                  <div class="comment-user">{{ comment.userName }}</div>
                  <div class="comment-text">{{ comment.content }}</div>
                  <div class="comment-time">{{ formatTime(comment.createdAt) }}</div>
                </div>
                <el-button text type="primary" @click="toggleLikeComment(comment)">
                  {{ hasLikedComment(comment) ? '已点赞' : '点赞' }} ({{ comment.likes || 0 }})
                </el-button>
              </div>
            </div>
            <el-empty v-else description="暂无评论" :image-size="80" />

            <div class="comment-editor">
              <el-input
                v-model="commentDrafts[post.id]"
                placeholder="写下你的评论..."
                maxlength="500"
                show-word-limit
              />
              <el-button type="primary" @click="addComment(post)">发送</el-button>
            </div>
          </div>
        </el-card>
      </div>

      <el-empty v-else description="还没有帖子，快来发布第一条吧" />
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import ImageUpload from '@/components/ImageUpload'
import useUserStore from '@/store/modules/user'
import { getUser } from '@/api/system/user'

const STORAGE_KEY = 'succour_forum_posts'
const BASE_URL = import.meta.env.VITE_APP_BASE_API

const userStore = useUserStore()
const userInfo = reactive({
  id: '',
  nickName: ''
})

const posts = ref([])
const commentDrafts = reactive({})
const newPost = reactive({
  title: '',
  content: '',
  images: ''
})

const totalComments = computed(() => posts.value.reduce((total, post) => total + (post.comments?.length || 0), 0))

const normalizePost = (post) => {
  const images = Array.isArray(post.images)
    ? post.images
    : typeof post.images === 'string' && post.images
      ? post.images.split(',').filter(Boolean)
      : []
  return {
    ...post,
    likes: post.likes || 0,
    likedUserIds: Array.isArray(post.likedUserIds) ? post.likedUserIds : [],
    comments: Array.isArray(post.comments) ? post.comments : [],
    images
  }
}

const loadPosts = () => {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) {
    posts.value = []
    return
  }
  try {
    const parsed = JSON.parse(raw)
    posts.value = Array.isArray(parsed) ? parsed.map(item => normalizePost(item)) : []
  } catch (e) {
    posts.value = []
  }
}

const savePosts = () => {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(posts.value))
}

const getCurrentUser = async () => {
  const id = userStore.id
  if (!id) {
    await userStore.getInfo()
  }
  const userId = userStore.id
  userInfo.id = String(userId)
  const res = await getUser(userId)
  userInfo.nickName = res?.data?.nickName || userStore.name || '匿名用户'
}

const createPost = () => {
  if (!newPost.title.trim()) {
    ElMessage.warning('请输入帖子标题')
    return
  }
  if (!newPost.content.trim()) {
    ElMessage.warning('请输入帖子内容')
    return
  }

  posts.value.unshift(normalizePost({
    id: `${Date.now()}_${Math.random().toString(16).slice(2)}`,
    title: newPost.title.trim(),
    content: newPost.content.trim(),
    images: newPost.images,
    authorId: userInfo.id,
    authorName: userInfo.nickName,
    likes: 0,
    likedUserIds: [],
    comments: [],
    createdAt: Date.now()
  }))

  newPost.title = ''
  newPost.content = ''
  newPost.images = ''
  savePosts()
  ElMessage.success('发布成功')
}

const hasLikedPost = (post) => post.likedUserIds?.includes(userInfo.id)

const toggleLikePost = (post) => {
  if (!post.likedUserIds) post.likedUserIds = []
  const index = post.likedUserIds.indexOf(userInfo.id)
  if (index > -1) {
    post.likedUserIds.splice(index, 1)
    post.likes = Math.max((post.likes || 1) - 1, 0)
  } else {
    post.likedUserIds.push(userInfo.id)
    post.likes = (post.likes || 0) + 1
  }
  savePosts()
}

const addComment = (post) => {
  const value = (commentDrafts[post.id] || '').trim()
  if (!value) {
    ElMessage.warning('请输入评论内容')
    return
  }
  if (!post.comments) {
    post.comments = []
  }
  post.comments.push({
    id: `${Date.now()}_${Math.random().toString(16).slice(2)}`,
    content: value,
    userId: userInfo.id,
    userName: userInfo.nickName,
    likes: 0,
    likedUserIds: [],
    createdAt: Date.now()
  })
  commentDrafts[post.id] = ''
  savePosts()
  ElMessage.success('评论成功')
}

const hasLikedComment = (comment) => comment.likedUserIds?.includes(userInfo.id)

const toggleLikeComment = (comment) => {
  if (!comment.likedUserIds) comment.likedUserIds = []
  const index = comment.likedUserIds.indexOf(userInfo.id)
  if (index > -1) {
    comment.likedUserIds.splice(index, 1)
    comment.likes = Math.max((comment.likes || 1) - 1, 0)
  } else {
    comment.likedUserIds.push(userInfo.id)
    comment.likes = (comment.likes || 0) + 1
  }
  savePosts()
}

const getImgUrl = (img) => {
  if (!img) return ''
  if (/^(https?:|blob:|data:)/.test(img)) {
    return img
  }
  return `${BASE_URL}${img}`
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const y = date.getFullYear()
  const m = `${date.getMonth() + 1}`.padStart(2, '0')
  const d = `${date.getDate()}`.padStart(2, '0')
  const h = `${date.getHours()}`.padStart(2, '0')
  const mm = `${date.getMinutes()}`.padStart(2, '0')
  return `${y}-${m}-${d} ${h}:${mm}`
}

onMounted(async () => {
  await getCurrentUser()
  loadPosts()
})
</script>

<style scoped lang="scss">
.forum-page {
  position: relative;
  overflow: hidden;
  min-height: calc(100vh - 70px);
  padding: 24px 0 36px;
  background: radial-gradient(circle at 20% 20%, #e8f4ff 0%, #f5f7fb 45%, #eef3ff 100%);
}

.forum-bg {
  position: absolute;
  width: 420px;
  height: 420px;
  border-radius: 50%;
  filter: blur(40px);
  pointer-events: none;
  opacity: 0.35;
}

.forum-bg-left {
  top: -120px;
  left: -90px;
  background: linear-gradient(135deg, #5b8cff, #50e3c2);
}

.forum-bg-right {
  right: -120px;
  bottom: -140px;
  background: linear-gradient(135deg, #7f56d9, #54d1db);
}

.forum-container {
  position: relative;
  z-index: 1;
  width: 1200px;
  max-width: 94%;
  margin: 0 auto;
}

.forum-hero {
  margin-bottom: 18px;
  padding: 22px 26px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 16px 45px rgba(59, 89, 152, 0.12);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;

  h2 {
    margin: 4px 0 8px;
    font-size: 30px;
    color: #233257;
  }
}

.hero-tag {
  margin: 0;
  font-size: 12px;
  letter-spacing: 1.6px;
  color: #6e7f9e;
}

.hero-subtitle {
  margin: 0;
  color: #6f7f99;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.stat-item {
  min-width: 110px;
  padding: 12px;
  border-radius: 14px;
  background: linear-gradient(160deg, #edf3ff, #ffffff);
  text-align: center;

  .stat-value {
    font-size: 22px;
    font-weight: 700;
    color: #2b4ca8;
  }

  .stat-label {
    margin-top: 3px;
    font-size: 12px;
    color: #6f7f99;
  }
}

.post-editor {
  margin-bottom: 18px;
  border-radius: 18px;
  border: none;
  box-shadow: 0 12px 30px rgba(30, 42, 65, 0.09);
}

.editor-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 17px;
  font-weight: 700;
  color: #263a60;
}

.editor-input {
  margin-bottom: 14px;
}

.image-editor {
  margin-bottom: 10px;

  .upload-label {
    display: inline-block;
    margin-bottom: 8px;
    color: #5d6d88;
    font-size: 13px;
  }
}

.editor-action {
  display: flex;
  justify-content: flex-end;
}

.post-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.post-item {
  border: none;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 10px 24px rgba(44, 61, 95, 0.12);
  transition: transform 0.25s ease, box-shadow 0.25s ease;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 16px 35px rgba(44, 61, 95, 0.18);
  }
}

.post-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;

  h3 {
    margin: 0;
    color: #233257;
    font-size: 19px;
  }
}

.like-btn {
  align-self: flex-start;
}

.post-meta {
  margin-top: 8px;
  color: #8a96ab;
  font-size: 13px;
}

.post-content {
  margin: 14px 0;
  line-height: 1.75;
  color: #2f3a4c;
  white-space: pre-wrap;
}

.post-gallery {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 14px;

  .el-image {
    width: 100%;
    height: 120px;
    border-radius: 10px;
    overflow: hidden;
  }
}

.comment-block {
  padding: 12px;
  background: #f8faff;
  border-radius: 12px;
}

.comment-title {
  font-size: 13px;
  color: #667795;
  margin-bottom: 10px;
}

.comment-list {
  max-height: 240px;
  overflow-y: auto;
}

.comment-item {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px dashed #e4eaf6;

  &:last-child {
    border-bottom: 0;
  }
}

.comment-user {
  font-weight: 700;
  color: #2f436d;
}

.comment-text {
  margin: 3px 0;
  color: #39465c;
}

.comment-time {
  font-size: 12px;
  color: #9ba7bd;
}

.comment-editor {
  margin-top: 10px;
  display: flex;
  gap: 8px;
}

@media (max-width: 992px) {
  .post-list {
    grid-template-columns: 1fr;
  }

  .forum-hero {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 768px) {
  .post-gallery {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
