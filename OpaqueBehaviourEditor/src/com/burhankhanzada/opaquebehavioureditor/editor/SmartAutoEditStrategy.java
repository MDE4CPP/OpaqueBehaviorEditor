package com.burhankhanzada.opaquebehavioureditor.editor;

import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;

public class SmartAutoEditStrategy implements IAutoEditStrategy {

    @Override
    public void customizeDocumentCommand(IDocument d, DocumentCommand c) {
        if (c.length == 0 && c.text != null && c.text.matches("\\r\\n|\\n|\\r")) {
            handleEnter(d, c);
        } else if ("{".equals(c.text)) {
            c.text = "{}";
            c.caretOffset = c.offset + 1;
            c.shiftsCaret = false;
        } else if ("[".equals(c.text)) {
            c.text = "[]";
            c.caretOffset = c.offset + 1;
            c.shiftsCaret = false;
        } else if ("(".equals(c.text)) {
            c.text = "()";
            c.caretOffset = c.offset + 1;
            c.shiftsCaret = false;
        } else if ("\"".equals(c.text)) {
            handleQuote(d, c, '"');
        } else if ("'".equals(c.text)) {
            handleQuote(d, c, '\'');
        } else if ("}".equals(c.text) || "]".equals(c.text) || ")".equals(c.text)) {
            handleClosingBracket(d, c);
        } else if ("}".equals(c.text)) {
            // Dedent if manually typing '}' on a whitespace line
            handleManualCloseBrace(d, c);
        }
    }

    private void handleEnter(IDocument d, DocumentCommand c) {
        try {
            int line = d.getLineOfOffset(c.offset);
            int lineStart = d.getLineOffset(line);
            String lineContent = d.get(lineStart, d.getLineLength(line));
            String indent = getIndent(lineContent);
            
            String nextIndent = indent;
            String beforeCursor = lineContent.substring(0, c.offset - lineStart);
            
            // If previous line ends with {
            if (beforeCursor.trim().endsWith("{")) {
                nextIndent = indent + "    "; // 4 spaces
            }
            
            // If hitting enter between { and }
            if (beforeCursor.trim().endsWith("{") && c.offset < d.getLength() && d.getChar(c.offset) == '}') {
                // Insert newline + nextIndent + newline + indent
                String nl = c.text;
                c.text = nl + nextIndent + nl + indent;
                c.caretOffset = c.offset + nl.length() + nextIndent.length();
                c.shiftsCaret = false;
                return;
            }
            
            c.text = c.text + nextIndent;
        } catch (Exception e) {
            // ignore
        }
    }

    private void handleQuote(IDocument d, DocumentCommand c, char quoteChar) {
        try {
            if (c.offset < d.getLength() && d.getChar(c.offset) == quoteChar) {
                // Just step over
                c.text = "";
                c.caretOffset = c.offset + 1;
                c.shiftsCaret = false;
                return;
            }
        } catch(Exception e){}
        c.text = String.valueOf(quoteChar) + quoteChar;
        c.caretOffset = c.offset + 1;
        c.shiftsCaret = false;
    }

    private void handleClosingBracket(IDocument d, DocumentCommand c) {
        try {
            if (c.offset < d.getLength() && d.getChar(c.offset) == c.text.charAt(0)) {
                // Just step over
                c.text = "";
                c.caretOffset = c.offset + 1;
                c.shiftsCaret = false;
                return;
            }
            if ("}".equals(c.text)) {
                handleManualCloseBrace(d, c);
            }
        } catch(Exception e){}
    }
    
    private void handleManualCloseBrace(IDocument d, DocumentCommand c) {
        try {
            int line = d.getLineOfOffset(c.offset);
            int lineStart = d.getLineOffset(line);
            String beforeCursor = d.get(lineStart, c.offset - lineStart);
            if (beforeCursor.trim().isEmpty() && beforeCursor.length() >= 4) {
                // Remove 4 spaces of indent
                d.replace(lineStart, beforeCursor.length(), beforeCursor.substring(4));
                c.offset -= 4;
            }
        } catch(Exception e){}
    }

    private String getIndent(String line) {
        StringBuilder sb = new StringBuilder();
        for (char c : line.toCharArray()) {
            if (c == ' ' || c == '\t') sb.append(c);
            else break;
        }
        return sb.toString();
    }
}
