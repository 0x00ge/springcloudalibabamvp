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

// 部门表格数据，所有列表内容都来自部门接口，不在页面中写死。
const departments = ref<DepartmentItem[]>([])
// 表格加载状态：部门列表请求期间使用。
const tableLoading = ref(false)
// 配置加载状态：状态选项、默认表单等配置请求期间使用。
const configLoading = ref(false)

// keyword 是查询条件，会传给 /departments 接口做筛选。
const keyword = ref('')
// 控制新增/编辑弹窗显示隐藏。
const dialogVisible = ref(false)
// editingId 有值表示正在编辑部门；为空表示新增部门。
const editingId = ref<number>()
// Element Plus 表单实例，用来执行 validate 和 clearValidate。
const formRef = ref<FormInstance>()
// 部门状态选项由 MockJS 配置接口返回，并携带 tagType 控制表格状态颜色。
const statusOptions = ref<OptionItem[]>([])

// form 是真正绑定到输入框上的表单数据，用户在弹窗里输入或编辑时，修改的就是它。
const form = reactive<DepartmentForm>({
  name: '',
  leader: '',
  memberCount: 0,
  status: '',
  description: '',
})

// defaultForm 是接口下发的默认表单模板，不直接绑定输入框，只用来重置 form。
const defaultForm = reactive<DepartmentForm>({
  name: '',
  leader: '',
  memberCount: 0,
  status: '',
  description: '',
})

