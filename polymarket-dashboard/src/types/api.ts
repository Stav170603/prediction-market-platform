export type BackendMarketStatus = 'OPEN' | 'CLOSED' | 'RESOLVED' | 'CANCELLED';
export type MarketState = 'active' | 'closed' | 'resolved' | 'cancelled';
export type UserRole = 'USER' | 'ADMIN';

export interface RegisterRequestDto {
  username: string;
  email: string;
  password: string;
}

export interface LoginRequestDto {
  usernameOrEmail: string;
  password: string;
}

export interface AuthResponseDto {
  userId: number;
  username: string;
  email: string;
  role: UserRole;
  walletBalance: number | string;
  token: string;
}

export type TradeType = 'BUY' | 'SELL';
export type TradeOutcomeName = 'YES' | 'NO';

export interface WalletResponseDto {
  walletId: number;
  userId: number;
  username: string;
  balance: number | string;
  reliabilityScore: number | string;
}

export interface WalletTransactionResponseDto {
  transactionId: number;
  userId: number;
  username: string;
  type: string;
  amount: number | string;
  balanceAfter: number | string;
  description?: string | null;
  createdAt: string;
}

export interface PositionResponseDto {
  positionId: number;
  userId: number;
  marketId: number;
  marketTitle: string;
  marketStatus: BackendMarketStatus;
  outcomeId: number;
  outcomeName: string;
  quantity: number | string;
  currentPrice: number | string;
  currentValue: number | string;
  unrealizedPnL: number | string;
}

export interface TradeRequestDto {
  userId: number;
  marketId: number;
  outcomeId: number;
  quantity: number | string;
  type: TradeType;
}

export interface TradeResponseDto {
  tradeId: number;
  userId: number;
  username?: string | null;
  marketId: number;
  outcomeId: number;
  outcomeName?: TradeOutcomeName | string | null;
  type: TradeType;
  quantity: number | string;
  price: number | string;
  totalCost: number | string;
  walletBalanceAfterTrade: number | string;
  positionQuantityAfterTrade: number | string;
  createdAt?: string | null;
}

export interface CreateMarketRequestDto {
  adminUserId: number;
  title: string;
  description: string;
  category: string;
  tradingCloseDate: string;
  resolutionDate: string;
  resolutionSource: string;
}

export interface ResolutionRequestDto {
  adminUserId: number;
  marketId: number;
  winningOutcomeId: number;
}

export interface BackendMarketResponseDto {
  marketId: number;
  title: string;
  description?: string | null;
  category: string;
  status: BackendMarketStatus;
  tradingCloseDate: string;
  resolutionDate: string;
  resolutionSource?: string | null;
  yesOutcomeId: number;
  noOutcomeId: number;
  yesPrice: number | string;
  noPrice: number | string;
}

export interface Market {
  id: string;
  marketId: number;
  title: string;
  description?: string;
  category?: string;
  state: MarketState;
  status: BackendMarketStatus;
  yesPrice: number;
  noPrice: number;
  liquidity?: number;
  volume24h?: number;
  endDate?: string;
  tradingCloseDate?: string;
  resolutionDate?: string;
  resolutionSource?: string;
  yesOutcomeId?: number;
  noOutcomeId?: number;
  outcomes?: string[];
}

export interface PriceHistory {
  timestamp: string;
  yesPrice: number;
  noPrice: number;
}

export interface MarketDetail extends Market {
  priceHistory?: PriceHistory[];
  recentTrades?: Array<{
    id: string;
    outcome: 'yes' | 'no';
    price: number;
    amount: number;
    timestamp: number;
  }>;
  relatedMarkets?: Market[];
}

export interface PriceHistoryDto {
  marketId: number;
  timestamp: string;
  yesPrice: number;
  noPrice: number;
}

export interface LeaderboardResponseDto {
  rank: number;
  userId: number;
  username: string;
  email: string;
  walletBalance: number | string;
  openPositions: number;
  totalTrades: number;
  portfolioValue: number | string;
  reliabilityScore: number | string;
}

export interface MarketStatisticsDto {
  marketId: number;
  totalTrades: number;
  totalVolume: number | string;
  liquidity: number | string;
  activeTraders: number;
}

export interface DashboardSummaryDto {
  openMarkets: number;
  totalMarkets: number;
  averagePrice: number | string;
  walletBalance: number | string;
  portfolioValue: number | string;
  totalTrades: number;
  openPositions: number;
}

export interface AdminDashboardDto {
  summary: {
    totalMarkets: number; openMarkets: number; closedMarkets: number;
    waitingForResolution: number; resolvedMarkets: number; cancelledMarkets: number;
    totalUsers: number; activeUsersLast24Hours: number; totalTrades: number;
    totalTradingVolume: number | string;
  };
  marketsRequiringAttention: Array<{
    marketId: number; title: string; category: string;
    tradingCloseDate: string; resolutionDate: string;
  }>;
  recentActivity: Array<{
    tradeId: number; username: string; marketId: number; marketTitle: string;
    type: TradeType; outcome: string; quantity: number | string;
    totalValue: number | string; createdAt: string;
  }>;
  sharpPriceMovements: Array<{
    marketId: number; marketTitle: string; previousYesPrice: number | string;
    currentYesPrice: number | string; absoluteChange: number | string;
    windowStart: string; windowEnd: string;
  }>;
  sharpMovementThreshold: number | string;
  sharpMovementWindowHours: number;
}

export interface MarketsResult {
  markets: Market[];
  total: number;
}
