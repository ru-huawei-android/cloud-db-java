/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2019-2019. All rights reserved.
 * Generated by the CloudDB ObjectType compiler.  DO NOT EDIT!
 */
package com.huawei.agc.clouddb.java.model;

import com.huawei.agconnect.cloud.database.ObjectTypeInfo;

import java.util.Arrays;

/**
 * Definition of ObjectType Helper.
 * Class Autogenerated from the console (developer account)
 *
 * @since 2020-05-19
 */
public class ObjectTypeInfoHelper {
    private final static int FORMAT_VERSION = 1;
    private final static int OBJECT_TYPE_VERSION = 5;

    public static ObjectTypeInfo getObjectTypeInfo() {
        ObjectTypeInfo objectTypeInfo = new ObjectTypeInfo();
        objectTypeInfo.setFormatVersion(FORMAT_VERSION);
        objectTypeInfo.setObjectTypeVersion(OBJECT_TYPE_VERSION);
        objectTypeInfo.setObjectTypes(Arrays.asList(Book.class));
        return objectTypeInfo;
    }
}
