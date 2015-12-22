package com.ukiuni.slite;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.util.Log;

import com.ukiuni.slite.model.Account;
import com.ukiuni.slite.model.Channel;
import com.ukiuni.slite.model.Content;
import com.ukiuni.slite.model.Group;
import com.ukiuni.slite.model.Message;
import com.ukiuni.slite.model.MyAccount;
import com.ukiuni.slite.util.Async;
import com.ukiuni.slite.util.JSONDate;
import com.ukiuni.slite.util.SS;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

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

    public MyAccount currentAccount() {
        return this.myAccount;
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
            this.host = myAccount.host;
            return myAccount;
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    private JSONObject httpJ(String method, String url, Map<String, String> form) throws IOException, JSONException {
        String json = http(method, url, form);
        return new JSONObject(json);

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
        if (contentJSON.has("type")) {
            content.type = contentJSON.getString("type");
        }
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

    @NonNull
    private Group convertGroup(JSONObject contentJSON) throws JSONException {
        Group group = new Group();
        group.id = contentJSON.getLong("id");
        group.accessKey = contentJSON.getString("accessKey");
        group.name = contentJSON.getString("name");
        if (contentJSON.has("Contents")) {
            List<Content> contents = new ArrayList<Content>();
            JSONArray contentsArray = contentJSON.getJSONArray("Contents");
            Log.d("", "-------contentsArray--- " + contentsArray.length());
            for (int i = 0; i < contentsArray.length(); i++) {
                contents.add(convertContent(contentsArray.getJSONObject(i)));
            }
            group.contents = contents;
        }
        if (contentJSON.has("Channels")) {
            List<Channel> channels = new ArrayList<Channel>();
            JSONArray channelsArray = contentJSON.getJSONArray("Channels");
            for (int i = 0; i < channelsArray.length(); i++) {
                Channel channel = new Channel();
                channel.name = channelsArray.getJSONObject(i).getString("name");
                channel.accessKey = channelsArray.getJSONObject(i).getString("accessKey");
                channels.add(channel);

            }
            group.channels = channels;
        }
        return group;
    }

    public Content loadContent(String accessKey) throws IOException {
        try {
            JSONObject contentJson = httpJ(GET, host + "/api/content/" + accessKey, SS.map("sessionKey", this.myAccount.sessionKey));
            return convertContent(contentJson);
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }


    public List<Group> loadMyGroups() throws IOException {
        try {
            JSONArray groupArray = httpJA(GET, host + "/api/groups/self", SS.map("sessionKey", this.myAccount.sessionKey));
            List<Group> groupList = new ArrayList<Group>(groupArray.length());
            for (int i = 0; i < groupArray.length(); i++) {
                JSONObject contentJSON = groupArray.getJSONObject(i);
                Group group = convertGroup(contentJSON);
                groupList.add(group);
            }
            return groupList;
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    public Group loadGroup(String accessKey) throws IOException {
        try {
            JSONObject json = httpJ(GET, host + "/api/groups/" + accessKey, SS.map("sessionKey", this.myAccount.sessionKey));
            return convertGroup(json);
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
            if (title == null) {
                title = "";
            }
            JSONObject contentJson = httpJ(POST, host + "/api/content/", SS.map("sessionKey", this.myAccount.sessionKey).p("title", title).p("article", article));
            return convertContent(contentJson);
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    public Content createCalendar(String title, String article) throws IOException {
        try {
            if (title == null) {
                title = "";
            }
            JSONObject contentJson = httpJ(POST, host + "/api/content/", SS.map("sessionKey", this.myAccount.sessionKey).p("title", title).p("article", article).p("type", Content.TYPE_CALENDAR));
            return convertContent(contentJson);
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    public void setMyAccount(MyAccount myAccount) {
        this.myAccount = myAccount;
        setHost(myAccount.host);
    }

    public void deleteContent(Content content) throws IOException {
        try {
            httpJ(DELETE, host + "/api/content/" + content.accessKey, SS.map("sessionKey", this.myAccount.sessionKey));
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    public void sendMessage(String channelAccessKey, String messageBody) throws IOException {
        http(POST, host + "/api/groups/global/channels/" + channelAccessKey + "/messages", SS.map("sessionKey", this.myAccount.sessionKey).p("body", messageBody));
    }

    public static interface Progress {
        public void sended(int current);
    }

    public String uploadImage(String accessKey, Bitmap thumbnail, Progress... progress) throws IOException {
        return uploadImage(accessKey, thumbnail, null, null, progress);
    }

    public String uploadImage(String accessKey, InputStream in, String name, Progress... progress) throws IOException {
        return uploadImage(accessKey, null, in, name, progress);
    }

    public String uploadImage(String accessKey, Bitmap thumbnail, InputStream fileIn, String name, Progress... progress) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(host + "/api/image/" + accessKey).openConnection();
        String boundary = "*****" + UUID.randomUUID().toString() + "*****";
        String crlf = "\r\n";
        String twoHyphens = "--";

        connection.setUseCaches(false);
        connection.setRequestMethod(POST);
        connection.setReadTimeout(30000);
        connection.setConnectTimeout(10000);
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

        DataOutputStream request = new DataOutputStream(
                connection.getOutputStream());

        request.writeBytes(twoHyphens + boundary + crlf);
        request.writeBytes("Content-Disposition: form-data; name=\"imageFile\";filename=\"tmpimage.jpg\"" + crlf);
        request.writeBytes("Content-Type: application/octet-stream" + crlf);
        request.writeBytes("Content-Transfer-Encoding: binary" + crlf);
        request.writeBytes(crlf);
        Progress currentProgress = null;
        if (null != progress && progress.length > 0) {
            currentProgress = progress[0];
        }
        if (null != thumbnail) {
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, connection.getOutputStream());
        } else {
            byte[] buffer = new byte[1024 * 1024];
            int readed = fileIn.read(buffer);
            int totalSize = 0;
            while (0 < readed) {
                request.write(buffer, 0, readed);
                totalSize += readed;
                if (null != currentProgress) {
                    currentProgress.sended(totalSize);
                }
                readed = fileIn.read(buffer);
            }
        }
        request.writeBytes(crlf);

        request.writeBytes(twoHyphens + boundary + crlf);
        request.writeBytes("Content-Disposition: form-data; name=\"sessionKey\"" + crlf);
        request.writeBytes("Content-Type: text/plain" + crlf);
        request.writeBytes(crlf);
        request.writeBytes(this.myAccount.sessionKey);
        request.writeBytes(crlf);
        if (null != name) {
            request.writeBytes(twoHyphens + boundary + crlf);
            request.writeBytes("Content-Disposition: form-data; name=\"name\"" + crlf);
            request.writeBytes("Content-Type: text/plain" + crlf);
            request.writeBytes(crlf);
            request.writeBytes(name);
            request.writeBytes(crlf);
        }
        request.writeBytes(twoHyphens + boundary + twoHyphens + crlf);

        request.flush();
        request.close();


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
        JSONObject object = null;
        try {
            object = new JSONObject(response);
            return object.getString("url");
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    public void listenChannel(final String channelAccessKey, final MessageHandle messageHandle) throws IOException {
        try {
            final Socket socket = io.socket.client.IO.socket(myAccount.host);
            messageHandle.setSocket(socket, channelAccessKey);

            socket.once(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    socket.emit("authorize", myAccount.sessionKey);
                    socket.emit("listenChannel", channelAccessKey);
                    socket.emit("requestMessage", SS.map("channelAccessKey", channelAccessKey).toJSON());
                }
            }).on(channelAccessKey, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    try {
                        final Event event = new Event(new JSONObject((String) args[0]));
                        Async.pushToOriginalThread(new Runnable() {
                            @Override
                            public void run() {
                                if ("message".equals(event.type)) {
                                    messageHandle.onMessage(event.message);
                                } else if ("historicalMessage".equals(event.type)) {
                                    messageHandle.onHistoricalMessage(event.messages);
                                } else if ("join".equals(event.type)) {
                                    messageHandle.onJoin(event.account);
                                    if (event.account.id != myAccount.id) {
                                        socket.emit("hello", SS.map("channelAccessKey", channelAccessKey).toJSON());
                                    }
                                } else if ("hello".equals(event.type)) {
                                    messageHandle.onJoin(event.account);
                                } else if ("reave".equals(event.type)) {
                                    messageHandle.onReave(event.account);
                                }
                            }
                        });
                    } catch (JSONException e) {
                        messageHandle.onError(e);
                        return;
                    }
                }
            }).on("exception", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Async.makeToast(null != args[0] ? args[0].toString() : SliteApplication.getInstance().getString(R.string.fail_to_access_to_server));
                }
            }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    messageHandle.onDisconnect();
                }
            });
            socket.connect();
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    public String registDevice(String deviceId) throws IOException {
        try {
            JSONObject notificationJson = httpJ(POST, host + "/api/account/devices", SS.map("sessionKey", this.myAccount.sessionKey).p("platform", "1").p("endpoint", deviceId));
            return notificationJson.getString("key");
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    public void deleteDevice(String key) throws IOException {
        http(DELETE, host + "/api/account/devices", SS.map("sessionKey", this.myAccount.sessionKey).p("key", key));
    }

    public class Event {
        public Event(JSONObject jObj) {
            try {
                this.type = jObj.getString("type");
                if ("historicalMessage".equals(this.type)) {
                    List<Message> messages = new ArrayList<>();
                    JSONArray jsMessages = jObj.getJSONArray("messages");
                    for (int i = 0; i < jsMessages.length(); i++) {
                        JSONObject messageJObj = jsMessages.getJSONObject(i);
                        Message message = parseMessage(messageJObj);
                        messages.add(message);
                    }
                    this.messages = messages;
                }
                if (jObj.has("message")) {
                    JSONObject messageJObj = jObj.getJSONObject("message");
                    Message message = parseMessage(messageJObj);
                    this.message = message;
                }
                if (jObj.has("account")) {
                    JSONObject ownerJObj = jObj.getJSONObject("account");
                    this.account = new Account();
                    this.account.id = ownerJObj.getLong("id");
                    this.account.name = ownerJObj.getString("name");
                    this.account.iconUrl = ownerJObj.getString("iconUrl");
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        public String type;
        public Message message;
        public List<Message> messages;
        public Account account;
        public Date createdAt;
        public Date updatedAt;
    }

    @NonNull
    private Message parseMessage(JSONObject messageJObj) throws JSONException {
        Message message = new Message();
        message.body = messageJObj.getString("body");
        message.id = messageJObj.getLong("id");
        message.createdAt = JSONDate.parse(messageJObj.getString("createdAt"));
        message.updatedAt = JSONDate.parse(messageJObj.getString("updatedAt"));
        message.localOwner = myAccount;
        JSONObject ownerJObj = messageJObj.getJSONObject("owner");
        message.owner = new Account();
        message.owner.id = ownerJObj.getLong("id");
        message.owner.name = ownerJObj.getString("name");
        message.owner.iconUrl = ownerJObj.getString("iconUrl");
        return message;
    }

    public static abstract class MessageHandle {
        private Socket socket;
        private String accessKey;

        private void setSocket(Socket socket, String accessKey) {
            this.socket = socket;
            this.accessKey = accessKey;
        }

        public abstract void onJoin(Account account);

        public abstract void onMessage(Message message);

        public abstract void onHistoricalMessage(List<Message> message);

        public abstract void onReave(Account account);

        public abstract void onError(Exception e);

        public abstract void onDisconnect();

        public final void requestOlder(long lastId) {
            this.socket.emit("requestMessage", SS.map("channelAccessKey", accessKey).p("idBefore", "" + lastId).toJSON());
        }

        public final void disconnect() {
            if (null != socket) {
                this.socket.off(this.accessKey);
                this.socket.off();
                this.socket.disconnect();
                this.socket.close();
            }
        }

    }
}
