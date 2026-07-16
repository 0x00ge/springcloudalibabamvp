<script setup lang="ts">
import {computed, reactive, ref} from 'vue'
import {ElMessage, ElMessageBox, type FormInstance, type FormRules} from 'element-plus'

import {createUser, deleteUser, selectUsers, updateUser} from '@/api/apiUser.js'
import type {OptionItem, UserConfig, UserForm, UserParams, UserQuery} from '@/types/userTypes'

const userId = ref<string>('')
const userList = ref<UserParams[]>([])
const isCreateOrUpdate = ref<boolean>()
const titleOfCreateOrUpdate = computed(() => {
  return isCreateOrUpdate.value ? '新增用户' : '编辑用户'
})
const isVisibleOfCreateOrUpdate = ref<boolean>()

// Element Plus 表单实例，用于触发表单校验和清空校验状态。
const formInstance = ref<FormInstance>()

// 查询表单。
const userQuery = reactive<UserQuery>({
  name: '',
  phone: '',
  permission: '',
  email: '',
  status: '',
})

// 用户管理配置。
const userConfig = ref<UserConfig>({
  roleOptions: [
    {value: '管理员', tagType: 'warning'},
    {value: '普通用户', tagType: 'info'},
  ],
  statusOptions: [
    {value: '正常', tagType: 'success'},
    {value: '禁用', tagType: 'info'},
    {value: '注销', tagType: 'danger'},
  ],
})

// 重制表单。
const resetUserForm = reactive<UserForm>({
  name: '',
  phone: '',
  permission: '',
  status: '',
  email: '',
  passwordHash: '',
})

// 表单数据。
const userForm = reactive<UserForm>({
  name: '',
  phone: '',
  permission: '',
  status: '',
  email: '',
  passwordHash: '',
})

