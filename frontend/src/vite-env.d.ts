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
  openLocalFile: () => Promise<FileResult | null>;
  saveFile: (filePath: string, content: string) => Promise<SaveResult>;
  saveAs: (content: string, defaultName: string) => Promise<SaveResult>;
  onFileOpened: (callback: (data: FileResult) => void) => () => void;
  onFileImported: (callback: (data: FileResult) => void) => () => void;
  onSaveRequested: (callback: () => void) => () => void;
  onSaveAsRequested: (callback: () => void) => () => void;
}

interface Window {
  electronAPI: ElectronAPI;
}
