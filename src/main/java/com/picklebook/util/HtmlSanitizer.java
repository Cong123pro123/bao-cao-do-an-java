package com.picklebook.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

/**
 * Làm sạch HTML do Admin nhập (tin tức, chính sách) để chống stored XSS.
 * Chỉ cho phép các thẻ định dạng cơ bản, không cho script/style/iframe.
 */
public final class HtmlSanitizer {

    private static final Safelist SAFELIST = Safelist.relaxed()
            .addTags("hr", "s", "u")
            .addAttributes("a", "target", "rel")
            .addAttributes("img", "alt", "width", "height")
            .removeProtocols("img", "src", "http", "https");

    private HtmlSanitizer() {
    }

    public static String clean(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return Jsoup.clean(html, SAFELIST);
    }
}
