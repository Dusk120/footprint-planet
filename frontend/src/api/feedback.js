import { http } from './http'

export const feedbackApi = {
  submit: (data) => http.post('/feedback', data)
}
