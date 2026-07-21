<script setup lang="ts">
import {computed, onMounted, reactive, ref} from 'vue'
import {ElMessage, ElMessageBox, type FormInstance, type FormRules} from 'element-plus'

import {createGoods, deleteGoods, selectGoods, updateGoods} from '@/api/apiGoods.ts'
import type {GoodsForm, GoodsParams, GoodsQuery} from '@/types/goodsTypes.ts'

const goodsId = ref('')
const goodsList = ref<GoodsParams[]>([])
const loading = ref(false)
/** true=新增 false=编辑 */
const isCreateOrUpdate = ref(true)
const isVisibleOfCreateOrUpdate = ref(false)
const formInstance = ref<FormInstance>()

const goodsQuery = reactive<GoodsQuery>({
  name: '',
  status: undefined,
})

const goodsPagination = reactive({
  page: 1,
  size: 10,
  total: 0,
})

const statusOptions = [
  {value: 1, label: '启用', tagType: 'success' as const},
  {value: 0, label: '禁用', tagType: 'info' as const},
]

const resetGoodsForm = (): GoodsForm => ({
  name: '',
  seckillPrice: undefined,
  totalStock: undefined,
  limitPerUser: 1,
  startTime: undefined,
  endTime: undefined,
  status: 1,
})

const goodsForm = reactive<GoodsForm>(resetGoodsForm())

const rules: FormRules<GoodsForm> = {
  name: [
    {required: true, message: '请输入商品名称', trigger: 'blur'},
    {max: 200, message: '商品名称不能超过 200 字', trigger: 'blur'},
  ],
  seckillPrice: [{required: true, message: '请输入秒杀价', trigger: 'change'}],
  totalStock: [{required: true, message: '请输入总库存', trigger: 'change'}],
  limitPerUser: [{required: true, message: '请输入限购数量', trigger: 'change'}],
  startTime: [{required: true, message: '请选择开始时间', trigger: 'change'}],
  endTime: [{required: true, message: '请选择结束时间', trigger: 'change'}],
  status: [{required: true, message: '请选择状态', trigger: 'change'}],
}

const titleOfCreateOrUpdate = computed(() => (isCreateOrUpdate.value ? '新增商品' : '编辑商品'))

const statusLabel = (status?: number) =>
    statusOptions.find((item) => item.value === status)?.label || String(status ?? '-')

const statusTagType = (status?: number) =>
    statusOptions.find((item) => item.value === status)?.tagType || 'info'

