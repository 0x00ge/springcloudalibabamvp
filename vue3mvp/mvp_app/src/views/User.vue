<script setup lang="ts">
import {computed, onMounted, reactive, ref} from 'vue'
import {ElMessage, ElMessageBox, type FormInstance, type FormRules} from 'element-plus'

import {createUser, deleteUser, fetchUserPageConfig, fetchUsers, updateUser} from '@/api/apiUser.js'
import type {OptionItem} from '@/types/types.js'
import type {UserForm, UserItem} from '@/types/userTypes'

// 用户表格数据，页面只关心渲染结果，真实数据来源统一交给 api 层。
const users = ref<UserItem[]>([])
// 表格加载状态：只控制用户列表请求期间的 loading。
const tableLoading = ref(false)
// 页面配置加载状态：角色、状态、默认表单等字典请求期间使用。
const configLoading = ref(false)

// 查询输入区的临时表单值。用户在输入框里改值时，只先改这里，不立刻过滤表格。
const queryForm = reactive({
  name: '',
  phone: '',
  role: '',
  email: '',
  status: '',
})

// 已生效的查询条件。点击“查询”时由 queryForm 复制到这里，filteredUsers 只读取 activeQuery。
// 这样用户输入一半时不会频繁改变表格结果，也方便“清空”一次性撤销所有条件。
const activeQuery = reactive({
  name: '',
  phone: '',
  role: '',
  email: '',
  status: '',
})

const isCreateOrUpdate = ref<boolean>()
const titleOfCreateOrUpdate = ref<string>(isCreateOrUpdate.value === true ? '新增用户' : '编辑用户')
const isVisibleOfCreateOrUpdate = ref<boolean>()
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
  passwordHash: '123456',
})

// defaultForm 是接口下发的默认表单模板，不直接绑定输入框，只用来重置 form。
const defaultForm = reactive<UserForm>({
  name: '',
  phone: '',
  role: '',
  status: '',
  email: '',
  passwordHash: '123456',
})

