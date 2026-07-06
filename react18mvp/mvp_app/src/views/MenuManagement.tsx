import { Button, Form, Input, InputNumber, Modal, Space, Table, Tag, message } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { useState } from 'react'

import { menuStore, type MenuFormValues, useMenuSnapshot } from '@/stores/menuStore'
import type { MenuItem } from '@/types/layoutTypes'
import './MenuManagement.css'

type ModalMode = 'create-root' | 'create-child' | 'edit'

interface ModalState {
  open: boolean
  mode: ModalMode
  currentMenu?: MenuItem
}

const defaultModalState: ModalState = {
  open: false,
  mode: 'create-root',
}

export default function MenuManagement() {
  const menus = useMenuSnapshot()
  const [form] = Form.useForm<MenuFormValues>()
  const [modalState, setModalState] = useState<ModalState>(defaultModalState)

  const isEditing = modalState.mode === 'edit'
  const modalTitle =
    modalState.mode === 'create-root'
      ? '新增根菜单'
      : modalState.mode === 'create-child'
        ? `给「${modalState.currentMenu?.title || ''}」添加子菜单`
        : '编辑菜单'

  const openCreateRoot = () => {
    form.setFieldsValue({
      title: '',
      path: '',
      sort: 1,
    })
    setModalState({
      open: true,
      mode: 'create-root',
    })
  }

  const openCreateChild = (menu: MenuItem) => {
    form.setFieldsValue({
      title: '',
      path: '',
      sort: (menu.children?.length || 0) + 1,
    })
    setModalState({
      open: true,
      mode: 'create-child',
      currentMenu: menu,
    })
  }

  const openEdit = (menu: MenuItem) => {
    form.setFieldsValue({
      title: menu.title,
      path: menu.path,
      sort: menu.sort ?? 1,
    })
    setModalState({
      open: true,
      mode: 'edit',
      currentMenu: menu,
    })
  }

  const closeModal = () => {
    setModalState(defaultModalState)
    form.resetFields()
  }

  const handleSubmit = async () => {
    const values = await form.validateFields()

    if (modalState.mode === 'create-root') {
      menuStore.addRootMenu(values)
      message.success('根菜单添加成功')
    }

    if (modalState.mode === 'create-child' && modalState.currentMenu) {
      menuStore.addChildMenu(modalState.currentMenu.id, values)
      message.success('子菜单添加成功')
    }

    if (modalState.mode === 'edit' && modalState.currentMenu) {
      menuStore.updateMenu(modalState.currentMenu.id, values)
      message.success('菜单更新成功')
    }

    closeModal()
  }

  const handleDelete = (menu: MenuItem) => {
    if (menu.locked) {
      message.warning('内置菜单不能删除')
      return
    }

    Modal.confirm({
      title: '删除菜单',
      content: `确定删除「${menu.title}」及其所有子菜单吗？`,
      okText: '删除',
      cancelText: '取消',
      okButtonProps: {
        danger: true,
      },
      onOk: () => {
        menuStore.deleteMenu(menu.id)
        message.success('菜单删除成功')
      },
    })
  }

  const handleReset = () => {
    Modal.confirm({
      title: '重置菜单',
      content: '确定恢复默认菜单吗？自定义菜单会被清空。',
      okText: '重置',
      cancelText: '取消',
      onOk: () => {
        menuStore.resetMenus()
        message.success('菜单已恢复默认')
      },
    })
  }

  const columns: ColumnsType<MenuItem> = [
    {
      title: '菜单名称',
      dataIndex: 'title',
      minWidth: 180,
      render: (title: string, menu) => (
        <Space>
          <span>{title}</span>
          {menu.locked ? <Tag color="processing">内置</Tag> : null}
        </Space>
      ),
    },
    {
      title: '路由路径',
      dataIndex: 'path',
      minWidth: 220,
    },
    {
      title: '排序',
      dataIndex: 'sort',
      width: 100,
      render: (sort?: number) => sort ?? '-',
    },
    {
      title: '操作',
      width: 250,
      fixed: 'right',
      render: (_, menu) => (
        <Space>
          <Button type="link" onClick={() => openCreateChild(menu)}>
            添加子菜单
          </Button>
          <Button type="link" onClick={() => openEdit(menu)}>
            编辑
          </Button>
          <Button type="link" danger disabled={menu.locked} onClick={() => handleDelete(menu)}>
            删除
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <div className="menu-page">
      <div className="menu-page-header">
        <div>
          <h2>菜单管理</h2>
          <p>维护左侧菜单树，支持给任意菜单继续添加子菜单。</p>
        </div>

        <Space>
          <Button onClick={handleReset}>恢复默认</Button>
          <Button type="primary" onClick={openCreateRoot}>
            新增根菜单
          </Button>
        </Space>
      </div>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={menus}
        pagination={false}
        scroll={{ x: 760 }}
      />

      <Modal
        title={modalTitle}
        open={modalState.open}
        okText={isEditing ? '保存' : '添加'}
        cancelText="取消"
        onOk={handleSubmit}
        onCancel={closeModal}
        afterClose={() => form.resetFields()}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            label="菜单名称"
            name="title"
            rules={[
              { required: true, message: '请输入菜单名称' },
              { max: 20, message: '菜单名称不能超过 20 个字符' },
            ]}
          >
            <Input placeholder="请输入菜单名称" />
          </Form.Item>

          <Form.Item
            label="路由路径"
            name="path"
            rules={[
              { required: true, message: '请输入路由路径' },
              {
                pattern: /^\//,
                message: '路由路径必须以 / 开头',
              },
            ]}
          >
            <Input placeholder="/home/example" />
          </Form.Item>

          <Form.Item
            label="排序"
            name="sort"
            rules={[{ required: true, message: '请输入排序值' }]}
          >
            <InputNumber min={1} precision={0} className="sort-input" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