// 表单校验规则集中维护，提交前通过 formInstance.validate() 统一触发。
const rules: FormRules<UserForm> = {
  name: [{required: true, message: '请输入用户名', trigger: 'blur'}],
  phone: [{required: true, message: '请输入手机号', trigger: 'blur'}],
  permission: [{required: true, message: '请选择角色', trigger: 'change'}],
  email: [{type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur'}],
  status: [{required: true, message: '请选择状态', trigger: 'change'}],
  passwordHash: [{required: true, message: '请输入初始密码', trigger: 'blur'}],
}

// 重置表单。新增前、弹窗关闭后都会调用，保证上一次编辑的数据不会残留到下一次新增。
const handleResetUserForm = () => {
  isCreateOrUpdate.value = undefined
  userId.value = ''
  Object.assign(userForm, resetUserForm)
  formInstance.value?.clearValidate()
}

// 应用查询条件，请求后端 /user/page 按条件查询。
const handleQueryUsers = async () => {
  await handleSelectUsers()
}

// 清空查询条件，并重新请求后端列表。
const handleClearQueryUsers = async () => {
  Object.assign(userQuery, resetUserForm)
  await handleSelectUsers()
}

const handleSelectUsers = async () => {
  userList.value = await selectUsers(userQuery)
}

const handleSaveUser = () => {
  handleResetUserForm()
  isCreateOrUpdate.value = true
  isVisibleOfCreateOrUpdate.value = true
}

const handleUpdateUser = (user: UserParams) => {
  isCreateOrUpdate.value = false
  userId.value = user.id || ''
  Object.assign(userForm, {
    name: user.name,
    phone: user.phone,
    permission: user.permission,
    status: user.status,
    email: user.email,
    passwordHash: user.passwordHash,
  })
  isVisibleOfCreateOrUpdate.value = true
}

const handleSaveOrUpdateUserSubmit = async () => {
  if (!formInstance.value) return

  await formInstance.value.validate()

  if (isCreateOrUpdate.value === true) {
    await createUser(userForm)
    ElMessage.success('用户新增成功')
  } else {
    await updateUser(userId.value, userForm)
    ElMessage.success('用户更新成功')
  }

  isVisibleOfCreateOrUpdate.value = false
  await handleSelectUsers()
}

const handleDeleteUser = async (user: UserParams) => {
  if (!user.id) return

  await ElMessageBox.confirm(`确定删除用户「${user.name}」吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })

  await deleteUser(user.id)
  ElMessage.success('用户删除成功')
  await handleSelectUsers()
}
</script>

<template>
  <div class="page-view">

    <!-- 顶部操作区：左侧是多字段联合查询，右侧是新增入口。 -->
    <div class="page-header">
      <div class="query-panel">
        <el-input v-model="userQuery.name" class="query-input" clearable placeholder="用户名"
                  @keyup.enter="handleQueryUsers"/>
        <el-input v-model="userQuery.phone" class="query-input" clearable placeholder="手机号"
                  @keyup.enter="handleQueryUsers"/>
        <el-select v-model="userQuery.permission" class="query-select" clearable placeholder="角色">
          <el-option
              v-for="item in userConfig.roleOptions"
              :key="item.value"
              :value="item.value"
              :label="item.value"
          />
        </el-select>
        <el-input v-model="userQuery.email" class="query-input" clearable placeholder="邮箱"
                  @keyup.enter="handleQueryUsers"/>
        <!-- 状态查询：选项来自 fetchUserPageConfig，和表格 tag 颜色共用同一份字典。 -->
        <el-select v-model="userQuery.status" class="query-select" clearable placeholder="状态">
          <el-option
              v-for="item in userConfig.statusOptions"
              :key="item.value"
              :value="item.value"
              :label="item.value"
          />
        </el-select>
        <el-button type="primary" @click="handleQueryUsers">查询</el-button>
        <el-button @click="handleClearQueryUsers">清空</el-button>
      </div>

      <el-button type="primary" @click="handleSaveUser">新增</el-button>
    </div>

    <!-- 用户表格：数据来自 userList，操作列调用同一个弹窗和删除流程。 -->
    <el-table :data="userList" stripe>
      <el-table-column prop="name" label="用户名" min-width="140"/>
      <el-table-column prop="phone" label="手机号" min-width="140"/>
      <el-table-column prop="permission" label="角色" min-width="140"/>
      <el-table-column prop="email" label="邮箱" min-width="220"/>
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="row.status">{{ row.status }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleUpdateUser(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDeleteUser(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑弹窗：通过 isCreateOrUpdate 区分模式，表单结构完全复用。 -->
    <el-dialog v-model="isVisibleOfCreateOrUpdate" :title="titleOfCreateOrUpdate" width="460px"
               @closed="handleResetUserForm">
      <el-form ref="formInstance" :model="userForm" :rules="rules" label-width="80px">
        <!-- 用户名：普通输入框，必填校验在 rules.name 中维护。 -->
        <el-form-item label="用户名" prop="name">
          <el-input v-model="userForm.name" placeholder="请输入用户名"/>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone" placeholder="请输入手机号"/>
        </el-form-item>
        <!-- 角色：选项从 /users/config 获取，不在页面里写死。 -->
        <el-form-item label="角色" prop="permission">
          <el-select v-model="userForm.permission" placeholder="请选择角色">
            <el-option
                v-for="item in userConfig.roleOptions"
                :key="item.value"
                :label="item.value"
                :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="请输入邮箱"/>
        </el-form-item>
        <!-- 初始密码只在新增用户时填写；编辑用户时保持后端已有 passwordHash，不在页面暴露。 -->
        <el-form-item v-if="isCreateOrUpdate" label="初始密码" prop="passwordHash">
          <el-input v-model="userForm.passwordHash" placeholder="请输入初始密码" show-password/>
        </el-form-item>
        <!-- 状态：状态选项从配置接口返回，和表格 tag 颜色使用同一份数据源。 -->
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="userForm.status">
            <el-radio-button
                v-for="item in userConfig.statusOptions"
                :key="item.value"
                :label="item.value"
                :value="item.value"
            />
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="isVisibleOfCreateOrUpdate = false">取消</el-button>
        <el-button type="primary" @click="handleSaveOrUpdateUserSubmit">提交</el-button>
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
