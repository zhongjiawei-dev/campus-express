package com.campus.express.common;

public final class MaskUtil {

    private MaskUtil() {
    }

    public static String phone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    public static String name(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        return name.charAt(0) + "**";
    }

    public static String tail(String value, int keep) {
        if (value == null) {
            return null;
        }
        if (value.length() <= keep) {
            return value;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length() - keep; i++) {
            sb.append('*');
        }
        return sb + value.substring(value.length() - keep);
    }

    public static String idCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 4);
    }
}
