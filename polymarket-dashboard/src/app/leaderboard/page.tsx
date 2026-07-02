'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import { BarChart3, Crown, Medal, Trophy, User } from 'lucide-react';
import { getLeaderboard } from '@/services/leaderboardService';
import { LeaderboardResponseDto } from '@/types/api';
import { EmptyState, ErrorBoundary, LoadingSpinner } from '@/components/Loading';
import { useAuth } from '@/contexts/AuthContext';

function toNumber(value: number | string | null | undefined): number {
  if (value === null || value === undefined) return 0;
  const parsed = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

function formatMoney(value: number | string): string {
  return `$${toNumber(value).toLocaleString(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 4,
  })}`;
}

function formatCount(value: number): string {
  return value.toLocaleString();
}

function getRankClasses(rank: number): string {
  if (rank === 1) return 'bg-yellow-100 text-yellow-800 ring-yellow-300 dark:bg-yellow-900 dark:text-yellow-100 dark:ring-yellow-700';
  if (rank === 2) return 'bg-slate-200 text-slate-800 ring-slate-300 dark:bg-slate-700 dark:text-slate-100 dark:ring-slate-500';
  if (rank === 3) return 'bg-orange-100 text-orange-800 ring-orange-300 dark:bg-orange-900 dark:text-orange-100 dark:ring-orange-700';
  return 'bg-slate-100 text-slate-700 ring-slate-200 dark:bg-slate-800 dark:text-slate-300 dark:ring-slate-700';
}

function RankBadge({ rank }: { rank: number }) {
  const icon = rank <= 3 ? <Medal className="h-4 w-4" /> : <Trophy className="h-4 w-4" />;

  return (
    <span className={`inline-flex min-w-[4rem] items-center justify-center gap-1 rounded-full px-3 py-1 text-sm font-semibold ring-1 ${getRankClasses(rank)}`}>
      {icon}
      #{rank}
    </span>
  );
}

function LeaderboardRow({ trader }: { trader: LeaderboardResponseDto }) {
  return (
    <tr className="border-b border-slate-200 hover:bg-slate-50 dark:border-slate-700 dark:hover:bg-slate-900">
      <td className="px-4 py-4">
        <RankBadge rank={trader.rank} />
      </td>
      <td className="px-4 py-4">
        <div className="flex items-center gap-3">
          <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-blue-100 text-blue-700 dark:bg-blue-900 dark:text-blue-200">
            <User className="h-4 w-4" />
          </div>
          <div>
            <p className="font-semibold text-slate-900 dark:text-white">{trader.username}</p>
            <p className="text-xs text-slate-500 dark:text-slate-400">{trader.email}</p>
          </div>
        </div>
      </td>
      <td className="px-4 py-4 text-right font-medium text-slate-900 dark:text-white">
        {formatMoney(trader.walletBalance)}
      </td>
      <td className="px-4 py-4 text-right font-semibold text-green-700 dark:text-green-300">
        {formatMoney(trader.portfolioValue)}
      </td>
      <td className="px-4 py-4 text-right font-semibold text-blue-700 dark:text-blue-300">
        {toNumber(trader.reliabilityScore).toFixed(2)}%
      </td>
      <td className="px-4 py-4 text-right text-slate-700 dark:text-slate-300">
        {formatCount(trader.openPositions)}
      </td>
      <td className="px-4 py-4 text-right text-slate-700 dark:text-slate-300">
        {formatCount(trader.totalTrades)}
      </td>
    </tr>
  );
}

export default function LeaderboardPage() {
  const router = useRouter();
  const { currentUser, isAuthInitialized } = useAuth();

  useEffect(() => {
    if (isAuthInitialized && !currentUser) {
      router.replace('/login');
    }
  }, [currentUser, isAuthInitialized, router]);

  const leaderboardQuery = useQuery({
    queryKey: ['leaderboard'],
    queryFn: getLeaderboard,
    enabled: isAuthInitialized && Boolean(currentUser),
    retry: false,
  });

  if (!isAuthInitialized || !currentUser) return <LoadingSpinner />;

  if (leaderboardQuery.error) {
    return <ErrorBoundary error={leaderboardQuery.error as Error} />;
  }

  const traders = leaderboardQuery.data ?? [];

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-4xl font-bold text-slate-900 dark:text-white">Top Traders</h1>
          <p className="mt-2 text-slate-600 dark:text-slate-400">
            Ranked by historical prediction accuracy, then portfolio value
          </p>
        </div>
        <div className="inline-flex items-center gap-2 self-start rounded-lg border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300 sm:self-auto">
          <Crown className="h-4 w-4 text-yellow-600" />
          {formatCount(traders.length)} traders
        </div>
      </div>

      {leaderboardQuery.isLoading ? (
        <LoadingSpinner />
      ) : traders.length === 0 ? (
        <div className="rounded-lg border border-slate-200 bg-white dark:border-slate-700 dark:bg-slate-900">
          <EmptyState
            title="No traders yet"
            description="The leaderboard will appear after users are created."
          />
        </div>
      ) : (
        <div className="overflow-hidden rounded-lg border border-slate-200 bg-white dark:border-slate-700 dark:bg-slate-900">
          <div className="flex items-center gap-2 border-b border-slate-200 px-4 py-3 dark:border-slate-700">
            <BarChart3 className="h-5 w-5 text-slate-500" />
            <h2 className="text-lg font-semibold text-slate-900 dark:text-white">Leaderboard</h2>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full min-w-[760px] text-sm">
              <thead>
                <tr className="border-b border-slate-200 bg-slate-50 dark:border-slate-700 dark:bg-slate-950">
                  <th className="px-4 py-3 text-left font-semibold text-slate-900 dark:text-white">Rank</th>
                  <th className="px-4 py-3 text-left font-semibold text-slate-900 dark:text-white">Username</th>
                  <th className="px-4 py-3 text-right font-semibold text-slate-900 dark:text-white">Wallet Balance</th>
                  <th className="px-4 py-3 text-right font-semibold text-slate-900 dark:text-white">Portfolio Value</th>
                  <th className="px-4 py-3 text-right font-semibold text-slate-900 dark:text-white">Reliability</th>
                  <th className="px-4 py-3 text-right font-semibold text-slate-900 dark:text-white">Open Positions</th>
                  <th className="px-4 py-3 text-right font-semibold text-slate-900 dark:text-white">Total Trades</th>
                </tr>
              </thead>
              <tbody>
                {traders.map((trader) => (
                  <LeaderboardRow key={trader.userId} trader={trader} />
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
