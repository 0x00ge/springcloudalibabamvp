import { useEffect, useMemo, useState } from 'react'

import {
  createUser,
  deleteUser,
  fetchUserPageConfig,
  selectUsers,
  updateUser,
} from '@/api/apiUser'
import type { OptionItem } from '@/types/types'
import type { UserForm, UserItem } from '@/types/userTypes'
import { notify } from '@/utils/notify'

const emptyQuery = {
  name: '',
  phone: '',
  role: '',
  email: '',
  status: '',
}

const emptyForm: UserForm = {
  name: '',
  phone: '',
  role: '',
  status: '',
  email: '',
  passwordHash: '123456',
}

type QueryForm = typeof emptyQuery

export default function User() {
  const [users, setUsers] = useState<UserItem[]>([])
  const [tableLoading, setTableLoading] = useState(false)
  const [configLoading, setConfigLoading] = useState(false)
  const [queryForm, setQueryForm] = useState<QueryForm>(emptyQuery)
  const [activeQuery, setActiveQuery] = useState<QueryForm>(emptyQuery)
  const [isCreate, setIsCreate] = useState<boolean | undefined>()
  const [dialogOpen, setDialogOpen] = useState(false)
  const [userId, setUserId] = useState('')
  const [roleOptions, setRoleOptions] = useState<OptionItem[]>([])
  const [statusOptions, setStatusOptions] = useState<OptionItem[]>([])
  const [defaultForm, setDefaultForm] = useState<UserForm>(emptyForm)
  const [form, setForm] = useState<UserForm>(emptyForm)
  const [formErrors, setFormErrors] = useState<Partial<Record<keyof UserForm, string>>>({})

  const pageLoading = tableLoading || configLoading

  const statusTagTypeMap = useMemo(
    () =>
      statusOptions.reduce<Record<string, OptionItem['tagType']>>((map, item) => {
        map[item.value] = item.tagType
        return map
      }, {}),
    [statusOptions],
  )

  const filteredUsers = useMemo(() => {
    const queryEntries = Object.entries(activeQuery)
      .map(([field, value]) => [field, value.trim().toLowerCase()] as const)
      .filter(([, value]) => value)

    if (queryEntries.length === 0) return users

    return users.filter((user) =>
      queryEntries.every(([field, value]) =>
        String(user[field as keyof Pick<UserItem, 'name' | 'phone' | 'role' | 'email' | 'status'>] || '')
          .toLowerCase()
          .includes(value),
      ),
    )
  }, [activeQuery, users])

  const loadUserPageConfig = async () => {
    setConfigLoading(true)

    try {
      const config = await fetchUserPageConfig()

      setRoleOptions(config.roleOptions)
      setStatusOptions(config.statusOptions)
      setDefaultForm(config.defaultForm)
      setForm(config.defaultForm)
    } finally {
      setConfigLoading(false)
    }
  }

  const handleSelectUsers = async () => {
    setTableLoading(true)

    try {
      setUsers(await selectUsers())
    } finally {
      setTableLoading(false)
    }
  }

  useEffect(() => {
    const load = async () => {
      await loadUserPageConfig()
      await handleSelectUsers()
    }

    load()
  }, [])

  const resetUserForm = () => {
    setIsCreate(undefined)
    setUserId('')
    setForm(defaultForm)
    setFormErrors({})
  }

  const handleQuery = () => {
    setActiveQuery(queryForm)
  }

  const handleClearQuery = () => {
    setQueryForm(emptyQuery)
    setActiveQuery(emptyQuery)
  }

  const handleSaveUser = () => {
    resetUserForm()
    setIsCreate(true)
    setDialogOpen(true)
  }

  const handleUpdateUser = (user: UserItem) => {
    setIsCreate(false)
    setUserId(user.id)
    setForm({
      name: user.name,
      phone: user.phone,
      role: user.role,
      status: user.status,
      email: user.email,
      passwordHash: user.passwordHash,
    })
    setFormErrors({})
    setDialogOpen(true)
  }

  const validateForm = () => {
    const errors: Partial<Record<keyof UserForm, string>> = {}

    if (!form.name) errors.name = '请输入用户名'
    if (!form.phone) errors.phone = '请输入手机号'
    if (!form.role) errors.role = '请选择角色'
    if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
      errors.email = '请输入正确的邮箱格式'
    }
    if (!form.status) errors.status = '请选择状态'
    if (isCreate && !form.passwordHash) errors.passwordHash = '请输入初始密码'

    setFormErrors(errors)
    return Object.keys(errors).length === 0
  }

  const closeDialog = () => {
    setDialogOpen(false)
    resetUserForm()
  }

  const handleSaveOrUpdateSubmit = async () => {
    if (!validateForm()) return

    if (isCreate === true) {
      await createUser(form)
      notify('用户新增成功', 'success')
    } else {
      await updateUser(userId, form)
      notify('用户更新成功', 'success')
    }

    closeDialog()
    await handleSelectUsers()
  }

  const handleDeleteUser = async (user: UserItem) => {
    const confirmed = window.confirm(`确定删除用户「${user.name}」吗？`)

    if (!confirmed) return

    await deleteUser(user.id)
    notify('用户删除成功', 'success')
    await handleSelectUsers()
  }

  const getStatusTagType = (status: string) => statusTagTypeMap[status] || 'info'

  const updateQueryField = (field: keyof QueryForm, value: string) => {
    setQueryForm((current) => ({
      ...current,
      [field]: value,
    }))
  }

  const updateFormField = (field: keyof UserForm, value: string) => {
    setForm((current) => ({
      ...current,
      [field]: value,
    }))
  }

  return (
    <div className="page-view">
      <div className="page-header">
        <div className="query-panel">
          <input
            className="control query-input"
            onChange={(event) => updateQueryField('name', event.target.value)}
            onKeyDown={(event) => event.key === 'Enter' && handleQuery()}
            placeholder="用户名"
            value={queryForm.name}
          />
          <input
            className="control query-input"
            onChange={(event) => updateQueryField('phone', event.target.value)}
            onKeyDown={(event) => event.key === 'Enter' && handleQuery()}
            placeholder="手机号"
            value={queryForm.phone}
          />
          <select
            className="control query-select"
            onChange={(event) => updateQueryField('role', event.target.value)}
            value={queryForm.role}
          >
            <option value="">角色</option>
            {roleOptions.map((item) => (
              <option key={item.value} value={item.value}>
                {item.label}
              </option>
            ))}
          </select>
          <input
            className="control query-input"
            onChange={(event) => updateQueryField('email', event.target.value)}
            onKeyDown={(event) => event.key === 'Enter' && handleQuery()}
            placeholder="邮箱"
            value={queryForm.email}
          />
          <select
            className="control query-select"
            onChange={(event) => updateQueryField('status', event.target.value)}
            value={queryForm.status}
          >
            <option value="">状态</option>
            {statusOptions.map((item) => (
              <option key={item.value} value={item.value}>
                {item.label}
              </option>
            ))}
          </select>
          <button className="button button-primary" onClick={handleQuery} type="button">
            查询
          </button>
          <button className="button" onClick={handleClearQuery} type="button">
            清空
          </button>
        </div>

        <button className="button button-primary" onClick={handleSaveUser} type="button">
          新增
        </button>
      </div>

      <div className={`table-card ${pageLoading ? 'is-loading' : ''}`}>
        {pageLoading && <div className="table-loading">加载中...</div>}
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr>
                <th>用户名</th>
                <th>手机号</th>
                <th>角色</th>
                <th>邮箱</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {filteredUsers.map((user) => (
                <tr key={user.id}>
                  <td>{user.name}</td>
                  <td>{user.phone}</td>
                  <td>{user.role}</td>
                  <td>{user.email}</td>
                  <td>
                    <span className={`tag tag-${getStatusTagType(user.status)}`}>{user.status}</span>
                  </td>
                  <td className="table-actions">
                    <button className="link-button" onClick={() => handleUpdateUser(user)} type="button">
                      编辑
                    </button>
                    <button
                      className="link-button danger"
                      onClick={() => handleDeleteUser(user)}
                      type="button"
                    >
                      删除
                    </button>
                  </td>
                </tr>
              ))}
              {filteredUsers.length === 0 && (
                <tr>
                  <td className="empty-cell" colSpan={6}>
                    暂无数据
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {dialogOpen && (
        <div className="modal-backdrop" role="presentation">
          <div className="modal" role="dialog" aria-modal="true" aria-labelledby="user-dialog-title">
            <div className="modal-header">
              <h2 id="user-dialog-title">{isCreate ? '新增用户' : '编辑用户'}</h2>
            </div>

            <div className="modal-body form-grid">
              <label className="labeled-field">
                <span>用户名</span>
                <input
                  className="control"
                  onChange={(event) => updateFormField('name', event.target.value)}
                  placeholder="请输入用户名"
                  value={form.name}
                />
                {formErrors.name && <em>{formErrors.name}</em>}
              </label>

              <label className="labeled-field">
                <span>手机号</span>
                <input
                  className="control"
                  onChange={(event) => updateFormField('phone', event.target.value)}
                  placeholder="请输入手机号"
                  value={form.phone}
                />
                {formErrors.phone && <em>{formErrors.phone}</em>}
              </label>

              <label className="labeled-field">
                <span>角色</span>
                <select
                  className="control"
                  onChange={(event) => updateFormField('role', event.target.value)}
                  value={form.role}
                >
                  <option value="">请选择角色</option>
                  {roleOptions.map((item) => (
                    <option key={item.value} value={item.value}>
                      {item.label}
                    </option>
                  ))}
                </select>
                {formErrors.role && <em>{formErrors.role}</em>}
              </label>

              <label className="labeled-field">
                <span>邮箱</span>
                <input
                  className="control"
                  onChange={(event) => updateFormField('email', event.target.value)}
                  placeholder="请输入邮箱"
                  value={form.email}
                />
                {formErrors.email && <em>{formErrors.email}</em>}
              </label>

              {isCreate && (
                <label className="labeled-field">
                  <span>初始密码</span>
                  <input
                    className="control"
                    onChange={(event) => updateFormField('passwordHash', event.target.value)}
                    placeholder="请输入初始密码"
                    type="password"
                    value={form.passwordHash || ''}
                  />
                  {formErrors.passwordHash && <em>{formErrors.passwordHash}</em>}
                </label>
              )}

              <div className="labeled-field">
                <span>状态</span>
                <div className="radio-buttons">
                  {statusOptions.map((item) => (
                    <button
                      className={form.status === item.value ? 'active' : ''}
                      key={item.value}
                      onClick={() => updateFormField('status', item.value)}
                      type="button"
                    >
                      {item.label}
                    </button>
                  ))}
                </div>
                {formErrors.status && <em>{formErrors.status}</em>}
              </div>
            </div>

            <div className="modal-footer">
              <button className="button" onClick={closeDialog} type="button">
                取消
              </button>
              <button className="button button-primary" onClick={handleSaveOrUpdateSubmit} type="button">
                提交
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
