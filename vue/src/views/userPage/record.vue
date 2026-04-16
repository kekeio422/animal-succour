<template>
  <div class="record-page">
    <section class="record-hero">
      <div>
        <h2>我的申请</h2>
        <p>查看领养申请进度，并按系统生成的回访任务提交宠物近况。</p>
      </div>
      <div class="pending-box">
        <span class="pending-num">{{ pendingCount }}</span>
        <span class="pending-label">待提交回访</span>
      </div>
    </section>

    <div v-if="adoptList.length" class="request-list">
      <el-card v-for="adopt in adoptList" :key="adopt.adoptId" class="request-card" shadow="hover">
        <div class="request-header">
          <div>
            <h3>{{ adopt.animalName }}</h3>
            <div class="request-time">申请时间：{{ adopt.createTime }}</div>
          </div>
          <el-tag type="primary" v-if="adopt.status === '审核中'">{{ adopt.status }}</el-tag>
          <el-tag type="danger" v-else-if="adopt.status === '已拒绝'">{{ adopt.status }}</el-tag>
          <el-tag type="success" v-else>{{ adopt.status }}</el-tag>
        </div>

        <div class="request-details">
          <div class="detail-row"><span>申请人：</span>{{ adopt.name }}</div>
          <div class="detail-row"><span>联系电话：</span>{{ adopt.phone }}</div>
          <div class="detail-row"><span>电子邮箱：</span>{{ adopt.email }}</div>
          <div class="detail-row"><span>居住地址：</span>{{ adopt.address }}</div>
          <div class="detail-row"><span>职业：</span>{{ adopt.occupation }}</div>
          <div class="detail-row"><span>申请理由：</span>{{ adopt.reason }}</div>
          <div class="detail-row"><span>处理说明：</span>{{ adopt.description }}</div>
        </div>

        <div class="request-footer">
          <el-button
            v-if="adopt.status === '审核中'"
            type="danger"
            plain
            size="small"
            @click="cancelRequest(adopt.adoptId)"
          >
            撤销申请
          </el-button>
        </div>

        <div v-if="followupMap[adopt.adoptId]?.length" class="followup-section">
          <div class="followup-section-title">回访记录</div>
          <div class="followup-list">
            <div v-for="item in followupMap[adopt.adoptId]" :key="item.followupId" class="followup-item">
              <div class="followup-main">
                <div class="followup-title">{{ item.taskTitle }}</div>
                <div class="followup-meta">
                  <span>回访时间：{{ item.dueTime || '-' }}</span>
                  <span>任务状态：{{ item.status }}</span>
                  <span>审核结果：{{ item.reviewStatus }}</span>
                </div>
              </div>
              <div class="followup-actions">
                <el-button v-if="item.status === '待提交'" type="primary" size="small" round @click="openSubmitDialog(item)">
                  填写回访
                </el-button>
                <el-button v-else-if="item.status === '待开始'" size="small" round disabled>
                  未到时间
                </el-button>
                <el-button v-else size="small" round @click="openViewDialog(item)">
                  查看表单
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <div v-else class="empty-state">
      <el-empty description="暂无领养申请记录">
        <el-button type="primary" @click="goToAdopt">去领养宠物</el-button>
      </el-empty>
    </div>

    <el-dialog v-model="submitDialogVisible" title="提交回访表单" width="720px" destroy-on-close>
      <div class="submit-pet" v-if="currentFollowup.followupId">
        <img :src="getFileUrl(currentFollowup.animalImage)" :alt="currentFollowup.animalName">
        <div>
          <div class="submit-pet-name">{{ currentFollowup.animalName }}</div>
          <div class="submit-pet-task">{{ currentFollowup.taskTitle }}</div>
          <div class="submit-pet-task">计划回访时间：{{ currentFollowup.dueTime }}</div>
        </div>
      </div>
      <el-form ref="followupFormRef" :model="followupForm" :rules="followupRules" label-width="110px">
        <el-form-item label="宠物状态" prop="petStatus">
          <el-select v-model="followupForm.petStatus" placeholder="请选择宠物状态" style="width: 100%">
            <el-option label="健康活泼" value="健康活泼" />
            <el-option label="生病中" value="生病中" />
            <el-option label="适应良好" value="适应良好" />
            <el-option label="其他情况" value="其他情况" />
          </el-select>
        </el-form-item>
        <el-form-item label="生活情况" prop="content">
          <el-input
            v-model="followupForm.content"
            type="textarea"
            :rows="5"
            maxlength="1000"
            show-word-limit
            placeholder="请填写宠物饮食、活动、居住环境变化等情况"
          />
        </el-form-item>
        <el-form-item label="生活照片">
          <image-upload v-model="followupForm.images" :limit="6" />
        </el-form-item>
        <el-form-item label="回访视频">
          <file-upload
            v-model="followupForm.videos"
            :limit="2"
            :file-size="50"
            :file-type="['mp4', 'mov', 'avi']"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="submitDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitFollowupForm">提交回访</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="viewDialogVisible" title="回访表单详情" width="760px" destroy-on-close>
      <div class="view-card">
        <div class="view-row"><span>宠物：</span>{{ currentFollowup.animalName }}</div>
        <div class="view-row"><span>任务：</span>{{ currentFollowup.taskTitle }}</div>
        <div class="view-row"><span>计划回访时间：</span>{{ currentFollowup.dueTime || '-' }}</div>
        <div class="view-row"><span>提交时间：</span>{{ currentFollowup.submitTime || '未提交' }}</div>
        <div class="view-row"><span>宠物状态：</span>{{ currentFollowup.petStatus || '未填写' }}</div>
        <div class="view-row"><span>审核结果：</span>{{ currentFollowup.reviewStatus || '待提交' }}</div>
        <div class="view-row"><span>生活情况：</span>{{ currentFollowup.content || '未填写' }}</div>
        <div class="view-row"><span>后台备注：</span>{{ currentFollowup.adminRemark || '无' }}</div>

        <div v-if="parseList(currentFollowup.images).length" class="view-media">
          <span>图片：</span>
          <div class="view-image-list">
            <el-image
              v-for="(img, index) in parseList(currentFollowup.images)"
              :key="`${currentFollowup.followupId}_img_${index}`"
              :src="getFileUrl(img)"
              :preview-src-list="parseList(currentFollowup.images).map(getFileUrl)"
              fit="cover"
            />
          </div>
        </div>

        <div v-if="parseList(currentFollowup.videos).length" class="view-row">
          <span>视频：</span>
          <div class="view-video-list">
            <el-link
              v-for="(video, index) in parseList(currentFollowup.videos)"
              :key="`${currentFollowup.followupId}_video_${index}`"
              :href="getFileUrl(video)"
              target="_blank"
            >
              视频{{ index + 1 }}
            </el-link>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import ImageUpload from '@/components/ImageUpload'
