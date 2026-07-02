import { Button, Form, Input, message, Modal, Radio, Select, Table, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useEffect, useMemo, useState } from 'react'

import { createUser, deleteUser, getUserInfoConfig, selectUsers, updateUser } from '@/api/apiUser'
import type { OptionItem } from '@/types/layoutTypes'
import type { UserForm, UserInfo, UserQuery } from '@/types/userTypes'
import './User.css'

const emptyQueryForm: UserQuery = {
  name: '',
  phone: '',
  role: '',
  email: '',
  status: '',
}

const emptyUserForm: UserForm = {
  name: '',
  phone: '',
  role: '',
  status: '',
  email: '',
  passwordHash: '123456',
}

export default function User() {
  const [queryForm] = Form.useForm<UserQuery>()
  const [userForm] = Form.useForm<UserForm>()
  const [userId, setUserId] = useState('')
  const [userList, setUserList] = useState<UserInfo[]>([])
  const [isCreateOrUpdate, setIsCreateOrUpdate] = useState<boolean>()
  const [isVisibleOfCreateOrUpdate, setIsVisibleOfCreateOrUpdate] = useState(false)
  const [roleOptions, setRoleOptions] = useState<OptionItem[]>([])
  const [statusOptions, setStatusOptions] = useState<OptionItem[]>([])
  const [defaultUserForm, setDefaultUserForm] = useState<UserForm>(emptyUserForm)
  const [loading, setLoading] = useState(false)

  const titleOfCreateOrUpdate = isCreateOrUpdate ? '新增用户' : '编辑用户'

  // 把接口返回的状态配置转换成 Map，表格渲染 tag 时可以快速按状态取颜色。
  const statusTagTypeMap = useMemo(
    () =>
      statusOptions.reduce<Record<string, OptionItem['tagType']>>((map, item) => {
        map[item.value] = item.tagType

        return map
      }, {}),
    [statusOptions],
  )

  // 状态颜色由配置中的 tagType 决定，页面不关心具体状态文案。
  // 如果后端新增了别的状态但没给颜色，默认使用 default，避免页面报错。
  const getStatusTagType = (status: string) => statusTagTypeMap[status] || 'default'

  const columns: ColumnsType<UserInfo> = [
    {
      title: '用户名',
      dataIndex: 'name',
      minWidth: 140,
    },
    {
      title: '手机号',
      dataIndex: 'phone',
      minWidth: 140,
    },
    {
      title: '角色',
      dataIndex: 'role',
      minWidth: 140,
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      minWidth: 220,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 120,
      render: (status: string) => <Tag color={getStatusTagType(status)}>{status}</Tag>,
    },
    {
      title: '操作',
      width: 160,
      fixed: 'right',
      render: (_, user) => (
        <div className="table-actions">
          <Button type="link" onClick={() => handleUpdateUser(user)}>
            编辑
          </Button>
          <Button type="link" danger onClick={() => handleDeleteUser(user)}>
            删除
          </Button>
        </div>
      ),
    },
  ]

  // 加载用户管理页面配置：
  // - roleOptions：角色下拉选项。
  // - statusOptions：状态单选项和表格 tag 颜色。
  // - defaultUserForm：新增用户时的默认表单值。
  // 这些都走接口，后续接真实后端时只需要替换接口返回即可。
  const handleUserInfoConfig = async () => {
    const config = await getUserInfoConfig()

    setRoleOptions(config.roleOptions)
    setStatusOptions(config.statusOptions)
    setDefaultUserForm(config.defaultUserForm)
    userForm.setFieldsValue(config.defaultUserForm)
  }

  // 重置弹窗表单：
  // 新增前、弹窗关闭后都会调用，保证上一次编辑的数据不会残留到下一次新增。
  const handleResetUserForm = () => {
    setIsCreateOrUpdate(undefined)
    setUserId('')
    userForm.setFieldsValue(defaultUserForm)
  }

  const handleSelectUsers = async () => {
    setLoading(true)
    try {
      const values = queryForm.getFieldsValue()
      setUserList(await selectUsers(values))
    } finally {
      setLoading(false)
    }
  }

  // 应用查询条件，请求后端 /user/page 按条件查询。
  const handleQuery = async () => {
    await handleSelectUsers()
  }

  // 清空查询条件，并重新请求后端列表。
  const handleClearQuery = async () => {
    queryForm.setFieldsValue(emptyQueryForm)
    await handleSelectUsers()
  }

  const handleSaveUser = () => {
    handleResetUserForm()
    setIsCreateOrUpdate(true)
    setIsVisibleOfCreateOrUpdate(true)
  }

  const handleUpdateUser = (user: UserInfo) => {
    setIsCreateOrUpdate(false)
    setUserId(user.id)
    userForm.setFieldsValue({
      name: user.name,
      phone: user.phone,
      role: user.role,
      status: user.status,
      email: user.email,
      passwordHash: user.passwordHash,
    })
    setIsVisibleOfCreateOrUpdate(true)
  }

  const handleSaveOrUpdateSubmit = async () => {
    const values = await userForm.validateFields()

    if (isCreateOrUpdate === true) {
      await createUser(values)
      message.success('用户新增成功')
    } else {
      await updateUser(userId, values)
      message.success('用户更新成功')
    }

    setIsVisibleOfCreateOrUpdate(false)
    await handleSelectUsers()
  }

  const handleDeleteUser = async (user: UserInfo) => {
    Modal.confirm({
      title: '删除确认',
      content: `确定删除用户「${user.name}」吗？`,
      okText: '删除',
      cancelText: '取消',
      okButtonProps: {
        danger: true,
      },
      onOk: async () => {
        await deleteUser(user.id)
        message.success('用户删除成功')
        await handleSelectUsers()
      },
    })
  }

  // 页面挂载后先加载字典配置，再加载列表。
  // 这样表格状态颜色、弹窗默认值都能在数据展示前准备好。
  useEffect(() => {
    const init = async () => {
      await handleUserInfoConfig()
      await handleSelectUsers()
    }

    void init()
  }, [])

  return (
    <div className="page-view">
      {/* 顶部操作区：左侧是多字段联合查询，右侧是新增入口。 */}
      <div className="page-header">
        <Form className="query-panel" form={queryForm} initialValues={emptyQueryForm} onFinish={handleQuery}>
          <Form.Item name="name" noStyle>
            <Input className="query-input" allowClear placeholder="用户名" />
          </Form.Item>
          <Form.Item name="phone" noStyle>
            <Input className="query-input" allowClear placeholder="手机号" />
          </Form.Item>
          <Form.Item name="role" noStyle>
            <Select
              className="query-select"
              allowClear
              placeholder="角色"
              options={roleOptions.map((item) => ({ label: item.label, value: item.value }))}
            />
          </Form.Item>
          <Form.Item name="email" noStyle>
            <Input className="query-input" allowClear placeholder="邮箱" />
          </Form.Item>
          {/* 状态查询：选项来自用户配置，和表格 tag 颜色共用同一份字典。 */}
          <Form.Item name="status" noStyle>
            <Select
              className="query-select"
              allowClear
              placeholder="状态"
              options={statusOptions.map((item) => ({ label: item.label, value: item.value }))}
            />
          </Form.Item>
          <Button type="primary" htmlType="submit">
            查询
          </Button>
          <Button onClick={handleClearQuery}>清空</Button>
        </Form>

        <Button type="primary" onClick={handleSaveUser}>
          新增
        </Button>
      </div>

      {/* 用户表格：数据来自 userList，操作列调用同一个弹窗和删除流程。 */}
      <Table
        rowKey="id"
        columns={columns}
        dataSource={userList}
        loading={loading}
        pagination={false}
        scroll={{ x: 920 }}
      />

      {/* 新增/编辑弹窗：通过 isCreateOrUpdate 区分模式，表单结构完全复用。 */}
      <Modal
        title={titleOfCreateOrUpdate}
        width={460}
        open={isVisibleOfCreateOrUpdate}
        onCancel={() => setIsVisibleOfCreateOrUpdate(false)}
        onOk={handleSaveOrUpdateSubmit}
        afterClose={handleResetUserForm}
        okText="提交"
        cancelText="取消"
      >
        <Form form={userForm} labelCol={{ span: 5 }} initialValues={defaultUserForm}>
          {/* 用户名：普通输入框，必填校验在 rules.name 中维护。 */}
          <Form.Item name="name" label="用户名" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input placeholder="请输入用户名" />
          </Form.Item>
          <Form.Item name="phone" label="手机号" rules={[{ required: true, message: '请输入手机号' }]}>
            <Input placeholder="请输入手机号" />
          </Form.Item>
          {/* 角色：选项从配置获取，不在页面里写死。 */}
          <Form.Item name="role" label="角色" rules={[{ required: true, message: '请选择角色' }]}>
            <Select
              placeholder="请选择角色"
              options={roleOptions.map((item) => ({ label: item.label, value: item.value }))}
            />
          </Form.Item>
          <Form.Item name="email" label="邮箱" rules={[{ type: 'email', message: '请输入正确的邮箱格式' }]}>
            <Input placeholder="请输入邮箱" />
          </Form.Item>
          {/* 初始密码只在新增用户时填写；编辑用户时保持后端已有 passwordHash，不在页面暴露。 */}
          {isCreateOrUpdate ? (
            <Form.Item name="passwordHash" label="初始密码" rules={[{ required: true, message: '请输入初始密码' }]}>
              <Input.Password placeholder="请输入初始密码" />
            </Form.Item>
          ) : null}
          {/* 状态：状态选项从配置接口返回，和表格 tag 颜色使用同一份数据源。 */}
          <Form.Item name="status" label="状态" rules={[{ required: true, message: '请选择状态' }]}>
            <Radio.Group
              optionType="button"
              buttonStyle="solid"
              options={statusOptions.map((item) => ({ label: item.label, value: item.value }))}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
