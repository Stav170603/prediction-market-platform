import axios, { AxiosError, AxiosRequestConfig } from 'axios';
import { toast } from 'sonner';
import { getAuthToken } from './authStorage';

export const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE || 'http://localhost:8080';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

export function authRequestConfig(): AxiosRequestConfig {
  const token = getAuthToken();

  return {
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  };
}

type ApiErrorBody = {
  message?: string;
  detail?: string;
  error?: string;
  errors?: Array<{ defaultMessage?: string; message?: string }>;
};

export function getApiErrorMessage(error: unknown): string {
  if (!axios.isAxiosError(error)) {
    return error instanceof Error ? error.message : 'The request could not be completed.';
  }

  const axiosError = error as AxiosError<ApiErrorBody>;
  const data = axiosError.response?.data;
  const validationMessage = data?.errors?.find(
    (item) => item.defaultMessage || item.message
  );

  if (data?.message) return data.message;
  if (validationMessage?.defaultMessage) return validationMessage.defaultMessage;
  if (validationMessage?.message) return validationMessage.message;
  if (data?.detail) return data.detail;
  if (data?.error) return data.error;

  if (!axiosError.response) {
    return `Cannot connect to the server at ${API_BASE_URL}. Please make sure the backend is running.`;
  }

  return `Request failed with status ${axiosError.response.status}.`;
}

apiClient.interceptors.request.use((config) => {
  const token = getAuthToken();

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const message = getApiErrorMessage(error);
    const method = error.config?.method?.toUpperCase() || 'REQUEST';
    const url = error.config?.url || 'unknown';
    const status = error.response?.status || 'network';

    toast.error(message, {
      id: `api-error:${method}:${url}:${status}`,
      duration: 4500,
    });

    return Promise.reject(error);
  }
);
