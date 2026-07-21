/** 订单列表查询条件。 */
export interface OrderQuery {
  userId?: string
  goodsId?: string
  /** 0-待支付 1-已支付 2-已取消 */
  status?: number
}

/**
 * 订单列表/详情。
 * 与后端 t_order / Order 实体字段对齐。
 */
export interface OrderParams {
  id?: string
  goodsId: string
  userId: string
  buyCount: number
  amount: number
  /** 0-待支付 1-已支付 2-已取消 */
  status: number
  createdAt?: string
  updatedAt?: string
}

/** 秒杀下单请求体。 */
export interface OrderSubmitForm {
  goodsId: string
  buyCount: number
}

/**
 * 秒杀结果。
 * status: 0-排队中 1-成功 2-失败
 */
export interface OrderResultParams {
  status: number
  requestNo?: string
  orderId?: string
  message?: string
}
