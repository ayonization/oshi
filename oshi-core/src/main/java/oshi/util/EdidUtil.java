/*
 * Copyright 2016-2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import oshi.annotation.SuppressForbidden;
import oshi.annotation.concurrent.ThreadSafe;

/**
 * EDID parsing utility.
 */
@ThreadSafe
public final class EdidUtil {

   // private static final Logger LOG = LoggerFactory.getLogger(EdidUtil.class);

    private EdidUtil() {
    }

    /**
     * Gets the Manufacturer ID from (up to) 3 5-bit characters in bytes 8 and 9
     *
     * @param edid The EDID byte array
     * @return The manufacturer ID
     */
    @SuppressForbidden(reason = "customized base 2 parsing not in Util class")
    public static String getManufacturerID(byte[] edid) {
        // Bytes 8-9 are manufacturer ID in 3 5-bit characters.
        String temp = String.format(Locale.ROOT, "%8s%8s", Integer.toBinaryString(edid[8] & 0xFF),
                Integer.toBinaryString(edid[9] & 0xFF)).replace(' ', '0');
        //LOG.debug("Manufacurer ID: {}", temp);
        return String.format(Locale.ROOT, "%s%s%s", (char) (64 + Integer.parseInt(temp.substring(1, 6), 2)),
                (char) (64 + Integer.parseInt(temp.substring(6, 11), 2)),
                (char) (64 + Integer.parseInt(temp.substring(11, 16), 2))).replace("@", "");
    }

    /**
     * Gets the Product ID, bytes 10 and 11
     *
     * @param edid The EDID byte array
     * @return The product ID
     */
    public static String getProductID(byte[] edid) {
        // Bytes 10-11 are product ID expressed in hex characters
        return Integer.toHexString(
                ByteBuffer.wrap(Arrays.copyOfRange(edid, 10, 12)).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xffff);
    }

    /**
     * Gets the Serial number, bytes 12-15
     *
     * @param edid The EDID byte array
     * @return If all 4 bytes represent alphanumeric characters, a 4-character string, otherwise a hex string.
     */
    public static String getSerialNo(byte[] edid) {
        // Bytes 12-15 are Serial number (last 4 characters)
//        if (LOG.isDebugEnabled()) {
//            LOG.debug("Serial number: {}", Arrays.toString(Arrays.copyOfRange(edid, 12, 16)));
//        }
        return String.format(Locale.ROOT, "%s%s%s%s", getAlphaNumericOrHex(edid[15]), getAlphaNumericOrHex(edid[14]),
                getAlphaNumericOrHex(edid[13]), getAlphaNumericOrHex(edid[12]));
    }

    private static String getAlphaNumericOrHex(byte b) {
        return Character.isLetterOrDigit((char) b) ? String.format(Locale.ROOT, "%s", (char) b)
                : String.format(Locale.ROOT, "%02X", b);
    }

    /**
     * Return the week of year of manufacture
     *
     * @param edid The EDID byte array
     * @return The week of year
     */
    public static byte getWeek(byte[] edid) {
        // Byte 16 is manufacture week
        return edid[16];
    }

    /**
     * Return the year of manufacture
     *
     * @param edid The EDID byte array
     * @return The year of manufacture
     */
    public static int getYear(byte[] edid) {
        // Byte 17 is manufacture year-1990
        byte temp = edid[17];
        //LOG.debug("Year-1990: {}", temp);
        return temp + 1990;
    }

    /**
     * Return the EDID version
     *
     * @param edid The EDID byte array
     * @return The EDID version
     */
    public static String getVersion(byte[] edid) {
        // Bytes 18-19 are EDID version
        return edid[18] + "." + edid[19];
    }

    /**
     * Test if this EDID is a digital monitor based on byte 20
     *
     * @param edid The EDID byte array
     * @return True if the EDID represents a digital monitor, false otherwise
     */
    public static boolean isDigital(byte[] edid) {
        // Byte 20 is Video input params
        return 1 == (edid[20] & 0xff) >> 7;
    }

    /**
     * Get monitor width in cm
     *
     * @param edid The EDID byte array
     * @return Monitor width in cm
     */
    public static int getHcm(byte[] edid) {
        // Byte 21 is horizontal size in cm
        return edid[21];
    }

    /**
     * Get monitor height in cm
     *
     * @param edid The EDID byte array
     * @return Monitor height in cm
     */
    public static int getVcm(byte[] edid) {
        // Byte 22 is vertical size in cm
        return edid[22];
    }

    /**
     * Get the VESA descriptors
     *
     * @param edid The EDID byte array
     * @return A 2D array with four 18-byte elements representing VESA descriptors
     */
    public static byte[][] getDescriptors(byte[] edid) {
        byte[][] desc = new byte[4][18];
        for (int i = 0; i < desc.length; i++) {
            System.arraycopy(edid, 54 + 18 * i, desc[i], 0, 18);
        }
        return desc;
    }

