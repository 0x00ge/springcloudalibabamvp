<script setup lang="ts">
import {computed, onMounted, reactive, ref} from 'vue'
import {ElMessage, ElMessageBox, type FormInstance, type FormRules} from 'element-plus'

import {
  createMenu,
  deleteMenu,
  getMenuTree,
  resetMenuTree,
  updateMenu,
} from '@/api/apiMenu.ts'
import type {MenuItem} from '@/types/layoutTypes.ts'
import type {MenuForm} from "@/types/appMenuTypes.ts";

interface MenuOption {
  id: string
  title: string
  disabled: boolean
}

const menuList = ref<MenuItem[]>([])
const loading = ref(false)
const isVisibleOfCreateOrUpdate = ref(false)
const isCreateOrUpdate = ref(true)
const currentMenuId = ref('')
const formInstance = ref<FormInstance>()

// 菜单表单只维护后端 /menu 新增、编辑需要的字段。
const menuForm = reactive<MenuForm>({
  parentId: '',
  title: '',
  path: '',
  icon: '',
  sortOrder: 0,
})

// 菜单校验规则和后端 MenuDto 保持一致，提交前先在前端拦截明显错误。
const rules: FormRules<MenuForm> = {
  title: [
    {required: true, message: '请输入菜单名称', trigger: 'blur'},
    {max: 50, message: '菜单名称长度不能超过 50 位', trigger: 'blur'},
  ],
  path: [
    {required: true, message: '请输入菜单路径', trigger: 'blur'},
    {pattern: /^\/.*/, message: '菜单路径必须以 / 开头', trigger: 'blur'},
    {max: 200, message: '菜单路径长度不能超过 200 位', trigger: 'blur'},
  ],
  icon: [{max: 50, message: '图标名称长度不能超过 50 位', trigger: 'blur'}],
  sortOrder: [{type: 'number', min: 0, message: '排序值不能小于 0', trigger: 'change'}],
}

const titleOfCreateOrUpdate = computed(() => (isCreateOrUpdate.value ? '新增菜单' : '编辑菜单'))

const disabledParentIds = computed(() => {
  if (isCreateOrUpdate.value || !currentMenuId.value) return new Set<string>()

  const ids = new Set<string>([currentMenuId.value])

  const collectChildren = (menus: MenuItem[]) => {
    for (const menu of menus) {
      if (ids.has(menu.parentId || '')) {
        ids.add(menu.id)
      }

      if (menu.children?.length) {
        collectChildren(menu.children)
      }
    }
  }

  collectChildren(menuList.value)

  return ids
})

// 父菜单下拉使用扁平列表展示，用缩进表达层级，新增子菜单时可以选任意已有菜单作为父级。
const parentOptions = computed<MenuOption[]>(() => {
  const options: MenuOption[] = []

  const walk = (menus: MenuItem[], level = 0) => {
    for (const menu of menus) {
      options.push({
        id: menu.id,
        title: `${'　'.repeat(level)}${menu.title}`,
        disabled: disabledParentIds.value.has(menu.id),
      })

      if (menu.children?.length) {
        walk(menu.children, level + 1)
      }
    }
  }

  walk(menuList.value)

  return options
})

const notifyMenuUpdated = () => {
  window.dispatchEvent(new Event('mvp:menu-updated'))
}

const handleSelectMenus = async () => {
  loading.value = true

  try {
    menuList.value = await getMenuTree()
  } finally {
    loading.value = false
  }
}

const handleResetMenuForm = () => {
  currentMenuId.value = ''
  Object.assign(menuForm, {
    parentId: '',
    title: '',
    path: '',
    icon: '',
    sortOrder: 0,
  })
  formInstance.value?.clearValidate()
}

const handleCreateRootMenu = () => {
  handleResetMenuForm()
  isCreateOrUpdate.value = true
  isVisibleOfCreateOrUpdate.value = true
}

const handleCreateChildMenu = (menu: MenuItem) => {
  handleResetMenuForm()
  isCreateOrUpdate.value = true
  menuForm.parentId = menu.id
  menuForm.path = menu.path.endsWith('/') ? menu.path : `${menu.path}/`
  isVisibleOfCreateOrUpdate.value = true
}

const handleUpdateMenu = (menu: MenuItem) => {
  handleResetMenuForm()
  isCreateOrUpdate.value = false
  currentMenuId.value = menu.id
  Object.assign(menuForm, {
    parentId: menu.parentId || '',
    title: menu.title,
    path: menu.path,
    icon: menu.icon || '',
    sortOrder: menu.sortOrder ?? 0,
  })
  isVisibleOfCreateOrUpdate.value = true
}

