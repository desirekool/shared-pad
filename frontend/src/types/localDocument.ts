export interface LocalDocument {
  id: string;
  title: string;
  filePath?: string;
  content: string;
  isLocal: true;
  createdAt: string;
  updatedAt: string;
}
