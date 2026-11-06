import { en } from './en';
import { vi } from './vi';

export type Language = 'vi' | 'en';
export type Strings = typeof vi;

const bundles: Record<Language, Strings> = {
  vi,
  en: en as unknown as Strings,
};

let currentLanguage: Language = 'vi';

export function setLanguage(language: Language): void {
  currentLanguage = language;
}

export function getLanguage(): Language {
  return currentLanguage;
}

type NestedValue = string | Record<string, string | Record<string, string>>;

function lookup(obj: NestedValue, parts: string[]): string | null {
  let current: NestedValue = obj;
  for (const part of parts) {
    if (typeof current !== 'object' || current === null || !(part in current)) {
      return null;
    }
    current = current[part] as NestedValue;
  }
  return typeof current === 'string' ? current : null;
}

export function t(key: string, params?: Record<string, string | number>): string {
  const parts = key.split('.');
  const raw = lookup(bundles[currentLanguage] as unknown as NestedValue, parts)
    ?? lookup(vi as unknown as NestedValue, parts)
    ?? key;
  if (!params) {
    return raw;
  }
  let result = raw;
  for (const [name, value] of Object.entries(params)) {
    result = result.replace(`{${name}}`, String(value));
  }
  return result;
}

export function useStrings(): Strings {
  return bundles[currentLanguage];
}
