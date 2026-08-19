/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.Display;
import oshi.hardware.common.AbstractDisplay;
import oshi.util.driver.unix.Xrandr;
import oshi.util.tuples.Pair;

/**
 * A Display
 */
@ThreadSafe
public final class UnixDisplay extends AbstractDisplay {

    private final String outputName;

    /**
     * Constructor for UnixDisplay.
     *
     * @param edid a byte array representing a display EDID
     */
    public UnixDisplay(byte[] edid) {
        super(edid);
        this.outputName = null;
    }

    /**
     * Constructor for UnixDisplay with device port.
     *
     * @param edid       a byte array representing a display EDID
     * @param devicePort the DRM connector name (e.g. {@code HDMI-A-1})
     */
    public UnixDisplay(byte[] edid, String devicePort) {
        super(edid, devicePort);
        this.outputName = null;
    }

    /**
     * Constructor for UnixDisplay with device port and xrandr output name.
     *
     * @param edid       a byte array representing a display EDID
     * @param devicePort the DRM connector name (e.g. {@code HDMI-A-1})
     * @param outputName the xrandr output name (e.g. {@code HDMI-1}), or {@code null} if not known
     */
    public UnixDisplay(byte[] edid, String devicePort, String outputName) {
        super(edid, devicePort);
        this.outputName = outputName;
    }

    @Override
    public Optional<String> getOutputName() {
        return Optional.ofNullable(this.outputName);
    }

    /**
     * Gets Display Information from xrandr. Used as a fallback when DRM sysfs is not available.
     *
     * @return An array of Display objects representing monitors, etc.
     */
    public static List<Display> getDisplays() {
        Map<String, Pair<Integer, byte[]>> data = Xrandr.getDisplayData();
        List<Display> displays = new ArrayList<>(data.size());
        for (Map.Entry<String, Pair<Integer, byte[]>> entry : data.entrySet()) {
            // When coming from xrandr only, the xrandr port name is the best device port we have
            displays.add(new UnixDisplay(entry.getValue().getB(), entry.getKey(), entry.getKey()));
        }
        return displays;
    }
}
