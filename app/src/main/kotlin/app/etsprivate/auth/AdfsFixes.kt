package app.etsprivate.auth

import android.net.Uri
import android.webkit.WebView

object AdfsFixes {
    private const val ADFS_HOST = "fs.etsmtl.ca"

    fun apply(view: WebView?, tag: String, pageUrl: String?) {
        if (Uri.parse(pageUrl).host != ADFS_HOST) return
        view?.evaluateJavascript(FIX_VIEWPORT_JS, null)
        view?.evaluateJavascript(FIX_ADFS_LAYOUT_JS) { result -> android.util.Log.d(tag, "adfsLayout $result") }
        view?.evaluateJavascript(FORCE_VISIBLE_JS) { result -> android.util.Log.d(tag, "forceVisible $result") }
    }
}

private const val FIX_VIEWPORT_JS = """
(function(){
  var m = document.querySelector('meta[name=viewport]');
  if (!m) { m = document.createElement('meta'); m.setAttribute('name','viewport'); document.head.appendChild(m); }
  m.setAttribute('content','width=device-width, initial-scale=1');
})()
"""

private const val FIX_ADFS_LAYOUT_JS = """
(function(){
  if (!document.getElementById('contentWrapper') && !document.getElementById('fullPage')) return 'not-adfs';
  var style = document.getElementById('ets-adfs-fix');
  if (!style) {
    style = document.createElement('style');
    style.id = 'ets-adfs-fix';
    document.head.appendChild(style);
  }
  style.textContent =
    'html,body{height:auto !important;min-height:100% !important;overflow:visible !important}' +
    '#fullPage,#contentWrapper,#content,#workArea,#loginArea,#authArea{' +
      'height:auto !important;min-height:0 !important;overflow:visible !important;float:none !important}';
  return 'applied bodyH=' + document.body.scrollHeight;
})()
"""

private const val FORCE_VISIBLE_JS = """
(function(){
  var fixed = [];
  var unhide = function(el){
    while (el && el.tagName) {
      var cs = getComputedStyle(el);
      if (parseFloat(cs.opacity) === 0) {
        el.style.setProperty('opacity','1','important');
        fixed.push('opacity:' + el.tagName + '#' + (el.id || '') + '.' + (el.className || ''));
      }
      if (cs.visibility === 'hidden') {
        el.style.setProperty('visibility','visible','important');
        fixed.push('visibility:' + el.tagName + '#' + (el.id || '') + '.' + (el.className || ''));
      }
      if (cs.display === 'none') {
        el.style.setProperty('display','block','important');
        fixed.push('display:' + el.tagName + '#' + (el.id || '') + '.' + (el.className || ''));
      }
      el = el.parentElement;
    }
  };
  var fields = document.querySelectorAll('input[type=email], input[type=password], input[type=submit], form');
  for (var i = 0; i < fields.length; i++) unhide(fields[i]);
  return JSON.stringify(fixed.slice(0, 12));
})()
"""
