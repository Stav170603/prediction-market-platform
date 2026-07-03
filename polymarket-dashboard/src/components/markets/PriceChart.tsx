'use client';

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
  const chartData = data.map((item) => ({
    timestamp: item.timestamp,
    yesPrice: Number(item.yesPrice),
    noPrice: Number(item.noPrice),
    time: new Date(item.timestamp).toLocaleTimeString(),
  }));

  return (
    <div className="w-full h-96 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 p-4">
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={chartData}>
          <CartesianGrid
            strokeDasharray="3 3"
            stroke="rgba(100, 116, 139, 0.2)"
          />
          <XAxis
            dataKey="time"
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
          />
          <Legend />
          <Line
            type="monotone"
            dataKey="yesPrice"
            stroke="#10b981"
            name="Yes Price"
            isAnimationActive={false}
            dot={false}
          />
          <Line
            type="monotone"
            dataKey="noPrice"
            stroke="#ef4444"
            name="No Price"
            isAnimationActive={false}
            dot={false}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
