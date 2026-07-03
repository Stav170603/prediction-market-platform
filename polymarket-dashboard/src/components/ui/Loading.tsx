'use client';

import { Loader } from 'lucide-react';

interface SkeletonLoaderProps {
  count?: number;
  className?: string;
}

export function SkeletonLoader({ count = 3, className = '' }: SkeletonLoaderProps) {
  return (
    <div className={`space-y-4 ${className}`}>
      {Array.from({ length: count }).map((_, i) => (
        <div
          key={i}
          className="skeleton h-12 rounded-lg"
        />
      ))}
    </div>
  );
}

export function LoadingSpinner() {
  return (
    <div className="flex items-center justify-center py-12">
      <Loader className="w-8 h-8 animate-spin text-blue-600" />
    </div>
  );
}

export function ErrorBoundary({
  error,
  message = 'Something went wrong',
}: {
  error?: Error;
  message?: string;
}) {
  return (
    <div className="rounded-lg border border-red-200 dark:border-red-900 bg-red-50 dark:bg-red-950 p-4">
      <h3 className="text-sm font-medium text-red-800 dark:text-red-200">
        Error
      </h3>
      <p className="mt-1 text-sm text-red-700 dark:text-red-300">
        {error?.message || message}
      </p>
    </div>
  );
}

export function EmptyState({
  title = 'No data',
  description = 'No data available',
}: {
  title?: string;
  description?: string;
}) {
  return (
    <div className="flex flex-col items-center justify-center py-12">
      <p className="text-lg font-medium text-slate-900 dark:text-white">
        {title}
      </p>
      <p className="mt-1 text-sm text-slate-600 dark:text-slate-400">
        {description}
      </p>
    </div>
  );
}
