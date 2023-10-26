package com.cleverpush.util;

public class VoucherCodeUtils {

  public static String replaceVoucherCodeString(String text, String voucherCode) {
    try {
      if (voucherCode != null && !voucherCode.isEmpty() && text.contains("{voucherCode}")) {
        text = text.replace("{voucherCode}", voucherCode);
      }
      return text;
    } catch (Exception e) {
      Logger.e("CleverPush", "replaceVoucherCodeString: " + e.getLocalizedMessage());
      return text;
    }
  }
}
