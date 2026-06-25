import axiosInstance from '@/utils/http/axios.ts'

interface ResponseData<T> {
  code: number
  message: string
  data: T
}

export async function get<T>(url: string, params?: any): Promise<T> {
  const response = await axiosInstance.get<ResponseData<T>>(url, { params })

  return response.data.data
}

export async function post<T>(url: string, data?: any): Promise<T> {
  const response = await axiosInstance.post<ResponseData<T>>(url, data)

  return response.data.data
}

export async function put<T>(url: string, data?: any): Promise<T> {
  const response = await axiosInstance.put<ResponseData<T>>(url, data)

  return response.data.data
}

export async function del<T>(url: string, params?: any): Promise<T> {
  const response = await axiosInstance.delete<ResponseData<T>>(url, { params })

  return response.data.data
}

export async function postParams<T>(url: string, params?: any): Promise<T> {
  const response = await axiosInstance.post<ResponseData<T>>(url, undefined, { params })

  return response.data.data
}
