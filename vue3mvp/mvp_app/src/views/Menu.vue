<script setup lang="ts">
import {computed, onMounted, reactive, ref} from 'vue'
import {ElMessage, ElMessageBox, type FormInstance, type FormRules} from 'element-plus'

import {
  createMenu,
  deleteMenu,
  getMenuPage,
  getMenuTree,
  updateMenu,
} from '@/api/apiMenu.ts'
import type {MenuItem} from '@/types/layoutTypes.ts'
import type {MenuParams, MenuQuery} from "@/types/menuTypes.ts";

/** 父菜单下拉选项：扁平化树后的单条记录。 */
interface MenuOption {
  id: string
  /** 展示名；前缀用全角空格缩进，表达树层级。 */
  title: string
  /** 编辑时禁止选自己及子孙，避免形成环。 */
  disabled: boolean
}

/** 后端 /menu/tree 返回的完整菜单树，表格与父级下拉共用。 */
const menuItems = ref<MenuItem[]>([])
/** 父菜单下拉使用的完整菜单树。 */
const menuTree = ref<MenuItem[]>([])
/** 表格加载态。 */
const loading = ref(false)
/** 新增/编辑弹窗是否可见。 */
const isVisibleOfCreateOrUpdate = ref(false)
/**
 * true = 新增，false = 编辑。
 * 决定弹窗标题、提交走 create 还是 update，以及是否计算 disabledParentIds。
 */
const isCreateOrUpdate = ref(true)
/** 当前正在编辑的菜单 id；新增时为空。 */
const currentMenuId = ref('')
/** el-form 实例，用于校验与清空校验态。 */
const formInstance = ref<FormInstance>()

const menuQuery = reactive<MenuQuery>({
  title: '',
  path: '',
})

const menuPagination = reactive({
  page: 1,
  size: 10,
  total: 0,
})

// 菜单表单只维护后端 /menu 新增、编辑需要的字段。
const menuForm = reactive<MenuParams>({
  parentId: '',
  title: '',
  path: '',
  icon: '',
  sortOrder: 0,
})

