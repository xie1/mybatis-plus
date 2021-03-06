/*
 * Copyright (c) 2011-2020, hubin (jobob@qq.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.baomidou.mybatisplus.core.metadata;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.TableInfoHelper;
import lombok.Data;
import lombok.experimental.Accessors;

import java.lang.reflect.Field;

/**
 * <p>
 * 数据库表字段反射信息
 * </p>
 *
 * @author hubin sjy willenfoo tantan
 * @since 2016-09-09
 */
@Data
@Accessors(chain = true)
public class TableFieldInfo {

    /**
     * 是否有存在字段名与属性名关联
     * true: 表示要进行 as
     */
    private boolean related = false;
    /**
     * 字段名
     */
    private String column;
    /**
     * 属性名
     */
    private String property;
    /**
     * 属性表达式#{property}, 可以指定jdbcType, typeHandler等
     */
    private String el;
    /**
     * 属性类型
     */
    private Class<?> propertyType;
    /**
     * 字段策略【 默认，自判断 null 】
     */
    private FieldStrategy fieldStrategy;
    /**
     * 逻辑删除值
     */
    private String logicDeleteValue;
    /**
     * 逻辑未删除值
     */
    private String logicNotDeleteValue;
    /**
     * 字段 update set 部分注入
     */
    private String update;
    /**
     * where 字段比较条件
     */
    private String condition = SqlCondition.EQUAL;
    /**
     * 字段填充策略
     */
    private FieldFill fieldFill = FieldFill.DEFAULT;
    /**
     * 标记该字段属于哪个类
     */
    private Class<?> clazz;

    /**
     * <p>
     * 存在 TableField 注解构造函数
     * </p>
     */
    public TableFieldInfo(GlobalConfig.DbConfig dbConfig, TableInfo tableInfo, Field field,
                          String column, String el, TableField tableField) {
        this.property = field.getName();
        this.propertyType = field.getType();
        this.fieldFill = tableField.fill();
        this.clazz = field.getDeclaringClass();
        this.update = tableField.update();
        this.el = el;
        tableInfo.setLogicDelete(this.initLogicDelete(dbConfig, field));

        if (StringUtils.isEmpty(tableField.value())) {
            if (dbConfig.isColumnUnderline()) {
                column = StringUtils.camelToUnderline(column);
            }
        }
        this.column = column;
        this.related = TableInfoHelper.checkRelated(dbConfig.isColumnUnderline(), this.property, this.column);

        /*
         * 优先使用单个字段注解，否则使用全局配置
         */
        if (dbConfig.getFieldStrategy() != tableField.strategy()) {
            this.fieldStrategy = tableField.strategy();
        } else {
            this.fieldStrategy = dbConfig.getFieldStrategy();
        }

        if (StringUtils.isNotEmpty(tableField.condition())) {
            // 细粒度条件控制
            this.condition = tableField.condition();
        } else {
            // 全局配置
            this.setCondition(dbConfig);
        }
    }

    public TableFieldInfo(GlobalConfig.DbConfig dbConfig, TableInfo tableInfo, Field field) {
        this.property = field.getName();
        this.el = field.getName();
        this.fieldStrategy = dbConfig.getFieldStrategy();
        this.propertyType = field.getType();
        this.setCondition(dbConfig);
        this.clazz = field.getDeclaringClass();
        tableInfo.setLogicDelete(this.initLogicDelete(dbConfig, field));

        String column = field.getName();
        if (dbConfig.isColumnUnderline()) {
            /* 开启字段下划线申明 */
            column = StringUtils.camelToUnderline(column);
        }
        if (dbConfig.isCapitalMode()) {
            /* 开启字段全大写申明 */
            column = column.toUpperCase();
        }
        this.column = column;
        this.related = TableInfoHelper.checkRelated(dbConfig.isColumnUnderline(), this.property, this.column);
    }

    /**
     * <p>
     * 逻辑删除初始化
     * </p>
     *
     * @param dbConfig 数据库全局配置
     * @param field    字段属性对象
     */
    private boolean initLogicDelete(GlobalConfig.DbConfig dbConfig, Field field) {
        if (null == dbConfig.getLogicDeleteValue()) {
            // 未设置逻辑删除值不进行
            return false;
        }
        /* 获取注解属性，逻辑处理字段 */
        TableLogic tableLogic = field.getAnnotation(TableLogic.class);
        if (null != tableLogic) {
            if (StringUtils.isNotEmpty(tableLogic.value())) {
                this.logicNotDeleteValue = tableLogic.value();
            } else {
                this.logicNotDeleteValue = dbConfig.getLogicNotDeleteValue();
            }
            if (StringUtils.isNotEmpty(tableLogic.delval())) {
                this.logicDeleteValue = tableLogic.delval();
            } else {
                this.logicDeleteValue = dbConfig.getLogicDeleteValue();
            }
            return true;
        }
        return false;
    }

    public boolean isRelated() {
        return related;
    }

    /**
     * 是否开启逻辑删除
     */
    public boolean isLogicDelete() {
        return StringUtils.isNotEmpty(logicDeleteValue);
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public void setCondition(GlobalConfig.DbConfig dbConfig) {
        /**
         * 全局配置开启字段 LIKE 并且为字符串类型字段
         * 注入 LIKE 查询！！！
         */
        if (null == this.condition || SqlCondition.EQUAL.equals(this.condition)) {
            if (dbConfig.isColumnLike() && StringUtils.isCharSequence(this.propertyType)) {
                this.condition = dbConfig.getDbType().getLike();
            }
        }
    }
}
