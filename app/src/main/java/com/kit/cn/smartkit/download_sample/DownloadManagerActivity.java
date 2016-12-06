package com.kit.cn.smartkit.download_sample;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.format.Formatter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.kit.cn.downloadlib.download.download.DownloadInfo;
import com.kit.cn.downloadlib.download.download.DownloadManager;
import com.kit.cn.downloadlib.download.download.DownloadService;
import com.kit.cn.downloadlib.download.listener.DownloadListener;
import com.kit.cn.downloadlib.download.task.ExecutorWithListener;
import com.kit.cn.library.utils.log.L;
import com.kit.cn.smartkit.R;
import com.kit.cn.smartkit.network_sample.Bean.ApkInfo;
import com.kit.cn.smartkit.network_sample.ui.NumberProgressBar;
import com.kit.cn.smartkit.network_sample.utils.ApkUtils;
import com.kit.cn.smartkit.network_sample.utils.AppCacheUtils;


import java.io.File;
import java.util.List;

public class DownloadManagerActivity extends AppCompatActivity implements View.OnClickListener, ExecutorWithListener.OnAllTaskEndListener {

    private List<DownloadInfo> mAllTask;
    private MyAdapter mAdapter;
    private DownloadManager mDownloadManager;


    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder;
    private Intent mIntent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_manager);

        mDownloadManager = DownloadService.getDownloadManager();
        mAllTask = mDownloadManager.getAllTask();
        ListView listView = (ListView) findViewById(R.id.listView);
        mAdapter = new MyAdapter();
        listView.setAdapter(mAdapter);

        mDownloadManager.getThreadPool().getExecutor().addOnAllTaskEndListener(this);

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(DownloadManagerActivity.this)
                .setSmallIcon(R.mipmap.ic_launcher);

        mBuilder.build().defaults =  Notification.DEFAULT_SOUND;

        // 指定个性化视图
