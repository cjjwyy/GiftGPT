const API_BASE = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1';

let authToken: string | null = null;

export function setToken(token: string | null) {
  authToken = token;
  if (typeof window !== 'undefined') {
    if (token) {
      localStorage.setItem('token', token);
    } else {
      localStorage.removeItem('token');
    }
  }
}

export function getToken(): string | null {
  if (authToken) return authToken;
  if (typeof window !== 'undefined') {
    authToken = localStorage.getItem('token');
  }
  return authToken;
}

interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

async function request<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const token = getToken();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> || {}),
  };
  if (token) {
    headers['Authorization'] = token;
  }

  const res = await fetch(API_BASE + path, { ...options, headers });
  const text = await res.text();
  let json: ApiResponse<T>;
  try { json = JSON.parse(text); } catch {
    throw new Error(text || `Server error: HTTP ${res.status}`);
  }
  if (json.code !== 200) {
    if (getToken() && !path.startsWith('/auth/') && (json.code === 401 || json.code === 500)) {
      // Don't clear token on 500 — could be a transient error
      if (json.code === 401) setToken(null);
    }
    throw new Error(json.message || 'Request failed');
  }
  return json.data;
}

// Auth
export const authApi = {
  login: (phone: string, password: string) =>
    request<{ token: string; userId: number; nickname: string }>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ phone, password }),
    }),
  register: (phone: string, password: string, nickname?: string) =>
    request<{ token: string; userId: number; nickname: string }>('/auth/register', {
      method: 'POST',
      body: JSON.stringify({ phone, password, nickname }),
    }),
  logout: () => request<void>('/auth/logout', { method: 'POST' }),
};

// Recipients
export const recipientApi = {
  list: (page = 1, size = 10) =>
    request<any>(`/recipients?page=${page}&size=${size}`),
  get: (id: number) => request<any>(`/recipients/${id}`),
  create: (data: any) =>
    request<any>('/recipients', { method: 'POST', body: JSON.stringify(data) }),
  update: (id: number, data: any) =>
    request<any>(`/recipients/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
  delete: (id: number) =>
    request<void>(`/recipients/${id}`, { method: 'DELETE' }),
};

// Recommendations
export const recommendApi = {
  search: (data: { recipientId: number; occasion: string; budget: number }) =>
    request<any>('/recommendations/search', { method: 'POST', body: JSON.stringify(data) }),
  analyze: (recipientId: number) =>
    request<any>('/recommendations/analyze', { method: 'POST', body: JSON.stringify({ recipientId }) }),
  aiGifts: (data: { recipientId: number; occasion: string; budget: number; extraNote?: string }) =>
    request<any>('/recommendations/ai-gifts', { method: 'POST', body: JSON.stringify(data) }),
  match: (data: any) =>
    request<any>('/recommendations/match', { method: 'POST', body: JSON.stringify(data) }),
  history: (page = 1, size = 10) =>
    request<any>(`/recommendations/history?page=${page}&size=${size}`),
  historyDetail: (id: number) =>
    request<any>(`/recommendations/history/${id}`),
  deleteHistory: (ids: number[]) =>
    request<void>(`/recommendations/history`, { method: 'DELETE', body: JSON.stringify({ ids }) }),
  feedback: (id: number, feedback: string) =>
    request<void>(`/recommendations/${id}/feedback`, {
      method: 'POST',
      body: JSON.stringify({ feedback }),
    }),
};

// Products
export const productApi = {
  search: (params: Record<string, string | number>) => {
    const qs = new URLSearchParams();
    Object.entries(params).forEach(([k, v]) => { if (v) qs.set(k, String(v)); });
    return request<any>(`/products/search?${qs.toString()}`);
  },
  get: (id: number) => request<any>(`/products/${id}`),
  pddAuthority: () => request<any>('/products/platforms/pinduoduo/authority'),
};

// Gifts
export const giftApi = {
  list: (page = 1, size = 10) =>
    request<any>(`/gifts?page=${page}&size=${size}`),
  get: (id: number) => request<any>(`/gifts/${id}`),
  createOrder: (id: number, data: any) =>
    request<any>(`/gifts/${id}/order`, { method: 'POST', body: JSON.stringify(data) }),
  logistics: (id: number) => request<any>(`/gifts/${id}/logistics`),
  feedback: (id: number, data: any) =>
    request<any>(`/gifts/${id}/feedback`, { method: 'POST', body: JSON.stringify(data) }),
};

// Greetings
export const greetingApi = {
  generate: (data: {
    recipientName: string;
    relation: string;
    occasion: string;
    senderName: string;
  }) => request<any>('/greetings/generate', { method: 'POST', body: JSON.stringify(data) }),
};

// Stories
export const storyApi = {
  list: (page = 1, size = 10) =>
    request<any>(`/stories?page=${page}&size=${size}`),
  create: (data: { title: string; content: string; giftRecordId?: number; isAnonymous?: number }) =>
    request<any>('/stories', { method: 'POST', body: JSON.stringify(data) }),
  like: (id: number) => request<any>(`/stories/${id}/like`, { method: 'POST' }),
  unlike: (id: number) => request<any>(`/stories/${id}/unlike`, { method: 'POST' }),
  getReplies: (storyId: number) => request<any>(`/stories/${storyId}/replies`),
  addReply: (storyId: number, data: { content: string }) =>
    request<any>(`/stories/${storyId}/replies`, { method: 'POST', body: JSON.stringify(data) }),
};

// Calendar
export const calendarApi = {
  list: (page = 1, size = 20) =>
    request<any>(`/calendar?page=${page}&size=${size}`),
  create: (data: any) =>
    request<any>('/calendar', { method: 'POST', body: JSON.stringify(data) }),
};

// Enterprise
export const enterpriseApi = {
  register: (data: any) =>
    request<any>('/enterprise/register', { method: 'POST', body: JSON.stringify(data) }),
  getMy: () => request<any>('/enterprise/my'),
  batchOrder: (data: any) =>
    request<any>('/enterprise/orders/batch', { method: 'POST', body: JSON.stringify(data) }),
};

// Packaging
export const packagingApi = {
  themes: () => request<any[]>('/packaging/themes'),
  aiRecommend: (data: any) =>
    request<any>('/packaging/ai-recommend', { method: 'POST', body: JSON.stringify(data) }),
  save: (data: any) =>
    request<any>('/packaging/save', { method: 'POST', body: JSON.stringify(data) }),
  list: (page = 1, size = 10) =>
    request<any>(`/packaging/list?page=${page}&size=${size}`),
};
