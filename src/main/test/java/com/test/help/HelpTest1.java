package com.test.help;

import org.springframework.util.PropertyPlaceholderHelper;

public class HelpTest1 {

    public static void main(String[] args) {
        String text = "classpath:spring.xml";
        PropertyPlaceholderHelper helper = new PropertyPlaceholderHelper("${", "}",
                ":", false);
        helper.replacePlaceholders(text, new PropertyPlaceholderHelper.PlaceholderResolver() {
            @Override
            public String resolvePlaceholder(String placeholderName) {
                System.out.println("xxxxxxxxxxxxxxxx");
                return "";
            }
        });
    }
}
