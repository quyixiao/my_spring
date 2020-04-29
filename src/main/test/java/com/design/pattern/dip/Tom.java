package com.design.pattern.dip;

public class Tom {


    public void studyJavaCourse() {
        System.out.println("Tom 在学习Java课程");

    }


    public void studyPythonCourse() {
        System.out.println("Tom 在学习Python 课程");
    }



    public void study(ICourse iCourse){
        iCourse.study();
    }

    public static void main(String[] args) {
        Tom tom = new Tom();
        tom.studyJavaCourse();
        tom.studyPythonCourse();
    }
}