    /**
     * Get the VESA descriptor type
     *
     * @param desc An 18-byte VESA descriptor
     * @return An integer representing the first four bytes of the VESA descriptor
     */
    public static int getDescriptorType(byte[] desc) {
        return ByteBuffer.wrap(Arrays.copyOfRange(desc, 0, 4)).getInt();
    }

    /**
     * Parse a detailed timing descriptor
     *
     * @param desc An 18-byte VESA descriptor
     * @return A string describing part of the detailed timing descriptor
     */
    public static String getTimingDescriptor(byte[] desc) {
        int clock = ByteBuffer.wrap(Arrays.copyOfRange(desc, 0, 2)).order(ByteOrder.LITTLE_ENDIAN).getShort() / 100;
        int hActive = (desc[2] & 0xff) + ((desc[4] & 0xf0) << 4);
        int vActive = (desc[5] & 0xff) + ((desc[7] & 0xf0) << 4);
        return String.format(Locale.ROOT, "Clock %dMHz, Active Pixels %dx%d ", clock, hActive, vActive);
    }

    /**
     * Parse descriptor range limits
     *
     * @param desc An 18-byte VESA descriptor
     * @return A string describing some of the range limits
     */
    public static String getDescriptorRangeLimits(byte[] desc) {
        return String.format(Locale.ROOT, "Field Rate %d-%d Hz vertical, %d-%d Hz horizontal, Max clock: %d MHz",
                desc[5], desc[6], desc[7], desc[8], desc[9] * 10);
    }

    /**
     * Parse descriptor text
     *
     * @param desc An 18-byte VESA descriptor
     * @return Plain text starting at the 4th byte
     */
    public static String getDescriptorText(byte[] desc) {
        return new String(Arrays.copyOfRange(desc, 4, 18), StandardCharsets.US_ASCII).trim();
    }

    /**
     * Get the preferred resolution for the monitor (Eg: 1920x1080)
     *
     * @param edid The edid Byte array
     * @return Plain text preferred resolution
     */

    public static String getPreferredResolution(byte[] edid) {
        int dtd = 54;
        int horizontalRes = (edid[dtd + 4] & 0xF0) << 4 | edid[dtd + 2] & 0xFF;
        int verticalRes = (edid[dtd + 7] & 0xF0) << 4 | edid[dtd + 5] & 0xFF;
        return horizontalRes + "x" + verticalRes;
    }

    /**
     * Get the monitor model from the EDID
     *
     * @param edid The edid Byte array
     * @return Plain text monitor model
     */

    public static String getModel(byte[] edid) {

        byte[][] desc = EdidUtil.getDescriptors(edid);
        String model = null;

        for (byte[] b : desc) {

            if (EdidUtil.getDescriptorType(b) == 0xfc) {
                model = EdidUtil.getDescriptorText(b);
                break;
            }
        }

        assert model != null;
        String[] tokens = model.split("\\s+");
        if (tokens.length >= 1) {
            model = tokens[tokens.length - 1];
        }
        return model.trim();
    }

