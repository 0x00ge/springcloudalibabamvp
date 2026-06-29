import { useEffect, useMemo, useState } from 'react'
import { Button, Form, Input, Modal, Radio, Select, Space, Table, Tag, message } from 'antd'
import type { ColumnsType } from 'antd/es/table'

import { createUser, deleteUser, getUserInfoConfig, selectUsers, updateUser } from '@/api/apiUser'
import type { OptionItem } from '@/types/common'
import type { UserForm, UserInfo, UserQuery } from '@/types/userTypes'

const emptyUserForm: UserForm = {
  name: '',
  phone: '',
  role: '普通用户',
  status: '正常',
  email: '',
  passwordHash: '',
}

const emptyQueryForm: UserQuery = {
  name: '',
  phone: '',
  role: '',
  status: '',
  email: '',
}

export default function User() {
  const [userId, setUserId] = useState('')
  const [userList, setUserList] = useState<UserInfo[]>([])
  const [roleOptions, setRoleOptions] = useState<OptionItem[]>([])
  const [statusOptions, setStatusOptions] = useState<OptionItem[]>([])
  const [defaultUserForm, setDefaultUserForm] = useState<UserForm>(emptyUserForm)
  const [modalOpen, setModalOpen] = useState(false)
  const [isCreate, setIsCreate] = useState(true)
  const [tableLoading, setTableLoading] = useState(false)
  const [submitLoading, setSubmitLoading] = useState(false)

  const [queryForm] = Form.useForm<UserQuery>()
  const [userForm] = Form.useForm<UserForm>()

  const statusTagTypeMap = useMemo(
    () =>
      statusOptions.reduce<Record<string, OptionItem['tagType']>>((map, item) => {
        map[item.value] = item.tagType

        return map
      }, {}),
    [statusOptions],
  )

  const handleSelectUsers = async (query?: UserQuery) => {
    setTableLoading(true)

    try {
      setUserList(await selectUsers(query || queryForm.getFieldsValue()))
    } finally {
      setTableLoading(false)
    }
  }

  const loadConfig = async () => {
    const config = await getUserInfoConfig()

    setRoleOptions(config.roleOptions)
    setStatusOptions(config.statusOptions)
    setDefaultUserForm(config.defaultUserForm)
    userForm.setFieldsValue(config.defaultUserForm)
  }

  useEffect(() => {
    loadConfig().then(() => handleSelectUsers(emptyQueryForm))
  }, [])

  const openCreateModal = () => {
    setIsCreate(true)
    setUserId('')
    userForm.setFieldsValue({ ...defaultUserForm, passwordHash: '123456' })
    setModalOpen(true)
  }

  const openUpdateModal = (user: UserInfo) => {
    setIsCreate(false)
    setUserId(user.id)
    userForm.setFieldsValue({
      name: user.name,
      phone: user.phone,
      role: user.role,
      status: user.status,
      email: user.email,
      passwordHash: user.passwordHash,
    })
    setModalOpen(true)
  }

  const handleResetModalForm = () => {
    setUserId('')
    userForm.resetFields()
    userForm.setFieldsValue(defaultUserForm)
  }

  const handleSaveOrUpdateSubmit = async () => {
    const values = await userForm.validateFields()

    setSubmitLoading(true)

    try {
      if (isCreate) {
        await createUser(values)
        message.success('用户新增成功')
      } else {
        await updateUser(userId, values)
        message.success('用户更新成功')
      }

      setModalOpen(false)
      await handleSelectUsers()
    } finally {
      setSubmitLoading(false)
    }
  }

  const handleDeleteUser = (user: UserInfo) => {
    Modal.confirm({
      title: '删除确认',
      content: `确定删除用户「${user.name}」吗？`,
      okText: '删除',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: async () => {
        await deleteUser(user.id)
        message.success('用户删除成功')
        await handleSelectUsers()
      },
    })
  }

  const handleClearQuery = async () => {
    queryForm.setFieldsValue(emptyQueryForm)
    await handleSelectUsers(emptyQueryForm)
  }

  const columns: ColumnsType<UserInfo> = [
    { title: '用户名', dataIndex: 'name', minWidth: 140 },
    { title: '手机号', dataIndex: 'phone', minWidth: 140 },
    { title: '角色', dataIndex: 'role', minWidth: 140 },
    { title: '邮箱', dataIndex: 'email', minWidth: 220 },
    {
      title: '状态',
      dataIndex: 'status',
      width: 120,
      render: (status: string) => <Tag color={statusTagTypeMap[status]}>{status}</Tag>,
    },
    {
      title: '操作',
      width: 160,
      fixed: 'right',
      render: (_, user) => (
        <Space>
          <Button type="link" onClick={() => openUpdateModal(user)}>
            编辑
          </Button>
          <Button type="link" danger onClick={() => handleDeleteUser(user)}>
            删除
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <div className="page-view">
      <div className="page-header">
        <Form form={queryForm} className="query-panel" initialValues={emptyQueryForm} onFinish={handleSelectUsers}>
          <Form.Item name="name">
            <Input allowClear placeholder="用户名" />
          </Form.Item>
          <Form.Item name="phone">
            <Input allowClear placeholder="手机号" />
          </Form.Item>
          <Form.Item name="role">
            <Select allowClear placeholder="角色" options={roleOptions} />
          </Form.Item>
          <Form.Item name="email">
            <Input allowClear placeholder="邮箱" />
          </Form.Item>
          <Form.Item name="status">
            <Select allowClear placeholder="状态" options={statusOptions} />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">
                查询
              </Button>
              <Button onClick={handleClearQuery}>清空</Button>
            </Space>
          </Form.Item>
        </Form>

        <Button type="primary" onClick={openCreateModal}>
          新增
        </Button>
      </div>

      <Table
        rowKey="id"
        dataSource={userList}
        columns={columns}
        loading={tableLoading}
        pagination={false}
        scroll={{ x: 960 }}
      />

      <Modal
        title={isCreate ? '新增用户' : '编辑用户'}
        width={460}
        open={modalOpen}
        okText="提交"
        cancelText="取消"
        confirmLoading={submitLoading}
        onOk={handleSaveOrUpdateSubmit}
        onCancel={() => setModalOpen(false)}
        afterClose={handleResetModalForm}
      >
        <Form form={userForm} className="user-modal-form" labelCol={{ span: 5 }} initialValues={defaultUserForm}>
          <Form.Item name="name" label="用户名" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input placeholder="请输入用户名" />
          </Form.Item>
          <Form.Item name="phone" label="手机号" rules={[{ required: true, message: '请输入手机号' }]}>
            <Input placeholder="请输入手机号" />
          </Form.Item>
          <Form.Item name="role" label="角色" rules={[{ required: true, message: '请选择角色' }]}>
            <Select placeholder="请选择角色" options={roleOptions} />
          </Form.Item>
          <Form.Item name="email" label="邮箱" rules={[{ type: 'email', message: '请输入正确的邮箱格式' }]}>
            <Input placeholder="请输入邮箱" />
          </Form.Item>
          {isCreate && (
            <Form.Item name="passwordHash" label="初始密码" rules={[{ required: true, message: '请输入初始密码' }]}>
              <Input.Password placeholder="请输入初始密码" />
            </Form.Item>
          )}
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
