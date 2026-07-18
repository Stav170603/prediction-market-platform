'use client';

import Link from 'next/link';
import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';
import { ArrowLeft } from 'lucide-react';
import { toast } from 'sonner';
import { formatPrice, formatVolume, getStateColor, getStateLabel } from '@/lib/utils';
import { MarketDetail } from '@/lib/api';
import { PriceChart } from './PriceChart';
import { updateCurrentUser } from '@/services/authStorage';
import { getMarketPriceHistory, getMarketStatistics } from '@/services/marketService';
import { executeTrade, getTradesByMarket } from '@/services/tradeService';
import { getMyPositions } from '@/services/positionService';
import { getMyWallet } from '@/services/walletService';
import { TradeOutcomeName, TradeResponseDto, TradeType } from '@/types/api';
import { EmptyState, LoadingSpinner } from '@/components/ui/Loading';
import { useAuth } from '@/contexts/AuthContext';

interface MarketDetailProps {
  market: MarketDetail;
  isLoading?: boolean;
}

function toNumber(value: number | string | null | undefined): number {
  if (value === null || value === undefined) return 0;
  const parsed = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

function getTradeOutcomeName(trade: TradeResponseDto, market: MarketDetail): string | null {
  if (trade.outcomeName) return String(trade.outcomeName);
  if (trade.outcomeId === market.yesOutcomeId) return 'YES';
  if (trade.outcomeId === market.noOutcomeId) return 'NO';
  return null;
}

function getTradeTime(trade: TradeResponseDto): string | null {
  if (!trade.createdAt) return null;
  const date = new Date(trade.createdAt);
  return Number.isNaN(date.getTime()) ? trade.createdAt : date.toLocaleString();
}

export function MarketDetailComponent({ market, isLoading }: MarketDetailProps) {
  const queryClient = useQueryClient();
  const { currentUser, refreshCurrentUser } = useAuth();
  const [tradeType, setTradeType] = useState<TradeType>('BUY');
  const [outcomeName, setOutcomeName] = useState<TradeOutcomeName>('YES');
  const [quantity, setQuantity] = useState('1');

  const yesPercentage = (market.yesPrice * 100).toFixed(1);
  const noPercentage = ((1 - market.yesPrice) * 100).toFixed(1);
  const selectedOutcomeId = outcomeName === 'YES' ? market.yesOutcomeId : market.noOutcomeId;
  const selectedPrice = outcomeName === 'YES' ? market.yesPrice : market.noPrice;
  const parsedQuantity = Number(quantity);
  const estimatedValue = useMemo(() => {
    if (!Number.isFinite(parsedQuantity) || parsedQuantity <= 0) return 0;
    return parsedQuantity * selectedPrice;
  }, [parsedQuantity, selectedPrice]);

  const walletQuery = useQuery({
    queryKey: ['wallet', currentUser?.userId],
    queryFn: getMyWallet,
    enabled: Boolean(currentUser),
    retry: false,
  });

  const positionsQuery = useQuery({
    queryKey: ['positions', currentUser?.userId],
    queryFn: getMyPositions,
    enabled: Boolean(currentUser),
    retry: false,
  });

  const walletBalance = walletQuery.data ? toNumber(walletQuery.data.balance) : null;
  const ownedQuantity = toNumber(
    positionsQuery.data?.find(
      (position) => position.marketId === market.marketId && position.outcomeId === selectedOutcomeId
    )?.quantity
  );
  const tradeValidationError = useMemo(() => {
    if (!Number.isInteger(parsedQuantity) || parsedQuantity < 1) {
      return 'Quantity must be a positive whole number';
    }
    if (tradeType === 'BUY'
        && walletBalance !== null
        && estimatedValue > walletBalance) {
      return 'Insufficient balance';
    }
    if (tradeType === 'SELL'
        && positionsQuery.data !== undefined
        && parsedQuantity > ownedQuantity) {
      return 'Insufficient position to sell';
    }
    return null;
  }, [
    estimatedValue,
    ownedQuantity,
    parsedQuantity,
    positionsQuery.data,
    tradeType,
    walletBalance,
  ]);
  const isTradeDataLoading = walletQuery.isLoading
    || (tradeType === 'SELL' && positionsQuery.isLoading);

  const recentTradesQuery = useQuery({
    queryKey: ['trades-by-market', market.marketId],
    queryFn: () => getTradesByMarket(market.marketId),
    enabled: Boolean(market.marketId),
    staleTime: 5000,
    refetchInterval: 5000,
  });

  const priceHistoryQuery = useQuery({
    queryKey: ['market-price-history', market.marketId],
    queryFn: () => getMarketPriceHistory(market.marketId),
    enabled: Boolean(market.marketId),
    staleTime: 5000,
    refetchInterval: 5000,
  });

  const marketStatisticsQuery = useQuery({
    queryKey: ['market-statistics', market.marketId],
    queryFn: () => getMarketStatistics(market.marketId),
    enabled: Boolean(market.marketId),
    staleTime: 5000,
    refetchInterval: 5000,
  });

  const tradeMutation = useMutation({
    mutationFn: () => {
      if (!currentUser) {
        throw new Error('Login is required before trading.');
      }
      if (!selectedOutcomeId) {
        throw new Error('Selected outcome is unavailable.');
      }
      if (!Number.isInteger(parsedQuantity) || parsedQuantity < 1) {
        throw new Error('Quantity must be a positive whole number.');
      }
      if (tradeType !== 'BUY' && tradeType !== 'SELL') {
        throw new Error('Trade type must be BUY or SELL.');
      }

      return executeTrade({
        userId: currentUser.userId,
        marketId: market.marketId,
        outcomeId: selectedOutcomeId,
        quantity: parsedQuantity,
        type: tradeType,
      });
    },
    onSuccess: async (response) => {
      setQuantity('1');
      updateCurrentUser({ walletBalance: response.walletBalanceAfterTrade });
      refreshCurrentUser();
      toast.success(`${tradeType} ${outcomeName} trade completed.`);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['market', market.id] }),
        queryClient.invalidateQueries({ queryKey: ['trades-by-market', market.marketId] }),
        queryClient.invalidateQueries({ queryKey: ['market-price-history', market.marketId] }),
        queryClient.invalidateQueries({ queryKey: ['market-statistics', market.marketId] }),
        queryClient.invalidateQueries({ queryKey: ['markets'] }),
        queryClient.invalidateQueries({ queryKey: ['markets-full'] }),
        queryClient.invalidateQueries({ queryKey: ['wallet', currentUser?.userId] }),
        queryClient.invalidateQueries({ queryKey: ['wallet-transactions', currentUser?.userId] }),
        queryClient.invalidateQueries({ queryKey: ['positions', currentUser?.userId] }),
        queryClient.invalidateQueries({ queryKey: ['trade-history', currentUser?.userId] }),
      ]);
    },
    onError: (err) => {
      if (!axios.isAxiosError(err)) {
        toast.error(err instanceof Error ? err.message : 'Trade failed. Check your balance and position.');
      }
    },
  });

  const handleTradeSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!currentUser) {
      toast.error('Login is required before trading.');
      return;
    }
    if (!selectedOutcomeId) {
      toast.error('Selected outcome is unavailable.');
      return;
    }
    if (!Number.isInteger(parsedQuantity) || parsedQuantity < 1) {
      toast.error('Quantity must be a positive whole number.');
      return;
    }
    if (tradeType !== 'BUY' && tradeType !== 'SELL') {
      toast.error('Trade type must be BUY or SELL.');
      return;
    }
    if (tradeType === 'BUY'
        && walletBalance !== null
        && estimatedValue > walletBalance) {
      toast.error('Insufficient balance.');
      return;
    }
    if (tradeType === 'SELL'
        && positionsQuery.data !== undefined
        && parsedQuantity > ownedQuantity) {
      toast.error('Insufficient position to sell.');
      return;
    }
    tradeMutation.mutate();
  };

  const recentTrades = useMemo(() => {
    return [...(recentTradesQuery.data ?? [])].sort((first, second) => {
      const firstTime = first.createdAt ? new Date(first.createdAt).getTime() : Number.NEGATIVE_INFINITY;
      const secondTime = second.createdAt ? new Date(second.createdAt).getTime() : Number.NEGATIVE_INFINITY;
      const normalizedFirstTime = Number.isNaN(firstTime) ? Number.NEGATIVE_INFINITY : firstTime;
      const normalizedSecondTime = Number.isNaN(secondTime) ? Number.NEGATIVE_INFINITY : secondTime;

      return normalizedSecondTime - normalizedFirstTime || second.tradeId - first.tradeId;
    });
  }, [recentTradesQuery.data]);
  const showTradeTimes = recentTrades.some((trade) => Boolean(trade.createdAt));
  const tradingDisabledMessage = market.status === 'CLOSED'
    ? 'This market is closed. Trading is no longer available.'
    : market.status === 'CANCELLED'
      ? 'This market was cancelled. Trading is unavailable.'
      : market.status === 'RESOLVED'
        ? 'This market has been resolved. Trading is complete.'
        : null;

  if (isLoading) {
    return <div className="text-center py-12">Loading market details...</div>;
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-start gap-4">
        <Link
          href="/markets"
          className="p-2 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
        >
          <ArrowLeft className="w-5 h-5" />
        </Link>
        <div className="flex-1">
          <h1 className="text-3xl font-bold text-slate-900 dark:text-white">
            {market.title}
          </h1>
          <p className="mt-2 text-slate-600 dark:text-slate-400">
            {market.description}
          </p>
        </div>
      </div>

      {/* Status */}
      <div className="flex items-center gap-4 flex-wrap">
        <span className={`px-4 py-2 rounded-full font-medium ${getStateColor(market.state)}`}>
          {getStateLabel(market.state)}
        </span>
        {market.category && (
          <span className="px-4 py-2 rounded-full bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 font-medium">
            {market.category}
          </span>
        )}
      </div>

      {/* Main Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column - Key Stats */}
        <div className="lg:col-span-1 space-y-4">
          <div className="rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 p-6">
            <h3 className="text-sm font-semibold text-slate-600 dark:text-slate-400 mb-4">
              Current Prices
            </h3>
            <div className="space-y-4">
              <div>
                <div className="flex items-baseline justify-between">
                  <span className="text-sm font-medium text-slate-700 dark:text-slate-300">
                    YES
                  </span>
                  <span className="text-2xl font-bold text-green-600">
                    {formatPrice(market.yesPrice)}
                  </span>
                </div>
                <div className="mt-1 w-full bg-slate-200 dark:bg-slate-700 rounded-full h-2">
                  <div
                    className="bg-green-600 h-2 rounded-full"
                    style={{ width: `${market.yesPrice * 100}%` }}
                  />
                </div>
                <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
                  {yesPercentage}% probability
                </p>
              </div>

              <div>
                <div className="flex items-baseline justify-between">
                  <span className="text-sm font-medium text-slate-700 dark:text-slate-300">
                    NO
                  </span>
                  <span className="text-2xl font-bold text-red-600">
                    {formatPrice(market.noPrice)}
                  </span>
                </div>
                <div className="mt-1 w-full bg-slate-200 dark:bg-slate-700 rounded-full h-2">
                  <div
                    className="bg-red-600 h-2 rounded-full"
                    style={{ width: `${market.noPrice * 100}%` }}
                  />
                </div>
                <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
                  {noPercentage}% probability
                </p>
              </div>
            </div>
          </div>

          {/* Stats Box */}
          <div className="rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 p-6">
            <h3 className="text-sm font-semibold text-slate-600 dark:text-slate-400 mb-4">
              Market Info
            </h3>
            <div className="space-y-3">
              {marketStatisticsQuery.isLoading ? (
                <p className="text-sm text-slate-600 dark:text-slate-400">Loading market statistics...</p>
              ) : marketStatisticsQuery.error ? (
                <p className="text-sm text-red-600 dark:text-red-400">Unable to load market statistics.</p>
              ) : marketStatisticsQuery.data ? (
                <>
                  <div>
                    <p className="text-sm text-slate-600 dark:text-slate-400">Total Trades</p>
                    <p className="text-lg font-bold text-slate-900 dark:text-white">
                      {marketStatisticsQuery.data.totalTrades.toLocaleString()}
                    </p>
                  </div>
                  <div>
                    <p className="text-sm text-slate-600 dark:text-slate-400">Total Volume</p>
                    <p className="text-lg font-bold text-slate-900 dark:text-white">
                      {formatVolume(toNumber(marketStatisticsQuery.data.totalVolume))}
                    </p>
                  </div>
                  <div>
                    <p className="text-sm text-slate-600 dark:text-slate-400">Liquidity</p>
                    <p className="text-lg font-bold text-slate-900 dark:text-white">
                      {formatVolume(toNumber(marketStatisticsQuery.data.liquidity))}
                    </p>
                  </div>
                  <div>
                    <p className="text-sm text-slate-600 dark:text-slate-400">Active Traders</p>
                    <p className="text-lg font-bold text-slate-900 dark:text-white">
                      {marketStatisticsQuery.data.activeTraders.toLocaleString()}
                    </p>
                  </div>
                </>
              ) : null}
              {market.endDate && (
                <div>
                  <p className="text-sm text-slate-600 dark:text-slate-400">End Date</p>
                  <p className="text-lg font-bold text-slate-900 dark:text-white">
                    {new Date(market.endDate).toLocaleDateString()}
                  </p>
                </div>
              )}
            </div>
          </div>

          <div className="rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 p-6">
            <h3 className="text-sm font-semibold text-slate-600 dark:text-slate-400 mb-4">
              Trade Ticket
            </h3>

            {tradingDisabledMessage ? (
              <div className="rounded-lg bg-slate-50 p-4 text-sm text-slate-600 dark:bg-slate-950 dark:text-slate-400">
                {tradingDisabledMessage}
              </div>
            ) : !currentUser ? (
              <div className="space-y-3">
                <p className="text-sm text-slate-600 dark:text-slate-400">
                  Login to buy or sell shares in this market.
                </p>
                <Link
                  href="/login"
                  className="block w-full px-6 py-3 rounded-lg bg-blue-600 hover:bg-blue-700 text-white text-center font-medium transition-colors"
                >
                  Login to trade
                </Link>
              </div>
            ) : (
              <form onSubmit={handleTradeSubmit} className="space-y-4">
                <div className="grid grid-cols-2 gap-2">
                  {(['BUY', 'SELL'] as TradeType[]).map((type) => (
                    <button
                      key={type}
                      type="button"
                      onClick={() => setTradeType(type)}
                      className={`px-4 py-2 rounded-lg border font-medium transition-colors ${
                        tradeType === type
                          ? 'border-blue-600 bg-blue-50 text-blue-700 dark:bg-blue-950 dark:text-blue-200'
                          : 'border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300'
                      }`}
                    >
                      {type}
                    </button>
                  ))}
                </div>

                <div className="grid grid-cols-2 gap-2">
                  {(['YES', 'NO'] as TradeOutcomeName[]).map((outcome) => (
                    <button
                      key={outcome}
                      type="button"
                      onClick={() => setOutcomeName(outcome)}
                      className={`px-4 py-2 rounded-lg border font-medium transition-colors ${
                        outcomeName === outcome
                          ? outcome === 'YES'
                            ? 'border-green-600 bg-green-50 text-green-700 dark:bg-green-950 dark:text-green-200'
                            : 'border-red-600 bg-red-50 text-red-700 dark:bg-red-950 dark:text-red-200'
                          : 'border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300'
                      }`}
                    >
                      {outcome}
                    </button>
                  ))}
                </div>

                <div>
                  <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">
                    Quantity
                  </label>
                  <input
                    type="number"
                    min="1"
                    step="1"
                    value={quantity}
                    onChange={(event) => {
                      const nextQuantity = event.target.value;
                      if (/^\d*$/.test(nextQuantity)) {
                        setQuantity(nextQuantity);
                      }
                    }}
                    className="w-full px-4 py-2 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-950 text-slate-900 dark:text-white"
                    required
                  />
                </div>

                <div className="rounded-lg bg-slate-50 dark:bg-slate-950 p-3 text-sm space-y-1">
                  <div className="flex justify-between">
                    <span className="text-slate-600 dark:text-slate-400">Price</span>
                    <span className="font-medium text-slate-900 dark:text-white">{formatPrice(selectedPrice)}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-600 dark:text-slate-400">Estimated value</span>
                    <span className="font-medium text-slate-900 dark:text-white">{formatPrice(estimatedValue)}</span>
                  </div>
                  {walletBalance !== null && (
                    <div className="flex justify-between">
                      <span className="text-slate-600 dark:text-slate-400">Wallet balance</span>
                      <span className="font-medium text-slate-900 dark:text-white">{formatPrice(walletBalance)}</span>
                    </div>
                  )}
                  {tradeType === 'SELL' && positionsQuery.data !== undefined && (
                    <div className="flex justify-between">
                      <span className="text-slate-600 dark:text-slate-400">Owned position</span>
                      <span className="font-medium text-slate-900 dark:text-white">{ownedQuantity}</span>
                    </div>
                  )}
                </div>

                {tradeValidationError && (
                  <p className="text-sm font-medium text-red-600 dark:text-red-400" role="alert">
                    {tradeValidationError}
                  </p>
                )}

                <button
                  type="submit"
                  disabled={tradeMutation.isPending || isTradeDataLoading || Boolean(tradeValidationError)}
                  className="w-full px-6 py-3 rounded-lg bg-blue-600 hover:bg-blue-700 disabled:opacity-60 disabled:cursor-not-allowed text-white font-medium transition-colors"
                >
                  {tradeMutation.isPending ? 'Submitting...' : `${tradeType} ${outcomeName}`}
                </button>
              </form>
            )}
          </div>
        </div>

        {/* Right Column - Chart */}
        <div className="lg:col-span-2">
          {priceHistoryQuery.isLoading ? (
            <div>
              <h3 className="text-lg font-semibold text-slate-900 dark:text-white mb-4">
                Price History
              </h3>
              <div className="h-96 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900">
                <LoadingSpinner />
              </div>
            </div>
          ) : priceHistoryQuery.error ? (
            <div className="rounded-lg border border-red-200 dark:border-red-900 bg-red-50 dark:bg-red-950 p-4 text-sm text-red-700 dark:text-red-300">
              Unable to load price history.
            </div>
          ) : priceHistoryQuery.data && priceHistoryQuery.data.length > 0 ? (
            <div>
              <h3 className="text-lg font-semibold text-slate-900 dark:text-white mb-4">
                Price History
              </h3>
              <PriceChart data={priceHistoryQuery.data} />
            </div>
          ) : (
            <div>
              <h3 className="text-lg font-semibold text-slate-900 dark:text-white mb-4">
                Price History
              </h3>
              <div className="h-96 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900">
                <EmptyState
                  title="No price history available yet."
                />
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Recent Trades */}
      <div>
        <h3 className="text-lg font-semibold text-slate-900 dark:text-white mb-4">
          Recent Trades
        </h3>
        {recentTradesQuery.isLoading ? (
          <div className="rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 p-6 text-slate-600 dark:text-slate-400">
            Loading recent trades...
          </div>
        ) : recentTradesQuery.error ? (
          <div className="rounded-lg border border-red-200 dark:border-red-900 bg-red-50 dark:bg-red-950 p-4 text-sm text-red-700 dark:text-red-300">
            Unable to load recent trades.
          </div>
        ) : recentTrades.length === 0 ? (
          <div className="rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 p-6 text-slate-600 dark:text-slate-400">
            No trades have been executed for this market yet.
          </div>
        ) : (
          <div className="overflow-x-auto rounded-lg border border-slate-200 dark:border-slate-700">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-900">
                  <th className="px-4 py-3 text-left font-semibold text-slate-900 dark:text-white">
                    Type
                  </th>
                  <th className="px-4 py-3 text-left font-semibold text-slate-900 dark:text-white">
                    Outcome
                  </th>
                  <th className="px-4 py-3 text-right font-semibold text-slate-900 dark:text-white">
                    Quantity
                  </th>
                  <th className="px-4 py-3 text-right font-semibold text-slate-900 dark:text-white">
                    Price
                  </th>
                  <th className="px-4 py-3 text-right font-semibold text-slate-900 dark:text-white">
                    Total Cost
                  </th>
                  <th className="px-4 py-3 text-right font-semibold text-slate-900 dark:text-white">
                    User
                  </th>
                  {showTradeTimes && (
                    <th className="px-4 py-3 text-right font-semibold text-slate-900 dark:text-white">
                      Time
                    </th>
                  )}
                </tr>
              </thead>
              <tbody>
                {recentTrades.map((trade) => {
                  const displayedOutcomeName = getTradeOutcomeName(trade, market);
                  const tradeTime = getTradeTime(trade);

                  return (
                    <tr
                      key={trade.tradeId}
                      className="border-b border-slate-200 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-900"
                    >
                      <td className="px-4 py-3">
                        <span className={`inline-block px-3 py-1 rounded-full text-xs font-medium ${
                          trade.type === 'BUY'
                            ? 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200'
                            : 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200'
                        }`}>
                          {trade.type}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-slate-600 dark:text-slate-400">
                        {displayedOutcomeName ?? `Outcome #${trade.outcomeId}`}
                      </td>
                      <td className="px-4 py-3 text-right text-slate-600 dark:text-slate-400">
                        {toNumber(trade.quantity).toFixed(4)}
                      </td>
                      <td className="px-4 py-3 text-right font-medium">
                        {formatPrice(toNumber(trade.price))}
                      </td>
                      <td className="px-4 py-3 text-right text-slate-600 dark:text-slate-400">
                        {formatPrice(toNumber(trade.totalCost))}
                      </td>
                      <td className="px-4 py-3 text-right text-slate-600 dark:text-slate-400">
                        {trade.username || `User #${trade.userId}`}
                      </td>
                      {showTradeTimes && (
                        <td className="px-4 py-3 text-right text-slate-600 dark:text-slate-400 text-xs">
                          {tradeTime ?? ''}
                        </td>
                      )}
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Related Markets */}
      <div>
        <h3 className="text-lg font-semibold text-slate-900 dark:text-white mb-4">
          Related Markets
        </h3>
        {market.relatedMarkets && market.relatedMarkets.length > 0 ? (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {market.relatedMarkets.map((relatedMarket) => (
              <Link
                key={relatedMarket.id}
                href={`/market/${relatedMarket.id}`}
                className="p-4 rounded-lg border border-slate-200 dark:border-slate-700 hover:border-blue-600 dark:hover:border-blue-400 hover:shadow-md dark:hover:shadow-blue-900 transition-all"
              >
                <h4 className="font-medium text-slate-900 dark:text-white truncate">
                  {relatedMarket.title}
                </h4>
                <div className="mt-2 flex items-center gap-4">
                  <div>
                    <p className="text-xs text-slate-600 dark:text-slate-400">YES</p>
                    <p className="font-semibold text-green-600">
                      {formatPrice(relatedMarket.yesPrice)}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs text-slate-600 dark:text-slate-400">NO</p>
                    <p className="font-semibold text-red-600">
                      {formatPrice(relatedMarket.noPrice)}
                    </p>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        ) : (
          <div className="rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 p-6 text-slate-600 dark:text-slate-400">
            Related markets are not available yet.
          </div>
        )}
      </div>
    </div>
  );
}
