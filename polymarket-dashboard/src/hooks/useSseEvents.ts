'use client';

import { useEffect } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { API_BASE_URL } from '@/services/http';
import { useAuth } from '@/contexts/AuthContext';
import { positionQueryKey } from '@/services/positionService';

interface MarketEventPayload {
  marketId?: number;
}

interface UserEventPayload {
  userId?: number;
}

function parseEventData<T>(event: MessageEvent): T | null {
  try {
    return JSON.parse(event.data) as T;
  } catch {
    return null;
  }
}

function streamUrl(): string {
  return `${API_BASE_URL.replace(/\/$/, '')}/api/events/stream`;
}

export function useSseEvents() {
  const queryClient = useQueryClient();
  const { currentUser } = useAuth();

  useEffect(() => {
    if (!currentUser || typeof window === 'undefined') return;

    const eventSource = new EventSource(streamUrl());

    const invalidateMarketQueries = (marketId?: number) => {
      queryClient.invalidateQueries({ queryKey: ['markets'] });
      queryClient.invalidateQueries({ queryKey: ['markets-full'] });
      queryClient.invalidateQueries({ queryKey: ['admin-markets'] });
      queryClient.invalidateQueries({ queryKey: ['admin-dashboard'] });

      if (marketId !== undefined) {
        queryClient.invalidateQueries({ queryKey: ['market', marketId] });
        queryClient.invalidateQueries({ queryKey: ['market', String(marketId)] });
        queryClient.invalidateQueries({ queryKey: ['market-price-history', marketId] });
        queryClient.invalidateQueries({ queryKey: ['market-statistics', marketId] });
        queryClient.invalidateQueries({ queryKey: ['trades-by-market', marketId] });
      }
    };

    const invalidateUserQueries = (userId?: number) => {
      const targetUserId = userId ?? currentUser.userId;

      queryClient.invalidateQueries({ queryKey: ['wallet', targetUserId] });
      queryClient.invalidateQueries({ queryKey: ['wallet-transactions', targetUserId] });
      queryClient.invalidateQueries({ queryKey: positionQueryKey(targetUserId) });
      queryClient.invalidateQueries({ queryKey: ['trade-history', targetUserId] });
      queryClient.invalidateQueries({ queryKey: ['leaderboard'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] });
    };

    eventSource.addEventListener('market-price-updated', (event) => {
      const payload = parseEventData<MarketEventPayload>(event);
      invalidateMarketQueries(payload?.marketId);
    });

    eventSource.addEventListener('trade-created', (event) => {
      const payload = parseEventData<MarketEventPayload & UserEventPayload>(event);
      invalidateMarketQueries(payload?.marketId);
      invalidateUserQueries(payload?.userId);
      queryClient.invalidateQueries({ queryKey: ['markets'] });
    });

    eventSource.addEventListener('market-resolved', (event) => {
      const payload = parseEventData<MarketEventPayload>(event);
      invalidateMarketQueries(payload?.marketId);
      queryClient.invalidateQueries({ queryKey: ['positions'] });
      queryClient.invalidateQueries({ queryKey: ['wallet'] });
      queryClient.invalidateQueries({ queryKey: ['leaderboard'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] });
    });

    eventSource.addEventListener('user-portfolio-updated', (event) => {
      const payload = parseEventData<UserEventPayload>(event);
      invalidateUserQueries(payload?.userId);
    });

    eventSource.onerror = () => {
      console.warn('SSE connection error. Polling remains active as fallback.');
    };

    return () => eventSource.close();
  }, [currentUser, queryClient]);
}
