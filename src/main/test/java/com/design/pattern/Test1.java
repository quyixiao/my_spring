package com.design.pattern;

import org.springframework.util.AntPathMatcher;

public class Test1 {


    public static void main(String[] args) {
        String a = "classpath*:com/**/***/*/*.class";
        determineRootDir(a);
    }

    protected static  String determineRootDir(String location) {
        int prefixEnd = location.indexOf(":") + 1;
        int rootDirEnd = location.length();
        System.out.println(rootDirEnd);
        while (rootDirEnd > prefixEnd &&  new AntPathMatcher().isPattern(location.substring(prefixEnd, rootDirEnd))) {
            rootDirEnd = location.lastIndexOf('/', rootDirEnd - 2) + 1;
            System.out.println(location.substring(0,rootDirEnd));
            System.out.println(rootDirEnd);
        }
        if (rootDirEnd == 0) {
            rootDirEnd = prefixEnd;
        }
        return location.substring(0, rootDirEnd);
    }


}
