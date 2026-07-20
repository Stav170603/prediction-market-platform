import { apiClient, authRequestConfig } from './http';
import {
  BackendMarketStatus,
  PositionResponseDto,
  TradeResponseDto,
} from '@/types/api';

type PositionPayload = Omit<
  PositionResponseDto,
  'positionId' | 'userId' | 'marketId' | 'marketStatus' | 'outcomeId'
> & {
  positionId: number | string;
  userId: number | string;
  marketId: number | string;
  marketStatus: string;
  outcomeId: number | string;
};

export const positionQueryKey = (userId?: number | string) =>
  ['positions', userId === undefined ? undefined : Number(userId)] as const;

function normalizePosition(position: PositionPayload): PositionResponseDto {
  return {
    ...position,
    positionId: Number(position.positionId),
    userId: Number(position.userId),
    marketId: Number(position.marketId),
    marketStatus: position.marketStatus.toUpperCase() as BackendMarketStatus,
    outcomeId: Number(position.outcomeId),
  };
}

export async function getPositionsByUserId(userId: number): Promise<PositionResponseDto[]> {
  const { data } = await apiClient.get<PositionPayload[]>(
    `/api/positions/user/${userId}`,
    authRequestConfig()
  );
  return data.map(normalizePosition);
}

export async function getMyPositions(): Promise<PositionResponseDto[]> {
  const { data } = await apiClient.get<PositionPayload[]>('/api/positions/me', authRequestConfig());
  return data.map(normalizePosition);
}

export function applyTradeToPositions(
  positions: PositionResponseDto[] | undefined,
  trade: TradeResponseDto,
  market: {
    title: string;
    status: BackendMarketStatus;
  }
): PositionResponseDto[] {
  const current = positions ?? [];
  const index = current.findIndex(
    (position) =>
      position.marketId === Number(trade.marketId)
      && position.outcomeId === Number(trade.outcomeId)
  );
  const quantity = Number(trade.positionQuantityAfterTrade);

  if (quantity <= 0) {
    return index < 0 ? current : current.filter((_, positionIndex) => positionIndex !== index);
  }

  if (index >= 0) {
    return current.map((position, positionIndex) =>
      positionIndex === index ? { ...position, quantity } : position
    );
  }

  return [
    ...current,
    {
      positionId: -Number(trade.tradeId),
      userId: Number(trade.userId),
      marketId: Number(trade.marketId),
      marketTitle: market.title,
      marketStatus: market.status,
      outcomeId: Number(trade.outcomeId),
      outcomeName: String(trade.outcomeName ?? ''),
      quantity,
      currentPrice: trade.price,
      currentValue: quantity * Number(trade.price),
      unrealizedPnL: 0,
    },
  ];
}