    public static List<String> parseEDIDResolutions(byte[] edid) {
        Set<String> resolutions = new LinkedHashSet<>();

        // --- 1. Established Timings ---
        byte t1 = edid[0x23];
        byte t2 = edid[0x24];
        byte t3 = edid[0x25];

        String[] establishedTimings = {
            "720x400", "720x400", "640x480", "640x480", "640x480", "640x480", "800x600", "800x600",
            "800x600", "800x600", "832x624", "1024x768", "1024x768", "1024x768", "1280x1024", "1152x870"
        };

        for (int i = 0; i < 8; i++) if ((t1 & (1 << (7 - i))) != 0) resolutions.add(establishedTimings[i]);
        for (int i = 0; i < 8; i++) if ((t2 & (1 << (7 - i))) != 0) resolutions.add(establishedTimings[i + 8]);
        if ((t3 & 0x80) != 0) resolutions.add(establishedTimings[15]);

        // --- 2. Standard Timings ---
        for (int i = 0x26; i <= 0x35; i += 2) {
            int b1 = edid[i] & 0xFF;
            int b2 = edid[i + 1] & 0xFF;
            if (b1 == 0x01 && b2 == 0x01) continue;

            int hActive = (b1 + 31) * 8;
            int aspectBits = (b2 >> 6) & 0x03;
            double aspectRatio;
            if (aspectBits == 0) {
                aspectRatio = 16.0 / 10;
            } else if (aspectBits == 1) {
                aspectRatio = 4.0 / 3;
            } else if (aspectBits == 2) {
                aspectRatio = 5.0 / 4;
            } else if (aspectBits == 3) {
                aspectRatio = 16.0 / 9;
            } else {
                aspectRatio = 1.0;
            }
            int vActive = (int) Math.round(hActive / aspectRatio);
            resolutions.add(hActive + "x" + vActive);
        }

        // --- 3. Detailed Timing Descriptors (DTDs) ---
        for (int i = 0x36; i <= 0x6C; i += 18) {
            int pixelClock = ((edid[i + 1] & 0xFF) << 8) | (edid[i] & 0xFF);
            if (pixelClock == 0) continue;

            int hActive = ((edid[i + 4] & 0xF0) << 4) | (edid[i + 2] & 0xFF);
            int vActive = ((edid[i + 7] & 0xF0) << 4) | (edid[i + 5] & 0xFF);

            if (hActive > 300 && vActive > 200 && hActive < 8000 && vActive < 8000)
                resolutions.add(hActive + "x" + vActive);
        }

        // --- 4. CTA-861 Extension Block ---
        int numExtensions = edid[0x7E] & 0xFF;
        if (numExtensions > 0 && edid.length >= 256) {
            int base = 128;
            if (edid[base] == 0x02) { // CTA-861 tag
                int dtdStart = edid[base + 2] & 0xFF;
                int i = base + 4;
                while (i < base + dtdStart) {
                    int tag = (edid[i] & 0xE0) >> 5;
                    int len = edid[i] & 0x1F;
                    if (tag == 0x02) { // Video Data Block (VDB)
                        for (int j = 1; j <= len; j++) {
                            int vic = edid[i + j] & 0x7F; // 7-bit VIC
                            String res = getResolutionForVIC(vic);
                            if (res != null) resolutions.add(res);
                        }
                    }
                    i += (1 + len);
                }
            }
        }

        return new ArrayList<>(resolutions);
    }

    // Maps CTA-861 VIC codes to common resolutions
    private static String getResolutionForVIC(int vic) {
        if (vic == 1) {
            return "640x480";
        } else if (vic == 2 || vic == 3) {
            return "720x480";
        } else if (vic == 4 || vic == 19) {
            return "1280x720";
        } else if (vic == 5 || vic == 20) {
            return "1920x1080i";
        } else if (vic == 16 || vic == 31) {
            return "1920x1080";
        } else if (vic == 17 || vic == 18) {
            return "720x576";
        } else {
            return null;
        }
    }

