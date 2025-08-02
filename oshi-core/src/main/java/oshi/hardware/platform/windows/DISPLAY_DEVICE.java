package oshi.hardware.platform.windows;

import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinDef;

import java.util.Arrays;
import java.util.List;


public class DISPLAY_DEVICE extends Structure {
    public WinDef.DWORD cb;
    public char[] DeviceName = new char[32];
    public char[] DeviceString = new char[128];
    public WinDef.DWORD StateFlags;
    public char[] DeviceID = new char[128];
    public char[] DeviceKey = new char[128];

    public DISPLAY_DEVICE() {
        cb = new WinDef.DWORD(this.size()); // Must be set before calling EnumDisplayDevices
    }

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("cb", "DeviceName", "DeviceString", "StateFlags", "DeviceID", "DeviceKey");
    }

    public static class DisplayDeviceFlags {
        public static final int DISPLAY_DEVICE_ACTIVE = 0x00000001;
        public static final int DISPLAY_DEVICE_PRIMARY_DEVICE = 0x00000004;
    }
}
