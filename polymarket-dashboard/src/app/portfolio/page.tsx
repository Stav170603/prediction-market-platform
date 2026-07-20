'use client';

import Link from 'next/link';
import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import { Briefcase } from 'lucide-react';
import { getMyPositions, positionQueryKey } from '@/services/positionService';
import { useAuth } from '@/contexts/AuthContext';
import { LoadingSpinner, ErrorBoundary } from '@/components/ui/Loading';

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

export default function PortfolioPage() {
  const router = useRouter();
  const { currentUser, isAuthInitialized } = useAuth();

  useEffect(() => {
    if (isAuthInitialized && !currentUser) {
      router.replace('/login');
    }
  }, [currentUser, isAuthInitialized, router]);

  const positionsQuery = useQuery({
    queryKey: positionQueryKey(currentUser?.userId),
    queryFn: getMyPositions,
    enabled: isAuthInitialized && Boolean(currentUser?.userId),
    retry: false,
  });

  if (!isAuthInitialized || !currentUser) return <LoadingSpinner />;

  if (positionsQuery.error) {
    return <ErrorBoundary error={positionsQuery.error as Error} />;
  }

  const positions = (positionsQuery.data ?? []).filter(
    (position) => position.marketStatus === 'OPEN' && toNumber(position.quantity) > 0
  );
  const totalValue = positions.reduce((sum, position) => sum + toNumber(position.currentValue), 0);
  const totalPnL = positions.reduce((sum, position) => sum + toNumber(position.unrealizedPnL), 0);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-4xl font-bold text-slate-900 dark:text-white">Portfolio</h1>
        <p className="mt-2 text-slate-600 dark:text-slate-400">
          Positions held by {currentUser.username}
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 p-6">
          <p className="text-sm text-slate-600 dark:text-slate-400">Current value</p>
          <p className="text-3xl font-bold text-slate-900 dark:text-white">{formatMoney(totalValue)}</p>
        </div>
        <div className="rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 p-6">
          <p className="text-sm text-slate-600 dark:text-slate-400">Unrealized PnL</p>
          <p className={`text-3xl font-bold ${totalPnL >= 0 ? 'text-green-600' : 'text-red-600'}`}>
            {formatMoney(totalPnL)}
          </p>
        </div>
      </div>

      {positionsQuery.isLoading ? (
        <LoadingSpinner />
      ) : positions.length === 0 ? (
        <div className="rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 p-6 text-center">
          <Briefcase className="w-10 h-10 text-slate-400 mx-auto mb-2" />
          <p className="text-slate-600 dark:text-slate-400">No positions yet.</p>
        </div>
      ) : (
        <div className="overflow-x-auto rounded-lg border border-slate-200 dark:border-slate-700">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-900">
                <th className="px-4 py-3 text-left font-semibold text-slate-900 dark:text-white">Market</th>
                <th className="px-4 py-3 text-left font-semibold text-slate-900 dark:text-white">Outcome</th>
                <th className="px-4 py-3 text-right font-semibold text-slate-900 dark:text-white">Quantity</th>
                <th className="px-4 py-3 text-right font-semibold text-slate-900 dark:text-white">Price</th>
                <th className="px-4 py-3 text-right font-semibold text-slate-900 dark:text-white">Value</th>
                <th className="px-4 py-3 text-right font-semibold text-slate-900 dark:text-white">PnL</th>
              </tr>
            </thead>
            <tbody>
              {positions.map((position) => {
                const pnl = toNumber(position.unrealizedPnL);

                return (
                  <tr key={position.positionId} className="border-b border-slate-200 dark:border-slate-700">
                    <td className="px-4 py-3">
                      <Link href={`/market/${position.marketId}`} className="font-medium text-slate-900 dark:text-white hover:text-blue-600">
                        {position.marketTitle}
                      </Link>
                    </td>
                    <td className="px-4 py-3">
                      <span className={`inline-block px-3 py-1 rounded-full text-xs font-medium ${
                        position.outcomeName === 'YES'
                          ? 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200'
                          : 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200'
                      }`}>
                        {position.outcomeName}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right">{toNumber(position.quantity).toFixed(4)}</td>
                    <td className="px-4 py-3 text-right">{formatMoney(position.currentPrice)}</td>
                    <td className="px-4 py-3 text-right">{formatMoney(position.currentValue)}</td>
                    <td className={`px-4 py-3 text-right font-medium ${pnl >= 0 ? 'text-green-600' : 'text-red-600'}`}>
                      {formatMoney(position.unrealizedPnL)}
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
