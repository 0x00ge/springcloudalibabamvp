import axiosInstance from '@/utils/http/axios.ts'

type RequestParams = Record<string, unknown>
type RequestData = unknown

interface ResponseData<T> {
  code: number
  message: string
  data: T
}

export async function get<T>(url: string, params?: RequestParams): Promise<T> {
  const response = await axiosInstance.get<ResponseData<T>>(url, { params })

  return response.data.data
}

export async function post<T>(url: string, data?: RequestData): Promise<T> {
  const response = await axiosInstance.post<ResponseData<T>>(url, data)

  return response.data.data
}
