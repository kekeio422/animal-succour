<template>
  <div class="forum-page">
    <div class="forum-bg forum-bg-left" />
    <div class="forum-bg forum-bg-right" />
    <div class="forum-grid" />

    <div class="forum-container">
      <section class="forum-hero">
        <div class="hero-copy">
          <p class="hero-tag">Animal Succour Community</p>
          <h2>社区论坛</h2>
          <p class="hero-subtitle">
            记录救助瞬间，分享养护经验，也给正在坚持的人一个回应。
          </p>
          <div class="hero-actions">
            <el-button type="primary" size="large" round @click="openCreateDialog">
              发布帖子
            </el-button>
            <el-button plain size="large" round @click="scrollToPosts">
              浏览帖子
            </el-button>
          </div>
        </div>

        <div class="hero-panel">
          <div class="panel-title">社区速览</div>
          <div class="panel-stats">
            <div class="stat-item">
              <span class="stat-value">{{ posts.length }}</span>
              <span class="stat-label">帖子总数</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ totalComments }}</span>
              <span class="stat-label">评论互动</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ totalImages }}</span>
              <span class="stat-label">图文分享</span>
            </div>
          </div>
          <div class="panel-tip">点击右下角加号，也可以随时发帖。</div>
        </div>
      </section>

      <section ref="postSectionRef" class="forum-feed" v-loading="loading">
        <div class="feed-header">
          <div>
            <div class="feed-title">最新动态</div>
          </div>

        </div>

        <div v-if="posts.length" class="post-list">
          <article v-for="post in posts" :key="post.id" class="post-card">
            <div class="post-card-top">
              <div class="post-head">
                <div class="author-avatar">
                  {{ (post.authorName || '匿').slice(0, 1) }}
                </div>
                <div>
                  <h3>{{ post.title }}</h3>
                  <div class="post-meta">
                    <span>{{ post.authorName }}</span>
                    <span>发布于 {{ formatTime(post.createTime) }}</span>
                  </div>
                </div>
              </div>

              <div class="post-action-group">
                <el-button v-if="isAuthor(post)" text type="primary" @click="openEditDialog(post)">编辑</el-button>
                <el-button v-if="canDeletePost(post)" text type="danger" @click="handleDeletePost(post)">删除</el-button>
                <el-button text type="primary" class="like-btn" @click="toggleLikePost(post)">
                  {{ hasLikedPost(post) ? '已点赞' : '点赞' }} {{ post.likes || 0 }}
                </el-button>
              </div>
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
              <div class="comment-header">
                <div class="comment-title">评论区</div>
                <div class="comment-count">{{ post.comments?.length || 0 }} 条评论</div>
              </div>

              <div v-if="post.comments?.length" class="comment-list">
                <div v-for="comment in post.comments" :key="comment.id" class="comment-item">
                  <div class="comment-main">
                    <div class="comment-top">
                      <div class="comment-user">{{ comment.userName }}</div>
                      <div class="comment-time">{{ formatTime(comment.createdAt) }}</div>
                    </div>
                    <div class="comment-text">{{ comment.content }}</div>
                  </div>
                  <div class="comment-actions">
                    <el-button text type="primary" @click="toggleLikeComment(post, comment)">
                      {{ hasLikedComment(comment) ? '已点赞' : '点赞' }} {{ comment.likes || 0 }}
                    </el-button>
                    <el-button
                      v-if="canDeleteComment(post, comment)"
                      text
                      type="danger"
                      @click="handleDeleteComment(post, comment)"
                    >
                      删除
                    </el-button>
                  </div>
                </div>
              </div>
              <el-empty v-else description="还没有评论，来留下第一句吧" :image-size="76" />

              <div class="comment-editor">
                <el-input
                  v-model="commentDrafts[post.id]"
                  placeholder="写下你的评论..."
                  maxlength="500"
                  show-word-limit
                />
                <el-button type="primary" round @click="addComment(post)">发送</el-button>
              </div>
            </div>
          </article>
        </div>

        <div v-else class="empty-shell">
          <el-empty description="还没有帖子，点击按钮发布第一条内容" />
          <el-button type="primary" round @click="openCreateDialog">立即发帖</el-button>
        </div>
      </section>
    </div>

    <button class="floating-create" type="button" @click="openCreateDialog" aria-label="发布帖子">
      +
    </button>

    <el-dialog
      v-model="createDialogVisible"
      title="发布帖子"
      width="720px"
      destroy-on-close
      class="forum-dialog"
    >
      <el-form label-width="84px">
        <el-form-item label="标题">
          <el-input v-model="newPost.title" maxlength="60" show-word-limit placeholder="请输入帖子标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="newPost.content"
            type="textarea"
            :rows="7"
            maxlength="1000"
            show-word-limit
            placeholder="分享救助故事、求助信息或养护经验..."
          />
        </el-form-item>
        <el-form-item label="图片">
          <image-upload v-model="newPost.images" :limit="6" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="createPost">发布</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="editDialogVisible"
      title="修改帖子"
      width="720px"
      destroy-on-close
      class="forum-dialog"
    >
      <el-form label-width="84px">
        <el-form-item label="标题">
          <el-input v-model="editForm.title" maxlength="60" show-word-limit />
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="editForm.content"
            type="textarea"
            :rows="7"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="图片">
          <image-upload v-model="editForm.images" :limit="6" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitEditPost">保存修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ImageUpload from '@/components/ImageUpload'
