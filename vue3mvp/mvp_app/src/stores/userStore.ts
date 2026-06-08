import {computed, ref} from 'vue'
import {defineStore} from 'pinia'

import {login, logout, refreshAccessToken, register} from '@/api/authApi.ts'
import type {LoginParams, RegisterParams} from '@/types/types.ts'
import {
    clearStoredAuthInfo,
    getAccessToken,
    getRefreshToken,
    isAccessTokenExpired,
    isRefreshTokenExpired,
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
    // 1. 调用 /auth/login，后端返回 accessToken 和 refreshToken。
    // 2. 前端保存双 token；后续业务接口只用 accessToken 放到 Authorization 请求头。
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
    // 3. refreshToken 通过请求参数传给后端，后端返回新的双 token。
    const refreshLoginStateAction = async () => {
        if (accessToken.value || getAccessToken()) {
            accessToken.value = getAccessToken()

            return accessToken.value
        }

        try {
            const refreshToken = getRefreshToken()
            if (!refreshToken || isRefreshTokenExpired()) {
                throw new Error('refreshToken 已失效')
            }

            const tokenResult = await refreshAccessToken(refreshToken)

            accessToken.value = tokenResult.accessToken
            setAuthTokens(tokenResult)

            return tokenResult.accessToken
        } catch (error) {
            clearLoginState()

            throw error
        }
    }

    // 退出登录：
    // 1. 如果 accessToken 已过期但 refreshToken 还可用，先刷新一次，让后端能校验登出请求。
    // 2. 调用 /auth/logout，让后端拉黑当前 accessToken 并删除 refreshToken。
    // 3. 不管后端接口成功还是失败，finally 都会清理前端 Pinia/内存状态。
    const logoutAction = async () => {
        try {
            let refreshToken = getRefreshToken()
            if (!refreshToken || isRefreshTokenExpired()) return

            if (!accessToken.value || isAccessTokenExpired()) {
                const tokenResult = await refreshAccessToken(refreshToken)
                accessToken.value = tokenResult.accessToken
                setAuthTokens(tokenResult)
                refreshToken = tokenResult.refreshToken
            }

            if (accessToken.value) {
                await logout(refreshToken)
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
