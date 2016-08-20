package com.kit.cn.smartkit.base;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.kit.cn.library.ioc.AnnotationProcessor;
import com.kit.cn.library.ioc.ViewFinder;

import java.lang.reflect.InvocationTargetException;

/**
 * activity 基类
 * @author zhouwen
 * @version 0.1
 * @since 16/7/23
 */
public class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ViewFinder viewFinder = new ViewFinder() {
            public View findViewById(int id) { return BaseActivity.this.findViewById(id); }
        };

        AnnotationProcessor.getInstance().invokeContentView(getClass(), this);
        try {
            AnnotationProcessor.getInstance().invokeChildViews(getClass(), this, viewFinder);
            AnnotationProcessor.getInstance().invokeString(getBaseContext(),getClass(),this,viewFinder);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
