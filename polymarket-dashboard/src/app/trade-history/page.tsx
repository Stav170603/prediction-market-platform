'use client';

import Link from 'next/link';
import { useEffect, useMemo } from 'react';
import { useRouter } from 'next/navigation';
import { useQueries, useQuery } from '@tanstack/react-query';
import { getMarketById } from '@/services/marketService';
import { getMyTrades } from '@/services/tradeService';
import { MarketDetail, TradeResponseDto } from '@/types/api';
import { formatPrice } from '@/lib/utils';
import { EmptyState, ErrorBoundary, LoadingSpinner } from '@/components/Loading';
import { useAuth } from '@/contexts/AuthContext';

function toNumber(value: number | string | null | undefined): number {
  if (value === null || value === undefined) return 0;
  const parsed = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

function formatQuantity(value: number | string): string {
  return toNumber(value).toLocaleString(undefined, {
    minimumFractionDigits: 0,
    maximumFractionDigits: 4,
  });
}

function formatCurrency(value: number | string): string {
  return formatPrice(toNumber(value));
}

function formatDate(value?: string | null): string {
  if (!value) return 'Unavailable';

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return 'Unavailable';

  return date.toLocaleString();
}

function getMarketTitle(marketId: number, marketsById: Map<number, MarketDetail>): string {
  return marketsById.get(marketId)?.title ?? `Market #${marketId}`;
}

function getOutcomeName(trade: TradeResponseDto, market?: MarketDetail): string {
  if (trade.outcomeName) return String(trade.outcomeName);
  if (market?.yesOutcomeId === trade.outcomeId) return 'YES';
  if (market?.noOutcomeId === trade.outcomeId) return 'NO';
  return `Outcome #${trade.outcomeId}`;
}

export default function TradeHistoryPage() {
  const router = useRouter();
  const { currentUser, isAuthInitialized } = useAuth();

  useEffect(() => {
    if (isAuthInitialized && !currentUser) {
      router.replace('/login');
    }
  }, [currentUser, isAuthInitialized, router]);

  const tradesQuery = useQuery({
    queryKey: ['trade-history', currentUser?.userId],
    queryFn: getMyTrades,
    enabled: isAuthInitialized && Boolean(currentUser?.userId),
    retry: false,
  });

  const trades = useMemo(() => tradesQuery.data ?? [], [tradesQuery.data]);
  const marketIds = useMemo(
    () => Array.from(new Set(trades.map((trade) => trade.marketId))),
    [trades]
  );

  const marketQueries = useQueries({
    queries: marketIds.map((marketId) => ({
      queryKey: ['market', marketId],
      queryFn: () => getMarketById(marketId),
      enabled: isAuthInitialized && Boolean(currentUser?.userId),
      retry: false,
    })),
  });

  const marketsById = useMemo(() => {
    const map = new Map<number, MarketDetail>();
    marketQueries.forEach((query) => {
      if (query.data) {
        map.set(query.data.marketId, query.data);
      }
    });
    return map;
  }, [marketQueries]);

  if (!isAuthInitialized || !currentUser) return <LoadingSpinner />;

  const marketError = marketQueries.find((query) => query.error)?.error;
  if (tradesQuery.error || marketError) {
    return <ErrorBoundary error={(tradesQuery.error || marketError) as Error} />;
  }

  const isLoadingMarkets = marketQueries.some((query) => query.isLoading);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-4xl font-bold text-slate-900 dark:text-white">Trade History</h1>
        <p className="mt-2 text-slate-600 dark:text-slate-400">
          Completed trades for {currentUser.username}
        </p>
      </div>

      {tradesQuery.isLoading || isLoadingMarkets ? (
        <LoadingSpinner />
      ) : trades.length === 0 ? (
        <div className="rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900">
          <EmptyState
            title="No trades yet"
            description="Your completed trades will appear here."
          />
        </div>
      ) : (
        <div className="overflow-x-auto rounded-lg border border-slate-200 dark:border-slate-700">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-900">
                <th className="px-4 py-3 text-left font-semibold text-slate-900 dark:text-white">Market</th>
                <th className="px-4 py-3 text-left font-semibold text-slate-900 dark:text-white">Outcome</th>
                <th className="px-4 py-3 text-left font-semibold text-slate-900 dark:text-white">BUY/SELL</th>
                <th className="px-4 py-3 text-right font-semibold text-slate-900 dark:text-white">Quantity</th>
                <th className="px-4 py-3 text-right font-semibold text-slate-900 dark:text-white">Price</th>
                <th className="px-4 py-3 text-right font-semibold text-slate-900 dark:text-white">Total cost</th>
                <th className="px-4 py-3 text-right font-semibold text-slate-900 dark:text-white">Wallet balance after trade</th>
                <th className="px-4 py-3 text-right font-semibold text-slate-900 dark:text-white">Created date</th>
              </tr>
            </thead>
            <tbody>
              {trades.map((trade) => {
                const market = marketsById.get(trade.marketId);
                const outcomeName = getOutcomeName(trade, market);

                return (
                  <tr
                    key={trade.tradeId}
                    className="border-b border-slate-200 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-900"
                  >
                    <td className="px-4 py-3 min-w-[220px]">
                      <Link
                        href={`/market/${trade.marketId}`}
                        className="font-medium text-slate-900 dark:text-white hover:text-blue-600 dark:hover:text-blue-400"
                      >
                        {getMarketTitle(trade.marketId, marketsById)}
                      </Link>
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className={`inline-block px-3 py-1 rounded-full text-xs font-medium ${
                          outcomeName === 'YES'
                            ? 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200'
                            : 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200'
                        }`}
                      >
                        {outcomeName}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className={`inline-block px-3 py-1 rounded-full text-xs font-medium ${
                          trade.type === 'BUY'
                            ? 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200'
                            : 'bg-amber-100 text-amber-800 dark:bg-amber-900 dark:text-amber-200'
                        }`}
                      >
                        {trade.type}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right">{formatQuantity(trade.quantity)}</td>
                    <td className="px-4 py-3 text-right">{formatCurrency(trade.price)}</td>
                    <td className="px-4 py-3 text-right font-medium">{formatCurrency(trade.totalCost)}</td>
                    <td className="px-4 py-3 text-right">{formatCurrency(trade.walletBalanceAfterTrade)}</td>
                    <td className="px-4 py-3 text-right text-xs text-slate-600 dark:text-slate-400">
                      {formatDate(trade.createdAt)}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
