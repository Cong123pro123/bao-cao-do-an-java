package com.picklebook.util;

import org.springframework.web.util.HtmlUtils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Thay thế các biến dạng {tenBien} trong mẫu email.
 * Giá trị được escape HTML để tránh chèn mã độc vào email.
 */
public final class TemplateRenderer {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{([a-zA-Z0-9_]+)}");

    private TemplateRenderer() {
    }

    public static String render(String template, Map<String, ?> variables) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = variables.get(key);
            String replacement = value == null ? "" : HtmlUtils.htmlEscape(String.valueOf(value));
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
