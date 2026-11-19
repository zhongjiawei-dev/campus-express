package com.campus.express.common;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 敏感词过滤：备注、评价等 UGC 内容入库前统一处理。
 * 演示版用词表替换，生产可替换为 DFA / 第三方内容安全服务。
 */
@Component
public class SensitiveWordFilter {

    private static final List<String> WORDS = List.of(
            "傻逼", "操你", "他妈的", "赌博", "博彩", "刷单", "代考", "毒品", "枪支", "违禁品",
            "诈骗", "洗钱", "裸聊", "约炮", "贷款套现");

    public String sanitize(String text) {
        if (text == null) {
            return null;
        }
        String result = text;
        for (String word : WORDS) {
            if (result.contains(word)) {
                result = result.replace(word, "**");
            }
        }
        return result.length() > 100 ? result.substring(0, 100) : result;
    }

    public boolean hit(String text) {
        if (text == null) {
            return false;
        }
        return WORDS.stream().anyMatch(text::contains);
    }

    public String maskPhone(String text) {
        return Pattern.compile("1[3-9]\\d{9}")
                .matcher(text == null ? "" : text)
                .replaceAll(m -> m.group().substring(0, 3) + "****" + m.group().substring(7));
    }
}
