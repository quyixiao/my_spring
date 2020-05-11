package com.test3.cn35.impl;

import com.test3.cn35.AnnotationService;
import org.springframework.stereotype.Service;

@Service
public class AnnotationServiceImpl implements AnnotationService {

    @Override
    public void save() {
        System.out.println("保存");
    }
}
