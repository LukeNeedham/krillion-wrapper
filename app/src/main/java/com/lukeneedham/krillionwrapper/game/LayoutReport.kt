package com.lukeneedham.krillionwrapper.game

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.webkit.WebView
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONTokener

/** Debug-only: describes what sits at the bottom of the game page, in a dialog whose text can be copied. */
internal object LayoutReport {

    /** Describes the elements at the bottom of the page and every fixed/sticky element. */
    private const val SCRIPT = """
    (function () {
      function d(el) {
        var cs = getComputedStyle(el), r = el.getBoundingClientRect();
        var cls = typeof el.className === 'string' && el.className.trim()
          ? '.' + el.className.trim().split(/\s+/).join('.') : '';
        return el.tagName.toLowerCase() + (el.id ? '#' + el.id : '') + cls +
          ' pos=' + cs.position + ' rect=' + [r.left, r.top, r.width, r.height].map(Math.round) +
          ' bottom=' + cs.bottom + ' pb=' + cs.paddingBottom + ' mb=' + cs.marginBottom +
          ' bg=' + cs.backgroundColor + ' z=' + cs.zIndex + ' pe=' + cs.pointerEvents;
      }
      var layout = document.querySelector('.kr-ad-layout');
      var out = [
        'viewport ' + innerWidth + 'x' + innerHeight + ' dpr=' + devicePixelRatio,
        'rail-bottom=' + (layout ? getComputedStyle(layout).getPropertyValue('--kr-rail-bottom') : 'no layout')
      ];
      [8, 40, 80, 120].forEach(function (off) {
        out.push('--- topmost at y=innerHeight-' + off);
        var el = document.elementFromPoint(innerWidth / 2, innerHeight - off), n = 0;
        while (el && n++ < 6) { out.push(d(el)); el = el.parentElement; }
      });
      out.push('--- selectors');
      ['.kr-ad-layout', '.ds-root', '.ds-done', '.ds-done-inner', '.ds-stage'].forEach(function (s) {
        var e = document.querySelector(s);
        out.push(s + ': ' + (e ? d(e) : 'none'));
      });
      out.push('--- fixed/sticky elements');
      document.querySelectorAll('body *').forEach(function (e) {
        var p = getComputedStyle(e).position;
        if (p === 'fixed' || p === 'sticky') out.push(d(e));
      });
      return out.join('\n');
    })()
"""

    fun show(context: Context, webView: WebView) {
        webView.evaluateJavascript(SCRIPT) { raw ->
            val report = runCatching { JSONTokener(raw).nextValue() as? String }.getOrNull() ?: raw
            val text = TextView(context).apply {
                this.text = report
                textSize = 11f
                setTextIsSelectable(true)
                setPadding(32, 24, 32, 24)
            }
            AlertDialog.Builder(context)
                .setTitle("Layout report")
                .setView(ScrollView(context).apply { addView(text) })
                .setPositiveButton("Copy") { _, _ ->
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("layout report", report))
                }
                .setNegativeButton("Close", null)
                .show()
        }
    }
}
