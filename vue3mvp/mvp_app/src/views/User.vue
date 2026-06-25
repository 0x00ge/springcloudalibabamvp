<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'

import { deleteUser, fetchUserPageConfig, fetchUsers, updateUser } from '@/api/apiUser.js'
import type { OptionItem, UserForm, UserItem } from '@/types/types.js'

// 用户表格数据，页面只关心渲染结果，真实数据来源统一交给 api 层。
const users = ref<UserItem[]>([])
// 表格加载状态：只控制用户列表请求期间的 loading。
const tableLoading = ref(false)
// 页面配置加载状态：角色、状态、默认表单等字典请求期间使用。
const configLoading = ref(false)

// keyword 是查询条件，会作为 query 参数传给 /users 接口。
const keyword = ref('')
// 控制新增/编辑弹窗显示隐藏，同一个弹窗复用两种场景。
const dialogVisible = ref(false)
// editingId 有值代表编辑模式。
const editingId = ref<string>()
// Element Plus 表单实例，用于触发表单校验和清空校验状态。
const formRef = ref<FormInstance>()
// 角色下拉选项由 MockJS 配置接口返回，避免页面写死业务字典。
const roleOptions = ref<OptionItem[]>([])
// 状态选项同样来自配置接口，并携带 tagType 用来控制表格标签颜色。
const statusOptions = ref<OptionItem[]>([])

// form 是真正绑定到输入框上的表单数据，用户在弹窗里输入或编辑时，修改的就是它。
const form = reactive<UserForm>({
  name: '',
  phone: '',
  role: '',
  status: '',
  email: '',
  passwordHash: '',
})

// defaultForm 是接口下发的默认表单模板，不直接绑定输入框，只用来重置 form。
const defaultForm = reactive<UserForm>({
  name: '',
  phone: '',
  role: '',
  status: '',
  email: '',
  passwordHash: '',
})

// 表单校验规则集中维护，提交前通过 formRef.validate() 统一触发。
const rules: FormRules<UserForm> = {
  name: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  email: [{ type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

const dialogTitle = computed(() => '编辑用户')
// 页面整体 loading 合并表格请求和配置请求，任意一个请求未完成时都显示加载态。
const pageLoading = computed(() => tableLoading.value || configLoading.value)
// 把接口返回的状态配置转换成 Map，表格渲染 tag 时可以快速按状态取颜色。
const statusTagTypeMap = computed(() =>
  statusOptions.value.reduce<Record<string, OptionItem['tagType']>>((map, item) => {
    map[item.value] = item.tagType

    return map
  }, {}),
)

// 加载用户列表：
// 1. 页面初始化、查询、重置、新增/编辑/删除成功后都会调用。
// 2. fetchUsers 内部走 utils/http 封装的 axios。
// 3. 用户数据来自真实后端 /user/page。
const loadUsers = async () => {
  tableLoading.value = true

  try {
    users.value = await fetchUsers(keyword.value)
  } finally {
    tableLoading.value = false
  }
}

// 加载用户管理页面配置：
// - roleOptions：角色下拉选项。
// - statusOptions：状态单选项和表格 tag 颜色。
// - defaultForm：新增用户时的默认表单值。
// 这些都走接口，后续接真实后端时只需要替换接口返回即可。
const loadUserPageConfig = async () => {
  configLoading.value = true

  try {
    const config = await fetchUserPageConfig()

    roleOptions.value = config.roleOptions
    statusOptions.value = config.statusOptions
    Object.assign(defaultForm, config.defaultForm)
    Object.assign(form, config.defaultForm)
  } finally {
    configLoading.value = false
  }
}

// 重置弹窗表单：
// 新增前、弹窗关闭后都会调用，保证上一次编辑的数据不会残留到下一次新增。
const resetForm = () => {
  editingId.value = undefined
  Object.assign(form, defaultForm)
  formRef.value?.clearValidate()
}

// 打开编辑弹窗：记录当前用户 id，并把当前行数据回填到表单中。
const openEditDialog = (user: UserItem) => {
  editingId.value = user.id
  Object.assign(form, {
    name: user.name,
    phone: user.phone,
    role: user.role,
    status: user.status,
    email: user.email,
    passwordHash: user.passwordHash,
  })
  dialogVisible.value = true
}

// 提交表单：
// - 先执行 Element Plus 表单校验。
// - 有 editingId 调更新接口；没有 editingId 调新增接口。
// - 成功后关闭弹窗并重新拉取列表，保证表格展示最新 mock 数据。
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate()

  if (!editingId.value) return

  await updateUser(editingId.value, form)
  ElMessage.success('用户修改成功')

  dialogVisible.value = false
  await loadUsers()
}

// 删除用户：
// 先弹出二次确认，用户确认后再请求删除接口，成功后刷新列表。
const handleDelete = async (user: UserItem) => {
  await ElMessageBox.confirm(`确定删除用户「${user.name}」吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })

  await deleteUser(user.id)
  ElMessage.success('用户删除成功')
  await loadUsers()
}

// 重置查询条件：清空关键字后重新请求完整用户列表。
const handleReset = () => {
  keyword.value = ''
  loadUsers()
}

// 状态颜色由接口配置中的 tagType 决定，页面不关心具体状态文案。
// 如果后端新增了别的状态但没给颜色，默认使用 info，避免页面报错。
const getStatusTagType = (status: string) => statusTagTypeMap.value[status] || 'info'

// 页面挂载后先加载字典配置，再加载列表。
// 这样表格状态颜色、弹窗默认值都能在数据展示前准备好。
onMounted(async () => {
  await loadUserPageConfig()
  await loadUsers()
})
</script>

<template>
  <section class="page-view">

    <!-- 用户列表卡片：loading 绑定 pageLoading，让配置或列表请求期间都有反馈。 -->
    <el-card v-loading="pageLoading" class="table-card" shadow="never">
      <!-- 查询工具栏：关键字双向绑定 keyword，查询和重置都重新请求接口。 -->
      <div class="toolbar">
        <el-input v-model="keyword" class="search-input" placeholder="搜索用户名称、手机号、角色或邮箱" clearable />
        <el-button type="primary" @click="loadUsers">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <!-- 用户表格：数据来自 users，操作列调用同一个弹窗和删除流程。 -->
      <el-table v-loading="tableLoading" :data="users" stripe>
        <el-table-column prop="name" label="用户名" min-width="140" />
        <el-table-column prop="phone" label="手机号" min-width="140" />
        <el-table-column prop="role" label="角色" min-width="140" />
        <el-table-column prop="email" label="邮箱" min-width="220" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗：通过 editingId 区分模式，表单结构完全复用。 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="460px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <!-- 用户名：普通输入框，必填校验在 rules.name 中维护。 -->
        <el-form-item label="用户名" prop="name">
          <el-input v-model="form.name" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <!-- 角色：选项从 /users/config 获取，不在页面里写死。 -->
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择角色">
            <el-option
              v-for="item in roleOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <!-- 邮箱：输入内容由 Element Plus 按 email 类型规则校验。 -->
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <!-- 状态：状态选项从配置接口返回，和表格 tag 颜色使用同一份数据源。 -->
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