// 表单校验规则集中维护，提交前通过 formRef.validate() 统一触发。
const rules: FormRules<UserForm> = {
  name: [{required: true, message: '请输入用户名', trigger: 'blur'}],
  phone: [{required: true, message: '请输入手机号', trigger: 'blur'}],
  role: [{required: true, message: '请选择角色', trigger: 'change'}],
  email: [{type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur'}],
  status: [{required: true, message: '请选择状态', trigger: 'change'}],
  passwordHash: [{required: true, message: '请输入初始密码', trigger: 'blur'}],
}


// 页面整体 loading 合并表格请求和配置请求，任意一个请求未完成时都显示加载态。
const pageLoading = computed(() => tableLoading.value || configLoading.value)
// 把接口返回的状态配置转换成 Map，表格渲染 tag 时可以快速按状态取颜色。
const statusTagTypeMap = computed(() =>
    statusOptions.value.reduce<Record<string, OptionItem['tagType']>>((map, item) => {
      map[item.value] = item.tagType

      return map
    }, {}),
)

// 表格最终展示的数据。
//
// 查询规则：
// 1. 每个字段都可以单独查询，例如只填手机号。
// 2. 多个字段同时填写时采用 AND 联合查询，所有条件都满足才展示。
// 3. 文本比较统一转成小写，降低大小写输入差异带来的影响。
const filteredUsers = computed(() => {
  // Object.entries 会把 activeQuery 转成 [字段名, 查询值] 列表。
  // 空字符串条件会被过滤掉，避免未填写字段参与匹配。
  const queryEntries = Object.entries(activeQuery)
      .map(([field, value]) => [field, value.trim().toLowerCase()] as const)
      .filter(([, value]) => value)

  if (queryEntries.length === 0) return users.value

  // every 表示联合查询：只有所有已填写字段都命中，当前用户才会保留。
  return users.value.filter((user) =>
      queryEntries.every(([field, value]) =>
          String(user[field as keyof Pick<UserItem, 'name' | 'phone' | 'role' | 'email' | 'status'>] || '')
              .toLowerCase()
              .includes(value),
      ),
  )
})

// 加载用户列表：
// 1. 页面初始化、新增/编辑/删除成功后都会调用。
// 2. fetchUsers 内部走 utils/http 封装的 axios。
// 3. 用户数据来自真实后端 /user/page。
const loadUsers = async () => {
  tableLoading.value = true

  try {
    users.value = await fetchUsers()
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
const resetUserForm =
    () => {
      isCreateOrUpdate.value = undefined
      Object.assign(form, defaultForm)
      formRef.value?.clearValidate()
    }

const handleSaveUser = () => {
  resetUserForm()
  isCreateOrUpdate.value = true
  isVisibleOfCreateOrUpdate.value = true
}

// 应用查询条件。
// 注意这里只更新 activeQuery，不重新请求后端；查询基于当前 users 列表在前端完成。
const handleQuery = () => {
  Object.assign(activeQuery, queryForm)
}

// 清空查询条件，并同步清空已经生效的 activeQuery。
const handleClearQuery = () => {
  Object.assign(queryForm, {
    name: '',
    phone: '',
    role: '',
    email: '',
    status: '',
  })
  Object.assign(activeQuery, queryForm)
}

// 打开编辑弹窗：记录当前用户 id，并把当前行数据回填到表单中。
const handleUpdateUser =
    (user: UserItem) => {
      Object.assign(form, {
        name: user.name,
        phone: user.phone,
        role: user.role,
        status: user.status,
        email: user.email,
        passwordHash: user.passwordHash,
      })
      isVisibleOfCreateOrUpdate.value = true
    }

// 提交表单：
// - 先执行 Element Plus 表单校验。
// - 有 isCreateOrUpdate 调更新接口；没有 isCreateOrUpdate 调新增接口。
// - 成功后关闭弹窗并重新拉取列表，保证表格展示最新 mock 数据。
const handleSaveOrUpdateSubmit =
    async () => {
      if (!formRef.value) return

      await formRef.value.validate()

      if (isCreateOrUpdate.value) {
        await updateUser(isCreateOrUpdate.value, form)
        ElMessage.success('用户新增成功')
      } else {
        await createUser(form)
        ElMessage.success('用户新增成功')
      }

      isVisibleOfCreateOrUpdate.value = false
      await loadUsers()
    }

// 删除用户: 确认->删除->刷新
const handleDeleteUser =
    async (user: UserItem) => {
      await ElMessageBox.confirm(`确定删除用户「${user.name}」吗？`, '删除确认', {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
      })

      await deleteUser(user.id)
      ElMessage.success('用户删除成功')
      await loadUsers()
    }

// 状态颜色由配置中的 tagType 决定，页面不关心具体状态文案。
// 如果后端新增了别的状态但没给颜色，默认使用 info，避免页面报错。
const getStatusTagType =
    (status: string) => statusTagTypeMap.value[status] || 'info'

// 页面挂载后先加载字典配置，再加载列表。
// 这样表格状态颜色、弹窗默认值都能在数据展示前准备好。
onMounted(
    async () => {
      await loadUserPageConfig()
      await loadUsers()
    })
</script>

<template>
  <div class="page-view">

    <!-- 顶部操作区：左侧是多字段联合查询，右侧是新增入口。 -->
    <div class="page-header">
      <div class="query-panel">
        <el-input v-model="queryForm.name" class="query-input" clearable placeholder="用户名"
                  @keyup.enter="handleQuery"/>
        <el-input v-model="queryForm.phone" class="query-input" clearable placeholder="手机号"
                  @keyup.enter="handleQuery"/>
        <el-select v-model="queryForm.role" class="query-select" clearable placeholder="角色">
          <el-option
              v-for="item in roleOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
          />
        </el-select>
        <el-input v-model="queryForm.email" class="query-input" clearable placeholder="邮箱"
                  @keyup.enter="handleQuery"/>
        <!-- 状态查询：选项来自 fetchUserPageConfig，和表格 tag 颜色共用同一份字典。 -->
        <el-select v-model="queryForm.status" class="query-select" clearable placeholder="状态">
          <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
          />
        </el-select>
        <el-button type="primary" @click="handleQuery">查询</el-button>
        <el-button @click="handleClearQuery">清空</el-button>
      </div>

      <el-button type="primary" @click="handleSaveUser">新增</el-button>
    </div>

    <!-- 用户列表卡片：loading 绑定 pageLoading，让配置或列表请求期间都有反馈。 -->
    <el-card v-loading="pageLoading" class="table-card" shadow="never">
      <!-- 用户表格：数据来自 users，操作列调用同一个弹窗和删除流程。 -->
      <el-table v-loading="tableLoading" :data="filteredUsers" stripe>
        <el-table-column prop="name" label="用户名" min-width="140"/>
        <el-table-column prop="phone" label="手机号" min-width="140"/>
        <el-table-column prop="role" label="角色" min-width="140"/>
        <el-table-column prop="email" label="邮箱" min-width="220"/>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleUpdateUser(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDeleteUser(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗：通过 isCreateOrUpdate 区分模式，表单结构完全复用。 -->
    <el-dialog v-model="isVisibleOfCreateOrUpdate" :title="titleOfCreateOrUpdate" width="460px" @closed="resetUserForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <!-- 用户名：普通输入框，必填校验在 rules.name 中维护。 -->
        <el-form-item label="用户名" prop="name">
          <el-input v-model="form.name" placeholder="请输入用户名"/>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号"/>
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
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱"/>
        </el-form-item>
        <!-- 初始密码只在新增用户时填写；编辑用户时保持后端已有 passwordHash，不在页面暴露。 -->
        <el-form-item v-if="isCreateOrUpdate" label="初始密码" prop="passwordHash">
          <el-input v-model="form.passwordHash" placeholder="请输入初始密码" show-password/>
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

      <template #footer>
        <el-button @click="isVisibleOfCreateOrUpdate = false">取消</el-button>
        <el-button type="primary" @click="handleSaveOrUpdateSubmit">提交</el-button>
      </template>
    </el-dialog>

  </div>
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

.query-panel {
  display: flex;
  align-items: center;
  gap: 10px;
}

.query-input {
  width: 150px;
}

.query-select {
  width: 128px;
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

@media (max-width: 768px) {
  .page-header {
    /* 小屏下改成纵向排列，避免按钮和输入框被挤压。 */
    align-items: stretch;
    flex-direction: column;
  }

  .query-panel {
    align-items: stretch;
    flex-direction: column;
  }

  .query-input,
  .query-select,
  .query-panel :deep(.el-button) {
    width: 100%;
  }
}
</style>
