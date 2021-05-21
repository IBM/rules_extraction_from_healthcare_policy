package com.ibm.wh.extractionservice.support;

import java.util.Optional;

public class StreamUtils {

    public static <T> Optional<T> getLast(Optional<T> first, Optional<T> second) {
        return second;
    }

}
