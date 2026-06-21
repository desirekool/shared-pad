import { contextBridge, ipcRenderer } from "electron";

contextBridge.exposeInMainWorld("electronAPI", {
  openFile: () => ipcRenderer.invoke("dialog:openFile"),
  saveFile: (filePath: string, content: string) =>
    ipcRenderer.invoke("dialog:saveFile", { filePath, content }),
  saveAs: (content: string, defaultName: string) =>
    ipcRenderer.invoke("dialog:saveAs", { content, defaultName }),
});
