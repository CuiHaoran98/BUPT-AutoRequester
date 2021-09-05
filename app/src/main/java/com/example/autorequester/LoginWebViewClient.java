package com.example.autorequester;

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

                    //选择外出时间
                    runJs("var evtf = document.createEvent(\"HTMLEvents\");evtf.initEvent(\"focus\", true, false);document.getElementsByClassName(\"el-input__inner\")[1].dispatchEvent(evtf);");
                    runJs("document.getElementsByClassName(\"el-time-panel__btn confirm\")[0].click();");

                    //选择返校时间
                    runJs("var evtf = document.createEvent(\"HTMLEvents\");evtf.initEvent(\"focus\", true, false);document.getElementsByClassName(\"el-input__inner\")[2].dispatchEvent(evtf);");
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
            Toast.makeText(loginActivity, "申请成功！", Toast.LENGTH_LONG);
            loginActivity.finish();
        }
        return response;
    }

//    public void postForm(String url) throws JSONException {
//        OkHttpClient okHttpClient = new OkHttpClient();
//        JSONObject jsonObject1 = new JSONObject();
//        try {
//            jsonObject1.put("name", "西土城校区")
//                    .put("value", "2")
//                    .put("default", 0)
//                    .put("imgdata", "");
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//        JSONObject jsonObject2 = new JSONObject();
//        try {
//            jsonObject2.put("uid", 197746)
//                    .put("name", "吕佳蔚")
//                    .put("number", "2020118020");
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//        JSONObject jsonObject3 = new JSONObject();
//        try {
//            jsonObject3.put("value", "1")
//                    .put("name", "本人已阅读并承诺");
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//        JSONObject jsonObject = new JSONObject();
//        try {
//            jsonObject.put("User_5", "崔浩然")
//                    .put("User_7", "2020141050")
//                    .put("User_9", "计算机学院（国家示范性软件学院）")
//                    .put("User_11", "18810778521")
//                    .put("SelectV2_58", new JSONObject[]{jsonObject1})
//                    .put("UserSearch_60", jsonObject2)
//                    .put("Calendar_62", "2021-09-02T15:28:57+08:00")
//                    .put("Calendar_50", "2021-09-02T07:35:34.000Z")
//                    .put("Calendar_47", "2021-09-02T07:37:25.000Z")
//                    .put("Input_28", "东信北邮")
//                    .put("MultiInput_30", "校外科研")
//                    .put("Radio_52", jsonObject3)
//                    .put("Validate_63", "")
//                    .put("Alert_65", "")
//                    .put("Validate_66", "")
//                    .put("Alert_67", "");
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//
//        JSONObject jsonObject4 = new JSONObject();
//        jsonObject4.put("1716", jsonObject);
//        JSONObject jsonObject5 = new JSONObject();
//        jsonObject5.put("app_id", "578")
//                .put("form_data", jsonObject4);
//
//        Log.d("CHR", "postForm: " + String.valueOf(jsonObject5));
//
//        RequestBody requestBody = FormBody.create(MediaType.parse("application/json; charset=utf-8")
//                , String.valueOf(jsonObject5));
////        RequestBody requestBody = new FormBody.Builder()
////                .add("data", String.valueOf(jsonObject))
////                .build();
//
//        Request request = new Request.Builder()
//                .url(url)
//                .post(requestBody)
//                .build();
//
//        okHttpClient.newCall(request).enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                Log.d("CHR", "onFailure: " + e.getMessage());
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                Log.d("CHR", response.protocol() + " " + response.code() + " " + response.message());
//                Headers headers = response.headers();
//                for (int i = 0; i < headers.size(); i++) {
//                    Log.d("CHR", headers.name(i) + ":" + headers.value(i));
//                }
//                Log.d("CHR", "onResponse: " + response.body().string());
//            }
//        });
//    }

}
