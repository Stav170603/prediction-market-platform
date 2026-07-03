'use client';

import { formatVolume, formatLiquidity } from '@/lib/utils';

interface StatsCard {
  label: string;
  value: string | number;
  change?: number;
  icon?: React.ReactNode;
}

interface StatsCardsProps {
  stats: StatsCard[];
}

export function StatsCards({ stats }: StatsCardsProps) {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
      {stats.map((stat, idx) => (
        <div
          key={idx}
          className="p-6 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 hover:shadow-md dark:hover:shadow-slate-900 transition-shadow"
        >
          <div className="flex items-start justify-between">
            <div>
              <p className="text-sm font-medium text-slate-600 dark:text-slate-400">
                {stat.label}
              </p>
              <p className="mt-2 text-2xl font-bold text-slate-900 dark:text-white">
                {stat.value}
              </p>
              {stat.change !== undefined && (
                <p
                  className={`mt-1 text-sm font-medium ${
                    stat.change >= 0 ? 'text-green-600' : 'text-red-600'
                  }`}
                >
                  {stat.change >= 0 ? '+' : ''}{stat.change.toFixed(1)}%
                </p>
              )}
            </div>
            {stat.icon && (
              <div className="text-slate-400 dark:text-slate-600">
                {stat.icon}
              </div>
            )}
          </div>
        </div>
      ))}
    </div>
  );
}
