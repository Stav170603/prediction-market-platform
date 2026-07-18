'use client';

import { Activity, AlertTriangle, ArrowDownRight, ArrowUpRight } from 'lucide-react';
import { AdminDashboardDto, Market } from '@/types/api';

type Props = { data: AdminDashboardDto; markets: Market[]; resolving: boolean; onResolve: (market: Market) => void };
const formatNumber = new Intl.NumberFormat();
const money = (value: number | string) => `${Number(value).toFixed(4)} pts`;
const dateTime = (value: string) => new Date(value).toLocaleString();
const Empty = ({ children }: { children: string }) => <div className="px-5 py-10 text-center text-sm text-slate-500 dark:text-slate-400">{children}</div>;

export function AdminOverview({ data, markets, resolving, onResolve }: Props) {
  const cards: Array<[string, number | string]> = [
    ['Total Markets', data.summary.totalMarkets], ['Open Markets', data.summary.openMarkets],
    ['Closed Markets', data.summary.closedMarkets], ['Waiting for Resolution', data.summary.waitingForResolution],
    ['Resolved Markets', data.summary.resolvedMarkets], ['Cancelled Markets', data.summary.cancelledMarkets],
    ['Total Users', data.summary.totalUsers], ['Active Users (24h)', data.summary.activeUsersLast24Hours],
    ['Total Trades', data.summary.totalTrades],
    ['Total Trading Volume', money(data.summary.totalTradingVolume)],
  ];
  const headings = (labels: string[]) => labels.map((label) =>
    <th key={label} className="px-4 py-3 text-left font-semibold text-slate-900 dark:text-white">{label}</th>);

  return <div className="space-y-6">
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
      {cards.map(([label, value]) => <div key={label} className="rounded-lg border border-slate-200 bg-white p-5 dark:border-slate-700 dark:bg-slate-900">
        <p className="text-sm font-medium text-slate-500 dark:text-slate-400">{label}</p>
        <p className="mt-2 text-2xl font-bold text-slate-900 dark:text-white">{typeof value === 'number' ? formatNumber.format(value) : value}</p>
      </div>)}
    </div>

    <section className="overflow-hidden rounded-lg border border-amber-200 bg-white dark:border-amber-900 dark:bg-slate-900">
      <div className="flex items-center gap-2 border-b border-amber-200 bg-amber-50 px-5 py-4 dark:border-amber-900 dark:bg-amber-950/40">
        <AlertTriangle className="h-5 w-5 text-amber-600" /><h2 className="text-xl font-bold text-slate-900 dark:text-white">Markets Requiring Attention</h2>
      </div>
      {!data.marketsRequiringAttention.length ? <Empty>No markets currently require resolution.</Empty> : <div className="overflow-x-auto">
        <table className="w-full min-w-[760px] text-sm"><thead className="bg-slate-50 dark:bg-slate-950"><tr>
          {headings(['Title', 'Category', 'Trading Close', 'Resolution Time', 'Action'])}
        </tr></thead><tbody>{data.marketsRequiringAttention.map((item) => {
          const market = markets.find((candidate) => candidate.marketId === item.marketId);
          return <tr key={item.marketId} className="border-t border-slate-200 dark:border-slate-700">
            <td className="px-4 py-3 font-medium text-slate-900 dark:text-white">{item.title}</td>
            <td className="px-4 py-3 text-slate-600 dark:text-slate-400">{item.category}</td>
            <td className="px-4 py-3 text-slate-600 dark:text-slate-400">{dateTime(item.tradingCloseDate)}</td>
            <td className="px-4 py-3 text-slate-600 dark:text-slate-400">{dateTime(item.resolutionDate)}</td>
            <td className="px-4 py-3"><button disabled={!market || resolving} onClick={() => market && onResolve(market)}
              className="rounded-lg bg-green-600 px-3 py-1.5 font-medium text-white hover:bg-green-700 disabled:opacity-50">Resolve</button></td>
          </tr>;
        })}</tbody></table>
      </div>}
    </section>

    <div className="grid grid-cols-1 gap-6 xl:grid-cols-2">
      <section className="overflow-hidden rounded-lg border border-slate-200 bg-white dark:border-slate-700 dark:bg-slate-900">
        <div className="flex items-center gap-2 border-b border-slate-200 px-5 py-4 dark:border-slate-700">
          <Activity className="h-5 w-5 text-blue-600" /><h2 className="text-xl font-bold text-slate-900 dark:text-white">Recent Activity</h2>
        </div>
        {!data.recentActivity.length ? <Empty>No trades have been placed yet.</Empty> : <div className="overflow-x-auto">
          <table className="w-full min-w-[800px] text-sm"><thead className="bg-slate-50 dark:bg-slate-950"><tr>
            {headings(['Username', 'Market', 'Side', 'Outcome', 'Quantity', 'Total Value', 'Time'])}
          </tr></thead><tbody>{data.recentActivity.map((trade) => <tr key={trade.tradeId} className="border-t border-slate-200 dark:border-slate-700">
            <td className="px-4 py-3 font-medium text-slate-900 dark:text-white">{trade.username}</td>
            <td className="max-w-56 truncate px-4 py-3 text-slate-600 dark:text-slate-400">{trade.marketTitle}</td>
            <td className={`px-4 py-3 font-semibold ${trade.type === 'BUY' ? 'text-green-600' : 'text-red-600'}`}>{trade.type}</td>
            <td className="px-4 py-3">{trade.outcome}</td><td className="px-4 py-3">{Number(trade.quantity).toFixed(4)}</td>
            <td className="px-4 py-3">{money(trade.totalValue)}</td><td className="whitespace-nowrap px-4 py-3 text-xs text-slate-500">{dateTime(trade.createdAt)}</td>
          </tr>)}</tbody></table>
        </div>}
      </section>

      <section className="overflow-hidden rounded-lg border border-slate-200 bg-white dark:border-slate-700 dark:bg-slate-900">
        <div className="border-b border-slate-200 px-5 py-4 dark:border-slate-700"><h2 className="text-xl font-bold text-slate-900 dark:text-white">Sharp Price Movements</h2>
          <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">YES price changes of at least {(Number(data.sharpMovementThreshold) * 100).toFixed(0)} percentage points in the last {data.sharpMovementWindowHours} hours.</p>
        </div>
        {!data.sharpPriceMovements.length ? <Empty>No sharp price movements in the current window.</Empty> : <div className="divide-y divide-slate-200 dark:divide-slate-700">
          {data.sharpPriceMovements.map((movement) => {
            const rising = Number(movement.currentYesPrice) >= Number(movement.previousYesPrice);
            const Icon = rising ? ArrowUpRight : ArrowDownRight;
            return <div key={movement.marketId} className="flex items-center justify-between gap-4 px-5 py-4"><div className="min-w-0">
              <p className="truncate font-medium text-slate-900 dark:text-white">{movement.marketTitle}</p>
              <p className="text-sm text-slate-500">{(Number(movement.previousYesPrice) * 100).toFixed(1)}% → {(Number(movement.currentYesPrice) * 100).toFixed(1)}%</p>
            </div><div className={`flex items-center gap-1 font-bold ${rising ? 'text-green-600' : 'text-red-600'}`}>
              <Icon className="h-5 w-5" />{(Number(movement.absoluteChange) * 100).toFixed(1)} pp
            </div></div>;
          })}
        </div>}
      </section>
    </div>
  </div>;
}
