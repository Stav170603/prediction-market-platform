import { apiClient } from './http';
import { AdminDashboardDto, DashboardSummaryDto } from '@/types/api';

export async function getDashboardSummary(): Promise<DashboardSummaryDto> {
  const { data } = await apiClient.get<DashboardSummaryDto>('/api/dashboard/summary');
  return data;
}

export async function getAdminDashboard(): Promise<AdminDashboardDto> {
  const { data } = await apiClient.get<AdminDashboardDto>('/api/dashboard/admin');
  return data;
}
