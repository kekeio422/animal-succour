<template>
  <div class="forum-page">
    <div class="forum-container">
      <div class="forum-header">
        <h2>社区论坛</h2>
        <p>分享救助故事、交流领养经验，传递更多温暖。</p>
      </div>

      <el-card class="post-editor" shadow="never">
        <template #header>
          <div class="editor-title">发布新帖子</div>
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
            <el-button text type="primary" @click="toggleLikePost(post)">
              {{ hasLikedPost(post) ? '已点赞' : '点赞' }} ({{ post.likes }})
            </el-button>
          </div>

          <div class="post-content">{{ post.content }}</div>

          <div class="comment-block">
            <div class="comment-title">评论区 ({{ post.comments.length }})</div>
            <div v-if="post.comments.length" class="comment-list">
              <div v-for="comment in post.comments" :key="comment.id" class="comment-item">
                <div>
                  <div class="comment-user">{{ comment.userName }}</div>
                  <div class="comment-text">{{ comment.content }}</div>
                  <div class="comment-time">{{ formatTime(comment.createdAt) }}</div>
                </div>
                <el-button text type="primary" @click="toggleLikeComment(post, comment)">
                  {{ hasLikedComment(comment) ? '已点赞' : '点赞' }} ({{ comment.likes }})
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
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import useUserStore from '@/store/modules/user'
import { getUser } from '@/api/system/user'

const STORAGE_KEY = 'succour_forum_posts'

const userStore = useUserStore()
const userInfo = reactive({
  id: '',
  nickName: ''
})

const posts = ref([])
const commentDrafts = reactive({})
const newPost = reactive({
  title: '',
  content: ''
})

const loadPosts = () => {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) {
    posts.value = []
    return
  }
  try {
    const parsed = JSON.parse(raw)
    posts.value = Array.isArray(parsed) ? parsed : []
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

  posts.value.unshift({
    id: `${Date.now()}_${Math.random().toString(16).slice(2)}`,
    title: newPost.title.trim(),
    content: newPost.content.trim(),
    authorId: userInfo.id,
    authorName: userInfo.nickName,
    likes: 0,
    likedUserIds: [],
    comments: [],
    createdAt: Date.now()
  })
  newPost.title = ''
  newPost.content = ''
  savePosts()
  ElMessage.success('发布成功')
}

const hasLikedPost = (post) => {
  return post.likedUserIds?.includes(userInfo.id)
}

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

const hasLikedComment = (comment) => {
  return comment.likedUserIds?.includes(userInfo.id)
}

const toggleLikeComment = (post, comment) => {
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

<style scoped>
.forum-page {
  background: #f5f7fa;
  min-height: calc(100vh - 70px);
  padding: 24px 0;
}

.forum-container {
  width: 1000px;
  max-width: 92%;
  margin: 0 auto;
}

.forum-header {
  margin-bottom: 16px;
}

.forum-header h2 {
  margin: 0;
  font-size: 28px;
  color: #303133;
}

.forum-header p {
  margin-top: 8px;
  color: #909399;
}

.post-editor,
.post-item {
  border-radius: 12px;
  margin-bottom: 16px;
}

.editor-title {
  font-weight: 600;
  color: #303133;
}

.editor-input {
  margin-bottom: 12px;
}

.editor-action {
  display: flex;
  justify-content: flex-end;
}

.post-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}

.post-top h3 {
  margin: 0 0 6px 0;
  color: #303133;
}

.post-meta {
  color: #909399;
  font-size: 13px;
}

.post-content {
  margin: 14px 0;
  color: #606266;
  white-space: pre-wrap;
  line-height: 1.7;
}

.comment-block {
  border-top: 1px solid #ebeef5;
  padding-top: 14px;
}

.comment-title {
  color: #606266;
  font-weight: 600;
  margin-bottom: 10px;
}

.comment-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 8px;
  gap: 10px;
}

.comment-user {
  font-size: 14px;
  color: #303133;
  font-weight: 600;
}

.comment-text {
  margin: 4px 0;
  color: #606266;
}

.comment-time {
  font-size: 12px;
  color: #909399;
}

.comment-editor {
  display: flex;
  gap: 10px;
  margin-top: 10px;
}
</style>
