<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" label-width="68px">
      <el-form-item label="宠物姓名" prop="animalName">
        <el-input v-model="queryParams.animalName" placeholder="请输入宠物姓名" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="申请状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择申请状态" style="width: 180px">
          <el-option label="审核中" value="审核中" />
          <el-option label="已拒绝" value="已拒绝" />
          <el-option label="已完成" value="已完成" />
        </el-select>
      </el-form-item>
      <el-form-item label="申请用户" prop="userName">
        <el-input v-model="queryParams.userName" placeholder="请输入申请用户" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="warning" plain @click="openRuleDialog">回访规则</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete">删除</el-button>
      </el-col>
    </el-row>

    <el-table
      ref="tableRef"
      v-loading="loading"
      border
      highlight-current-row
      :data="adoptList"
      @row-click="clickRow"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="序号" align="center" type="index" :index="indexMethod" width="70" />
      <el-table-column label="宠物姓名" align="center" prop="animalName" />
      <el-table-column label="申请人" align="center" prop="name" />
      <el-table-column label="联系电话" align="center" prop="phone" />
      <el-table-column label="申请状态" align="center" prop="status" width="110">
        <template #default="scope">
          <el-tag type="primary" v-if="scope.row.status === '审核中'">{{ scope.row.status }}</el-tag>
          <el-tag type="danger" v-else-if="scope.row.status === '已拒绝'">{{ scope.row.status }}</el-tag>
          <el-tag type="success" v-else>{{ scope.row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="处理说明" align="center" prop="description" show-overflow-tooltip />
      <el-table-column label="申请用户" align="center" prop="userName" />
      <el-table-column label="申请时间" align="center" prop="createTime" width="170" />
      <el-table-column label="操作" align="center" width="240">
        <template #default="scope">
          <el-button v-if="scope.row.status === '审核中'" type="primary" link @click="handleToExamine(scope.row)">审核</el-button>
          <el-button v-if="scope.row.status === '已完成'" type="success" link @click="openFollowupDialog(scope.row)">回访记录</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <vxe-modal :title="reviewTitle" v-model="open" width="560px" show-maximize showFooter resize>
      <el-form ref="adoptRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="申请人">
          <el-input v-model="form.name" disabled />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" disabled />
        </el-form-item>
        <el-form-item label="电子邮箱">
          <el-input v-model="form.email" disabled />
        </el-form-item>
        <el-form-item label="居住地址">
          <el-input v-model="form.address" type="textarea" disabled />
        </el-form-item>
        <el-form-item label="职业">
          <el-input v-model="form.occupation" disabled />
        </el-form-item>
        <el-form-item label="申请理由">
          <el-input v-model="form.reason" type="textarea" disabled />
        </el-form-item>
        <el-form-item label="养宠经验">
          <el-input v-model="form.petExperience" disabled />
        </el-form-item>
        <el-form-item label="处理说明" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入处理说明" />
        </el-form-item>
        <el-form-item label="审核处理" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="已完成" border>同意领养</el-radio>
            <el-radio value="已拒绝" border>拒绝领养</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确定</el-button>
          <el-button @click="cancel">取消</el-button>
        </div>
      </template>
    </vxe-modal>

    <el-dialog v-model="ruleDialogVisible" title="回访规则设置" width="760px" destroy-on-close>
      <div class="rule-toolbar">
        <el-button type="primary" plain @click="handleAddRule">新增规则</el-button>
      </div>
      <el-table :data="ruleList" border v-loading="ruleLoading">
        <el-table-column label="规则名称" prop="ruleName" align="center" />
        <el-table-column label="领养后天数" prop="delayDays" align="center" width="140" />
        <el-table-column label="排序" prop="sortNum" align="center" width="100" />
        <el-table-column label="状态" align="center" width="100">
          <template #default="scope">
            <el-tag :type="scope.row.status === '0' ? 'success' : 'info'">
              {{ scope.row.status === '0' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="180">
          <template #default="scope">
            <el-button type="primary" link @click="handleEditRule(scope.row)">编辑</el-button>
            <el-button type="danger" link @click="handleDeleteRule(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog v-model="ruleFormVisible" :title="ruleForm.ruleId ? '编辑回访规则' : '新增回访规则'" width="520px">
      <el-form ref="ruleFormRef" :model="ruleForm" :rules="ruleRules" label-width="110px">
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="ruleForm.ruleName" placeholder="如：领养后第1周回访" />
        </el-form-item>
        <el-form-item label="回访天数" prop="delayDays">
          <el-input-number v-model="ruleForm.delayDays" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="排序" prop="sortNum">
          <el-input-number v-model="ruleForm.sortNum" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="ruleForm.status">
            <el-radio value="0" border>启用</el-radio>
            <el-radio value="1" border>停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleFormVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRuleForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="followupDialogVisible" title="回访记录" width="900px" destroy-on-close>
      <div v-loading="followupLoading">
        <el-empty v-if="!followupList.length" description="当前领养暂无回访任务" />
        <div v-else class="followup-list">
          <el-card v-for="item in followupList" :key="item.followupId" shadow="never" class="followup-card">
            <template #header>
              <div class="followup-header">
                <div>
                  <div class="followup-title">{{ item.taskTitle }}</div>
                  <div class="followup-meta">计划回访时间：{{ item.dueTime || '-' }}</div>
                </div>
                <div class="followup-tags">
                  <el-tag>{{ item.status }}</el-tag>
                  <el-tag type="success" v-if="item.reviewStatus === '正常'">{{ item.reviewStatus }}</el-tag>
                  <el-tag type="danger" v-else-if="item.reviewStatus === '异常'">{{ item.reviewStatus }}</el-tag>
                  <el-tag type="warning" v-else-if="item.reviewStatus === '失联'">{{ item.reviewStatus }}</el-tag>
                  <el-tag type="info" v-else>{{ item.reviewStatus }}</el-tag>
                </div>
              </div>
            </template>
            <div class="followup-body">
              <div class="followup-row"><span>宠物状态：</span>{{ item.petStatus || '未填写' }}</div>
              <div class="followup-row"><span>生活情况：</span>{{ item.content || '未填写' }}</div>
              <div class="followup-row"><span>提交时间：</span>{{ item.submitTime || '未提交' }}</div>
              <div class="followup-row"><span>后台备注：</span>{{ item.adminRemark || '无' }}</div>
              <div v-if="parseList(item.images).length" class="followup-media">
                <span>图片：</span>
                <div class="image-list">
                  <el-image
                    v-for="(img, index) in parseList(item.images)"
                    :key="`${item.followupId}_img_${index}`"
                    :src="getFileUrl(img)"
                    :preview-src-list="parseList(item.images).map(getFileUrl)"
                    fit="cover"
                  />
                </div>
              </div>
              <div v-if="parseList(item.videos).length" class="followup-row">
                <span>视频：</span>
                <div class="video-link-list">
                  <el-link
                    v-for="(video, index) in parseList(item.videos)"
                    :key="`${item.followupId}_video_${index}`"
                    :href="getFileUrl(video)"
                    target="_blank"
                  >
                    视频{{ index + 1 }}
                  </el-link>
                </div>
              </div>
            </div>
            <div class="followup-actions">
              <el-button v-if="item.status === '已提交'" type="primary" plain size="small" @click="openFollowupReview(item)">
                审核回访
              </el-button>
            </div>
          </el-card>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="followupReviewVisible" title="审核回访表单" width="540px">
      <el-form ref="followupReviewRef" :model="followupReviewForm" :rules="followupReviewRules" label-width="110px">
        <el-form-item label="回访任务">
          <el-input v-model="followupReviewForm.taskTitle" disabled />
        </el-form-item>
        <el-form-item label="审核结果" prop="reviewStatus">
          <el-radio-group v-model="followupReviewForm.reviewStatus">
            <el-radio value="正常" border>正常</el-radio>
            <el-radio value="异常" border>异常</el-radio>
            <el-radio value="失联" border>失联</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="后台备注" prop="adminRemark">
          <el-input v-model="followupReviewForm.adminRemark" type="textarea" :rows="4" placeholder="请输入审核备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="followupReviewVisible = false">取消</el-button>
        <el-button type="primary" @click="submitFollowupReview">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, toRefs } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { delAdopt, getAdopt, listAdopt, updateAdopt } from '@/api/succour/adopt'
import { updateAnimal } from '@/api/succour/animal'
import { listAdoptFollowupByAdoptId, reviewAdoptFollowup } from '@/api/succour/adoptFollowup'
import {
  addAdoptFollowupRule,
  delAdoptFollowupRule,
  listAdoptFollowupRule,
  updateAdoptFollowupRule
} from '@/api/succour/adoptFollowupRule'

const BASE_URL = import.meta.env.VITE_APP_BASE_API

const queryRef = ref()
const adoptRef = ref()
const tableRef = ref()
const ruleFormRef = ref()
const followupReviewRef = ref()

const adoptList = ref([])
const loading = ref(true)
const ids = ref([])
const multiple = ref(true)
const total = ref(0)
const open = ref(false)
const reviewTitle = ref('')

const ruleDialogVisible = ref(false)
const ruleFormVisible = ref(false)
const ruleLoading = ref(false)
const ruleList = ref([])

const followupDialogVisible = ref(false)
const followupReviewVisible = ref(false)
const followupLoading = ref(false)
const followupList = ref([])

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    animalName: null,
    status: null,
    userName: null
  },
  rules: {
    description: [{ required: true, message: '处理说明不能为空', trigger: 'blur' }],
    status: [{ required: true, message: '请选择审核处理', trigger: 'change' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

const ruleForm = reactive({
  ruleId: null,
  ruleName: '',
  delayDays: 7,
  sortNum: 1,
  status: '0'
})

const ruleRules = {
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  delayDays: [{ required: true, message: '请输入回访天数', trigger: 'change' }],
  sortNum: [{ required: true, message: '请输入排序', trigger: 'change' }]
}

const followupReviewForm = reactive({
  followupId: '',
  taskTitle: '',
  reviewStatus: '',
  adminRemark: ''
})

const followupReviewRules = {
  reviewStatus: [{ required: true, message: '请选择审核结果', trigger: 'change' }]
}

const reset = () => {
  form.value = {
    adoptId: null,
    animalId: null,
    name: null,
    phone: null,
    email: null,
    address: null,
    occupation: null,
    reason: null,
    petExperience: null,
    status: null,
    description: null,
    userId: null,
    createTime: null
  }
  adoptRef.value?.resetFields()
}

const getList = () => {
  loading.value = true
  listAdopt(queryParams.value).then(response => {
    adoptList.value = response.rows || []
    total.value = response.total || 0
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

const handleQuery = () => {
  queryParams.value.pageNum = 1
  getList()
}

const resetQuery = () => {
  queryRef.value?.resetFields()
  handleQuery()
}

const clickRow = (row) => {
  tableRef.value?.clearSelection()
  tableRef.value?.toggleRowSelection(row, true)
}

const handleSelectionChange = (selection) => {
  ids.value = selection.map(item => item.adoptId)
  multiple.value = !selection.length
}

const indexMethod = (index) => {
  const pageNum = queryParams.value.pageNum - 1
  return pageNum > 0 ? index + 1 + pageNum * queryParams.value.pageSize : index + 1
}

const cancel = () => {
  open.value = false
  reset()
}

const handleToExamine = (row) => {
  reset()
  getAdopt(row.adoptId).then(response => {
    form.value = response.data
    form.value.description = ''
    reviewTitle.value = '审核领养申请'
    open.value = true
  })
}

const submitForm = () => {
  adoptRef.value.validate(valid => {
    if (!valid) return
    const animalStatus = form.value.status === '已完成' ? '已领养' : '可领养'
    updateAnimal({ animalId: form.value.animalId, status: animalStatus }).then(() => {
      return updateAdopt(form.value)
    }).then(() => {
      ElMessage.success('审核成功')
      open.value = false
      getList()
    })
  })
}

const handleDelete = (row) => {
  const adoptIds = row?.adoptId || ids.value
  ElMessageBox.confirm('确认删除选中的领养申请吗？', '系统提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => delAdopt(adoptIds)).then(() => {
    ElMessage.success('删除成功')
    getList()
  }).catch(() => {})
}

const getRuleList = () => {
  ruleLoading.value = true
  listAdoptFollowupRule({ pageNum: 1, pageSize: 100 }).then(response => {
    ruleList.value = response.rows || []
    ruleLoading.value = false
  }).catch(() => {
    ruleLoading.value = false
  })
}

const openRuleDialog = () => {
  ruleDialogVisible.value = true
  getRuleList()
}

const resetRuleForm = () => {
  ruleForm.ruleId = null
  ruleForm.ruleName = ''
  ruleForm.delayDays = 7
  ruleForm.sortNum = 1
  ruleForm.status = '0'
  ruleFormRef.value?.resetFields()
}

const handleAddRule = () => {
  resetRuleForm()
  ruleFormVisible.value = true
}

const handleEditRule = (row) => {
  ruleForm.ruleId = row.ruleId
  ruleForm.ruleName = row.ruleName
  ruleForm.delayDays = row.delayDays
  ruleForm.sortNum = row.sortNum
  ruleForm.status = row.status
  ruleFormVisible.value = true
}

const submitRuleForm = () => {
  ruleFormRef.value.validate(valid => {
    if (!valid) return
    const request = ruleForm.ruleId ? updateAdoptFollowupRule(ruleForm) : addAdoptFollowupRule(ruleForm)
    request.then(() => {
      ElMessage.success(ruleForm.ruleId ? '修改成功' : '新增成功')
      ruleFormVisible.value = false
      getRuleList()
    })
  })
}

const handleDeleteRule = (row) => {
  ElMessageBox.confirm('确认删除这条回访规则吗？', '系统提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => delAdoptFollowupRule(row.ruleId)).then(() => {
    ElMessage.success('删除成功')
    getRuleList()
  }).catch(() => {})
}

const openFollowupDialog = (row) => {
  followupDialogVisible.value = true
  followupLoading.value = true
  listAdoptFollowupByAdoptId(row.adoptId).then(response => {
    followupList.value = response.data || []
    followupLoading.value = false
  }).catch(() => {
    followupLoading.value = false
  })
}

const openFollowupReview = (item) => {
  followupReviewForm.followupId = item.followupId
  followupReviewForm.taskTitle = item.taskTitle
  followupReviewForm.reviewStatus = item.reviewStatus === '待查看' ? '' : item.reviewStatus
  followupReviewForm.adminRemark = item.adminRemark || ''
  followupReviewVisible.value = true
}

const submitFollowupReview = () => {
  followupReviewRef.value.validate(valid => {
    if (!valid) return
    reviewAdoptFollowup({
      followupId: followupReviewForm.followupId,
      reviewStatus: followupReviewForm.reviewStatus,
      adminRemark: followupReviewForm.adminRemark
    }).then(() => {
      ElMessage.success('回访审核成功')
      followupReviewVisible.value = false
      const adoptId = followupList.value[0]?.adoptId
      if (!adoptId) return
      return listAdoptFollowupByAdoptId(adoptId).then(response => {
        followupList.value = response.data || []
      })
    })
  })
}

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

onMounted(() => {
  getList()
})
</script>

<style scoped lang="scss">
.rule-toolbar {
  margin-bottom: 12px;
}

.followup-list {
  display: grid;
  gap: 14px;
}

.followup-card {
  border-radius: 14px;
}

.followup-header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.followup-title {
  font-size: 16px;
  font-weight: 600;
  color: #27364f;
}

.followup-meta {
  margin-top: 4px;
  color: #7a879b;
  font-size: 13px;
}

.followup-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.followup-body {
  display: grid;
  gap: 10px;
}

.followup-row {
  line-height: 1.7;
  color: #4a5872;
}

.followup-row span,
.followup-media > span {
  color: #27364f;
  font-weight: 600;
}

.followup-media {
  display: grid;
  gap: 10px;
}

.image-list {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.image-list :deep(.el-image) {
  width: 112px;
  height: 112px;
  border-radius: 10px;
}

.video-link-list {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.followup-actions {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
</style>