// 菜单校验规则和后端 MenuDto 保持一致，提交前先在前端拦截明显错误。
const rules: FormRules<MenuParams> = {
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

/** 弹窗标题：随新增/编辑模式切换。 */
const titleOfCreateOrUpdate = computed(() => (isCreateOrUpdate.value ? '新增菜单' : '编辑菜单'))

/**
 * 编辑时不可选作「父菜单」的 id 集合。
 *
 * 包含当前菜单自身及其所有子孙：若把父级改成自己或子节点，会形成环，树无法正常展示。
 * 新增模式或未选定编辑对象时返回空集，父级下拉全部可选。
 */
const disabledParentIds = computed(() => {
  // 仅编辑场景需要禁用；新增时父级无环风险。
  if (isCreateOrUpdate.value || !currentMenuId.value) return new Set<string>()

  const ids = new Set<string>([currentMenuId.value])

  // 只要 parentId 已在禁用集合中，则该节点及其子树也一并禁用。
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

  collectChildren(menuTree.value)

  return ids
})

// 父菜单下拉使用扁平列表展示，用缩进表达层级，新增子菜单时可以选任意已有菜单作为父级。
const parentOptions = computed<MenuOption[]>(() => {
  const options: MenuOption[] = []

  // 深度优先遍历树，level 控制标题前的全角空格数量。
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

  walk(menuTree.value)

  return options
})

/**
 * 通知布局侧栏等监听方：菜单数据已变更，需重新拉取侧栏菜单。
 * 使用自定义 DOM 事件，避免与 pinia/路由强耦合。
 */
const notifyMenuUpdated = () => {
  window.dispatchEvent(new Event('mvp:menu-updated'))
}

/** 拉取完整菜单树，供父菜单下拉使用。 */
const handleSelectMenuTree = async () => {
  menuTree.value = await getMenuTree()
}

/** 按分页参数刷新菜单表格。 */
const handleSelectMenus = async () => {
  loading.value = true

  try {
    const page = await getMenuPage(menuQuery, {
      page: menuPagination.page,
      size: menuPagination.size,
    })
    menuItems.value = page.records
    menuPagination.total = page.total
    menuPagination.page = page.current
    menuPagination.size = page.size
  } finally {
    // 无论成功失败都结束 loading，避免表格一直转圈。
    loading.value = false
  }
}

const handleQueryMenus = async () => {
  menuPagination.page = 1
  await handleSelectMenus()
}

const handleClearQueryMenus = async () => {
  Object.assign(menuQuery, {
    title: '',
    path: '',
  })
  menuPagination.page = 1
  await handleSelectMenus()
}

const handleMenuPageSizeChange = async (size: number) => {
  menuPagination.size = size
  menuPagination.page = 1
  await handleSelectMenus()
}

const handleMenuPageChange = async (page: number) => {
  menuPagination.page = page
  await handleSelectMenus()
}

/** 清空表单、当前编辑 id 与校验状态，供弹窗打开前/关闭后复用。 */
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

/** 打开「新增根菜单」弹窗：无父级、路径由用户填写。 */
const handleCreateRootMenu = () => {
  handleResetMenuForm()
  isCreateOrUpdate.value = true
  isVisibleOfCreateOrUpdate.value = true
}

/**
 * 打开「添加子菜单」弹窗。
 * 预填 parentId 为当前行 id；路径预填为父 path + '/'，便于在子路径上继续编辑。
 */
const handleCreateChildMenu = (menu: MenuItem) => {
  handleResetMenuForm()
  isCreateOrUpdate.value = true
  menuForm.parentId = menu.id
  // 保证子路径以父路径为前缀，且中间有分隔斜杠。
  menuForm.path = menu.path.endsWith('/') ? menu.path : `${menu.path}/`
  isVisibleOfCreateOrUpdate.value = true
}

/** 打开「编辑菜单」弹窗，把当前行数据回填到表单。 */
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

/**
 * 校验通过后提交：按 isCreateOrUpdate 走新增或更新接口。
 * 成功后关闭弹窗、刷新表格，并广播菜单变更事件。
 */
const handleSubmit = async () => {
  if (!formInstance.value) return

  await formInstance.value.validate()

  // 空字符串转 undefined：根菜单 parentId、可选 icon 不传给后端。
  const payload: MenuParams = {
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
  await Promise.all([handleSelectMenus(), handleSelectMenuTree()])
  notifyMenuUpdated()
}

/**
 * 删除菜单（含后端级联的所有子菜单）。
 * 先二次确认；取消时 ElMessageBox 会 reject，后续请求不会执行。
 */
const handleDeleteMenu = async (menu: MenuItem) => {
  await ElMessageBox.confirm(`确定删除菜单「${menu.title}」及其所有子菜单吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })

  await deleteMenu(menu.id)
  ElMessage.success('菜单删除成功')
  await Promise.all([handleSelectMenus(), handleSelectMenuTree()])
  notifyMenuUpdated()
}

// 进入页面即加载菜单树。
onMounted(() => {
  Promise.all([handleSelectMenus(), handleSelectMenuTree()])
})
</script>

<template>
  <div class="page-view">
    <div class="page-header">
      <div class="query-panel">
        <el-input v-model="menuQuery.title" class="query-input" clearable placeholder="菜单名称"
                  @keyup.enter="handleQueryMenus"/>
        <el-input v-model="menuQuery.path" class="query-input" clearable placeholder="路由路径"
                  @keyup.enter="handleQueryMenus"/>
        <el-button type="primary" @click="handleQueryMenus">查询</el-button>
        <el-button @click="handleClearQueryMenus">清空</el-button>
      </div>
      <div class="actions">
        <el-button type="primary" @click="handleCreateRootMenu">新增根菜单</el-button>
      </div>
    </div>

    <!-- 菜单树表：children 字段由后端 /menu/tree 返回，Element Plus 会自动渲染层级。 -->
    <el-table
        v-loading="loading"
        :data="menuItems"
        row-key="id"
        stripe
    >
      <el-table-column prop="title" label="菜单名称" min-width="250"/>
      <el-table-column prop="createdAt" label="创建时间" min-width="250"/>
      <el-table-column prop="sortOrder" label="排序" width="250"/>
      <el-table-column prop="" label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleCreateChildMenu(row)">添加子菜单</el-button>
          <el-button link type="primary" @click="handleUpdateMenu(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDeleteMenu(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
        v-model:current-page="menuPagination.page"
        v-model:page-size="menuPagination.size"
        class="pagination-bar"
        :page-sizes="[10, 20, 50, 100]"
        :total="menuPagination.total"
        background
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleMenuPageSizeChange"
        @current-change="handleMenuPageChange"
    />

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

.query-panel {
  display: flex;
  align-items: center;
  gap: 10px;
}

.query-input {
  width: 180px;
}

.actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pagination-bar {
  justify-content: flex-end;
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

  .query-panel {
    align-items: stretch;
    flex-direction: column;
  }

  .query-input,
  .query-panel :deep(.el-button),
  .actions :deep(.el-button) {
    width: 100%;
  }

  .pagination-bar {
    justify-content: flex-start;
  }
}
</style>
