package com.ibm.wh.extractionservice.support;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;

public class NlpUtils {

    private static final Map<String, Integer> numbersMap;

    static {
        numbersMap = Maps.newHashMap();
        numbersMap.put("zero", 0);
        numbersMap.put("one", 1);
        numbersMap.put("two", 2);
        numbersMap.put("three", 3);
        numbersMap.put("four", 4);
        numbersMap.put("five", 5);
        numbersMap.put("six", 6);
        numbersMap.put("seven", 7);
        numbersMap.put("eight", 8);
        numbersMap.put("nine", 9);
        numbersMap.put("ten", 10);
        numbersMap.put("eleven", 11);
        numbersMap.put("twelve", 12);
    }

    public static int wordsCount(String text) {
        if (text == null || text.isEmpty()) return 0;
        return new StringTokenizer(text).countTokens();
    }

    // Numeric values helpers

    public static int integerValueOf(String text) throws NumberFormatException {
        if (StringUtils.isNumeric(cleanText(text))) return Integer.valueOf(cleanText(text));
        return textToInteger(text).orElseThrow(() -> new NumberFormatException(String.format("Integer value not supported for text '%s'", text)));
    }

    private static Optional<Integer> textToInteger(String text) {
        return Optional.ofNullable(numbersMap.get(cleanText(text)));
    }

    public static boolean isInteger(String text) {
        text = cleanText(text);
        return StringUtils.isNumeric(text.toLowerCase()) || textToInteger(text).isPresent();
    }

    public static boolean isNumeric(String text) {
        try {
            Double.parseDouble(text);
        } catch (NumberFormatException | NullPointerException nfe) {
            return false;
        }
        return true;
    }

    private static String cleanText (String text) {
        if (text.startsWith("(") && text.endsWith(")")) {
            text = text.replace("(", "");
            text = text.replace(")", "");
        }
        return text.toLowerCase().trim();
    }

}
