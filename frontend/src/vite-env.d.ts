/// <reference types="vite/client" />

interface FileResult {
  path: string;
  name: string;
  content: string;
}

interface SaveResult {
  success: boolean;
  error?: string;
  path?: string;
}

interface ElectronAPI {
  openFile: () => Promise<FileResult | null>;
  saveFile: (filePath: string, content: string) => Promise<SaveResult>;
  saveAs: (content: string, defaultName: string) => Promise<SaveResult>;
}

interface Window {
  electronAPI: ElectronAPI;
}
