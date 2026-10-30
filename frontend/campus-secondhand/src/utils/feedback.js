import { ElMessage, ElMessageBox } from 'element-plus'

export const notify = (message) => ElMessage({ message, duration: 3000, showClose: true })

export async function confirmAction(message, title = '请确认', confirmButtonText = '确认') {
  try {
    await ElMessageBox.confirm(message, title, {
      confirmButtonText,
      cancelButtonText: '再想想',
      closeOnClickModal: false,
      distinguishCancelAndClose: true,
      autofocus: false,
      customClass: 'app-confirm-dialog',
    })
    return true
  } catch {
    return false
  }
}
