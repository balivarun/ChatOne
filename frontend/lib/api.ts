import axios from 'axios';

const API_BASE = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export const api = axios.create({
  baseURL: `${API_BASE}/api`,
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token =
    typeof window !== 'undefined' ? localStorage.getItem('accessToken') : null;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config;
    if (error.response?.status === 401 && !original._retry) {
      original._retry = true;
      try {
        const refresh = localStorage.getItem('refreshToken');
        if (!refresh) throw new Error('No refresh token');
        const { data } = await axios.post(`${API_BASE}/api/auth/refresh`, {
          refreshToken: refresh,
        });
        localStorage.setItem('accessToken', data.data.accessToken);
        original.headers.Authorization = `Bearer ${data.data.accessToken}`;
        return api(original);
      } catch {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

// Auth
export const authApi = {
  getMe: () => api.get('/auth/me'),
  refresh: (token: string) => api.post('/auth/refresh', { refreshToken: token }),
  logout: () => api.post('/auth/logout'),
};

// Users
export const userApi = {
  getMe: () => api.get('/users/me'),
  updateProfile: (data: { displayName?: string; bio?: string }) =>
    api.put('/users/me', data),
  updateAvatar: (file: File) => {
    const form = new FormData();
    form.append('file', file);
    return api.post('/users/me/avatar', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
  searchUsers: (query: string, page = 0) =>
    api.get('/users/search', { params: { query, page, size: 20 } }),
  getUser: (id: string) => api.get(`/users/${id}`),
  blockUser: (id: string) => api.post(`/users/${id}/block`),
  unblockUser: (id: string) => api.delete(`/users/${id}/block`),
  getBlockedUsers: () => api.get('/users/blocked'),
};

// Conversations
export const conversationApi = {
  getAll: () => api.get('/conversations'),
  create: (participantId: string) =>
    api.post('/conversations', { participantId }),
  getOne: (id: string) => api.get(`/conversations/${id}`),
  getMessages: (id: string, page = 0) =>
    api.get(`/conversations/${id}/messages`, { params: { page, size: 30 } }),
  archive: (id: string) => api.put(`/conversations/${id}/archive`),
  pin: (id: string) => api.put(`/conversations/${id}/pin`),
  mute: (id: string) => api.put(`/conversations/${id}/mute`),
  searchMessages: (id: string, query: string, page = 0) =>
    api.get(`/conversations/${id}/messages/search`, {
      params: { query, page, size: 20 },
    }),
};

// Messages
export const messageApi = {
  send: (data: {
    conversationId: string;
    content?: string;
    type?: string;
    replyToId?: string;
  }) => api.post('/messages', data),
  edit: (id: string, content: string) => api.put(`/messages/${id}`, { content }),
  delete: (id: string) => api.delete(`/messages/${id}`),
  forward: (id: string, conversationId: string) =>
    api.post(`/messages/${id}/forward`, { conversationId }),
  markRead: (messageIds: string[]) =>
    api.put('/messages/read', { messageIds }),
};

// Groups
export const groupApi = {
  create: (data: {
    name: string;
    description?: string;
    memberIds: string[];
  }) => api.post('/groups', data),
  getMine: () => api.get('/groups/mine'),
  search: (query: string, page = 0) =>
    api.get('/groups/search', { params: { query, page, size: 20 } }),
  getOne: (id: string) => api.get(`/groups/${id}`),
  update: (id: string, data: { name?: string; description?: string }) =>
    api.put(`/groups/${id}`, data),
  delete: (id: string) => api.delete(`/groups/${id}`),
  updateAvatar: (id: string, file: File) => {
    const form = new FormData();
    form.append('file', file);
    return api.post(`/groups/${id}/avatar`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
  getMessages: (id: string, page = 0) =>
    api.get(`/groups/${id}/messages`, { params: { page, size: 30 } }),
  sendMessage: (
    id: string,
    data: { content?: string; type?: string; replyToId?: string }
  ) => api.post(`/groups/${id}/messages`, data),
  addMembers: (id: string, userIds: string[]) =>
    api.post(`/groups/${id}/members`, { userIds }),
  removeMember: (id: string, userId: string) =>
    api.delete(`/groups/${id}/members/${userId}`),
  promoteToAdmin: (id: string, userId: string) =>
    api.put(`/groups/${id}/members/${userId}/admin`),
  getMembers: (id: string) => api.get(`/groups/${id}/members`),
};

// Files
export const fileApi = {
  upload: (file: File) => {
    const form = new FormData();
    form.append('file', file);
    return api.post('/files/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },
};

// Notifications
export const notificationApi = {
  getAll: (page = 0) =>
    api.get('/notifications', { params: { page, size: 20 } }),
  getUnreadCount: () => api.get('/notifications/unread-count'),
  markAllRead: () => api.put('/notifications/read-all'),
  markRead: (id: string) => api.put(`/notifications/${id}/read`),
};
