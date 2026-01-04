package com.suchi.musicisland.Utils;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import java.util.Locale;

public class Util {
    public static String formatTime(int ms) {
        int totalSeconds = ms / 1000;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    //转换track to int
    public static int parseTrackNumber(String trackStr) {
        if (trackStr == null) return 0;

        trackStr = trackStr.trim();
        if (trackStr.isEmpty()) return 0;

        // 常见格式： "1/12"、"03/12"
        if (trackStr.contains("/")) {
            trackStr = trackStr.substring(0, trackStr.indexOf("/"));
        }

        // 兜底：提取前面的数字
        StringBuilder number = new StringBuilder();
        for (int i = 0; i < trackStr.length(); i++) {
            char c = trackStr.charAt(i);
            if (Character.isDigit(c)) {
                number.append(c);
            } else if (number.length() > 0) {
                break;
            }
        }

        try {
            return Integer.parseInt(number.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static int dpToPx(Context context, int dp) {
        return Math.round(
                dp * context.getResources().getDisplayMetrics().density
        );
    }

    public static class ToastUtil {
        public static void showShortToast(Context context, String message, int durationMillis) {
            Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
            toast.show();

            new Handler().postDelayed(toast::cancel, durationMillis);
        }
    }
}