const handleSubmit = async () => {
  if (!formInstance.value) return

  await formInstance.value.validate()

  const payload: MenuForm = {
    parentId: menuForm.parentId || undefined,
    title: menuForm.title,
    path: menuForm.path,
    icon: menuForm.icon || undefined,
    sortOrder: menuForm.sortOrder ?? 0,
  }

  if (isCreateOrUpdate.value) {
    await createMenu(payload)
    ElMessage.success('菜单新增成功')
  } else {
    await updateMenu(currentMenuId.value, payload)
    ElMessage.success('菜单更新成功')
  }

  isVisibleOfCreateOrUpdate.value = false
  await handleSelectMenus()
  notifyMenuUpdated()
}

const handleDeleteMenu = async (menu: MenuItem) => {
  await ElMessageBox.confirm(`确定删除菜单「${menu.title}」及其所有子菜单吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })

  await deleteMenu(menu.id)
  ElMessage.success('菜单删除成功')
  await handleSelectMenus()
  notifyMenuUpdated()
}

const handleResetDefaultMenus = async () => {
  await ElMessageBox.confirm('确定恢复当前用户的默认菜单吗？现有菜单会被重置。', '恢复默认菜单', {
    type: 'warning',
    confirmButtonText: '恢复',
    cancelButtonText: '取消',
  })

  menuList.value = await resetMenuTree()
  ElMessage.success('默认菜单已恢复')
  notifyMenuUpdated()
}

onMounted(handleSelectMenus)
</script>

<template>
  <div class="page-view">
    <!-- 顶部操作区：菜单管理只做树形维护，不做分页。 -->
    <div class="page-header">
      <div>
        <h2>菜单管理</h2>
      </div>

      <div class="actions">
        <el-button @click="handleResetDefaultMenus">恢复默认</el-button>
        <el-button type="primary" @click="handleCreateRootMenu">新增根菜单</el-button>
      </div>
    </div>

    <!-- 菜单树表：children 字段由后端 /menu/tree 返回，Element Plus 会自动渲染层级。 -->
    <el-table
        v-loading="loading"
        :data="menuList"
        row-key="id"
        stripe
        default-expand-all
    >
      <el-table-column prop="title" label="菜单名称" min-width="180"/>
      <el-table-column prop="path" label="路由路径" min-width="220"/>
      <el-table-column prop="sortOrder" label="排序" width="100"/>
      <el-table-column prop="id" label="菜单 ID" min-width="260"/>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleCreateChildMenu(row)">添加子菜单</el-button>
          <el-button link type="primary" @click="handleUpdateMenu(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDeleteMenu(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑弹窗：新增根菜单时父菜单为空，添加子菜单时自动带入 parentId。 -->
    <el-dialog
        v-model="isVisibleOfCreateOrUpdate"
        :title="titleOfCreateOrUpdate"
        width="520px"
        @closed="handleResetMenuForm"
    >
      <el-form ref="formInstance" :model="menuForm" :rules="rules" label-width="90px">
        <el-form-item label="父菜单" prop="parentId">
          <el-select v-model="menuForm.parentId" clearable placeholder="不选择则作为根菜单">
            <el-option label="根菜单" value=""/>
            <el-option
                v-for="item in parentOptions"
                :key="item.id"
                :disabled="item.disabled"
                :label="item.title"
                :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="菜单名称" prop="title">
          <el-input v-model="menuForm.title" placeholder="请输入菜单名称"/>
        </el-form-item>

        <el-form-item label="路由路径" prop="path">
          <el-input v-model="menuForm.path" placeholder="例如 /home/user"/>
        </el-form-item>

        <el-form-item label="图标名称" prop="icon">
          <el-input v-model="menuForm.icon" placeholder="当前菜单不展示图标，可留空"/>
        </el-form-item>

        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="menuForm.sortOrder" :min="0" :step="1" controls-position="right"/>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="isVisibleOfCreateOrUpdate = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">提交</el-button>
      </template>
    </el-dialog>
  </div>
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

.page-header h2 {
  margin: 0;
  color: #111827;
  font-size: 20px;
  line-height: 1.4;
}

.page-header p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 14px;
}

.actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

@media (max-width: 768px) {
  .page-header {
    align-items: stretch;
    flex-direction: column;
  }

  .actions {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
