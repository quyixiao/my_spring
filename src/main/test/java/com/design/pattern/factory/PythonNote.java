package com.design.pattern.factory;

public class PythonNote implements INote {
    @Override
    public void edit() {
        System.out.println("编写python 笔记");
    }
}
