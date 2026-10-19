package com.maharecruitment.gov.in.common.audit;

import java.net.InetAddress;
import java.net.UnknownHostException;

import jakarta.servlet.http.HttpServletRequest;

/** Accepts literal IPv4/IPv6 addresses only; never resolves user-supplied host names. */
public final class ClientIpAddress {
    private ClientIpAddress() { }

    public static String resolve(HttpServletRequest request, boolean trustForwardedHeaders) {
        if (trustForwardedHeaders) {
            String forwarded = request.getHeader("X-Forwarded-For");
            String candidate = literal(first(forwarded));
            if (candidate == null) candidate = literal(request.getHeader("X-Real-IP"));
            if (candidate != null) return candidate;
        }
        String remote = literal(request.getRemoteAddr());
        return remote == null ? "UNKNOWN" : remote;
    }

    public static String literal(String value) {
        if (value == null) return null;
        String address = value.trim();
        if (address.isEmpty() || address.length() > 64) return null;
        if (address.indexOf(':') >= 0) {
            if (!address.matches("[0-9a-fA-F:.]+")) return null;
            try {
                InetAddress.getByName(address);
                return address;
            } catch (UnknownHostException invalid) {
                return null;
            }
        }
        String[] octets = address.split("\\.", -1);
        if (octets.length != 4) return null;
        for (String octet : octets) {
            if (!octet.matches("[0-9]{1,3}") || Integer.parseInt(octet) > 255) return null;
        }
        return address;
    }

    private static String first(String header) {
        if (header == null) return null;
        int separator = header.indexOf(',');
        return separator < 0 ? header : header.substring(0, separator);
    }
}
