'use client';

import { useQuery } from '@tanstack/react-query';
import { fetchMarkets } from '@/lib/api';
import { StatsCards } from '@/components/dashboard/StatsCards';
import { MarketsList } from '@/components/markets/MarketsList';
import { LoadingSpinner, ErrorBoundary } from '@/components/ui/Loading';
import { getDashboardSummary } from '@/services/dashboardService';
import { Activity, BarChart3, Briefcase, Target, TrendingUp, Wallet, Zap } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';

function toNumber(value: number | string | null | undefined): number {
  if (value === null || value === undefined) return 0;
  const parsed = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

function formatMoney(value: number | string): string {
  return `${toNumber(value).toLocaleString(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 4,
  })} pts`;
}

export default function DashboardPage() {
  const { currentUser, isAuthInitialized } = useAuth();

  const { data, isLoading, error } = useQuery({
    queryKey: ['markets'],
    queryFn: () => fetchMarkets(1, 100),
    staleTime: 5000,
    refetchInterval: 5000,
  });

  const dashboardSummaryQuery = useQuery({
    queryKey: ['dashboard-summary'],
    queryFn: getDashboardSummary,
    staleTime: 5000,
    refetchInterval: 5000,
  });

  if (error || (isAuthInitialized && currentUser && dashboardSummaryQuery.error)) {
    return <ErrorBoundary error={(error || dashboardSummaryQuery.error) as Error} />;
  }

  const markets = data?.markets || [];
  const dashboardSummary = dashboardSummaryQuery.data;
  const totalMarkets = data?.total || 0;
  const openMarkets = markets.filter((market) => market.state === 'active').length;
  const averagePrice = markets.length > 0
    ? markets.reduce((sum, market) => sum + market.yesPrice, 0) / markets.length
    : 0;
  const displayedAveragePrice = dashboardSummary?.averagePrice !== undefined
    ? toNumber(dashboardSummary.averagePrice)
    : averagePrice;
  const topGainers = [...markets]
    .sort((a, b) => b.yesPrice - a.yesPrice)
    .slice(0, 3);

  const stats = [
    {
      label: 'Total Markets',
      value: dashboardSummary?.totalMarkets ?? totalMarkets,
      icon: <Target className="w-6 h-6" />,
    },
    {
      label: 'Open Markets',
      value: dashboardSummary?.openMarkets ?? openMarkets,
      icon: <Activity className="w-6 h-6" />,
    },
    ...(markets.length > 0
      ? [{
          label: 'Avg. Price',
          value: `${displayedAveragePrice.toFixed(2)} pts`,
          icon: <Zap className="w-6 h-6" />,
        }]
      : []),
    ...(isAuthInitialized && currentUser && dashboardSummary
      ? [
          {
            label: 'Wallet Balance',
            value: formatMoney(dashboardSummary.walletBalance),
            icon: <Wallet className="w-6 h-6" />,
          },
          {
            label: 'Portfolio Value',
            value: formatMoney(dashboardSummary.portfolioValue),
            icon: <Briefcase className="w-6 h-6" />,
          },
          {
            label: 'Total Trades',
            value: dashboardSummary.totalTrades,
            icon: <TrendingUp className="w-6 h-6" />,
          },
          {
            label: 'Open Positions',
            value: dashboardSummary.openPositions,
            icon: <BarChart3 className="w-6 h-6" />,
          },
        ]
      : []),
  ];

  return (
    <div className="space-y-8">
      {/* Header */}
      <div>
        <h1 className="text-4xl font-bold text-slate-900 dark:text-white">
          Polymarket Dashboard
        </h1>
        <p className="mt-2 text-slate-600 dark:text-slate-400">
          Real-time visualization of prediction markets
        </p>
      </div>

      {/* Stats Cards */}
      {isAuthInitialized && currentUser && dashboardSummaryQuery.isLoading ? (
        <LoadingSpinner />
      ) : (
        <StatsCards stats={stats} />
      )}

      {/* Top Gainers */}
      {topGainers.length > 0 && (
        <div>
          <h2 className="text-2xl font-bold text-slate-900 dark:text-white mb-4">
            Top Gainers (Highest YES Price)
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {topGainers.map((market) => (
              <div
                key={market.id}
                className="p-4 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 hover:shadow-md dark:hover:shadow-slate-900 transition-shadow"
              >
                <h3 className="font-semibold text-slate-900 dark:text-white truncate">
                  {market.title}
                </h3>
                <div className="mt-2 flex items-baseline gap-2">
                  <span className="text-2xl font-bold text-green-600">
                    {market.yesPrice.toFixed(4)} pts
                  </span>
                  <span className="text-xs text-slate-600 dark:text-slate-400">
                    YES
                  </span>
                </div>
                <div className="mt-1 flex items-baseline gap-2">
                  <span className="text-sm font-medium text-red-600">
                    {market.noPrice.toFixed(4)} pts
                  </span>
                  <span className="text-xs text-slate-600 dark:text-slate-400">
                    NO
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Markets List */}
      <div>
        <h2 className="text-2xl font-bold text-slate-900 dark:text-white mb-4">
          All Markets
        </h2>
        {isLoading ? (
          <LoadingSpinner />
        ) : (
          <MarketsList markets={markets} isLoading={isLoading} />
        )}
      </div>
    </div>
  );
}
