'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import { BarChart3, Briefcase, Mail, Shield, Target, User, Wallet } from 'lucide-react';
import { getMyPositions } from '@/services/positionService';
import { getMyTrades } from '@/services/tradeService';
import { getMyWallet } from '@/services/walletService';
import { useAuth } from '@/contexts/AuthContext';
import { ErrorBoundary, LoadingSpinner } from '@/components/Loading';

function toNumber(value: number | string | null | undefined): number {
  if (value === null || value === undefined) return 0;
  const parsed = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

function formatMoney(value: number | string | null | undefined): string {
  return `${toNumber(value).toLocaleString(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 4,
  })} pts`;
}

export default function ProfilePage() {
  const router = useRouter();
  const { currentUser, isAuthInitialized } = useAuth();

  useEffect(() => {
    if (isAuthInitialized && !currentUser) {
      router.replace('/login');
    }
  }, [currentUser, isAuthInitialized, router]);

  const walletQuery = useQuery({
    queryKey: ['wallet', currentUser?.userId],
    queryFn: getMyWallet,
    enabled: isAuthInitialized && Boolean(currentUser?.userId),
    retry: false,
  });

  const tradesQuery = useQuery({
    queryKey: ['trade-history', currentUser?.userId],
    queryFn: getMyTrades,
    enabled: isAuthInitialized && Boolean(currentUser?.userId),
    retry: false,
  });

  const positionsQuery = useQuery({
    queryKey: ['positions', currentUser?.userId],
    queryFn: getMyPositions,
    enabled: isAuthInitialized && Boolean(currentUser?.userId),
    retry: false,
  });

  if (!isAuthInitialized || !currentUser) return <LoadingSpinner />;

  if (walletQuery.error || tradesQuery.error || positionsQuery.error) {
    return <ErrorBoundary error={(walletQuery.error || tradesQuery.error || positionsQuery.error) as Error} />;
  }

  const trades = tradesQuery.data ?? [];
  const positions = positionsQuery.data ?? [];
  const openPositions = positions.filter((position) => toNumber(position.quantity) > 0);
  const walletBalance = walletQuery.data?.balance ?? currentUser.walletBalance;
  const isLoading = walletQuery.isLoading || tradesQuery.isLoading || positionsQuery.isLoading;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-4xl font-bold text-slate-900 dark:text-white">Profile</h1>
        <p className="mt-2 text-slate-600 dark:text-slate-400">
          Account details and activity summary
        </p>
      </div>

      <div className="rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 p-6">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-4">
            <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-blue-100 text-blue-700 dark:bg-blue-900 dark:text-blue-200">
              <User className="h-6 w-6" />
            </div>
            <div>
              <h2 className="text-2xl font-bold text-slate-900 dark:text-white">
                {currentUser.username}
              </h2>
              <div className="mt-1 flex items-center gap-2 text-sm text-slate-600 dark:text-slate-400">
                <Mail className="h-4 w-4" />
                {currentUser.email}
              </div>
            </div>
          </div>
          <div className="inline-flex items-center gap-2 self-start rounded-full bg-slate-100 px-4 py-2 text-sm font-medium text-slate-700 dark:bg-slate-800 dark:text-slate-300 sm:self-center">
            <Shield className="h-4 w-4" />
            {currentUser.role}
          </div>
        </div>
      </div>

      {isLoading ? (
        <LoadingSpinner />
      ) : (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
          <div className="rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 p-6">
            <div className="flex items-center gap-3">
              <div className="rounded-lg bg-green-100 p-2 text-green-700 dark:bg-green-900 dark:text-green-200">
                <Wallet className="h-5 w-5" />
              </div>
              <div>
                <p className="text-sm text-slate-600 dark:text-slate-400">Wallet balance</p>
                <p className="text-3xl font-bold text-slate-900 dark:text-white">
                  {formatMoney(walletBalance)}
                </p>
              </div>
            </div>
          </div>

          <div className="rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 p-6">
            <div className="flex items-center gap-3">
              <div className="rounded-lg bg-purple-100 p-2 text-purple-700 dark:bg-purple-900 dark:text-purple-200">
                <Target className="h-5 w-5" />
              </div>
              <div>
                <p className="text-sm text-slate-600 dark:text-slate-400">Reliability</p>
                <p className="text-3xl font-bold text-slate-900 dark:text-white">
                  {toNumber(walletQuery.data?.reliabilityScore).toFixed(2)}%
                </p>
              </div>
            </div>
          </div>

          <div className="rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 p-6">
            <div className="flex items-center gap-3">
              <div className="rounded-lg bg-blue-100 p-2 text-blue-700 dark:bg-blue-900 dark:text-blue-200">
                <BarChart3 className="h-5 w-5" />
              </div>
              <div>
                <p className="text-sm text-slate-600 dark:text-slate-400">Number of trades</p>
                <p className="text-3xl font-bold text-slate-900 dark:text-white">{trades.length}</p>
              </div>
            </div>
          </div>

          <div className="rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 p-6">
            <div className="flex items-center gap-3">
              <div className="rounded-lg bg-amber-100 p-2 text-amber-700 dark:bg-amber-900 dark:text-amber-200">
                <Briefcase className="h-5 w-5" />
              </div>
              <div>
                <p className="text-sm text-slate-600 dark:text-slate-400">Open positions</p>
                <p className="text-3xl font-bold text-slate-900 dark:text-white">{openPositions.length}</p>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