import useUserStore from '@/store/modules/user'
import { getUser } from '@/api/system/user'
import {
  addForumComment,
  addForumPost,
  delForumComment,
  delForumPost,
  likeForumComment,
  likeForumPost,
  listForumPost,
  updateForumPost
} from '@/api/succour/forum'

const BASE_URL = import.meta.env.VITE_APP_BASE_API

const userStore = useUserStore()
const loading = ref(false)
const submitting = ref(false)
const posts = ref([])
const commentDrafts = reactive({})
const createDialogVisible = ref(false)
const editDialogVisible = ref(false)
const postSectionRef = ref()

const userInfo = reactive({
  id: '',
  nickName: ''
})

const newPost = reactive({
  title: '',
  content: '',
  images: ''
})

const editForm = reactive({
  id: '',
  title: '',
  content: '',
  images: ''
})

const totalComments = computed(() => posts.value.reduce((total, post) => total + (post.comments?.length || 0), 0))
const totalImages = computed(() => posts.value.reduce((total, post) => total + (post.images?.length || 0), 0))
const isAdmin = computed(() => String(userInfo.id) === '1')

const parseImageList = (images) => {
  if (Array.isArray(images)) {
    return images.filter(Boolean)
  }
  if (typeof images === 'string' && images.trim()) {
    return images.split(',').map(item => item.trim()).filter(Boolean)
  }
  return []
}

const parseLikedUserIds = (likedUserIds) => {
  if (Array.isArray(likedUserIds)) {
    return likedUserIds.map(item => String(item))
  }
  if (typeof likedUserIds === 'string' && likedUserIds.trim()) {
    return likedUserIds.split(',').map(item => item.trim()).filter(Boolean)
  }
  return []
}

const normalizeComment = (comment) => ({
  ...comment,
  userId: comment?.userId == null ? '' : String(comment.userId),
  likes: Number(comment?.likes || 0),
  likedUserIds: parseLikedUserIds(comment?.likedUserIds)
})

const parseComments = (comments) => {
  if (Array.isArray(comments)) {
    return comments.map(item => normalizeComment(item))
  }
  if (typeof comments === 'string' && comments.trim()) {
    try {
      const parsed = JSON.parse(comments)
      return Array.isArray(parsed) ? parsed.map(item => normalizeComment(item)) : []
    } catch (error) {
      return []
    }
  }
  return []
}

const normalizePost = (post) => ({
  ...post,
  authorId: post?.authorId == null ? '' : String(post.authorId),
  likes: Number(post?.likes || 0),
  images: parseImageList(post?.images),
  likedUserIds: parseLikedUserIds(post?.likedUserIds),
  comments: parseComments(post?.comments)
})

const loadPosts = async () => {
  loading.value = true
  try {
    const res = await listForumPost()
    posts.value = Array.isArray(res?.rows) ? res.rows.map(item => normalizePost(item)) : []
  } finally {
    loading.value = false
  }
}

const getCurrentUser = async () => {
  if (!userStore.id) {
    await userStore.getInfo()
  }
  const userId = userStore.id
  userInfo.id = String(userId)
  const res = await getUser(userId)
  userInfo.nickName = res?.data?.nickName || userStore.name || '匿名用户'
}

const resetNewPost = () => {
  newPost.title = ''
  newPost.content = ''
  newPost.images = ''
}

const validatePost = (post) => {
  if (!post.title.trim()) {
    ElMessage.warning('请输入帖子标题')
    return false
  }
  if (!post.content.trim()) {
    ElMessage.warning('请输入帖子内容')
    return false
  }
  return true
}

const openCreateDialog = () => {
  resetNewPost()
  createDialogVisible.value = true
}

const openEditDialog = (post) => {
  editForm.id = post.id
  editForm.title = post.title
  editForm.content = post.content
  editForm.images = post.images.join(',')
  editDialogVisible.value = true
}

