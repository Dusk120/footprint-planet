import { http } from './http'

export const uploadApi = {
  uploadImage: (file) => http.upload('/upload', file)
}