const formatDateTime = (value?: string | Date) => {
  if (!value) return '-'
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

const handleResetGoodsForm = () => {
  goodsId.value = ''
  Object.assign(goodsForm, resetGoodsForm())
  formInstance.value?.clearValidate()
}

const handleSelectGoods = async () => {
  loading.value = true
  try {
    const page = await selectGoods(goodsQuery, {
      page: goodsPagination.page,
      size: goodsPagination.size,
    })
    goodsList.value = page.records
    goodsPagination.total = page.total
    goodsPagination.page = page.current
    goodsPagination.size = page.size
  } finally {
    loading.value = false
  }
}

const handleQueryGoods = async () => {
  goodsPagination.page = 1
  await handleSelectGoods()
}

const handleClearQueryGoods = async () => {
  Object.assign(goodsQuery, {name: '', status: undefined})
  goodsPagination.page = 1
  await handleSelectGoods()
}

const handleGoodsPageSizeChange = async (size: number) => {
  goodsPagination.size = size
  goodsPagination.page = 1
  await handleSelectGoods()
}

const handleGoodsPageChange = async (page: number) => {
  goodsPagination.page = page
  await handleSelectGoods()
}

const handleSaveGoods = () => {
  handleResetGoodsForm()
  isCreateOrUpdate.value = true
  isVisibleOfCreateOrUpdate.value = true
}

const handleUpdateGoods = (row: GoodsParams) => {
  isCreateOrUpdate.value = false
  goodsId.value = row.id || ''
  Object.assign(goodsForm, {
    name: row.name,
    seckillPrice: row.seckillPrice,
    totalStock: row.totalStock,
    limitPerUser: row.limitPerUser,
    startTime: row.startTime,
    endTime: row.endTime,
    status: row.status,
  })
  isVisibleOfCreateOrUpdate.value = true
}

const handleSaveOrUpdateGoodsSubmit = async () => {
  if (!formInstance.value) return
  await formInstance.value.validate()

  if (goodsForm.startTime && goodsForm.endTime) {
    const start = new Date(goodsForm.startTime).getTime()
    const end = new Date(goodsForm.endTime).getTime()
    if (start >= end) {
      ElMessage.warning('结束时间必须晚于开始时间')
      return
    }
  }

  if (isCreateOrUpdate.value) {
    await createGoods(goodsForm)
    ElMessage.success('商品新增成功')
  } else {
    await updateGoods(goodsId.value, goodsForm)
    ElMessage.success('商品更新成功')
  }

  isVisibleOfCreateOrUpdate.value = false
  await handleSelectGoods()
}

const handleDeleteGoods = async (row: GoodsParams) => {
  if (!row.id) return
  await ElMessageBox.confirm(`确定删除商品「${row.name}」吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  })
  await deleteGoods(row.id)
  ElMessage.success('商品删除成功')
  await handleSelectGoods()
}

onMounted(() => {
  handleSelectGoods()
})
</script>

<template>
  <div class="page-view">
    <div class="page-header">
      <div class="query-panel">
        <el-input
            v-model="goodsQuery.name"
            class="query-input"
            clearable
            placeholder="商品名称"
            @keyup.enter="handleQueryGoods"
        />
        <el-select v-model="goodsQuery.status" class="query-select" clearable placeholder="状态">
          <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
          />
        </el-select>
        <el-button type="primary" @click="handleQueryGoods">查询</el-button>
        <el-button @click="handleClearQueryGoods">清空</el-button>
      </div>
      <el-button type="primary" @click="handleSaveGoods">新增商品</el-button>
    </div>

    <el-table v-loading="loading" :data="goodsList" stripe>
      <el-table-column prop="name" label="商品名称" min-width="160"/>
      <el-table-column prop="seckillPrice" label="秒杀价" width="110"/>
      <el-table-column prop="totalStock" label="总库存" width="100"/>
      <el-table-column prop="limitPerUser" label="限购" width="90"/>
      <el-table-column label="开始时间" min-width="170">
        <template #default="{ row }">{{ formatDateTime(row.startTime) }}</template>
      </el-table-column>
      <el-table-column label="结束时间" min-width="170">
        <template #default="{ row }">{{ formatDateTime(row.endTime) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleUpdateGoods(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDeleteGoods(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
        v-model:current-page="goodsPagination.page"
        v-model:page-size="goodsPagination.size"
        class="pagination-bar"
        :page-sizes="[10, 20, 50]"
        :total="goodsPagination.total"
        background
        layout="total, sizes, prev, pager, next"
        @size-change="handleGoodsPageSizeChange"
        @current-change="handleGoodsPageChange"
    />

    <el-dialog
        v-model="isVisibleOfCreateOrUpdate"
        :title="titleOfCreateOrUpdate"
        width="560px"
        @closed="handleResetGoodsForm"
    >
      <el-form ref="formInstance" :model="goodsForm" :rules="rules" label-width="100px">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="goodsForm.name" placeholder="请输入商品名称"/>
        </el-form-item>
        <el-form-item label="秒杀价" prop="seckillPrice">
          <el-input-number
              v-model="goodsForm.seckillPrice"
              :min="0"
              :precision="2"
              :step="1"
              controls-position="right"
          />
        </el-form-item>
        <el-form-item label="总库存" prop="totalStock">
          <el-input-number
              v-model="goodsForm.totalStock"
              :min="0"
              :step="1"
              controls-position="right"
          />
          <div class="form-tip">仅作 Redis 库存初始化种子，真实剩余库存以 Redis 为准</div>
        </el-form-item>
        <el-form-item label="每人限购" prop="limitPerUser">
          <el-input-number
              v-model="goodsForm.limitPerUser"
              :min="1"
              :step="1"
              controls-position="right"
          />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
              v-model="goodsForm.startTime"
              type="datetime"
              placeholder="秒杀开始时间"
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
              v-model="goodsForm.endTime"
              type="datetime"
              placeholder="秒杀结束时间"
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="goodsForm.status">
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
        <el-button type="primary" @click="handleSaveOrUpdateGoodsSubmit">提交</el-button>
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

.query-select {
  width: 128px;
}

.pagination-bar {
  justify-content: flex-end;
}

.form-tip {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}

@media (max-width: 768px) {
  .page-header {
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

  .pagination-bar {
    justify-content: flex-start;
  }
}
</style>
