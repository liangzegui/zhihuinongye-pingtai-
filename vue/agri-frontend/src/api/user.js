import request from '@/utils/request'

// 获取个人信息
export const getUserProfile = () => {
  return request.get('/api/auth/user/profile')
}

// 修改密码
export const updatePassword = (oldPwd, newPwd) => {
  return request.post('/api/auth/update-pwd', { oldPwd, newPwd })
}
