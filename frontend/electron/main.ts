import { app, BrowserWindow, ipcMain, dialog, Menu } from "electron";
import path from "path";
import fs from "fs";

let mainWindow: BrowserWindow | null = null;

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1200,
    height: 800,
    webPreferences: {
      preload: path.join(__dirname, "preload.js"),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: false,
    },
  });

  if (process.env.VITE_DEV_SERVER_URL) {
    mainWindow.loadURL(process.env.VITE_DEV_SERVER_URL);
  } else {
    mainWindow.loadFile(path.join(__dirname, "../dist/index.html"));
  }
}

function createMenu() {
  const menuTemplate: Electron.MenuItemConstructorOptions[] = [
    {
      label: "File",
      submenu: [
        {
          label: "Open as Local",
          accelerator: "CmdOrCtrl+Shift+O",
          click: async () => {
            const result = await dialog.showOpenDialog(mainWindow!, {
              properties: ["openFile"],
              filters: [
                { name: "All Files", extensions: ["*"] },
                { name: "Text", extensions: ["txt", "md", "json", "xml", "yaml", "yml"] },
                { name: "Code", extensions: ["js", "ts", "jsx", "tsx", "py", "java", "go", "rs"] },
              ],
            });
            if (result.canceled || result.filePaths.length === 0) return;
            const filePath = result.filePaths[0];
            const content = fs.readFileSync(filePath, "utf-8");
            mainWindow?.webContents.send("menu:file-opened", {
              path: filePath,
              name: path.basename(filePath),
              content,
            });
          },
        },
        { type: "separator" },
        {
          label: "Open (Import to Server)",
          accelerator: "CmdOrCtrl+O",
          click: async () => {
            const result = await dialog.showOpenDialog(mainWindow!, {
              properties: ["openFile"],
              filters: [
                { name: "All Files", extensions: ["*"] },
                { name: "Text", extensions: ["txt", "md", "json", "xml", "yaml", "yml"] },
                { name: "Code", extensions: ["js", "ts", "jsx", "tsx", "py", "java", "go", "rs"] },
              ],
            });
            if (result.canceled || result.filePaths.length === 0) return;
            const filePath = result.filePaths[0];
            const content = fs.readFileSync(filePath, "utf-8");
            mainWindow?.webContents.send("menu:file-imported", {
              path: filePath,
              name: path.basename(filePath),
              content,
            });
          },
        },
        { type: "separator" },
        {
          label: "Save",
          accelerator: "CmdOrCtrl+S",
          click: () => {
            mainWindow?.webContents.send("menu:file-save");
          },
        },
        {
          label: "Save As...",
          accelerator: "CmdOrCtrl+Shift+S",
          click: () => {
            mainWindow?.webContents.send("menu:file-save-as");
          },
        },
        { type: "separator" },
        { role: "quit" },
      ],
    },
    {
      label: "Edit",
      submenu: [
        { role: "undo" },
        { role: "redo" },
        { type: "separator" },
        { role: "cut" },
        { role: "copy" },
        { role: "paste" },
        { role: "selectAll" },
      ],
    },
  ];

  Menu.setApplicationMenu(Menu.buildFromTemplate(menuTemplate));
}

app.whenReady().then(() => {
  createWindow();
  createMenu();
});

app.on("window-all-closed", () => {
  if (process.platform !== "darwin") {
    app.quit();
  }
});

app.on("activate", () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    createWindow();
  }
});

ipcMain.handle("dialog:openLocalFile", async () => {
  const result = await dialog.showOpenDialog(mainWindow!, {
    properties: ["openFile"],
    filters: [
      { name: "All Files", extensions: ["*"] },
      { name: "Text", extensions: ["txt", "md", "json", "xml", "yaml", "yml"] },
      { name: "Code", extensions: ["js", "ts", "jsx", "tsx", "py", "java", "go", "rs"] },
    ],
  });
  if (result.canceled || result.filePaths.length === 0) return null;
  const filePath = result.filePaths[0];
  const content = fs.readFileSync(filePath, "utf-8");
  return { path: filePath, name: path.basename(filePath), content };
});

ipcMain.handle("dialog:openFile", async () => {
  const result = await dialog.showOpenDialog(mainWindow!, {
    properties: ["openFile"],
    filters: [
      { name: "All Files", extensions: ["*"] },
      { name: "Text", extensions: ["txt", "md", "json", "xml", "yaml", "yml"] },
      { name: "Code", extensions: ["js", "ts", "jsx", "tsx", "py", "java", "go", "rs"] },
    ],
  });
  if (result.canceled || result.filePaths.length === 0) return null;
  const filePath = result.filePaths[0];
  const content = fs.readFileSync(filePath, "utf-8");
  return { path: filePath, name: path.basename(filePath), content };
});

ipcMain.handle("dialog:saveFile", async (_event, { filePath, content }: { filePath: string; content: string }) => {
  try {
    fs.writeFileSync(filePath, content, "utf-8");
    return { success: true };
  } catch (err) {
    return { success: false, error: String(err) };
  }
});

ipcMain.on("set-title", (_event, title: string) => {
  mainWindow?.setTitle(title);
});

ipcMain.handle("dialog:saveAs", async (_event, { content, defaultName }: { content: string; defaultName: string }) => {
  const result = await dialog.showSaveDialog(mainWindow!, {
    defaultPath: defaultName,
    filters: [
      { name: "All Files", extensions: ["*"] },
      { name: "Text", extensions: ["txt", "md"] },
    ],
  });
  if (result.canceled || !result.filePath) return { success: false };
  try {
    fs.writeFileSync(result.filePath, content, "utf-8");
    return { success: true, path: result.filePath };
  } catch (err) {
    return { success: false, error: String(err) };
  }
});
