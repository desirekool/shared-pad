import { contextBridge, ipcRenderer } from "electron";

contextBridge.exposeInMainWorld("electronAPI", {
  openFile: () => ipcRenderer.invoke("dialog:openFile"),
  openLocalFile: () => ipcRenderer.invoke("dialog:openLocalFile"),
  saveFile: (filePath: string, content: string) =>
    ipcRenderer.invoke("dialog:saveFile", { filePath, content }),
  saveAs: (content: string, defaultName: string) =>
    ipcRenderer.invoke("dialog:saveAs", { content, defaultName }),
  setTitle: (title: string) => ipcRenderer.send("set-title", title),
  onFileOpened: (callback: (data: { path: string; name: string; content: string }) => void) => {
    const handler = (_event: Electron.IpcRendererEvent, data: { path: string; name: string; content: string }) => callback(data);
    ipcRenderer.on("menu:file-opened", handler);
    return () => { ipcRenderer.removeListener("menu:file-opened", handler); };
  },
  onFileImported: (callback: (data: { path: string; name: string; content: string }) => void) => {
    const handler = (_event: Electron.IpcRendererEvent, data: { path: string; name: string; content: string }) => callback(data);
    ipcRenderer.on("menu:file-imported", handler);
    return () => { ipcRenderer.removeListener("menu:file-imported", handler); };
  },
  onSaveRequested: (callback: () => void) => {
    const handler = () => callback();
    ipcRenderer.on("menu:file-save", handler);
    return () => { ipcRenderer.removeListener("menu:file-save", handler); };
  },
  onSaveAsRequested: (callback: () => void) => {
    const handler = () => callback();
    ipcRenderer.on("menu:file-save-as", handler);
    return () => { ipcRenderer.removeListener("menu:file-save-as", handler); };
  },
});
