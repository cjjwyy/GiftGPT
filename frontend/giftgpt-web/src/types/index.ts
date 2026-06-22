export interface User {
  id: number;
  phone: string;
  nickname: string;
  avatarUrl?: string;
  gender?: number;
}

export interface Recipient {
  id: number;
  userId: number;
  name: string;
  relation?: string;
  gender?: number;
  ageRange?: string;
  mbti?: string;
  personality?: string;
  recentPurchases?: string;
  note?: string;
  createTime?: string;
}

export interface RecipientDetail {
  id: number;
  name: string;
  relation?: string;
  gender?: number;
  ageRange?: string;
  mbti?: string;
  personality?: string;
  recentPurchases?: string;
  note?: string;
  tags: string[];
  personalityDesc?: string;
  hobbyList?: string;
  socialAnalysis?: string;
}

export interface RecommendItem {
  productId: number;
  productName: string;
  price: number;
  imageUrl: string;
  platform: string;
  platformUrl: string;
  score: number;
  reason: string;
  matchTags: string[];
}

export interface RecommendResponse {
  recipientId: number;
  recipientName: string;
  occasion: string;
  budget: number;
  items: RecommendItem[];
  summary: string;
}

export interface Product {
  id: number;
  name: string;
  price: number;
  category?: string;
  platform?: string;
  platformUrl?: string;
  imageUrl?: string;
  description?: string;
  salesCount?: number;
  rating?: number;
}

export interface GiftRecord {
  id: number;
  userId: number;
  recipientId: number;
  occasion: string;
  budget: number;
  productId?: number;
  greetingCardId?: number;
  status: string;
  createTime?: string;
}

export interface Order {
  id: number;
  giftRecordId: number;
  orderNo: string;
  totalAmount: number;
  status: string;
  logisticsNo?: string;
  logisticsCompany?: string;
}

export interface StoryItem {
  id: number;
  userId: number;
  giftRecordId?: number;
  title: string;
  content: string;
  images?: string;
  likes: number;
  isAnonymous: number;
  status?: number;
  nickname?: string;
  liked?: number;
  createTime?: string;
}

export interface CalendarEvent {
  id?: number;
  userId?: number;
  recipientId?: number;
  title: string;
  occasion?: string;
  eventDate: string;
  remindBeforeDays?: number;
}

export interface PageData<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}
