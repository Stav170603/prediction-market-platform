'use client';

import { useQuery } from '@tanstack/react-query';
import { fetchMarkets } from '@/lib/api';
import { MarketsList } from '@/components/markets/MarketsList';
import { LoadingSpinner, ErrorBoundary } from '@/components/ui/Loading';

export default function MarketsPage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['markets-full'],
    queryFn: () => fetchMarkets(1, 100),
    staleTime: 5000,
    refetchInterval: 5000,
  });

  if (error) {
    return <ErrorBoundary error={error as Error} />;
  }

  const markets = data?.markets || [];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-4xl font-bold text-slate-900 dark:text-white">
          Markets
        </h1>
        <p className="mt-2 text-slate-600 dark:text-slate-400">
          Browse and explore all Polymarket prediction markets
        </p>
      </div>

      {isLoading ? (
        <LoadingSpinner />
      ) : (
        <MarketsList markets={markets} isLoading={isLoading} />
      )}
    </div>
  );
}
