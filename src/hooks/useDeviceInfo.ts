import { useEffect, useState } from 'react';
import { getDeviceInfo } from '../services/bridge';
import type { DeviceInfo } from '../services/types';

export interface UseDeviceInfoResult {
  deviceInfo: DeviceInfo | null;
  loading: boolean;
  error: string | null;
  reload: () => void;
}

export function useDeviceInfo(): UseDeviceInfoResult {
  const [deviceInfo, setDeviceInfo] = useState<DeviceInfo | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadToken, setReloadToken] = useState<number>(0);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    getDeviceInfo()
      .then((info) => {
        if (!cancelled) {
          setDeviceInfo(info);
          setError(null);
        }
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Không đọc được thông tin thiết bị.');
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [reloadToken]);

  return {
    deviceInfo,
    loading,
    error,
    reload: () => setReloadToken((token) => token + 1),
  };
}
