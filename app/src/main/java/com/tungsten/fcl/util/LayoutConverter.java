package com.tungsten.fcl.util;

import android.os.Build;
import java.io.File;

/** JNI facade matching the upstream control-converter ABI. */
public final class LayoutConverter {
    private static final Throwable LOAD_ERROR;

    static {
        Throwable error = null;
        try {
            System.loadLibrary("cc");
        } catch (Throwable t) {
            error = t;
        }
        LOAD_ERROR = error;
    }

    private LayoutConverter() {}

    public static boolean isSupported() {
        if (LOAD_ERROR != null) return false;
        for (String abi : Build.SUPPORTED_ABIS) {
            if ("arm64-v8a".equals(abi)) return true;
        }
        return false;
    }

    public static Throwable loadFailure() {
        return LOAD_ERROR;
    }

    /** JNI method exported by the latest control-converter libcc.so. */
    public static native String convertFclToZl2Native(String inputPath, String outputPath);

    /** JNI method for ZL2 -> FCL conversion (added in the extended libcc.so). */
    public static native String convertZl2ToFclNative(String inputPath, String outputPath);

    public static String convertFclToZl2(File input, File output) {
        if (!isSupported()) {
            throw new IllegalStateException("官方 control-converter libcc.so 不可用：" +
                    (LOAD_ERROR == null ? "仅支持 arm64-v8a" : LOAD_ERROR.getMessage()));
        }
        return convertFclToZl2Native(input.getAbsolutePath(), output.getAbsolutePath());
    }

    public static String convertZl2ToFcl(File input, File output) {
        if (!isSupported()) {
            throw new IllegalStateException("官方 control-converter libcc.so 不可用：" +
                    (LOAD_ERROR == null ? "仅支持 arm64-v8a" : LOAD_ERROR.getMessage()));
        }
        return convertZl2ToFclNative(input.getAbsolutePath(), output.getAbsolutePath());
    }
}
