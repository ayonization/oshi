/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.unix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;

/**
 * Utility to query xrandr
 */
@ThreadSafe
public final class Xrandr {

    private static final String[] XRANDR_VERBOSE = { "xrandr", "--verbose" };

    private Xrandr() {
    }

    /**
     * Gets EDID byte arrays from the running X server via xrandr.
     *
     * @return a list of EDID byte arrays
     */
    public static List<byte[]> getEdidArrays() {
        // Special handling for X commands, don't use LC_ALL
        return getEdidArrays(runXrandr());
    }

    /**
     * Parse EDID arrays from xrandr verbose output.
     *
     * @param xrandr output of {@code xrandr --verbose}
     * @return a list of EDID byte arrays (at least 128 bytes each)
     */
    static List<byte[]> getEdidArrays(List<String> xrandr) {
        Map<String, Pair<Integer, byte[]>> data = getDisplayData(xrandr);
        List<byte[]> edids = new ArrayList<>(data.size());
        for (Pair<Integer, byte[]> pair : data.values()) {
            edids.add(pair.getB());
        }
        return Collections.unmodifiableList(edids);
    }

    /**
     * Gets display data from the running X server via xrandr, mapping each connected output's xrandr port name to its
     * connector ID and EDID.
     *
     * @return an ordered map of xrandr port name to {@link Pair} of connector ID ({@code -1} if not available) and EDID
     *         byte array
     */
    public static Map<String, Pair<Integer, byte[]>> getDisplayData() {
        return getDisplayData(runXrandr());
    }

    /**
     * Parse display data from xrandr verbose output. For each connected output, extracts the xrandr port name (the
     * first whitespace-delimited token on the output header line), the {@code CONNECTOR_ID} property (if present,
     * requires Linux 6.5+), and the EDID byte array. The parser is order-independent: {@code CONNECTOR_ID} may appear
     * before or after {@code EDID:}.
     *
     * @param xrandr output of {@code xrandr --verbose}
     * @return an ordered map of xrandr port name to {@link Pair} of connector ID ({@code -1} if not available) and EDID
     *         byte array (at least 128 bytes). Only connected outputs with a valid EDID are included.
     */
    static Map<String, Pair<Integer, byte[]>> getDisplayData(List<String> xrandr) {
        if (xrandr.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Pair<Integer, byte[]>> results = new LinkedHashMap<>();
        String currentPort = "";
        boolean currentConnected = false;
        int currentConnectorId = -1;
        byte[] currentEdid = null;
        StringBuilder sb = null;
        for (String s : xrandr) {
            // Output headers start at column 0; properties and modes are always indented
            if (!s.isEmpty() && !Character.isWhitespace(s.charAt(0))) {
                // Flush the previous output before starting a new one
                if (currentConnected && currentEdid != null) {
                    results.put(currentPort, new Pair<>(currentConnectorId, currentEdid));
                }
                String[] words = ParseUtil.whitespaces.split(s.trim(), -1);
                currentPort = words[0];
                currentConnected = words.length > 1 && "connected".equals(words[1]);
                currentConnectorId = -1;
                currentEdid = null;
                sb = null;
                continue;
            }
            String trimmed = s.trim();
            if (trimmed.startsWith("CONNECTOR_ID:")) {
                currentConnectorId = ParseUtil.parseLastInt(trimmed, -1);
            } else if (trimmed.equals("EDID:")) {
                sb = new StringBuilder();
            } else if (sb != null) {
                sb.append(trimmed);
                if (sb.length() < 256) {
                    continue;
                }
                currentEdid = ParseUtil.hexStringToByteArray(sb.toString());
                if (currentEdid.length < 128) {
                    currentEdid = null;
                }
                sb = null;
            }
        }
        // Flush the last output
        if (currentConnected && currentEdid != null) {
            results.put(currentPort, new Pair<>(currentConnectorId, currentEdid));
        }
        return Collections.unmodifiableMap(results);
    }

    private static List<String> runXrandr() {
        if (System.getenv("DISPLAY") == null) {
            return Collections.emptyList();
        }
        return ExecutingCommand.runNative(XRANDR_VERBOSE, null);
    }
}
