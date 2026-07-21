import {get, post} from '@/utils/http/http.ts'

import type {
    OrderParams,
    OrderQuery,
    OrderResultParams,
    OrderSubmitForm,
} from '@/types/orderTypes.ts'
import type {PageQuery, PageResult} from '@/types/pageTypes.ts'

/** 管理端：分页查询订单。 */
export const selectOrders =
    (query?: Partial<OrderQuery>, pagination: PageQuery = {page: 1, size: 10}) =>
        get<PageResult<OrderParams>>('/order/page', {
            page: pagination.page,
            size: pagination.size,
            userId: query?.userId || undefined,
            goodsId: query?.goodsId || undefined,
            status: query?.status,
        })

/** 管理端：订单详情。 */
export const getOrderDetail =
    (id: string) => get<OrderParams>('/order/detail/' + id)

/**
 * 发起秒杀（异步）：返回排队中/成功/失败。
 * 用户 ID 由网关从 Token 注入 X-User-Id，前端无需传。
 */
export const submitOrder =
    (data: OrderSubmitForm) => post<OrderResultParams>('/order/submit', data)

/**
 * 查询当前用户对某商品的秒杀结果（轮询用）。
 */
export const queryOrderResult =
    (goodsId: string) => get<OrderResultParams>('/order/result', {goodsId})
