import { useEffect, useRef, useCallback } from "react";
import * as monaco from "monaco-editor";

export interface EditorOperation {
  type: "INSERT" | "DELETE" | "REPLACE";
  position: number;
  text?: string;
  length?: number;
  version: number;
}

interface SyncEditorProps {
  value: string;
  version: number;
  language?: string;
  onChange?: (value: string) => void;
  onOperation?: (op: EditorOperation) => void;
  applyRemoteOperation?: (fn: (op: EditorOperation) => void) => void;
  readonly?: boolean;
}

export default function SyncEditor({
  value,
  version,
  language,
  onChange,
  onOperation,
  applyRemoteOperation,
  readonly,
}: SyncEditorProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const editorRef = useRef<monaco.editor.IStandaloneCodeEditor | null>(null);
  const versionRef = useRef(version);
  const isRemoteOp = useRef(false);
  const onOperationRef = useRef(onOperation);
  onOperationRef.current = onOperation;

  const initialValueSynced = useRef(false);

  useEffect(() => {
    versionRef.current = version;
  }, [version]);

  useEffect(() => {
    if (!containerRef.current) return;

    const editor = monaco.editor.create(containerRef.current, {
      value,
      language: language || "plaintext",
      theme: "vs-dark",
      readOnly: readonly,
      automaticLayout: true,
      minimap: { enabled: true },
      scrollBeyondLastLine: false,
      fontSize: 14,
    });

    editorRef.current = editor;

    editor.onDidChangeModelContent((e) => {
      if (isRemoteOp.current) {
        isRemoteOp.current = false;
        return;
      }

      const newValue = editor.getValue();
      onChange?.(newValue);

      for (const change of e.changes) {
        const position = change.rangeOffset;
        const op: EditorOperation = {
          type: "INSERT",
          position,
          version: versionRef.current,
        };

        if (change.text && change.rangeLength > 0) {
          op.type = "REPLACE";
          op.text = change.text;
          op.length = change.rangeLength;
        } else if (change.text) {
          op.type = "INSERT";
          op.text = change.text;
          op.length = 0;
        } else {
          op.type = "DELETE";
          op.length = change.rangeLength;
        }

        onOperationRef.current?.(op);
      }
    });

    return () => {
      editor.dispose();
    };
  }, []);

  useEffect(() => {
    if (initialValueSynced.current) return;
    if (!value) return;
    const editor = editorRef.current;
    if (!editor) return;
    const model = editor.getModel();
    if (!model) return;
    if (value !== model.getValue()) {
      isRemoteOp.current = true;
      model.setValue(value);
    }
    initialValueSynced.current = true;
  }, [value]);

  const applyOperation = useCallback((op: EditorOperation) => {
    const editor = editorRef.current;
    if (!editor) return;

    isRemoteOp.current = true;
    const model = editor.getModel();
    if (!model) return;

    const fullText = model.getValue();

    switch (op.type) {
      case "INSERT":
        if (op.text != null) {
          const newText =
            fullText.slice(0, op.position) + op.text + fullText.slice(op.position);
          model.setValue(newText);
        }
        break;
      case "DELETE":
        if (op.length != null) {
          const newText =
            fullText.slice(0, op.position) +
            fullText.slice(op.position + op.length);
          model.setValue(newText);
        }
        break;
      case "REPLACE":
        if (op.text != null && op.length != null) {
          const newText =
            fullText.slice(0, op.position) +
            op.text +
            fullText.slice(op.position + op.length);
          model.setValue(newText);
        }
        break;
    }
  }, []);

  useEffect(() => {
    if (applyRemoteOperation && applyOperation) {
      applyRemoteOperation(applyOperation);
    }
  }, [applyRemoteOperation]);

  return <div ref={containerRef} className="w-full h-full" />;
}
