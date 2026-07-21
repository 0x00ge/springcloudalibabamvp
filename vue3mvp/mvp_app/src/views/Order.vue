<script setup lang="ts">
import {onMounted, onUnmounted, reactive, ref} from 'vue'
import {ElMessage, type FormInstance, type FormRules} from 'element-plus'

import {queryOrderResult, selectOrders, submitOrder} from '@/api/apiOrder.ts'
import {selectGoods} from '@/api/apiGoods.ts'
import type {GoodsParams} from '@/types/goodsTypes.ts'
import type {OrderParams, OrderQuery, OrderSubmitForm} from '@/types/orderTypes.ts'

const orderList = ref<OrderParams[]>([])
const goodsOptions = ref<GoodsParams[]>([])
const loading = ref(false)
const seckillLoading = ref(false)
const isVisibleOfSeckill = ref(false)
const formInstance = ref<FormInstance>()
/** 轮询定时器，离开页面时清理 */
let pollTimer: number | undefined

const orderQuery = reactive<OrderQuery>({
  userId: '',
  goodsId: '',
  status: undefined,
})

const orderPagination = reactive({
  page: 1,
  size: 10,
  total: 0,
})

/** 订单支付状态（t_order.status） */
const orderStatusOptions = [
  {value: 0, label: '待支付', tagType: 'warning' as const},
  {value: 1, label: '已支付', tagType: 'success' as const},
  {value: 2, label: '已取消', tagType: 'info' as const},
]

/** 秒杀结果状态（OrderResultDto.status） */
const resultStatusText: Record<number, string> = {
  0: '排队中',
  1: '秒杀成功',
  2: '秒杀失败',
}

const seckillForm = reactive<OrderSubmitForm>({
  goodsId: '',
  buyCount: 1,
})

const seckillRules: FormRules<OrderSubmitForm> = {
  goodsId: [{required: true, message: '请选择商品', trigger: 'change'}],
  buyCount: [{required: true, message: '请输入购买数量', trigger: 'change'}],
}

const orderStatusLabel = (status?: number) =>
    orderStatusOptions.find((item) => item.value === status)?.label || String(status ?? '-')

const orderStatusTagType = (status?: number) =>
    orderStatusOptions.find((item) => item.value === status)?.tagType || 'info'

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

const stopPolling = () => {
  if (pollTimer !== undefined) {
    window.clearInterval(pollTimer)
    pollTimer = undefined
  }
}

const handleSelectOrders = async () => {
  loading.value = true
  try {
    const page = await selectOrders(orderQuery, {
      page: orderPagination.page,
      size: orderPagination.size,
    })
    orderList.value = page.records || []
    orderPagination.total = page.total
    orderPagination.page = page.current
    orderPagination.size = page.size
  } finally {
    loading.value = false
  }
}

const handleQueryOrders = async () => {
  orderPagination.page = 1
  await handleSelectOrders()
}

const handleClearQueryOrders = async () => {
  Object.assign(orderQuery, {userId: '', goodsId: '', status: undefined})
  orderPagination.page = 1
  await handleSelectOrders()
}

const handleOrderPageSizeChange = async (size: number) => {
  orderPagination.size = size
  orderPagination.page = 1
  await handleSelectOrders()
}

const handleOrderPageChange = async (page: number) => {
  orderPagination.page = page
  await handleSelectOrders()
}

/** 加载启用中的商品，供秒杀下单下拉选择 */
const handleLoadGoodsOptions = async () => {
  const page = await selectGoods({status: 1}, {page: 1, size: 100})
  goodsOptions.value = page.records || []
}

const handleOpenSeckill = async () => {
  seckillForm.goodsId = ''
  seckillForm.buyCount = 1
  formInstance.value?.clearValidate()
  await handleLoadGoodsOptions()
  isVisibleOfSeckill.value = true
}

/**
 * 提交秒杀：异步排队后轮询 /order/result。
 * 最多轮询约 15 次（约 15 秒），成功/失败后停止并刷新订单列表。
 */
const handleSubmitSeckill = async () => {
  if (!formInstance.value) return
  await formInstance.value.validate()

  seckillLoading.value = true
  stopPolling()

  try {
    const first = await submitOrder({
      goodsId: seckillForm.goodsId,
      buyCount: seckillForm.buyCount,
    })

    // 同步降级或已有结果时可能直接成功/失败
    if (first.status === 1) {
      ElMessage.success(first.message || `秒杀成功，订单号 ${first.orderId || ''}`)
      isVisibleOfSeckill.value = false
      await handleSelectOrders()
      return
    }
    if (first.status === 2) {
      ElMessage.error(first.message || '秒杀失败')
      return
    }

    ElMessage.info(first.message || '已进入排队，正在查询结果…')

    let tries = 0
    const maxTries = 15
    const goodsId = seckillForm.goodsId

    pollTimer = window.setInterval(async () => {
      tries += 1
      try {
        const result = await queryOrderResult(goodsId)
        if (result.status === 1) {
          stopPolling()
          seckillLoading.value = false
          ElMessage.success(result.message || `秒杀成功，订单号 ${result.orderId || ''}`)
          isVisibleOfSeckill.value = false
          await handleSelectOrders()
          return
        }
        if (result.status === 2) {
          stopPolling()
          seckillLoading.value = false
          ElMessage.error(result.message || '秒杀失败')
          return
        }
        // status=0 继续排队
        if (tries >= maxTries) {
          stopPolling()
          seckillLoading.value = false
          ElMessage.warning('仍在排队中，请稍后在订单列表刷新查看')
          isVisibleOfSeckill.value = false
          await handleSelectOrders()
        }
      } catch {
        if (tries >= maxTries) {
          stopPolling()
          seckillLoading.value = false
          ElMessage.warning('结果查询超时，请稍后刷新订单列表')
        }
      }
    }, 1000)
  } catch {
    // axios 拦截器已提示业务错误
  } finally {
    // 若已进入轮询，loading 由轮询结束关闭；同步结束则关闭
    if (pollTimer === undefined) {
      seckillLoading.value = false
    }
  }
}

