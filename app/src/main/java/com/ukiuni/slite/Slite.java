package com.ukiuni.slite;

import android.support.annotation.NonNull;

import com.ukiuni.slite.model.Account;
import com.ukiuni.slite.model.Content;
import com.ukiuni.slite.model.MyAccount;
import com.ukiuni.slite.util.JSONDate;
import com.ukiuni.slite.util.SS;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by tito on 15/10/08.
 */
public class Slite {
    private static final String GET = "GET";
    private static final String POST = "POST";
    private static final String PUT = "PUT";
    private static final String DELETE = "DELETE";
    private String host = "http://192.168.56.1:3030";
    private MyAccount myAccount;

    public Slite() {

    }

    public void setHost(String host) {
        this.host = host;
    }

    public Slite(MyAccount myAccount) {
        this.myAccount = myAccount;
        setHost(this.myAccount.host);
    }

    public MyAccount signin(final String mail, final String password) throws IOException {
        try {
            String connectHost = this.host;
            JSONObject respJSON = httpJ(GET, connectHost + "/api/account/signin", SS.map("mail", mail).p("password", password));

            MyAccount myAccount = new MyAccount();
            JSONObject accountJSON = respJSON.getJSONObject("account");
            myAccount.id = accountJSON.getLong("id");
            myAccount.name = accountJSON.getString("name");
            myAccount.mail = accountJSON.getString("name");
            myAccount.iconUrl = accountJSON.getString("iconUrl");
            myAccount.sessionKey = respJSON.getJSONObject("sessionKey").getString("secret");
            myAccount.lastLoginedAt = new Date();
            myAccount.host = connectHost;
            this.myAccount = myAccount;
            return myAccount;
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    private JSONObject httpJ(String method, String url, Map<String, String> form) throws IOException, JSONException {
        return new JSONObject(http(method, url, form));

    }

    private JSONArray httpJA(String method, String url, Map<String, String> form) throws IOException, JSONException {
        return new JSONArray(http(method, url, form));

    }

    private String http(String method, String url, Map<String, String> form) throws IOException {
        String requestParam = "";
        for (String key : form.keySet()) {
            requestParam += URLEncoder.encode(key, "UTF-8");
            requestParam += "=";
            requestParam += URLEncoder.encode(form.get(key), "UTF-8");
            requestParam += "&";
        }
        if ("GET".equals(method) || "DELETE".equals(method)) {
            url += "?" + requestParam.substring(0, requestParam.length() - 1);
        }
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setUseCaches(false);
        connection.setRequestMethod(method);
        if ("POST".equals(method) || "PUT".equals(method)) {
            connection.setDoOutput(true);
        }
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        if ("POST".equals(method) || "PUT".equals(method)) {
            OutputStream out = connection.getOutputStream();
            out.write(requestParam.substring(0, requestParam.length() - 1).getBytes("UTF-8"));
        }

        String errorResponse = "";
        InputStream errorInput = connection.getErrorStream();
        if (null != errorInput) {
            BufferedReader errorIn = new BufferedReader(new InputStreamReader(errorInput));

            for (String line = errorIn.readLine(); line != null; line = errorIn.readLine()) {
                errorResponse += line;
            }
            if (!"".equals(errorResponse)) {
                throw new IOException(errorResponse + " ,status = " + connection.getResponseCode());
            }
        }
        if (connection.getResponseCode() >= 400) {
            throw new IOException(String.valueOf(connection.getResponseCode()));
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String response = "";
        for (String line = in.readLine(); line != null; line = in.readLine()) {
            response += line;
        }
        return response;
    }

    public List<Content> loadMyContent() throws IOException {
        try {
            JSONArray contentArray = httpJA(GET, host + "/api/content", SS.map("sessionKey", this.myAccount.sessionKey));
            List<Content> contentList = new ArrayList<Content>(contentArray.length());
            for (int i = 0; i < contentArray.length(); i++) {
                JSONObject contentJSON = contentArray.getJSONObject(i);
                Content content = convertContent(contentJSON);
                contentList.add(content);
            }
            return contentList;
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    @NonNull
    private Content convertContent(JSONObject contentJSON) throws JSONException {
        Content content = new Content();
        content.id = contentJSON.getLong("id");
        content.accessKey = contentJSON.getString("accessKey");
        content.createdAt = JSONDate.parse(contentJSON.getString("createdAt"));
        content.updatedAt = JSONDate.parse(contentJSON.getString("updatedAt"));

        JSONObject contentBody = contentJSON.getJSONArray("ContentBodies").getJSONObject(0);
        content.imageUrl = contentBody.getString("topImageUrl");
        if (contentBody.has("article")) {
            content.article = contentBody.getString("article");
        }
        content.title = contentBody.getString("title");

        JSONObject account = contentJSON.getJSONObject("owner");
        content.owner = new Account();
        content.owner.id = contentJSON.getLong("ownerId");
        content.owner.name = account.getString("name");
        content.owner.iconUrl = account.getString("iconUrl");
        content.loadAccount = myAccount;
        return content;
    }

    public Content loadContent(String accessKey) throws IOException {
        try {
            JSONObject contentJson = httpJ(GET, host + "/api/content/" + accessKey, SS.map("sessionKey", this.myAccount.sessionKey));
            return convertContent(contentJson);
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    public Content appendContent(String accessKey, String appendsText) throws IOException {
        try {
            JSONObject contentJson = httpJ(PUT, host + "/api/content/" + accessKey, SS.map("sessionKey", this.myAccount.sessionKey).p("article", appendsText).p("appends", "before"));
            return convertContent(contentJson);
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    public Content updateContent(Content content) throws IOException {
        try {
            JSONObject contentJson = httpJ(PUT, host + "/api/content/" + content.accessKey, SS.map("sessionKey", this.myAccount.sessionKey).p("title", content.title).p("article", content.article));
            return convertContent(contentJson);
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    public Content createContent(String title, String article) throws IOException {
        try {
            JSONObject contentJson = httpJ(POST, host + "/api/content/", SS.map("sessionKey", this.myAccount.sessionKey).p("title", title).p("article", article));
            return convertContent(contentJson);
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    public void setMyAccount(MyAccount myAccount) {
        this.myAccount = myAccount;
    }

    public void deleteContent(Content content) throws IOException {
        try {
            httpJ(DELETE, host + "/api/content/" + content.accessKey, SS.map("sessionKey", this.myAccount.sessionKey));
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }
}
