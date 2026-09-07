import api from './client'

export const authApi = {
  register: (payload) => api.post('/v1/auth/register', payload),
  login: (payload) => api.post('/v1/auth/login', payload),
  me: () => api.get('/v1/auth/me'),
}

export const accountApi = {
  list: (userId) => api.get('/v1/accounts', { params: { userId } }),
  get: (id) => api.get(`/v1/accounts/${id}`),
  create: (payload) => api.post('/v1/accounts', payload),
  deposit: (id, payload) => api.post(`/v1/accounts/${id}/deposit`, payload),
  withdraw: (id, payload) => api.post(`/v1/accounts/${id}/withdraw`, payload),
  freeze: (id) => api.post(`/v1/accounts/${id}/freeze`),
  unfreeze: (id) => api.post(`/v1/accounts/${id}/unfreeze`),
  close: (id) => api.post(`/v1/accounts/${id}/close`),
}

export const transferApi = {
  initiate: (payload) => api.post('/v1/transfers', payload),
  get: (id) => api.get(`/v1/transfers/${id}`),
  byAccount: (accountId) => api.get('/v1/transfers', { params: { accountId } }),
}

export const assetApi = {
  list: () => api.get('/v1/assets'),
  rates: () => api.get('/v1/assets/rates'),
  convert: (payload) => api.post('/v1/assets/rates/convert', payload),
}

export const exchangeApi = {
  placeOrder: (payload) => api.post('/v1/exchange/orders', payload),
  cancelOrder: (id) => api.delete(`/v1/exchange/orders/${id}`),
}

export const loanApi = {
  apply: (payload) => api.post('/v1/loans/apply', payload),
  approve: (id, payload) => api.post(`/v1/loans/${id}/approve`, payload),
  byBorrower: (borrowerId) => api.get('/v1/loans/borrower/' + borrowerId),
}

export const stablecoinApi = {
  mint: (payload) => api.post('/v1/stablecoins/mint', payload),
  burn: (payload) => api.post('/v1/stablecoins/burn', payload),
  supply: (symbol) => api.get(`/v1/stablecoins/${symbol}/supply`),
  collateralRatio: (symbol) => api.get(`/v1/stablecoins/${symbol}/collateral-ratio`),
}

export const governanceApi = {
  polls: () => api.get('/v1/governance/polls'),
  allPolls: () => api.get('/v1/governance/polls/all'),
  createPoll: (payload) => api.post('/v1/governance/polls', payload),
  vote: (pollId, payload) => api.post(`/v1/governance/polls/${pollId}/vote`, payload),
  results: (pollId) => api.get(`/v1/governance/polls/${pollId}/results`),
}