// 部门表单校验规则，提交前统一通过 formRef.validate() 触发。
const rules: FormRules<DepartmentForm> = {
  name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
  leader: [{ required: true, message: '请输入负责人', trigger: 'blur' }],
  memberCount: [{ required: true, message: '请输入成员数量', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

// 弹窗标题根据 editingId 自动切换，避免新增和编辑维护两份 UI。
const dialogTitle = computed(() => (editingId.value ? '编辑部门' : '新增部门'))
// 页面整体 loading 合并配置请求和表格请求，任意请求未结束都显示加载态。
const pageLoading = computed(() => tableLoading.value || configLoading.value)
// 把接口返回的状态选项转换成 Map，表格渲染时可按状态快速取 tagType。
const statusTagTypeMap = computed(() =>
  statusOptions.value.reduce<Record<string, OptionItem['tagType']>>((map, item) => {
    map[item.value] = item.tagType

    return map
  }, {}),
)

// 加载部门列表：
// 1. 页面初始化、查询、重置、新增/编辑/删除成功后都会调用。
// 2. fetchDepartments 内部通过 axios 发起请求。
// 3. 本地开发时由 MockJS 拦截 /api/departments 并返回动态数据。
const loadDepartments = async () => {
  tableLoading.value = true

  try {
    departments.value = await fetchDepartments(keyword.value)
  } finally {
    tableLoading.value = false
  }
}

// 加载部门管理页面配置：
// - statusOptions：状态单选项和表格 tag 颜色。
// - defaultForm：新增部门时的默认表单值。
// 这些配置都走接口，后续接后端时页面代码不用改。
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

// 重置弹窗表单：
// 新增前、弹窗关闭后都会调用，避免上一次编辑的数据污染下一次新增。
const resetForm = () => {
  editingId.value = undefined
  Object.assign(form, defaultForm)
  formRef.value?.clearValidate()
}

// 打开新增弹窗：先恢复默认表单，再显示弹窗。
const openCreateDialog = () => {
  resetForm()
  dialogVisible.value = true
}

// 打开编辑弹窗：记录当前部门 id，并把表格当前行数据回填到表单。
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

// 提交表单：
// - 先做表单校验。
// - 编辑模式调用 updateDepartment。
// - 新增模式调用 createDepartment。
// - 成功后关闭弹窗并刷新列表。
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

// 删除部门：
// 通过确认弹窗降低误删风险，确认后调用删除接口并刷新列表。
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

// 重置查询条件：清空关键字后重新加载完整部门列表。
const handleReset = () => {
  keyword.value = ''
  loadDepartments()
}

// 状态颜色由接口配置中的 tagType 决定，页面只负责按配置渲染。
// 如果接口没有返回对应颜色，默认使用 info，保证 UI 有兜底。
const getStatusTagType = (status: string) => statusTagTypeMap.value[status] || 'info'

// 页面挂载后先加载配置，再加载列表。
// 这样列表状态颜色和新增默认表单都能拿到接口配置。
onMounted(async () => {
  await loadDepartmentPageConfig()
  await loadDepartments()
})
</script>

<template>
  <section class="page-view">
    <!-- 页面头部：左侧显示模块标题，右侧提供新增部门入口。 -->
    <div class="page-header">
      <div>
        <p class="eyebrow">Department Management</p>
        <h1>部门管理</h1>
      </div>
      <el-button type="primary" @click="openCreateDialog">新增部门</el-button>
    </div>

    <!-- 部门列表卡片：pageLoading 同时覆盖配置请求和列表请求。 -->
    <el-card v-loading="pageLoading" class="table-card" shadow="never">
      <!-- 查询工具栏：关键字支持部门名称、负责人、状态的 mock 筛选。 -->
      <div class="toolbar">
        <el-input v-model="keyword" class="search-input" placeholder="搜索部门名称、负责人或状态" clearable />
        <el-button type="primary" @click="loadDepartments">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <!-- 部门表格：展示接口返回的部门列表，操作列负责打开编辑弹窗或删除确认。 -->
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

    <!-- 新增/编辑弹窗：同一套表单通过 editingId 区分新增和编辑。 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <!-- 部门名称：必填字段，用来标识部门。 -->
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入部门名称" />
        </el-form-item>
        <!-- 负责人：必填字段，当前使用普通输入框，后续可替换为用户选择器。 -->
        <el-form-item label="负责人" prop="leader">
          <el-input v-model="form.leader" placeholder="请输入负责人" />
        </el-form-item>
        <!-- 成员数量：使用数字输入框，限制最小值为 0，避免录入负数。 -->
        <el-form-item label="成员数量" prop="memberCount">
          <el-input-number v-model="form.memberCount" :min="0" :step="1" controls-position="right" />
        </el-form-item>
        <!-- 状态：选项来自 /departments/config，和表格 tag 颜色共用同一份配置。 -->
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
        <!-- 说明：非必填字段，用 textarea 方便填写较长描述。 -->
        <el-form-item label="说明">
          <el-input v-model="form.description" :rows="3" placeholder="请输入部门说明" type="textarea" />
        </el-form-item>
      </el-form>

      <!-- 弹窗底部操作：取消只关闭弹窗，保存会触发表单校验和接口提交。 -->
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped lang="less">
.page-view {
  /* 当前页面内部统一盒模型，配合全局清零样式保证宽度计算稳定。 */
  box-sizing: border-box;
  /* 页面使用网格纵向排列，统一控制头部和表格之间的间距。 */
  display: grid;
  gap: 16px;
}

.page-view *,
.page-view *::before,
.page-view *::after {
  /* 只在当前页面范围内继承 border-box，不修改 style.less。 */
  box-sizing: border-box;
}

.page-header {
  /* 标题区和新增按钮左右分布，保持后台页面常见操作区结构。 */
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.page-header h1 {
  /* 页面主标题字号比普通卡片标题更醒目。 */
  margin-top: 4px;
  color: #1f2937;
  font-size: 26px;
  line-height: 1.2;
}

.eyebrow {
  /* 英文辅助标题弱化显示，只作为页面识别补充。 */
  color: #64748b;
  font-size: 13px;
  font-weight: 600;
}

.table-card {
  /* 表格区域使用轻边框卡片，和后台主体浅灰背景拉开层级。 */
  border: 1px solid #e1e6ef;
  border-radius: 8px;
}

.table-card :deep(.el-card__body) {
  /* 卡片内容区保持统一内边距，避免全局清零后只依赖组件默认值。 */
  padding: 18px;
}

.toolbar {
  /* 查询条件和按钮横向排列，便于快速筛选。 */
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.search-input {
  /* 搜索框固定宽度，避免桌面端占用过多工具栏空间。 */
  width: 320px;
}

@media (max-width: 768px) {
  .page-header,
  .toolbar {
    /* 小屏下改成纵向排列，避免按钮和输入框被挤压。 */
    align-items: stretch;
    flex-direction: column;
  }

  .search-input {
    /* 移动端搜索框占满一行，提升输入体验。 */
    width: 100%;
  }

  .toolbar :deep(.el-button) {
    /* 移动端工具栏按钮占满整行，避免宽度不一致。 */
    width: 100%;
  }
}
</style>
