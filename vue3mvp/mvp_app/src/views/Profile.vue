<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

import { fetchProfile, fetchProfilePageConfig, updateProfile } from '@/api/profile.ts'
import type { OptionItem, ProfileData } from '@/types/types.js'

const loading = ref(false)
const departmentOptions = ref<OptionItem[]>([])
const statusOptions = ref<OptionItem[]>([])
const profile = reactive<ProfileData>({
  name: '',
  role: '',
  department: '',
  email: '',
  phone: '',
  status: '',
  avatarText: '',
})

// 页面配置由 MockJS 接口返回，部门和状态选项不写死在组件里。
const loadProfilePageConfig = async () => {
  const config = await fetchProfilePageConfig()

  departmentOptions.value = config.departmentOptions
  statusOptions.value = config.statusOptions
}

// 个人资料统一通过 api 层请求，后续切换真实后端时页面逻辑不用改。
const loadProfile = async () => {
  Object.assign(profile, await fetchProfile())
}

const handleSave = async () => {
  Object.assign(profile, await updateProfile(profile))
  ElMessage.success('个人资料保存成功')
}

onMounted(async () => {
  loading.value = true

  try {
    await Promise.all([loadProfilePageConfig(), loadProfile()])
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section v-loading="loading" class="page-view">
    <div class="page-header">
      <div>
        <p class="eyebrow">Profile</p>
        <h1>个人中心</h1>
      </div>
    </div>

    <el-row :gutter="16" class="profile-grid">
      <el-col :xs="24" :lg="8">
        <el-card class="profile-card" shadow="never">
          <div class="user-summary">
            <el-avatar :size="72">{{ profile.avatarText || '-' }}</el-avatar>
            <div>
              <h2>{{ profile.name || '-' }}</h2>
              <p>{{ profile.role || '-' }}</p>
            </div>
          </div>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="部门">{{ profile.department || '-' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ profile.email || '-' }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ profile.status || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="16">
        <el-card class="profile-card" shadow="never">
          <template #header>基础资料</template>
          <el-form label-width="90px">
            <el-form-item label="用户名">
              <el-input v-model="profile.name" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="profile.email" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="profile.phone" />
            </el-form-item>
            <el-form-item label="部门">
              <el-select v-model="profile.department" placeholder="请选择部门">
                <el-option
                  v-for="item in departmentOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-radio-group v-model="profile.status">
                <el-radio-button
                  v-for="item in statusOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-radio-group>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSave">保存修改</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </section>
</template>

<style scoped lang="less">
.page-view {
  /* 当前页面内部统一盒模型，配合全局清零样式保证宽度计算稳定。 */
  box-sizing: border-box;
  /* 页面内容按纵向网格排列，统一控制各模块之间的间距。 */
  display: grid;
  gap: 16px;
}

.page-view *,
.page-view *::before,
.page-view *::after {
  /* 只在当前页面范围内继承 border-box，不修改 style.less。 */
  box-sizing: border-box;
}

.page-header h1 {
  /* 页面标题保持后台内容区的主标题层级。 */
  margin-top: 4px;
  color: #1f2937;
  font-size: 26px;
  line-height: 1.2;
}

.eyebrow {
  /* 英文辅助标题弱化显示，用来补充页面语义。 */
  color: #64748b;
  font-size: 13px;
  font-weight: 600;
}

.profile-grid {
  /* Element Plus 栅格只处理列间距，这里补充换行后的行间距。 */
  row-gap: 16px;
}

.profile-card {
  /* 个人中心卡片使用浅边框，和主体背景形成清晰分区。 */
  border: 1px solid #e1e6ef;
  border-radius: 8px;
}

.profile-card :deep(.el-card__body) {
  /* 卡片内容区保持统一内边距，避免全局清零后只依赖组件默认值。 */
  padding: 18px;
}

.user-summary {
  /* 头像和用户摘要横向排列，信息集中且便于扫描。 */
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 18px;
}

.user-summary h2 {
  /* 用户名作为卡片内主信息，字号略大于普通正文。 */
  color: #111827;
  font-size: 22px;
  line-height: 1.2;
}

.user-summary p {
  /* 角色说明弱化显示，避免和用户名抢层级。 */
  margin-top: 4px;
  color: #64748b;
  font-size: 14px;
}
</style>
