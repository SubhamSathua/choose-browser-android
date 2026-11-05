package com.hyper.choosebrowsernew.ui.viewer;

import android.content.Context;
import android.util.Base64;

import androidx.annotation.NonNull;

import com.hyper.choosebrowsernew.R;
import com.hyper.choosebrowsernew.util.ThemeHelper;

import java.nio.charset.StandardCharsets;

/**
 * Modular HTML/CSS/JS generator for rendering Text and Markdown previews in a themed WebView.
 */
public class PreviewHtmlBuilder {

    @NonNull
    public static String buildTextPreviewHtml(@NonNull Context context, @NonNull String rawText, boolean isWrapped) {
        boolean isDark = ThemeHelper.isDarkTheme(context);
        int surface = ThemeHelper.resolveThemeColor(context, R.attr.colorPopupSurface, R.color.backgroundSecondary);
        int text = ThemeHelper.resolveThemeColor(context, R.attr.colorPopupText, R.color.text);

        String bgHex = String.format("#%06X", (0xFFFFFF & surface));
        String textHex = String.format("#%06X", (0xFFFFFF & text));
        String selectionBg = isDark ? "#377aff55" : "#377aff33";

        String base64Content = Base64.encodeToString(rawText.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
        String initialClass = isWrapped ? "wrap-mode" : "unwrap-mode";

        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "  <meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=5.0'>\n" +
                "  <style>\n" +
                "    * { box-sizing: border-box; }\n" +
                "    html, body {\n" +
                "      margin: 0;\n" +
                "      padding: 0;\n" +
                "      background-color: " + bgHex + ";\n" +
                "      color: " + textHex + ";\n" +
                "    }\n" +
                "    body {\n" +
                "      padding: 16px;\n" +
                "      font-family: 'JetBrains Mono', 'SF Mono', Consolas, Menlo, monospace;\n" +
                "      font-size: 14px;\n" +
                "      line-height: 1.6;\n" +
                "    }\n" +
                "    pre {\n" +
                "      margin: 0;\n" +
                "      padding: 0;\n" +
                "      font-family: inherit;\n" +
                "      font-size: inherit;\n" +
                "    }\n" +
                "    pre.wrap-mode {\n" +
                "      white-space: pre-wrap;\n" +
                "      word-break: break-word;\n" +
                "      overflow-x: hidden;\n" +
                "    }\n" +
                "    pre.unwrap-mode {\n" +
                "      white-space: pre;\n" +
                "      overflow-x: auto;\n" +
                "      min-width: max-content;\n" +
                "    }\n" +
                "    ::selection {\n" +
                "      background: " + selectionBg + ";\n" +
                "    }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <pre id='fileContent' class='" + initialClass + "'></pre>\n" +
                "  <script>\n" +
                "    try {\n" +
                "      const b64 = '" + base64Content + "';\n" +
                "      const bytes = Uint8Array.from(atob(b64), c => c.charCodeAt(0));\n" +
                "      const decoded = new TextDecoder('utf-8').decode(bytes);\n" +
                "      document.getElementById('fileContent').textContent = decoded;\n" +
                "    } catch (e) {\n" +
                "      document.getElementById('fileContent').textContent = 'Error decoding file content.';\n" +
                "    }\n" +
                "    window.setWrapMode = function(wrapped) {\n" +
                "      const el = document.getElementById('fileContent');\n" +
                "      if (el) {\n" +
                "        el.className = wrapped ? 'wrap-mode' : 'unwrap-mode';\n" +
                "      }\n" +
                "    };\n" +
                "  </script>\n" +
                "</body>\n" +
                "</html>";
    }

    @NonNull
    public static String buildMarkdownPreviewHtml(@NonNull Context context, @NonNull String rawMarkdown, boolean isWrapped) {
        boolean isDark = ThemeHelper.isDarkTheme(context);
        int surface = ThemeHelper.resolveThemeColor(context, R.attr.colorPopupSurface, R.color.backgroundSecondary);
        int text = ThemeHelper.resolveThemeColor(context, R.attr.colorPopupText, R.color.text);

        String bgHex = String.format("#%06X", (0xFFFFFF & surface));
        String textHex = String.format("#%06X", (0xFFFFFF & text));
        String codeBg = isDark ? "#2A2D35" : "#E8EAED";
        String codeText = isDark ? "#FFD700" : "#D32F2F";
        String preBg = isDark ? "#1C1D21" : "#F1F3F4";
        String borderColor = isDark ? "#383A42" : "#DADCE0";
        String accentColor = "#377aff";
        String selectionBg = isDark ? "#377aff55" : "#377aff33";

        String base64Content = Base64.encodeToString(rawMarkdown.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
        String initialClass = isWrapped ? "wrap-mode" : "unwrap-mode";

        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "  <meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=5.0'>\n" +
                "  <script src='file:///android_asset/src/marked.min.js'></script>\n" +
                "  <style>\n" +
                "    @font-face { font-family: 'Poppins'; src: url('file:///android_res/font/poppins_medium.ttf'); }\n" +
                "    * { box-sizing: border-box; }\n" +
                "    html, body {\n" +
                "      margin: 0;\n" +
                "      padding: 0;\n" +
                "      background-color: " + bgHex + ";\n" +
                "      color: " + textHex + ";\n" +
                "    }\n" +
                "    body {\n" +
                "      padding: 16px 20px 48px 20px;\n" +
                "      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;\n" +
                "      font-size: 15px;\n" +
                "      line-height: 1.65;\n" +
                "    }\n" +
                "    .wrap-mode {\n" +
                "      white-space: normal;\n" +
                "      word-break: break-word;\n" +
                "    }\n" +
                "    .wrap-mode pre, .wrap-mode code {\n" +
                "      white-space: pre-wrap !important;\n" +
                "      word-break: break-all !important;\n" +
                "    }\n" +
                "    .unwrap-mode {\n" +
                "      min-width: max-content;\n" +
                "    }\n" +
                "    .unwrap-mode p, .unwrap-mode li, .unwrap-mode h1, .unwrap-mode h2, .unwrap-mode h3, .unwrap-mode h4, .unwrap-mode h5, .unwrap-mode h6 {\n" +
                "      white-space: nowrap !important;\n" +
                "    }\n" +
                "    .unwrap-mode pre, .unwrap-mode code {\n" +
                "      white-space: pre !important;\n" +
                "      overflow-x: auto !important;\n" +
                "    }\n" +
                "    h1, h2, h3, h4, h5, h6 {\n" +
                "      font-family: 'Poppins', sans-serif;\n" +
                "      margin-top: 1.2em;\n" +
                "      margin-bottom: 0.5em;\n" +
                "      font-weight: 600;\n" +
                "    }\n" +
                "    h1 {\n" +
                "      font-size: 24px;\n" +
                "      border-bottom: 2px solid " + accentColor + ";\n" +
                "      padding-bottom: 6px;\n" +
                "    }\n" +
                "    h2 {\n" +
                "      font-size: 20px;\n" +
                "      border-left: 4px solid " + accentColor + ";\n" +
                "      padding-left: 10px;\n" +
                "    }\n" +
                "    code {\n" +
                "      background: " + codeBg + ";\n" +
                "      padding: 2px 6px;\n" +
                "      border-radius: 4px;\n" +
                "      font-family: 'JetBrains Mono', Consolas, monospace;\n" +
                "      font-size: 0.88em;\n" +
                "      color: " + codeText + ";\n" +
                "    }\n" +
                "    pre {\n" +
                "      background: " + preBg + ";\n" +
                "      padding: 14px;\n" +
                "      border-radius: 12px;\n" +
                "      border: 1px solid " + borderColor + ";\n" +
                "      overflow-x: auto;\n" +
                "      margin: 16px 0;\n" +
                "    }\n" +
                "    pre code {\n" +
                "      background: transparent;\n" +
                "      padding: 0;\n" +
                "      color: " + textHex + ";\n" +
                "    }\n" +
                "    blockquote {\n" +
                "      border-left: 4px solid " + accentColor + ";\n" +
                "      padding: 6px 14px;\n" +
                "      margin: 16px 0;\n" +
                "      background: " + preBg + ";\n" +
                "      border-radius: 0 8px 8px 0;\n" +
                "      opacity: 0.9;\n" +
                "    }\n" +
                "    table {\n" +
                "      border-collapse: collapse;\n" +
                "      width: 100%;\n" +
                "      margin: 16px 0;\n" +
                "    }\n" +
                "    th, td {\n" +
                "      border: 1px solid " + borderColor + ";\n" +
                "      padding: 8px 12px;\n" +
                "      text-align: left;\n" +
                "    }\n" +
                "    th {\n" +
                "      background: " + preBg + ";\n" +
                "    }\n" +
                "    hr {\n" +
                "      border: 0;\n" +
                "      border-top: 1px solid " + borderColor + ";\n" +
                "      margin: 24px 0;\n" +
                "    }\n" +
                "    a {\n" +
                "      color: " + accentColor + ";\n" +
                "      text-decoration: none;\n" +
                "      font-weight: 500;\n" +
                "    }\n" +
                "    img {\n" +
                "      max-width: 100%;\n" +
                "      border-radius: 8px;\n" +
                "    }\n" +
                "    ::selection {\n" +
                "      background: " + selectionBg + ";\n" +
                "    }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body class='" + initialClass + "'>\n" +
                "  <div id='content'></div>\n" +
                "  <script>\n" +
                "    try {\n" +
                "      const b64 = '" + base64Content + "';\n" +
                "      const bytes = Uint8Array.from(atob(b64), c => c.charCodeAt(0));\n" +
                "      const decoded = new TextDecoder('utf-8').decode(bytes);\n" +
                "      if (typeof marked !== 'undefined') {\n" +
                "        document.getElementById('content').innerHTML = marked.parse(decoded);\n" +
                "      } else {\n" +
                "        document.getElementById('content').textContent = decoded;\n" +
                "      }\n" +
                "    } catch (e) {\n" +
                "      document.getElementById('content').textContent = 'Error parsing markdown content.';\n" +
                "    }\n" +
                "    window.setWrapMode = function(wrapped) {\n" +
                "      if (wrapped) {\n" +
                "        document.body.classList.remove('unwrap-mode');\n" +
                "        document.body.classList.add('wrap-mode');\n" +
                "      } else {\n" +
                "        document.body.classList.remove('wrap-mode');\n" +
                "        document.body.classList.add('unwrap-mode');\n" +
                "      }\n" +
                "    };\n" +
                "  </script>\n" +
                "</body>\n" +
                "</html>";
    }
}