import FileUpload from '@/components/FileUpload'
import { listAdopt, revoke } from '@/api/succour/adopt'
import { listAdoptFollowupByAdoptId, submitAdoptFollowup } from '@/api/succour/adoptFollowup'

const BASE_URL = import.meta.env.VITE_APP_BASE_API

const router = useRouter()
const adoptList = ref([])
const followupMap = reactive({})
const submitDialogVisible = ref(false)
const viewDialogVisible = ref(false)
const followupFormRef = ref()

const currentFollowup = reactive({
  followupId: '',
  adoptId: '',
  animalName: '',
  animalImage: '',
  taskTitle: '',
  dueTime: '',
  submitTime: '',
  petStatus: '',
  content: '',
  images: '',
  videos: '',
  reviewStatus: '',
  adminRemark: ''
})

const followupForm = reactive({
  followupId: '',
  petStatus: '',
  content: '',
  images: '',
  videos: ''
})

const followupRules = {
  petStatus: [{ required: true, message: '请选择宠物状态', trigger: 'change' }]
}

const pendingCount = computed(() => {
  return Object.values(followupMap).reduce((total, list) => {
    return total + list.filter(item => item.status === '待提交').length
  }, 0)
})

const parseList = (value) => {
  if (!value) return []
  if (Array.isArray(value)) return value
  return String(value).split(',').map(item => item.trim()).filter(Boolean)
}

const getFileUrl = (value) => {
  if (!value) return ''
  if (/^(https?:|blob:|data:)/.test(value)) return value
  return `${BASE_URL}${value}`
}

const loadFollowups = async (adoptId) => {
  const res = await listAdoptFollowupByAdoptId(adoptId)
  followupMap[adoptId] = res.data || []
}

const getList = async () => {
  const res = await listAdopt({ pageNum: 1, pageSize: 100 })
  adoptList.value = res.rows || []
  Object.keys(followupMap).forEach(key => {
    delete followupMap[key]
  })
  const completedAdopts = adoptList.value.filter(adopt => adopt.status === '已完成')
  await Promise.all(completedAdopts.map(adopt => loadFollowups(adopt.adoptId)))
}

const cancelRequest = (adoptId) => {
  ElMessageBox.confirm('确定要撤销此领养申请吗？', '撤销申请', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => revoke(adoptId)).then(() => {
    ElMessage.success('申请已撤销')
    return getList()
  }).catch(() => {})
}

const goToAdopt = () => {
  router.push('/user/animal')
}

