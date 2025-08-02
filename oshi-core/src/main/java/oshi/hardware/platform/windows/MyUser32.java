package oshi.hardware.platform.windows;

import com.sun.jna.Native;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

public interface MyUser32 extends StdCallLibrary {

    MyUser32 INSTANCE = Native.load("user32", MyUser32.class, W32APIOptions.DEFAULT_OPTIONS);

    boolean EnumDisplayDevices(String lpDevice, int iDevNum, DISPLAY_DEVICE lpDisplayDevice, int dwFlags);
}
