<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'

import { createMenu, deleteMenu, userMenuTreeCheck, updateMenu } from '@/api/menu.ts'
import { useAuthStore } from '@/stores/authStore.ts'
import type { MenuForm, MenuIconName, MenuItem } from '@/types/types.ts'

const authStore = useAuthStore()

const menuTree = ref<MenuItem[]>([])
const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const dialogVisible = ref(false)
const editingId = ref<string>()
const formRef = ref<FormInstance>()

const iconOptions: Array<{ label: string; value: MenuIconName }> = [
  { label: '设置', value: 'Setting' },
  { label: '菜单', value: 'Menu' },
  { label: '用户', value: 'UserFilled' },
  { label: '部门', value: 'OfficeBuilding' },
  { label: '票据', value: 'Tickets' },
]

const defaultForm = (): MenuForm => ({
  userId: authStore.currentAuth?.id || '',
  parentId: null,
  title: '',
  path: '',
  icon: 'Menu',
  sortOrder: 0,
})

const form = reactive<MenuForm>(defaultForm())

const rules: FormRules<MenuForm> = {
  userId: [{ required: true, message: '用户ID不能为空', trigger: 'blur' }],
  title: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  path: [{ required: true, message: '请输入路由路径', trigger: 'blur' }],
  sortOrder: [{ required: true, message: '请输入排序值', trigger: 'change' }],
}

const dialogTitle = computed(() => (editingId.value ? '编辑菜单' : '新增菜单'))

const parentOptions = computed(() => {
  const options: Array<{ label: string; value: string; disabled: boolean }> = []

  const walk = (menus: MenuItem[], prefix = '') => {
    menus.forEach((menu) => {
      options.push({
        label: prefix + menu.title,
        value: menu.id,
        disabled: menu.id === editingId.value,
      })
      if (menu.children?.length) {
        walk(menu.children, prefix + menu.title + ' / ')
      }
    })
  }

  walk(menuTree.value)
  return options
})

const filteredMenuTree = computed(() => {
  const value = keyword.value.trim().toLowerCase()
  if (!value) return menuTree.value

  const filterTree = (menus: MenuItem[]): MenuItem[] =>
    menus
      .map((menu) => {
        const children = filterTree(menu.children || [])
        const matched =
          menu.title.toLowerCase().includes(value) ||
          menu.path.toLowerCase().includes(value) ||
          (menu.icon || '').toLowerCase().includes(value)

        if (!matched && children.length === 0) return undefined

        return {
          ...menu,
          children,
        }
      })
      .filter(Boolean) as MenuItem[]

  return filterTree(menuTree.value)
})

const loadMenus = async () => {
  loading.value = true

  try {
    menuTree.value = await userMenuTreeCheck(authStore.currentAuth?.id)
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  editingId.value = undefined
  Object.assign(form, defaultForm())
  formRef.value?.clearValidate()
}

const openCreateDialog = () => {
  resetForm()
  dialogVisible.value = true
}

const openEditDialog = (menu: MenuItem) => {
  editingId.value = menu.id
  Object.assign(form, {
    userId: menu.userId || authStore.currentAuth?.id || '',
    parentId: menu.parentId || null,
    title: menu.title,
    path: menu.path,
    icon: menu.icon || 'Menu',
    sortOrder: menu.sortOrder || 0,
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate()
  saving.value = true

  try {
    if (editingId.value) {
      await updateMenu(editingId.value, form)
      ElMessage.success('菜单修改成功')
    } else {
      await createMenu(form)
      ElMessage.success('菜单新增成功')
    }

    dialogVisible.value = false
    await loadMenus()
  } finally {
    saving.value = false
  }
}

const handleDelete = async (menu: MenuItem) => {
  await ElMessageBox.confirm(`确定删除菜单「${menu.title}」吗？子菜单也会一起删除。`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })

  await deleteMenu(menu.id)
  ElMessage.success('菜单删除成功')
  await loadMenus()
}

const handleReset = () => {
  keyword.value = ''
}

onMounted(async () => {
  if (!authStore.currentAuth) {
    await authStore.getAuthAction()
  }
  Object.assign(form, defaultForm())
  await loadMenus()
})
</script>

<template>
  <section class="page-view">
    <div class="page-header">
      <div>
        <p class="eyebrow">Menu Management</p>
        <h1>菜单管理</h1>
      </div>
      <el-button type="primary" @click="openCreateDialog">新增菜单</el-button>
    </div>

    <el-card v-loading="loading" class="table-card" shadow="never">
      <div class="toolbar">
        <el-input v-model="keyword" class="search-input" placeholder="搜索菜单名称、路径或图标" clearable />
        <el-button type="primary" @click="loadMenus">刷新</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="filteredMenuTree"
        row-key="id"
        stripe
        default-expand-all
      >
        <el-table-column prop="title" label="菜单名称" min-width="180" />
        <el-table-column prop="path" label="路由路径" min-width="220" show-overflow-tooltip />
        <el-table-column prop="icon" label="图标" width="130">
          <template #default="{ row }">
            <el-tag v-if="row.icon" type="info">{{ row.icon }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="层级" width="90" />
        <el-table-column prop="sortOrder" label="排序" width="90" />
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="所属用户" prop="userId">
          <el-input v-model="form.userId" disabled />
        </el-form-item>
        <el-form-item label="父级菜单">
          <el-select v-model="form.parentId" clearable placeholder="一级菜单">
            <el-option label="一级菜单" :value="null" />
            <el-option
              v-for="item in parentOptions"
              :key="item.value"
              :disabled="item.disabled"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="菜单名称" prop="title">
          <el-input v-model="form.title" placeholder="请输入菜单名称" />
        </el-form-item>
        <el-form-item label="路由路径" prop="path">
          <el-input v-model="form.path" placeholder="/home/menu" />
        </el-form-item>
        <el-form-item label="图标">
          <el-select v-model="form.icon" placeholder="请选择图标">
            <el-option
              v-for="item in iconOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :step="10" controls-position="right" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button :loading="saving" type="primary" @click="handleSubmit">保存</el-button>
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

  .search-input,
  .toolbar :deep(.el-button) {
    width: 100%;
  }
}
</style>
