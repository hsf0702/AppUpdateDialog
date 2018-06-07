package sskj.lee.appupdatelibrary;

import android.Manifest;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
/**
 * ProjectName：DialogDemo
 * DESC: (类描述)
 * Created by 李岩 on 2018/6/4 0004
 * updateName:(修改人名称)
 * updateTime:(修改时间)
 * updateDesc:(修改内容)
 */
public abstract class BaseUpdateDialogFragment extends DialogFragment {

    private Activity mActivity;
    public BaseVersion mVersionData;
    private int PERMISSION_REQUEST_WRITE_SD = 1024;
    public static String INTENT_KEY = "result_key";
    private DownloadTask mDownloadTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Theme_Design_BottomSheetDialog_NoActionBar);

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View inflate = LayoutInflater.from(getActivity()).inflate(getLayoutId(), null);
        mActivity = getActivity();
        return inflate;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mVersionData = (BaseVersion) getArguments().getSerializable(INTENT_KEY);
        initView(view, mVersionData);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDownloadTask!= null){
            mDownloadTask.cancel(true);
        }
    }

    public abstract int getLayoutId();

    /**
     * 初始化UI
     * @param view
     * @param versionData
     */
    protected abstract void initView(View view, BaseVersion versionData);
    /**
     * 当下载开始前（MainThred）
     */
    protected abstract void onDownLoadStart();

    /**
     * 当下载中（MainThred）
     * @param progress
     */
    protected abstract void onDownLoadUpdate(int progress);

    /**
     * 当下在完成
     */
    protected void onDownLoadFinish(){};

    /**
     * 校验权限
     */
    void checkPermission() {
        if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_WRITE_SD);
        } else {
            if (mVersionData.getViewStyle() == BaseVersion.NOTIFYCATION_STYLE){
                mActivity.startService(new Intent(mActivity, NotifyDownloadService.class).putExtra(INTENT_KEY, mVersionData));
                dismiss();//关掉更新提示dialog
            }else {
                mDownloadTask = new DownloadTask();
                mDownloadTask.execute(mVersionData.getUrl());
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_WRITE_SD && grantResults[0] == PackageManager.PERMISSION_GRANTED) {//开通权限
            startDownload();
        }
    }

    protected void startDownload() {
        checkPermission();
    }

//    /**
//     * 创建弹窗下载
//     */
//    private void showDialog() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
//        View inflate = LayoutInflater.from(mActivity).inflate(R.layout.dialog_download_layout, null);
//        mProgressbar = inflate.findViewById(R.id.download_progressbar);
//        mProgressText = inflate.findViewById(R.id.download_progress_text);
//        builder.setView(inflate);
//        builder.setCancelable(false);
//        mDownloadingDialog = builder.create();
//        mDownloadingDialog.show();
//        mDownloadTask = new DownloadTask();
//        mDownloadTask.execute(mVersionData.getUrl());
//    }

    class DownloadTask extends AsyncTask<String, Integer, File> {

        private boolean mCancelUpdata = false;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            onDownLoadStart();
        }

        @Override
        protected File doInBackground(String... strings) {
            // 判断SD卡是否存在，并且是否具有读写权限
            File apkFile= null;
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
                InputStream is= null;
                FileOutputStream fos= null;
                HttpURLConnection conn= null;
                try {
                    String savePath = String.format(Contacts.DOWNLOAD_PATH, Environment.getExternalStorageDirectory(), mActivity.getPackageName());
                    URL url = new URL(mVersionData.getUrl());
                    conn = (HttpURLConnection) url.openConnection();
                    conn.connect();
                    long length = conn.getContentLength();
                    File file = new File(savePath);
                    if (!file.exists()){
                        file.mkdirs();
                    }
                    apkFile = new File(savePath, TextUtils.isEmpty(mVersionData.getVersionName()) ? String.valueOf(System.currentTimeMillis() + ".apk") : mVersionData.getVersionName()+ ".apk");
                    if (apkFile.exists()){
                        apkFile.delete();
                    }
                    is = conn.getInputStream();//创建输入流
                    fos = new FileOutputStream(apkFile);
                    long count= 0;
                    byte[] buf = new byte[1024];
                    do {
                        int readNum = is.read(buf);
                        if (readNum <= 0){//下载完成
                            mCancelUpdata = true;
                            break;
                        }
                        count += readNum;
                        fos.write(buf, 0, readNum);// 写入文件
                        publishProgress((int)(count * 100 / length));//计算当前进度， 更新进度
                    }while (!mCancelUpdata);
                    fos.close();
                    is.close();

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }finally {
                    try {
                        if (fos != null) fos.close();
                        if (is != null) is.close();
                    } catch (IOException ignored) {
                    }
                    if (conn != null) conn.disconnect();
                }
            }else {
                Toast.makeText(mActivity, Contacts.DIALOG_SDCARD_NULL, Toast.LENGTH_SHORT).show();
            }
            return apkFile;
        }

        /**
         * 更新进度
         * @param values
         */
        @Override
        protected void onProgressUpdate(Integer... values) {
            onDownLoadUpdate(values[0]);
//            mProgressbar.setProgress(values[0]);
//            mProgressText.setText(values[0] + "%");
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        /**
         * 下载完成
         * @param file
         */
        @Override
        protected void onPostExecute(File file) {
            super.onPostExecute(file);
            dismiss();//关闭下载中弹窗
            onDownLoadFinish();
            if (file != null && file.exists()){
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setFlags(FLAG_GRANT_READ_URI_PERMISSION);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Uri contentUri = FileProvider.getUriForFile(mActivity, mActivity.getPackageName() + ".fileProvider", file);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, contentUri);
                    intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
                } else {
                    intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                startActivity(intent);
            }
            mDownloadTask.cancel(true);
        }
    }
}