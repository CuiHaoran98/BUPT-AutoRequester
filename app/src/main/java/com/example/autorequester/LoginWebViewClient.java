package com.example.autorequester;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static android.content.ContentValues.TAG;

public class LoginWebViewClient extends WebViewClient {

    private LoginActivity loginActivity;
    private WebView webView;
    private String UnAndPdFilePath;
    private String flag;

    public void setData(ArrayList<String> data) {
        this.data = data;
        Log.d(TAG, "setData: " + data);
    }

    private ArrayList<String> data;

    private int jumpFlag = 0;

    @Override
    public void onPageFinished(WebView view, String url) {
        Log.i("LoginWebViewClient", url);
        super.onPageFinished(view, url);
        //如果有效期内没有登陆过则会访问登录页面
        if (url.startsWith("https://auth.bupt.edu.cn/authserver/login")) {
            //读配置文件获取学号和密码
            Map<String, String> userNameAndPassword = getUserNameAndPassword(UnAndPdFilePath);
            String studentNumber = userNameAndPassword.get("studentId");
            String passWord = userNameAndPassword.get("passWord");

            //自动在登录界面填入学号和密码
            webView.loadUrl("javascript:document.getElementById('username').value='" + studentNumber + "';document.getElementById('password').value='" + passWord + "';document.getElementsByName('submit')[0].click();");
            return;
        }
        if (url.startsWith("https://service.bupt.edu.cn/v2/matter/start?id=578") || url.startsWith("https://service.bupt.edu.cn/v2/matter/m_start?id=578")) {
            jumpFlag++;
            if (jumpFlag <= 1)
                return;

            (new Thread(new Runnable() {
                @Override
                public void run() {
                    flag = "0";
                    if (data.get(1).equals("西土城校区")) {
                        flag = "1";
                    }

                    sleep(2000);

                    runJs("document.getElementsByClassName(\"zl-button\")[2]" + ".click();");

                    sleep(3000);

                    runJs(
                                    //输入手机号
                            "document.getElementsByClassName(\"dplugin-inputView\")[3].value=\"" + data.get(0) + "\";"
                                    + "var evt = document.createEvent(\"HTMLEvents\");evt.initEvent(\"input\", true, false);"
                                    + "document.getElementsByClassName(\"dplugin-inputView\")[3].dispatchEvent(evt);"
                                    //输入外出去向
                                    + "document.getElementsByClassName(\"dplugin-mobile-newInput\")[0].value=\"" + data.get(5) + "\";"
                                    + "document.getElementsByClassName(\"dplugin-mobile-newInput\")[0].dispatchEvent(evt);"
                                    //输入外出事由
                                    + "document.getElementsByClassName(\"dplugin_multiInput\")[0].value=\"" + data.get(6) + "\";"
                                    + "document.getElementsByClassName(\"dplugin_multiInput\")[0].dispatchEvent(evt);"
                    );

                    //选择辅导员
                    runJs("var evt = document.createEvent(\"HTMLEvents\");"
                            + "evt.initEvent(\"click\", true, false);"
                            + "document.getElementsByClassName(\"el-icon-search\")[0].dispatchEvent(evt);");
                    runJs("document.getElementById(\"UserSearch_60\").value=\"" + data.get(2) + "\";"
                            + "var evt = document.createEvent(\"HTMLEvents\");evt.initEvent(\"input\", true, false);"
                            + "document.getElementById(\"UserSearch_60\").dispatchEvent(evt)");

                    sleep(2000);

                    runJs("var evt = document.createEvent(\"HTMLEvents\");"
                            + "evt.initEvent(\"click\", true, false);"
                            + "document.getElementsByClassName(\"li-style\")[0].dispatchEvent(evt);");

                    runJs("var evtf = document.createEvent(\"HTMLEvents\");evtf.initEvent(\"focus\", true, false);document.getElementsByClassName(\"el-input__inner\")[1].dispatchEvent(evtf);");
                    sleep(500);
                    runJs("document.getElementsByClassName(\"available today\")[0].click();");

                    //选择外出时间
                    runJs("var evtf = document.createEvent(\"HTMLEvents\");evtf.initEvent(\"focus\", true, false);document.getElementsByClassName(\"el-input__inner\")[2].dispatchEvent(evtf);");
                    runJs("document.getElementsByClassName(\"el-time-panel__btn confirm\")[0].click();");

                    //选择返校时间
                    runJs("var evtf = document.createEvent(\"HTMLEvents\");evtf.initEvent(\"focus\", true, false);document.getElementsByClassName(\"el-input__inner\")[3].dispatchEvent(evtf);");
                    runJs("document.getElementsByClassName(\"el-time-spinner__item\")[167].click();");
                    runJs("document.getElementsByClassName(\"el-time-spinner__item\")[227].click();");
                    runJs("document.getElementsByClassName(\"el-time-spinner__item\")[287].click();");
                    runJs("document.getElementsByClassName(\"el-time-panel__btn confirm\")[1].click();");

                    //选择校区
                    runJs("document.getElementsByClassName(\"el-select-dropdown__item\")[" + flag + "].click();");

                    //同意承诺
                    runJs("document.getElementsByClassName(\"icon\")[0].click();");

                    //提交表单
                    runJs("document.getElementsByClassName(\"wxformbtn_center\")[0].children[0].click();");
                }
            })).start();
            return;
        }
        if (url.startsWith("https://service.bupt.edu.cn/v2/matter/m_launch?type=1")) {
            Toast.makeText(loginActivity, "申请成功！", Toast.LENGTH_LONG);
            loginActivity.finish();
        } else {
            webView.loadUrl("https://service.bupt.edu.cn/v2/matter/m_start?id=578");
        }
    }

    public void sleep(int millis){
        try {
            Thread.sleep(millis);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void runJs(final String jsCmd){

        sleep(500);

        loginActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: " + jsCmd);
                webView.loadUrl("javascript:"
                        + jsCmd
                );
            }
        });
    }

    public void setLoginActivity(LoginActivity loginActivity, WebView webView, String UnAndPdFilePath) {
        this.loginActivity = loginActivity;
        this.webView = webView;
        this.UnAndPdFilePath = UnAndPdFilePath;
    }

    //读取学号和密码
    public static Map<String, String> getUserNameAndPassword(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            Log.i("LoginWebViewClient", "matter information file not exists.");
            return null;
        }
        //用户文件转化为流
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.i("LoginWebViewClient", "File conversion to stream failed.");
        }
        //以读取配置文件的方式读取外出事项信息
        Properties props = new Properties();
        try {
            props.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
            Log.i("LoginWebViewClient", "File read failed.");
        }
        Map<String, String> matterResult = new HashMap<>();
        for (Object key : props.keySet()) {
            matterResult.put(key.toString(), props.getProperty(key.toString()));
        }
        //关闭文件
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return matterResult;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Nullable
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        WebResourceResponse response = super.shouldInterceptRequest(view, request);
//        https://service.bupt.edu.cn/site/process/my-todo?status=0&p=1&page_size=1
        if (request.getUrl().toString().equals("https://service.bupt.edu.cn/site/process/inst-list?status=1&p=1&starter_depart_id=181789")) {
            Toast.makeText(this.loginActivity, "申请成功！", Toast.LENGTH_LONG);
            loginActivity.finish();
        }
        return response;
    }

}
