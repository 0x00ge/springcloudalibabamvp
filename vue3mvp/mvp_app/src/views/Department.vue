<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'

import {
  createDepartment,
  deleteDepartment,
  fetchDepartmentPageConfig,
  fetchDepartments,
  updateDepartment,
} from '@/api/department.ts'
import type { DepartmentForm, DepartmentItem, OptionItem } from '@/types/types.js'

const departments = ref<DepartmentItem[]>([])
const tableLoading = ref(false)
const configLoading = ref(false)

const keyword = ref('')
const dialogVisible = ref(false)
const editingId = ref<number>()
const formRef = ref<FormInstance>()
const statusOptions = ref<OptionItem[]>([])

const form = reactive<DepartmentForm>({
  name: '',
  leader: '',
  memberCount: 0,
  status: '',
  description: '',
})

const defaultForm = reactive<DepartmentForm>({
  name: '',
  leader: '',
  memberCount: 0,
  status: '',
  description: '',
})

const rules: FormRules<DepartmentForm> = {
  name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
  leader: [{ required: true, message: '请输入负责人', trigger: 'blur' }],
  memberCount: [{ required: true, message: '请输入成员数量', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

const dialogTitle = computed(() => (editingId.value ? '编辑部门' : '新增部门'))
const pageLoading = computed(() => tableLoading.value || configLoading.value)
const statusTagTypeMap = computed(() =>
  statusOptions.value.reduce<Record<string, OptionItem['tagType']>>((map, item) => {
    map[item.value] = item.tagType

    return map
  }, {}),
)

const loadDepartments = async () => {
  tableLoading.value = true

  try {
    departments.value = await fetchDepartments(keyword.value)
  } finally {
    tableLoading.value = false
  }
}

const loadDepartmentPageConfig = async () => {
  configLoading.value = true

  try {
    const config = await fetchDepartmentPageConfig()

    statusOptions.value = config.statusOptions
    Object.assign(defaultForm, config.defaultForm)
    Object.assign(form, config.defaultForm)
  } finally {
    configLoading.value = false
  }
}

const resetForm = () => {
  editingId.value = undefined
  Object.assign(form, defaultForm)
  formRef.value?.clearValidate()
}

const openCreateDialog = () => {
  resetForm()
  dialogVisible.value = true
}

const openEditDialog = (department: DepartmentItem) => {
  editingId.value = department.id
  Object.assign(form, {
    name: department.name,
    leader: department.leader,
    memberCount: department.memberCount,
    status: department.status,
    description: department.description,
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate()

  if (editingId.value) {
    await updateDepartment(editingId.value, form)
    ElMessage.success('部门修改成功')
  } else {
    await createDepartment(form)
    ElMessage.success('部门新增成功')
  }

  dialogVisible.value = false
  await loadDepartments()
}

const handleDelete = async (department: DepartmentItem) => {
  await ElMessageBox.confirm(`确定删除部门「${department.name}」吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })

  await deleteDepartment(department.id)
  ElMessage.success('部门删除成功')
  await loadDepartments()
}

const handleReset = () => {
  keyword.value = ''
  loadDepartments()
}

const getStatusTagType = (status: string) => statusTagTypeMap.value[status] || 'info'

onMounted(async () => {
  await loadDepartmentPageConfig()
  await loadDepartments()
})
</script>

<template>
  <section class="page-view">
    <div class="page-header">
      <div>
        <p class="eyebrow">Department Management</p>
        <h1>部门管理</h1>
      </div>
      <el-button type="primary" @click="openCreateDialog">新增部门</el-button>
    </div>

    <el-card v-loading="pageLoading" class="table-card" shadow="never">
      <div class="toolbar">
        <el-input v-model="keyword" class="search-input" placeholder="搜索部门名称、负责人或状态" clearable />
        <el-button type="primary" @click="loadDepartments">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <el-table v-loading="tableLoading" :data="departments" stripe>
        <el-table-column prop="name" label="部门名称" min-width="160" />
        <el-table-column prop="leader" label="负责人" min-width="120" />
        <el-table-column prop="memberCount" label="成员数量" width="120" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入部门名称" />
        </el-form-item>
        <el-form-item label="负责人" prop="leader">
          <el-input v-model="form.leader" placeholder="请输入负责人" />
        </el-form-item>
        <el-form-item label="成员数量" prop="memberCount">
          <el-input-number v-model="form.memberCount" :min="0" :step="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio-button
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-radio-group>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" :rows="3" placeholder="请输入部门说明" type="textarea" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped lang="less">
.page-view {
  box-sizing: border-box;
  display: grid;
  gap: 16px;
}

.page-view *,
.page-view *::before,
.page-view *::after {
  box-sizing: border-box;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.page-header h1 {
  margin-top: 4px;
  color: #1f2937;
  font-size: 26px;
  line-height: 1.2;
}

.eyebrow {
  color: #64748b;
  font-size: 13px;
  font-weight: 600;
}

.table-card {
  border: 1px solid #e1e6ef;
  border-radius: 8px;
}

.table-card :deep(.el-card__body) {
  padding: 18px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.search-input {
  width: 320px;
}

@media (max-width: 768px) {
  .page-header,
  .toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .search-input {
    width: 100%;
  }

  .toolbar :deep(.el-button) {
    width: 100%;
  }
}
</style>