const scrollToPosts = async () => {
  await nextTick()
  postSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const createPost = async () => {
  if (!validatePost(newPost)) {
    return
  }
  submitting.value = true
  try {
    await addForumPost({
      title: newPost.title.trim(),
      content: newPost.content.trim(),
      images: newPost.images || ''
    })
    createDialogVisible.value = false
    resetNewPost()
    await loadPosts()
    ElMessage.success('发布成功')
  } finally {
    submitting.value = false
  }
}

const submitEditPost = async () => {
  if (!validatePost(editForm)) {
    return
  }
  submitting.value = true
  try {
    await updateForumPost({
      id: editForm.id,
      title: editForm.title.trim(),
      content: editForm.content.trim(),
      images: editForm.images || ''
    })
    editDialogVisible.value = false
    await loadPosts()
    ElMessage.success('修改成功')
  } finally {
    submitting.value = false
  }
}

const isAuthor = (post) => String(post.authorId) === String(userInfo.id)
const canDeletePost = (post) => isAuthor(post) || isAdmin.value
const canDeleteComment = (post, comment) => {
  const currentUserId = String(userInfo.id)
  return isAdmin.value || String(post.authorId) === currentUserId || String(comment.userId) === currentUserId
}
const hasLikedPost = (post) => post.likedUserIds?.includes(String(userInfo.id))
const hasLikedComment = (comment) => comment.likedUserIds?.includes(String(userInfo.id))

const handleDeletePost = async (post) => {
  await ElMessageBox.confirm('删除后无法恢复，确定继续吗？', '系统提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await delForumPost(post.id)
  await loadPosts()
  ElMessage.success('删除成功')
}

const handleDeleteComment = async (post, comment) => {
  await ElMessageBox.confirm('确定删除这条评论吗？', '系统提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await delForumComment(post.id, comment.id)
  await loadPosts()
  ElMessage.success('评论已删除')
}

const toggleLikePost = async (post) => {
  await likeForumPost(post.id)
  await loadPosts()
}

const addComment = async (post) => {
  const value = (commentDrafts[post.id] || '').trim()
  if (!value) {
    ElMessage.warning('请输入评论内容')
    return
  }
  await addForumComment(post.id, { content: value })
  commentDrafts[post.id] = ''
  await loadPosts()
  ElMessage.success('评论成功')
}

const toggleLikeComment = async (post, comment) => {
  await likeForumComment(post.id, comment.id)
  await loadPosts()
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
  const date = typeof time === 'string' ? new Date(time.replace(/-/g, '/')) : new Date(time)
  if (Number.isNaN(date.getTime())) {
    return time
  }
  const y = date.getFullYear()
  const m = `${date.getMonth() + 1}`.padStart(2, '0')
  const d = `${date.getDate()}`.padStart(2, '0')
  const h = `${date.getHours()}`.padStart(2, '0')
  const mm = `${date.getMinutes()}`.padStart(2, '0')
  return `${y}-${m}-${d} ${h}:${mm}`
}

onMounted(async () => {
  await getCurrentUser()
  await loadPosts()
})
</script>

<style scoped lang="scss">
.forum-page {
  position: relative;
  overflow: hidden;
  min-height: calc(100vh - 70px);
  padding: 28px 0 48px;
  background:
    radial-gradient(circle at top left, rgba(80, 153, 255, 0.22), transparent 28%),
    radial-gradient(circle at right 18%, rgba(80, 227, 194, 0.2), transparent 22%),
    linear-gradient(180deg, #eef6ff 0%, #f8fbff 48%, #eef4ff 100%);
}

.forum-bg {
  position: absolute;
  width: 420px;
  height: 420px;
  border-radius: 50%;
  filter: blur(48px);
  pointer-events: none;
  opacity: 0.32;
}

.forum-bg-left {
  top: -120px;
  left: -90px;
  background: linear-gradient(135deg, #66b6ff, #7cf0cf);
}

.forum-bg-right {
  right: -120px;
  bottom: -150px;
  background: linear-gradient(135deg, #8ba8ff, #7dd7ff);
}

.forum-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(117, 144, 187, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(117, 144, 187, 0.08) 1px, transparent 1px);
  background-size: 30px 30px;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.5), transparent 88%);
  pointer-events: none;
}

.forum-container {
  position: relative;
  z-index: 1;
  width: 1200px;
  max-width: 94%;
  margin: 0 auto;
}

.forum-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(280px, 0.7fr);
  gap: 18px;
  margin-bottom: 22px;
}

.hero-copy,
.hero-panel,
.post-card {
  border: 1px solid rgba(255, 255, 255, 0.72);
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(12px);
  box-shadow: 0 20px 55px rgba(56, 83, 132, 0.12);
}

.hero-copy {
  padding: 34px 34px 30px;
  border-radius: 28px;
}

.hero-tag {
  margin: 0 0 12px;
  color: #6b7f9d;
  font-size: 12px;
  letter-spacing: 2px;
  text-transform: uppercase;
}

.hero-copy h2 {
  margin: 0;
  font-size: 38px;
  line-height: 1.1;
  color: #1f3256;
}

.hero-subtitle {
  margin: 14px 0 0;
  max-width: 560px;
  font-size: 16px;
  line-height: 1.8;
  color: #61738f;
}

.hero-actions {
  display: flex;
  gap: 12px;
  margin-top: 28px;
  flex-wrap: wrap;
}

.hero-panel {
  padding: 26px 24px;
  border-radius: 28px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.panel-title {
  font-size: 16px;
  font-weight: 700;
  color: #223960;
}

.panel-stats {
  display: grid;
  gap: 12px;
  margin: 18px 0;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  border-radius: 18px;
  background: linear-gradient(135deg, #eef5ff, #f9fdff);
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #22457c;
}

.stat-label {
  color: #61738f;
  font-size: 14px;
}

.panel-tip {
  font-size: 13px;
  line-height: 1.7;
  color: #7c8ca5;
}

.forum-feed {
  padding-bottom: 8px;
}

.feed-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.feed-title {
  font-size: 22px;
  font-weight: 700;
  color: #1f3358;
}

.feed-subtitle {
  margin-top: 4px;
  color: #70819c;
  font-size: 14px;
}

.post-list {
  display: grid;
  gap: 18px;
}

.post-card {
  border-radius: 26px;
  padding: 24px 24px 20px;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.post-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 22px 60px rgba(56, 83, 132, 0.16);
}

.post-card-top {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 16px;
}

.post-head {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.author-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  border-radius: 16px;
  background: linear-gradient(135deg, #5c9dff, #5ee1bf);
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  box-shadow: 0 10px 20px rgba(92, 157, 255, 0.28);
}

.post-head h3 {
  margin: 0 0 6px;
  font-size: 24px;
  color: #22375d;
  line-height: 1.35;
}

.post-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: #73839d;
  font-size: 13px;
}

.post-action-group {
  display: flex;
  align-items: flex-start;
  gap: 2px;
  flex-wrap: wrap;
}

.post-content {
  color: #3f506a;
  line-height: 1.9;
  white-space: pre-wrap;
  word-break: break-word;
}

.post-gallery {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.post-gallery :deep(.el-image) {
  width: 100%;
  height: 160px;
  overflow: hidden;
  border-radius: 18px;
  box-shadow: 0 10px 24px rgba(85, 114, 164, 0.16);
}

.comment-block {
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px solid rgba(125, 144, 173, 0.18);
}

.comment-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.comment-title {
  font-size: 16px;
  font-weight: 700;
  color: #253c67;
}

.comment-count {
  color: #8090a8;
  font-size: 13px;
}

.comment-list {
  display: grid;
  gap: 12px;
}

.comment-item {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  padding: 14px 16px;
  border-radius: 18px;
  background: linear-gradient(135deg, #f6f9ff, #fbfdff);
  border: 1px solid rgba(162, 182, 219, 0.14);
}

.comment-main {
  flex: 1;
  min-width: 0;
}

.comment-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.comment-user {
  color: #29406a;
  font-weight: 700;
}

.comment-time {
  color: #8a98ad;
  font-size: 12px;
}

.comment-text {
  margin-top: 8px;
  color: #4b5d79;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.comment-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  white-space: nowrap;
}

.comment-editor {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-top: 16px;
}

.comment-editor :deep(.el-input) {
  flex: 1;
}

.empty-shell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 40px 16px 18px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.72);
}

.floating-create {
  position: fixed;
  right: 28px;
  bottom: 36px;
  z-index: 15;
  width: 62px;
  height: 62px;
  border: none;
  border-radius: 22px;
  background: linear-gradient(135deg, #4e90ff, #58dfba);
  color: #fff;
  font-size: 34px;
  line-height: 1;
  cursor: pointer;
  box-shadow: 0 18px 36px rgba(78, 144, 255, 0.34);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.floating-create:hover {
  transform: translateY(-2px) scale(1.02);
  box-shadow: 0 22px 42px rgba(78, 144, 255, 0.42);
}

.forum-dialog :deep(.el-dialog) {
  border-radius: 24px;
  overflow: hidden;
}

@media (max-width: 900px) {
  .forum-hero {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .forum-page {
    padding-top: 18px;
  }

  .hero-copy,
  .hero-panel,
  .post-card {
    padding-left: 18px;
    padding-right: 18px;
  }

  .feed-header,
  .post-card-top,
  .comment-item,
  .comment-editor,
  .comment-top {
    flex-direction: column;
  }

  .post-action-group,
  .comment-actions {
    align-items: flex-start;
  }

  .post-gallery {
    grid-template-columns: repeat(2, 1fr);
  }

  .floating-create {
    right: 18px;
    bottom: 22px;
    width: 56px;
    height: 56px;
    border-radius: 18px;
  }
}
</style>
