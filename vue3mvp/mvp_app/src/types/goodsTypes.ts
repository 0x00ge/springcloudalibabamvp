/** 商品查询条件（管理端列表过滤）。 */
export interface GoodsQuery {
  name?: string
  /** 0-禁用 1-启用 */
  status?: number
}

/**
 * 商品表单 / 列表行。
 * 字段与后端 GoodsDto 对齐（camelCase JSON）。
 */
export interface GoodsParams {
  id?: string
  name: string
  seckillPrice: number | undefined
  totalStock: number | undefined
  limitPerUser: number | undefined
  /** 秒杀开始时间，提交时为 ISO 字符串或 Date */
  startTime: string | Date | undefined
  /** 秒杀结束时间 */
  endTime: string | Date | undefined
  /** 0-禁用 1-启用 */
  status: number | undefined
  createdAt?: string
  updatedAt?: string
}

/** 新增/编辑弹窗使用的表单模型。 */
export type GoodsForm = Omit<GoodsParams, 'id' | 'createdAt' | 'updatedAt'>
