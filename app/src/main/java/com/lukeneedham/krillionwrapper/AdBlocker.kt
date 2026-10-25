package com.lukeneedham.krillionwrapper

import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

/**
 * Keeps ads out of the game, like a browser's content blocker would:
 * requests to known ad hosts are answered with nothing, and banner containers that still end up
 * in the page are hidden with CSS.
 */
object AdBlocker {

    /** A request to one of these hosts, or any subdomain of them, is blocked. */
    private val AD_HOSTS = listOf(
        // Google
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "googletagservices.com",
        "adservice.google.com",
        // Other large ad networks and exchanges
        "adnxs.com",
        "amazon-adsystem.com",
        "criteo.com",
        "criteo.net",
        "taboola.com",
        "outbrain.com",
        "rubiconproject.com",
        "pubmatic.com",
        "openx.net",
        "adsrvr.org",
        "casalemedia.com",
        "33across.com",
        "smartadserver.com",
        "adform.net",
        "moatads.com",
        "advertising.com",
        "media.net",
        "yieldmo.com",
        "sharethrough.com",
        "indexww.com",
        "lijit.com",
        "sovrn.com",
        "teads.tv",
        "adcolony.com",
        "applovin.com",
        "unityads.unity3d.com",
        "a-ads.com",
        "ezoic.net",
        "mediavine.com",
        "adsterra.com",
        "exoclick.com",
        "propellerads.com",
        "monetag.com",
        // Web game ad providers
        "gamedistribution.com",
        "adinplay.com",
        "venatus.com",
        "playwire.com",
        "cpmstar.com",
        "adsafeprotected.com",
    )

    fun isAdHost(host: String): Boolean {
        val h = host.lowercase()
        return AD_HOSTS.any { h == it || h.endsWith(".$it") }
    }

    fun emptyResponse() = WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))

    /** Adds a stylesheet that hides the usual ad containers. Safe to run more than once per page. */
    const val HIDE_ADS_SCRIPT = """
        (function () {
          if (document.getElementById('krillion-wrapper-hide-ads')) return;
          var style = document.createElement('style');
          style.id = 'krillion-wrapper-hide-ads';
          style.textContent = [
            // krillion.io's own ad slots (Mediavine) and sticky ad dock
            '.kr-ad-placement',
            '.kr-ad-results',
            '.mv_slot_target',
            '[class*="kr-ad-dock"]',
            '[class*="kr-ad-rail"]',
            '[id^="mv-"]',
            '[class*="mv-sticky"]',
            'ins.adsbygoogle',
            '[class*="adsbygoogle"]',
            '[id^="google_ads"]',
            '[id^="div-gpt-ad"]',
            '[id^="aswift"]',
            'iframe[src*="doubleclick.net"]',
            'iframe[src*="googlesyndication.com"]',
            'iframe[src*="amazon-adsystem.com"]',
            'iframe[src*="adnxs.com"]',
            '[id*="banner-ad" i]',
            '[class*="banner-ad" i]',
            '[id*="bannerad" i]',
            '[class*="bannerad" i]',
            '[id*="ad-banner" i]',
            '[class*="ad-banner" i]',
            '[id*="adbanner" i]',
            '[class*="adbanner" i]',
            '[id*="ad-container" i]',
            '[class*="ad-container" i]',
            '[id*="adcontainer" i]',
            '[class*="adcontainer" i]'
          ].join(',') + '{display:none !important;height:0 !important;min-height:0 !important;}' +
            // The page reserves room at the bottom for the ad dock; give that space back to the game.
            '.kr-ad-layout{--kr-rail-bottom:0px !important;--kr-ad-dock-lift:0px !important;}';
          (document.head || document.documentElement).appendChild(style);

          // With the ad requests blocked, the game's ad slot is left behind as an empty bar pinned to
          // the bottom of the screen that still covers the content. Hide any such empty bar entirely.
          function hideEmptyBottomBars() {
            var vw = window.innerWidth, vh = window.innerHeight;
            var all = document.body ? document.body.getElementsByTagName('*') : [];
            for (var i = 0; i < all.length; i++) {
              var el = all[i];
              if (el.dataset && el.dataset.krillionHidden) continue;
              var cs = getComputedStyle(el);
              if (cs.position !== 'fixed' && cs.position !== 'sticky') continue;
              var r = el.getBoundingClientRect();
              if (r.height <= 0 || r.height > vh * 0.3 || r.width < vw * 0.8) continue;
              if (Math.abs(r.bottom - vh) > 4) continue;
              if ((el.innerText || '').trim() !== '') continue;
              if (el.querySelector('img,canvas,video,svg,button,input,a,[role="button"]')) continue;
              el.style.setProperty('display', 'none', 'important');
              el.dataset.krillionHidden = '1';
            }
          }
          var scheduled = false;
          function schedule() {
            if (scheduled) return;
            scheduled = true;
            setTimeout(function () { scheduled = false; hideEmptyBottomBars(); }, 300);
          }
          hideEmptyBottomBars();
          new MutationObserver(schedule).observe(document.documentElement, {
            childList: true, subtree: true, attributes: true, attributeFilter: ['style', 'class']
          });
          window.addEventListener('resize', schedule);
          setInterval(hideEmptyBottomBars, 2000);
        })();
    """
}
