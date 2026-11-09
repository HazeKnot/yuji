package com.yuji.family;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;

/**
 * 纯 Mockito 单测里没有 MyBatis 容器，LambdaQueryWrapper 的方法引用需要 TableInfo 缓存，
 * 否则会抛 "can not find lambda cache for this entity"。这里手工初始化一次。
 */
public final class TableInfoInitializer {

    private TableInfoInitializer() {
    }

    public static void init(Class<?>... entities) {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        for (Class<?> entity : entities) {
            if (TableInfoHelper.getTableInfo(entity) == null) {
                TableInfoHelper.initTableInfo(assistant, entity);
            }
        }
    }
}
