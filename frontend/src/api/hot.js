import { http } from './http'

export const hotApi = {
  list: () => http.get('/hot-destinations')
}
