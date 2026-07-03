'use client';

import { useQuery } from '@tanstack/react-query';
import { fetchMarketDetail } from '@/lib/api';
import { MarketDetailComponent } from '@/components/markets/MarketDetail';
import { LoadingSpinner, ErrorBoundary } from '@/components/ui/Loading';

interface MarketPageProps {
  params: {
    id: string;
  };
}

export default function MarketPage({ params }: MarketPageProps) {
  const { data, isLoading, error } = useQuery({
    queryKey: ['market', params.id],
    queryFn: () => fetchMarketDetail(params.id),
    staleTime: 5000,
    refetchInterval: 5000,
  });

  if (error) {
    return <ErrorBoundary error={error as Error} />;
  }

  if (isLoading) {
    return <LoadingSpinner />;
  }

  if (!data) {
    return <ErrorBoundary message="Market not found" />;
  }

  return <MarketDetailComponent market={data} isLoading={isLoading} />;
}
