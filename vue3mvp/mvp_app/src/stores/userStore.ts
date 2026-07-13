import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { getCurrentAuth } from '@/api/apiAuth'
import type { AuthParams } from '@/types/authTypes.ts'
import type { UserInfo } from '@/types/userTypes.ts'

export const useUserStore = defineStore('user', () => {
    const currentAuth = ref<AuthParams>()

    const currentUserInfo = computed<UserInfo>(() => {
        if (!currentAuth.value) {
            return { id: '', name: '', phone: '', email: '', role: '', status: '' }
        }
        const displayName = currentAuth.value.name || currentAuth.value.phone || '用户'
        return {
            id: currentAuth.value.id || '',
            name: displayName,
            phone: currentAuth.value.phone || '',
            email: '',
            role: '',
            status: '',
        }
    })

    const fetchUserInfo = async () => {
        const user = await getCurrentAuth()
        currentAuth.value = user
        return user
    }

    const clearUserInfo = () => {
        currentAuth.value = undefined
    }

    return {
        currentAuth,
        currentUserInfo,
        fetchUserInfo,
        clearUserInfo,
    }
})
