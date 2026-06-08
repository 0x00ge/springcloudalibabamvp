import {computed, ref} from 'vue'
import {defineStore} from 'pinia'

import {login, logout, refreshAccessToken, register} from '@/api/authApi.ts'
import type {LoginParams, RegisterParams} from '@/types/types.ts'
import {
    clearStoredAuthInfo,
    getAccessToken,
    setAuthTokens,
} from '@/stores/authStore.ts'

export const useUserStore = defineStore('user', () => {
    // accessToken：短期 token，只保存在 Pinia/内存里，会被 axios.ts 放到 Authorization 请求头中。
    const accessToken = ref(getAccessToken())

    // 是否已登录：只看当前页面运行时是否已经拿到 accessToken。
    const isLogin = computed(() => Boolean(accessToken.value))

    const hasValidLogin = () => Boolean(accessToken.value || getAccessToken())

    // 清理 Pinia 状态和内存缓存，只负责前端状态，不调用后端接口。
    const clearLoginState = () => {
        accessToken.value = ''
        clearStoredAuthInfo()
    }

    // 登录流程：
    // 1. 调用 /auth/login，后端响应体返回 accessToken，并通过 HttpOnly Cookie 写入 refreshToken。
    // 2. 前端只保存 accessToken；后续业务接口只用 accessToken 放到 Authorization 请求头。
    // 3. 登录流程不再额外请求用户信息接口。
    const loginAction = async (params: LoginParams) => {
        const tokenResult = await login(params)

        accessToken.value = tokenResult.accessToken
        setAuthTokens(tokenResult)

        return tokenResult
    }

    // 注册流程：
    // 1. 页面先调用 /auth/register/code 发送短信验证码。
    // 2. 用户提交手机号、验证码、密码后调用 /auth/register。
    // 3. 注册成功后不自动登录，由页面提示用户再登录。
    const registerAction = async (params: RegisterParams) => {
        return register(params)
    }

    // 恢复登录态：
    // 1. 如果内存里还有 accessToken，直接认为当前前端运行态可用。
    // 2. 如果页面刷新导致 accessToken 丢失，则调用 /auth/refresh。
    // 3. 浏览器自动携带 HttpOnly refreshToken Cookie，后端返回新的 accessToken。
    const refreshLoginStateAction = async () => {
        if (accessToken.value || getAccessToken()) {
            accessToken.value = getAccessToken()

            return accessToken.value
        }

        try {
            const tokenResult = await refreshAccessToken()

            accessToken.value = tokenResult.accessToken
            setAuthTokens(tokenResult)

            return tokenResult.accessToken
        } catch (error) {
            clearLoginState()

            throw error
        }
    }

    // 退出登录：
    // 1. 如果存在 accessToken，调用 /auth/logout，让后端拉黑当前 accessToken、删除 refreshToken 白名单并清 Cookie。
    // 2. 不管后端接口成功还是失败，finally 都会清理前端 Pinia/内存状态。
    const logoutAction = async () => {
        try {
            if (accessToken.value) {
                await logout()
            }
        } finally {
            clearLoginState()
        }
    }

    return {
        accessToken,
        isLogin,
        hasValidLogin,
        clearLoginState,
        loginAction,
        registerAction,
        refreshLoginStateAction,
        logoutAction,
    }
})
