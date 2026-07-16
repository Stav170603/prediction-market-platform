'use client';

import { useMemo } from 'react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from 'recharts';
import { PriceHistoryDto } from '@/types/api';

interface PriceChartProps {
  data: PriceHistoryDto[];
}

export function PriceChart({ data }: PriceChartProps) {
  const chartData = useMemo(
    () => data
      .map((item, originalIndex) => ({
        timestamp: new Date(item.timestamp).getTime(),
        yesPrice: Number(item.yesPrice),
        noPrice: Number(item.noPrice),
        originalIndex,
      }))
      .sort((left, right) => (
        left.timestamp - right.timestamp || left.originalIndex - right.originalIndex
      )),
    [data]
  );

  const formatTimestamp = (timestamp: number) => new Date(timestamp).toLocaleString();

  if (chartData.length === 0) {
    return (
      <div className="flex h-96 w-full items-center justify-center rounded-lg border border-slate-200 bg-white p-4 text-slate-600 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-400">
        No price history available yet.
      </div>
    );
  }

  return (
    <div className="w-full h-96 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 p-4">
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={chartData}>
          <CartesianGrid
            strokeDasharray="3 3"
            stroke="rgba(100, 116, 139, 0.2)"
          />
          <XAxis
            dataKey="timestamp"
            type="number"
            scale="time"
            domain={['dataMin', 'dataMax']}
            tickFormatter={(timestamp) => new Date(timestamp).toLocaleTimeString()}
            stroke="rgb(100, 116, 139)"
            tick={{ fontSize: 12 }}
          />
          <YAxis
            stroke="rgb(100, 116, 139)"
            tick={{ fontSize: 12 }}
            domain={[0, 1]}
          />
          <Tooltip
            contentStyle={{
              backgroundColor: 'rgb(15, 23, 42)',
              border: '1px solid rgb(51, 65, 85)',
              borderRadius: '8px',
            }}
            labelStyle={{ color: '#fff' }}
            labelFormatter={(timestamp) => `Time: ${formatTimestamp(Number(timestamp))}`}
            formatter={(value) => Number(value).toFixed(4)}
          />
          <Legend />
          <Line
            type="monotone"
            dataKey="yesPrice"
            stroke="#10b981"
            name="YES price"
            isAnimationActive={false}
            dot={false}
          />
          <Line
            type="monotone"
            dataKey="noPrice"
            stroke="#ef4444"
            name="NO price"
            isAnimationActive={false}
            dot={false}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