const fillCurrentFollowup = (item) => {
  currentFollowup.followupId = item.followupId
  currentFollowup.adoptId = item.adoptId
  currentFollowup.animalName = item.animalName
  currentFollowup.animalImage = item.animalImage
  currentFollowup.taskTitle = item.taskTitle
  currentFollowup.dueTime = item.dueTime
  currentFollowup.submitTime = item.submitTime
  currentFollowup.petStatus = item.petStatus
  currentFollowup.content = item.content
  currentFollowup.images = item.images
  currentFollowup.videos = item.videos
  currentFollowup.reviewStatus = item.reviewStatus
  currentFollowup.adminRemark = item.adminRemark
}

const openSubmitDialog = (item) => {
  fillCurrentFollowup(item)
  followupForm.followupId = item.followupId
  followupForm.petStatus = item.petStatus || ''
  followupForm.content = item.content || ''
  followupForm.images = item.images || ''
  followupForm.videos = item.videos || ''
  submitDialogVisible.value = true
  nextTick(() => {
    followupFormRef.value?.clearValidate()
  })
}

const openViewDialog = (item) => {
  fillCurrentFollowup(item)
  viewDialogVisible.value = true
}

const hasFollowupContent = () => {
  return !!(
    followupForm.content?.trim()
    || parseList(followupForm.images).length
    || parseList(followupForm.videos).length
  )
}

const submitFollowupForm = () => {
  followupFormRef.value.validate(valid => {
    if (!valid) return
    if (!hasFollowupContent()) {
      ElMessage.warning('请至少填写生活情况或上传照片/视频')
      return
    }
    submitAdoptFollowup({
      followupId: followupForm.followupId,
      petStatus: followupForm.petStatus,
      content: followupForm.content,
      images: followupForm.images,
      videos: followupForm.videos
    }).then(() => {
      ElMessage.success('回访提交成功')
      submitDialogVisible.value = false
      return loadFollowups(currentFollowup.adoptId)
    })
  })
}

onMounted(() => {
  getList()
})
</script>

<style scoped lang="scss">
.record-page {
  width: 1200px;
  max-width: 94%;
  margin: 0 auto;
  padding: 24px 0 40px;
}

.record-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
  padding: 24px 28px;
  border-radius: 22px;
  background: linear-gradient(135deg, #eef6ff, #fbfdff);
  box-shadow: 0 14px 34px rgba(46, 74, 126, 0.08);
}

.record-hero h2 {
  margin: 0;
  color: #23375c;
}

.record-hero p {
  margin: 10px 0 0;
  color: #687b98;
}

.pending-box {
  min-width: 140px;
  padding: 16px 18px;
  border-radius: 18px;
  background: #fff;
  text-align: center;
}

.pending-num {
  display: block;
  font-size: 28px;
  font-weight: 700;
  color: #2468ff;
}

.pending-label {
  color: #73839f;
  font-size: 13px;
}

.request-list {
  display: grid;
  gap: 18px;
}

.request-card {
  border-radius: 18px;
}

.request-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.request-header h3 {
  margin: 0;
  font-size: 22px;
  color: #243a62;
}

.request-time {
  margin-top: 8px;
  font-size: 13px;
  color: #7d8da7;
}

.request-details {
  display: grid;
  gap: 10px;
  margin-top: 18px;
}

.detail-row {
  line-height: 1.7;
  color: #52637e;
}

.detail-row span {
  color: #243a62;
  font-weight: 600;
}

.request-footer {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.followup-section {
  margin-top: 20px;
  padding-top: 18px;
  border-top: 1px solid #edf1f6;
}

.followup-section-title {
  margin-bottom: 12px;
  font-size: 16px;
  font-weight: 700;
  color: #23375c;
}

.followup-list {
  display: grid;
  gap: 12px;
}

.followup-item {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
  padding: 14px 16px;
  border-radius: 14px;
  background: #f7faff;
}

.followup-title {
  font-weight: 700;
  color: #28406d;
}

.followup-meta {
  margin-top: 6px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 13px;
  color: #7d8da7;
}

.submit-pet {
  display: flex;
  gap: 16px;
  align-items: center;
  margin-bottom: 20px;
  padding: 14px;
  border-radius: 14px;
  background: #f7faff;
}

.submit-pet img {
  width: 92px;
  height: 92px;
  border-radius: 14px;
  object-fit: cover;
}

.submit-pet-name {
  font-size: 20px;
  font-weight: 700;
  color: #23375c;
}

.submit-pet-task {
  margin-top: 6px;
  color: #7486a3;
}

.view-card {
  display: grid;
  gap: 12px;
}

.view-row {
  line-height: 1.8;
  color: #51627d;
}

.view-row span,
.view-media > span {
  color: #243a62;
  font-weight: 600;
}

.view-media {
  display: grid;
  gap: 12px;
}

.view-image-list {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.view-image-list :deep(.el-image) {
  width: 120px;
  height: 120px;
  border-radius: 12px;
}

.view-video-list {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.empty-state {
  padding: 36px 0;
}

@media (max-width: 768px) {
  .record-hero,
  .request-header,
  .followup-item,
  .submit-pet {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