//        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.download_notification_layout);
//        contentView.setTextViewText(R.id.fileName, "AngryBird.apk");
//        mBuilder.build().contentView = contentView;
    }

    @Override
    public void onAllTaskEnd() {
        for (DownloadInfo downloadInfo : mAllTask) {
            if (downloadInfo.getState() != DownloadManager.FINISH) {
                Toast.makeText(DownloadManagerActivity.this, "所有下载线程结束，部分下载未完成", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        Toast.makeText(DownloadManagerActivity.this, "所有下载任务完成", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //记得移除，否者会回调多次
        mDownloadManager.getThreadPool().getExecutor().removeOnAllTaskEndListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAdapter.notifyDataSetChanged();
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.removeAll:
                mDownloadManager.removeAllTask();
                mAdapter.notifyDataSetChanged();  //移除的时候需要调用
                break;
            case R.id.pauseAll:
                mDownloadManager.pauseAllTask();
                break;
            case R.id.stopAll:
                mDownloadManager.stopAllTask();
                break;
            case R.id.startAll:
                mDownloadManager.startAllTask();
                break;
        }
    }
    ApkInfo apk;
    private class MyAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mAllTask.size();
        }

        @Override
        public DownloadInfo getItem(int position) {
            return mAllTask.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            DownloadInfo downloadInfo = getItem(position);
            ViewHolder holder;
            if (convertView == null) {
                convertView = View.inflate(DownloadManagerActivity.this, R.layout.item_download_manager, null);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.refresh(downloadInfo);

            //对于非进度更新的ui放在这里，对于实时更新的进度ui，放在holder中
            apk = (ApkInfo) AppCacheUtils.getInstance(DownloadManagerActivity.this).getObject(downloadInfo.getUrl());
            if (apk != null) {
                Glide.with(DownloadManagerActivity.this).load(apk.getIconUrl()).error(R.mipmap.ic_launcher).into(holder.icon);
                holder.name.setText(apk.getName());
            } else {
                holder.name.setText(downloadInfo.getFileName());
            }
            holder.download.setOnClickListener(holder);
            holder.remove.setOnClickListener(holder);
            holder.restart.setOnClickListener(holder);

            DownloadListener downloadListener = new MyDownloadListener();
            downloadListener.setUserTag(holder);
            downloadInfo.setListener(downloadListener);
            return convertView;
        }
    }

    private class ViewHolder implements View.OnClickListener {
        private DownloadInfo downloadInfo;
        private ImageView icon;
        private TextView name;
        private TextView downloadSize;
        private TextView tvProgress;
        private TextView netSpeed;
        private NumberProgressBar pbProgress;
        private Button download;
        private Button remove;
        private Button restart;

        public ViewHolder(View convertView) {
            icon = (ImageView) convertView.findViewById(R.id.icon);
            name = (TextView) convertView.findViewById(R.id.name);
            downloadSize = (TextView) convertView.findViewById(R.id.downloadSize);
            tvProgress = (TextView) convertView.findViewById(R.id.tvProgress);
            netSpeed = (TextView) convertView.findViewById(R.id.netSpeed);
            pbProgress = (NumberProgressBar) convertView.findViewById(R.id.pbProgress);
            download = (Button) convertView.findViewById(R.id.start);
            remove = (Button) convertView.findViewById(R.id.remove);
            restart = (Button) convertView.findViewById(R.id.restart);
        }

        public void refresh(DownloadInfo downloadInfo) {
            this.downloadInfo = downloadInfo;
            refresh();
        }

        //对于实时更新的进度ui，放在这里，例如进度的显示，而图片加载等，不要放在这，会不停的重复回调
        //也会导致内存泄漏
        private void refresh() {
            String downloadLength = Formatter.formatFileSize(DownloadManagerActivity.this, downloadInfo.getDownloadLength());
            String totalLength = Formatter.formatFileSize(DownloadManagerActivity.this, downloadInfo.getTotalLength());
            downloadSize.setText(downloadLength + "/" + totalLength);
            if (downloadInfo.getState() == DownloadManager.NONE) {
                netSpeed.setText("停止");
                download.setText("下载");
            } else if (downloadInfo.getState() == DownloadManager.PAUSE) {
                netSpeed.setText("暂停中");
                download.setText("继续");
            } else if (downloadInfo.getState() == DownloadManager.ERROR) {
                netSpeed.setText("下载出错");
                download.setText("出错");
            } else if (downloadInfo.getState() == DownloadManager.WAITING) {
                netSpeed.setText("等待中");
                download.setText("等待");
            } else if (downloadInfo.getState() == DownloadManager.FINISH) {
                if (ApkUtils.isAvailable(DownloadManagerActivity.this, new File(downloadInfo.getTargetPath()))) {
                    download.setText("卸载");
                } else {
                    download.setText("安装");
                }
                netSpeed.setText("下载完成");
            } else if (downloadInfo.getState() == DownloadManager.DOWNLOADING) {
                String networkSpeed = Formatter.formatFileSize(DownloadManagerActivity.this, downloadInfo.getNetworkSpeed());
                netSpeed.setText(networkSpeed + "/s");
                download.setText("暂停");
            }
            tvProgress.setText((Math.round(downloadInfo.getProgress() * 10000) * 1.0f / 100) + "%");
            pbProgress.setMax((int) downloadInfo.getTotalLength());
            pbProgress.setProgress((int) downloadInfo.getDownloadLength());
        }

        @Override
        public void onClick(View v) {
            mBuilder.setContentTitle(apk.getName());
            if (v.getId() == download.getId()) {
                switch (downloadInfo.getState()) {
                    case DownloadManager.PAUSE:
                    case DownloadManager.NONE:
                    case DownloadManager.ERROR:
                        mDownloadManager.addTask(downloadInfo.getUrl(), downloadInfo.getRequest(), downloadInfo.getListener());
                        break;
                    case DownloadManager.DOWNLOADING:
                        mDownloadManager.pauseTask(downloadInfo.getUrl());
                        break;
                    case DownloadManager.FINISH:
                        if (ApkUtils.isAvailable(DownloadManagerActivity.this, new File(downloadInfo.getTargetPath()))) {
                            ApkUtils.uninstall(DownloadManagerActivity.this, ApkUtils.getPackageName(DownloadManagerActivity.this, downloadInfo.getTargetPath()));
                        } else {
                            ApkUtils.install(DownloadManagerActivity.this, new File(downloadInfo.getTargetPath()));
                        }
                        break;
                }
                refresh();
            } else if (v.getId() == remove.getId()) {
                mDownloadManager.removeTask(downloadInfo.getUrl());
                mAdapter.notifyDataSetChanged();
            } else if (v.getId() == restart.getId()) {
                mDownloadManager.restartTask(downloadInfo.getUrl());
            }
        }
    }

    private class MyDownloadListener extends DownloadListener {

        @Override
        public void onProgress(DownloadInfo downloadInfo) {
            if (getUserTag() == null) return;

            L.e("onProgress>>>" + String.valueOf((int) (downloadInfo.getProgress() * 100)));
            mBuilder.setContentText(String.valueOf((int) downloadInfo.getProgress() * 100)+"%");
//            RemoteViews contentView = mBuilder.build().contentView;
//            contentView.setTextViewText(R.id.rate, (int) (downloadInfo.getProgress() * 100) + "%");
//            contentView.setProgressBar(R.id.progress, 100, (int) (downloadInfo.getProgress() * 100), false);
            // state
            mBuilder.setProgress(100, (int) (downloadInfo.getProgress() * 100), false);
            // Displays the progress bar for the first time.
            mNotificationManager.notify(110, mBuilder.build());

            ViewHolder holder = (ViewHolder) getUserTag();
            holder.refresh();  //这里不能使用传递进来的 DownloadInfo，否者会出现条目错乱的问题
        }

        @Override
        public void onFinish(DownloadInfo downloadInfo) {

            mBuilder.setAutoCancel(true);

            mIntent = new Intent(Intent.ACTION_VIEW);
            mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mIntent.setDataAndType(Uri.parse("file://" + downloadInfo.getTargetPath()),"application/vnd.android.package-archive");
            startActivity(mIntent);
            PendingIntent pendingIntent = PendingIntent.getActivity(DownloadManagerActivity.this,100, mIntent,PendingIntent.FLAG_UPDATE_CURRENT);

            mBuilder.setContentIntent(pendingIntent);

            L.e("downloadInfo>>>"+downloadInfo.getFileName());
            mBuilder.setContentTitle(downloadInfo.getFileName());
            mBuilder.setContentText("Download complete")
                    // Removes the progress bar
                    .setProgress(0,0,false);
            mNotificationManager.notify(110, mBuilder.build());

            Toast.makeText(DownloadManagerActivity.this, "下载完成:" + downloadInfo.getTargetPath(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onError(DownloadInfo downloadInfo, String errorMsg, Exception e) {
            if (errorMsg != null) Toast.makeText(DownloadManagerActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
        }
    }
}
