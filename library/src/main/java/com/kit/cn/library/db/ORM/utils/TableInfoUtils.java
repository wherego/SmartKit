package com.kit.cn.library.db.ORM.utils;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.kit.cn.library.db.ORM.annotation.TableName;
import com.kit.cn.library.db.ORM.extra.TableColumn;
import com.kit.cn.library.db.ORM.extra.TableInfo;
import com.kit.cn.library.utils.log.L;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author zhouwen
 * @version 0.1
 * @since 2016/07/26
 */
public class TableInfoUtils {

    public static final String TAG = "TableInfoUtils";

    private static final HashMap<String, TableInfo> tableInfoMap;

    static {
        tableInfoMap = new HashMap<String, TableInfo>();
    }

    public static <T> TableInfo exist(String dbName, Class<T> clazz) {
        return tableInfoMap.get(dbName + "-" + getTableName(clazz));
    }

    /**
     * 当没有注解的时候默认用类的名称作为表名,并把点（.）替换为下划线(_)
     *
     * @param clazz
     * @return
     */
    public static String getTableName(Class<?> clazz) {
        TableName table = clazz.getAnnotation(TableName.class);
        if (table == null || table.table().trim().length() == 0) {
            return clazz.getName().replace('.', '_');
        }
        return table.table();
    }

    public static <T> TableInfo newTable(String dbName, SQLiteDatabase db, Class<T> clazz) {
        Cursor cursor = null;

        TableInfo tableInfo = new TableInfo(clazz);
        tableInfoMap.put(dbName + "-" + getTableName(clazz), tableInfo);

        try {
            // 检查表是否存在
            String sql = "SELECT COUNT(*) AS c FROM sqlite_master WHERE type ='table' AND name ='" + tableInfo.getTableName() + "' ";

            cursor = db.rawQuery(sql, null);
            if (cursor != null && cursor.moveToNext()) {
                int count = cursor.getInt(0);
                if (count > 0) {
                    cursor.close();
                    L.d(TAG, "表 %s 已存在", tableInfo.getTableName());

                    cursor = db.rawQuery("PRAGMA table_info" + "(" + tableInfo.getTableName() + ")", null);
                    // table的所有字段名称
                    List<String> tableColumns = new ArrayList<String>();
                    if (cursor != null && cursor.moveToNext()) {
                        do {
                            tableColumns.add(cursor.getString(cursor.getColumnIndex("name")));
                        } while (cursor.moveToNext());
                    }
                    if (cursor != null) {
                        cursor.close();
                    }

                    // 检查新对象的是否更新
                    List<String> properList = new ArrayList<String>();
                    for (TableColumn column : tableInfo.getColumns()) {
                        properList.add(column.getColumn());
                    }

                    // 如果有新增字段，自动添加，暂时不能删除字段
                    List<String> newFieldList = new ArrayList<String>();
                    for (String field : properList) {
                        if (tableInfo.getPrimaryKey().getColumn().equals(field))
                            continue;

                        boolean isNew = true;

                        for (String tableColumn : tableColumns) {
                            if (tableColumn.equals(field)) {
                                isNew = false;
                                break;
                            }
                        }

                        if (isNew)
                            newFieldList.add(field);
                    }

                    for (String newField : newFieldList) {
                        db.execSQL(String.format("ALTER TABLE %s ADD %s TEXT", tableInfo.getTableName(), newField));
                        L.d(TAG, "表 %s 新增字段 %s", tableInfo.getTableName(), newField);
                    }

                    return tableInfo;
                }
            }

            // 创建一张新的表
            String createSql = SqlUtils.getTableSql(tableInfo);
            db.execSQL(createSql);
            L.d(TAG, "创建一张新表 %s", tableInfo.getTableName());
        } catch (Exception e) {
            e.printStackTrace();

            L.d(TAG, e.getMessage() + "");
        } finally {
            if (cursor != null)
                cursor.close();
            cursor = null;
        }

        return tableInfo;
    }

}
