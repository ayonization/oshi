/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux;

import java.util.*;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.BluetoothDevice;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.Display;
import oshi.hardware.GlobalMemory;
import oshi.hardware.Sensors;
import oshi.hardware.SoundCard;
import oshi.hardware.common.AbstractHardwareAbstractionLayer;
import oshi.hardware.common.platform.unix.UnixDisplay;
import oshi.util.driver.linux.DrmEdid;
import oshi.util.driver.unix.Xrandr;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

/**
 * LinuxHardwareAbstractionLayer class.
 */
@ThreadSafe
public abstract class LinuxHardwareAbstractionLayer extends AbstractHardwareAbstractionLayer {

    /**
     * Default constructor.
     */
    protected LinuxHardwareAbstractionLayer() {
    }

    @Override
    public ComputerSystem createComputerSystem() {
        return new LinuxComputerSystem();
    }

    @Override
    public abstract GlobalMemory createMemory();

    @Override
    public abstract CentralProcessor createProcessor();

    @Override
    public Sensors createSensors() {
        return new LinuxSensors();
    }

    @Override
    protected List<Display> createDisplays() {
        List<Triplet<String, Integer, byte[]>> drmData = DrmEdid.getDisplayData();
        if (!drmData.isEmpty()) {
            return correlateWithXrandr(drmData);
        }
        return UnixDisplay.getDisplays();
    }

    private static List<Display> correlateWithXrandr(List<Triplet<String, Integer, byte[]>> drmData) {
        Map<String, Pair<Integer, byte[]>> xrandrData = Xrandr.getDisplayData();
        List<Display> displays = new ArrayList<>(drmData.size());
        for (Triplet<String, Integer, byte[]> drm : drmData) {
            String xrandrName = findXrandrOutputName(drm, xrandrData);
            displays.add(new UnixDisplay(drm.getC(), drm.getA(), xrandrName));
        }
        return displays;
    }

    private static String findXrandrOutputName(Triplet<String, Integer, byte[]> drm,
            Map<String, Pair<Integer, byte[]>> xrandrData) {
        int drmConnectorId = drm.getB();
        // First try matching by CONNECTOR_ID (Linux 6.5+)
        if (drmConnectorId >= 0) {
            for (Map.Entry<String, Pair<Integer, byte[]>> entry : xrandrData.entrySet()) {
                if ((Objects.nonNull(entry.getValue().getA()) && entry.getValue().getA()== drmConnectorId)) {
                    return entry.getKey();
                }
            }
        }
        // Fallback: match by first 128 bytes of EDID
        byte[] drmEdid = drm.getC();
        for (Map.Entry<String, Pair<Integer, byte[]>> entry : xrandrData.entrySet()) {
            byte[] xrandrEdid = entry.getValue().getB();
            if (Arrays.equals(Arrays.copyOf(drmEdid, 128), Arrays.copyOf(xrandrEdid, 128))) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    protected List<SoundCard> createSoundCards() {
        return LinuxSoundCard.getSoundCards();
    }

    @Override
    public List<BluetoothDevice> getBluetoothDevices() {
        return LinuxBluetoothDevice.getBluetoothDevices();
    }
}
