package com.kit.cn.smartkit;


import android.os.Bundle;

import com.kit.cn.library.pagekit.ActivityWrapper;
import com.kit.cn.library.pagekit.FragmentWrapper;


/**
 * User: zhouwen
 * Date: 2016-08-1
 * Time: 09:48
 */
public class MainActivity extends ActivityWrapper {

    @Override
    protected FragmentWrapper getRootFragment() {
        return new ComponentHomeFragment();
    }

    @Override
    public void onCreateFirst(Bundle savedInstanceState) {
        setAnim(R.anim.next_in, R.anim.next_out, R.anim.quit_in, R.anim.quit_out);
    }

    /**
     * Set the time to click to Prevent repeated clicks,default 500ms
     *
     * @param clickInterval Repeat click time(ms)
     */
    public void setClickSpace(long clickInterval) {
        mManager.setClickSpace(clickInterval);
    }
}