onMounted(() => {
  handleSelectOrders()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<template>
  <div class="page-view">
    <div class="page-header">
      <div class="query-panel">
        <el-input
            v-model="orderQuery.userId"
            class="query-input"
            clearable
            placeholder="用户 ID"
            @keyup.enter="handleQueryOrders"
        />
        <el-input
            v-model="orderQuery.goodsId"
            class="query-input wide"
            clearable
            placeholder="商品 ID"
            @keyup.enter="handleQueryOrders"
        />
        <el-select v-model="orderQuery.status" class="query-select" clearable placeholder="订单状态">
          <el-option
              v-for="item in orderStatusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
          />
        </el-select>
        <el-button type="primary" @click="handleQueryOrders">查询</el-button>
        <el-button @click="handleClearQueryOrders">清空</el-button>
      </div>
      <el-button type="primary" @click="handleOpenSeckill">发起秒杀</el-button>
    </div>

    <el-alert
        class="hint"
        type="info"
        :closable="false"
        title="秒杀下单走 /order/submit（异步 RocketMQ），结果轮询 /order/result；下方表格为已落库订单。"
    />

    <el-table v-loading="loading" :data="orderList" stripe>
      <el-table-column prop="id" label="订单 ID" min-width="220" show-overflow-tooltip/>
      <el-table-column prop="goodsId" label="商品 ID" min-width="220" show-overflow-tooltip/>
      <el-table-column prop="userId" label="用户 ID" min-width="220" show-overflow-tooltip/>
      <el-table-column prop="buyCount" label="数量" width="80"/>
      <el-table-column prop="amount" label="金额" width="100"/>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="orderStatusTagType(row.status)">{{ orderStatusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" min-width="170">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
    </el-table>

    <el-pagination
        v-model:current-page="orderPagination.page"
        v-model:page-size="orderPagination.size"
        class="pagination-bar"
        :page-sizes="[10, 20, 50]"
        :total="orderPagination.total"
        background
        layout="total, sizes, prev, pager, next"
        @size-change="handleOrderPageSizeChange"
        @current-change="handleOrderPageChange"
    />

    <el-dialog
        v-model="isVisibleOfSeckill"
        title="发起秒杀"
        width="480px"
        :close-on-click-modal="!seckillLoading"
        @closed="stopPolling(); seckillLoading = false"
    >
      <el-form ref="formInstance" :model="seckillForm" :rules="seckillRules" label-width="90px">
        <el-form-item label="商品" prop="goodsId">
          <el-select
              v-model="seckillForm.goodsId"
              filterable
              placeholder="选择已启用的秒杀商品"
              style="width: 100%"
          >
            <el-option
                v-for="item in goodsOptions"
                :key="item.id"
                :label="`${item.name}（¥${item.seckillPrice} / 库存${item.totalStock}）`"
                :value="item.id || ''"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="购买数量" prop="buyCount">
          <el-input-number
              v-model="seckillForm.buyCount"
              :min="1"
              :max="100"
              :step="1"
              controls-position="right"
          />
        </el-form-item>
      </el-form>
      <p class="dialog-tip">
        结果状态说明：{{ resultStatusText[0] }} → {{ resultStatusText[1] }} / {{ resultStatusText[2] }}
      </p>
      <template #footer>
        <el-button :disabled="seckillLoading" @click="isVisibleOfSeckill = false">取消</el-button>
        <el-button type="primary" :loading="seckillLoading" @click="handleSubmitSeckill">
          {{ seckillLoading ? '处理中…' : '提交秒杀' }}
        </el-button>
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
  flex-wrap: wrap;
}

.query-input {
  width: 160px;
}

.query-input.wide {
  width: 220px;
}

.query-select {
  width: 128px;
}

.pagination-bar {
  justify-content: flex-end;
}

.hint {
  margin: 0;
}

.dialog-tip {
  margin: 0 0 0 90px;
  font-size: 12px;
  color: #909399;
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
  .query-input.wide,
  .query-select,
  .query-panel :deep(.el-button) {
    width: 100%;
  }

  .pagination-bar {
    justify-content: flex-start;
  }
}
</style>
