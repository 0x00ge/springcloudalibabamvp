import {del, get, post, put} from '@/utils/http/http.ts'

import type {GoodsForm, GoodsParams, GoodsQuery} from '@/types/goodsTypes.ts'
import type {PageQuery, PageResult} from '@/types/pageTypes.ts'

/** 分页查询商品（管理端 CRUD）。 */
export const selectGoods =
    async (query?: Partial<GoodsQuery>, pagination: PageQuery = {page: 1, size: 10}) => {
        // 后端 BaseController.page 暂不支持 name/status 过滤，前端先拉分页再本地筛选可选；
        // 当前直接透传 page/size，条件留给后续后端扩展。
        const page = await get<PageResult<GoodsParams>>('/goods/page', {
            page: pagination.page,
            size: pagination.size,
        })

        let records = page.records || []
        if (query?.name) {
            const keyword = query.name.trim().toLowerCase()
            records = records.filter((item) => (item.name || '').toLowerCase().includes(keyword))
        }
        if (query?.status !== undefined && query?.status !== null) {
            records = records.filter((item) => item.status === query.status)
        }

        return {
            ...page,
            records,
            // 本地过滤时 total 与后端 total 可能不一致；管理端数据量小时可接受
            total: query?.name || query?.status !== undefined ? records.length : page.total,
        }
    }

/** 按 id 查商品详情。 */
export const getGoodsById =
    (id: string) => get<GoodsParams>('/goods/' + id)

/** 新增商品配置。 */
export const createGoods =
    (data: GoodsForm) => post<string>('/goods', toGoodsDto(data))

/** 更新商品配置。 */
export const updateGoods =
    (id: string, data: GoodsForm) => put<void>('/goods/' + id, toGoodsDto(data))

/** 删除商品配置。 */
export const deleteGoods =
    (id: string) => del<void>('/goods/' + id)

/**
 * 前端表单 → 后端 GoodsDto。
 * 日期统一转 ISO 字符串，避免时区/序列化歧义。
 */
const toGoodsDto =
    (data: GoodsForm) => ({
        name: data.name,
        seckillPrice: data.seckillPrice,
        totalStock: data.totalStock,
        limitPerUser: data.limitPerUser,
        startTime: toIsoDate(data.startTime),
        endTime: toIsoDate(data.endTime),
        status: data.status,
    })

const toIsoDate =
    (value: string | Date | undefined) => {
        if (value == null || value === '') return undefined
        if (value instanceof Date) return value.toISOString()
        return value
    }
