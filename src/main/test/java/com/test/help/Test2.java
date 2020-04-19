package com.test.help;

import org.springframework.util.StringUtils;

import java.util.Arrays;

public class Test2 {

    public static final String MULTI_VALUE_ATTRIBUTE_DELIMITERS = ",; ";

    public static void main(String[] args) {
        String nameAttr = "a,b,c;d;e;f; hahahaha | xxx";
        String[] nameArr = StringUtils.tokenizeToStringArray(nameAttr, MULTI_VALUE_ATTRIBUTE_DELIMITERS);
        System.out.println(Arrays.toString(nameArr));
    }
}