    // Example usage
    public static void main(String[] args) {
        byte[] edid = new byte[] {
            (byte)0x00, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0x00,
            (byte)0x10, (byte)0xac, (byte)0x13, (byte)0x41, (byte)0x4c, (byte)0x57, (byte)0x53, (byte)0x42,
            (byte)0x16, (byte)0x1c, (byte)0x01, (byte)0x04, (byte)0xa5, (byte)0x35, (byte)0x1e, (byte)0x78,
            (byte)0x3e, (byte)0xee, (byte)0x95, (byte)0xa3, (byte)0x54, (byte)0x4c, (byte)0x99, (byte)0x26,
            (byte)0x0f, (byte)0x50, (byte)0x54, (byte)0xa5, (byte)0x4b, (byte)0x80, (byte)0x71, (byte)0x4f,
            (byte)0x81, (byte)0x00, (byte)0x81, (byte)0x80, (byte)0xa9, (byte)0xc0, (byte)0xd1, (byte)0xc0,
            (byte)0x01, (byte)0x01, (byte)0x01, (byte)0x01, (byte)0x02, (byte)0x3a, (byte)0x80, (byte)0x18,
            (byte)0x71, (byte)0x38, (byte)0x2d, (byte)0x40, (byte)0x58, (byte)0x2c, (byte)0x45, (byte)0x00,
            (byte)0x0f, (byte)0x28, (byte)0x21, (byte)0x00, (byte)0x00, (byte)0x1e, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0xff, (byte)0x00, (byte)0x54, (byte)0x56, (byte)0x54, (byte)0x37, (byte)0x46,
            (byte)0x38, (byte)0x35, (byte)0x55, (byte)0x42, (byte)0x53, (byte)0x57, (byte)0x4c, (byte)0x0a,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0xfc, (byte)0x00, (byte)0x44, (byte)0x45, (byte)0x4c,
            (byte)0x4c, (byte)0x20, (byte)0x50, (byte)0x32, (byte)0x34, (byte)0x31, (byte)0x38, (byte)0x48,
            (byte)0x54, (byte)0x0a, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0xfd, (byte)0x00, (byte)0x32,
            (byte)0x4c, (byte)0x1e, (byte)0x53, (byte)0x11, (byte)0x00, (byte)0x0a, (byte)0x20, (byte)0x20,
            (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x01, (byte)0x7e, (byte)0x02, (byte)0x03,
            (byte)0x18, (byte)0xf1, (byte)0x4b, (byte)0x90, (byte)0x05, (byte)0x04, (byte)0x03, (byte)0x02,
            (byte)0x01, (byte)0x11, (byte)0x12, (byte)0x13, (byte)0x14, (byte)0x1f, (byte)0x23, (byte)0x09,
            (byte)0x07, (byte)0x07, (byte)0x83, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x02, (byte)0x3a,
            (byte)0x80, (byte)0x18, (byte)0x71, (byte)0x38, (byte)0x2d, (byte)0x40, (byte)0x58, (byte)0x2c,
            (byte)0x45, (byte)0x00, (byte)0x0f, (byte)0x28, (byte)0x21, (byte)0x00, (byte)0x00, (byte)0x1e,
            (byte)0x01, (byte)0x1d, (byte)0x80, (byte)0x18, (byte)0x71, (byte)0x1c, (byte)0x16, (byte)0x20,
            (byte)0x58, (byte)0x2c, (byte)0x25, (byte)0x00, (byte)0x0f, (byte)0x28, (byte)0x21, (byte)0x00,
            (byte)0x00, (byte)0x9e, (byte)0x01, (byte)0x1d, (byte)0x00, (byte)0x72, (byte)0x51, (byte)0xd0,
            (byte)0x1e, (byte)0x20, (byte)0x6e, (byte)0x28, (byte)0x55, (byte)0x00, (byte)0x0f, (byte)0x28,
            (byte)0x21, (byte)0x00, (byte)0x00, (byte)0x1e, (byte)0x8c, (byte)0x0a, (byte)0xd0, (byte)0x8a,
            (byte)0x20, (byte)0xe0, (byte)0x2d, (byte)0x10, (byte)0x10, (byte)0x3e, (byte)0x96, (byte)0x00,
            (byte)0x0f, (byte)0x28, (byte)0x21, (byte)0x00, (byte)0x00, (byte)0x18, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0xcf
        };

        List<String> resolutions = parseEDIDResolutions(edid);
        System.out.println("Supported Resolutions:");
        for (String res : resolutions) {
            System.out.println(res);
        }
    }

    /**
     * Parse an EDID byte array into user-readable information
     *
     * @param edid An EDID byte array
     * @return User-readable text represented by the EDID
     */
    public static String toString(byte[] edid) {
        StringBuilder sb = new StringBuilder();
        sb.append("  Manuf. ID=").append(EdidUtil.getManufacturerID(edid));
        sb.append(", Product ID=").append(EdidUtil.getProductID(edid));
        sb.append(", ").append(EdidUtil.isDigital(edid) ? "Digital" : "Analog");
        sb.append(", Serial=").append(EdidUtil.getSerialNo(edid));
        sb.append(", ManufDate=").append(EdidUtil.getWeek(edid) * 12 / 52 + 1).append('/')
                .append(EdidUtil.getYear(edid));
        sb.append(", EDID v").append(EdidUtil.getVersion(edid));
        int hSize = EdidUtil.getHcm(edid);
        int vSize = EdidUtil.getVcm(edid);
        sb.append(String.format(Locale.ROOT, "%n  %d x %d cm (%.1f x %.1f in)", hSize, vSize, hSize / 2.54,
                vSize / 2.54));
        byte[][] desc = EdidUtil.getDescriptors(edid);
        for (byte[] b : desc) {
            switch (EdidUtil.getDescriptorType(b)) {
            case 0xff:
                sb.append("\n  Serial Number: ").append(EdidUtil.getDescriptorText(b));
                break;
            case 0xfe:
                sb.append("\n  Unspecified Text: ").append(EdidUtil.getDescriptorText(b));
                break;
            case 0xfd:
                sb.append("\n  Range Limits: ").append(EdidUtil.getDescriptorRangeLimits(b));
                break;
            case 0xfc:
                sb.append("\n  Monitor Name: ").append(EdidUtil.getDescriptorText(b));
                break;
            case 0xfb:
                sb.append("\n  White Point Data: ").append(ParseUtil.byteArrayToHexString(b));
                break;
            case 0xfa:
                sb.append("\n  Standard Timing ID: ").append(ParseUtil.byteArrayToHexString(b));
                break;
            default:
                if (EdidUtil.getDescriptorType(b) <= 0x0f && EdidUtil.getDescriptorType(b) >= 0x00) {
                    sb.append("\n  Manufacturer Data: ").append(ParseUtil.byteArrayToHexString(b));
                } else {
                    sb.append("\n  Preferred Timing: ").append(EdidUtil.getTimingDescriptor(b));
                }
                break;
            }
        }
        return sb.toString();
    }
}
