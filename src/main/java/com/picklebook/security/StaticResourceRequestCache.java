package com.picklebook.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.Locale;
import java.util.regex.Pattern;

public class StaticResourceRequestCache extends HttpSessionRequestCache {

    private static final Pattern STATIC_PATH = Pattern.compile(
            "(?:^|/)(?:css|js|img|vendor|uploads)(?:/|$)|\\.(?:css|js|map|ico|png|jpe?g|gif|svg|webp|avif|woff2?|ttf|eot)(?:\\?.*)?$",
            Pattern.CASE_INSENSITIVE);

    public StaticResourceRequestCache() {
        setRequestMatcher(request -> "GET".equalsIgnoreCase(request.getMethod())
                && !isStaticResourcePath(request.getServletPath()));
    }

    @Override
    public SavedRequest getRequest(HttpServletRequest request, HttpServletResponse response) {
        SavedRequest savedRequest = super.getRequest(request, response);
        if (savedRequest != null && isStaticResourcePath(savedRequest.getRedirectUrl())) {
            removeRequest(request, response);
            return null;
        }
        return savedRequest;
    }

    private static boolean isStaticResourcePath(String path) {
        return path != null && STATIC_PATH.matcher(path.toLowerCase(Locale.ROOT)).find();
    }
}
