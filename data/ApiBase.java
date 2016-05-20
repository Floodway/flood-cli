
import android.app.Activity;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.koushikdutta.async.http.Headers;
import com.koushikdutta.async.http.WebSocket;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Cookie on 07.05.2016.
 */
public abstract class ApiBase {

    abstract String getBaseUrl();

    private String settings_name = "flood-settings";
    private boolean showConnectingDialog = true;

    Activity activity;
    private ProgressDialog progressDialog;

    private Handler h = new Handler();

    private boolean connected = false;


    private WebSocket socket;
    private ReadyCallback readyCallback;

    private List<String> pendingRequests = new ArrayList<>();

    private List<Request> requests = new ArrayList<>();


    private Runnable disconnectAfterIdle = new Runnable() {
        @Override
        public void run() {
            disconnect();
        }
    };


    public void mountActivity(Activity activity){
        hideDialog();
        this.activity = activity;

        h.removeCallbacks(disconnectAfterIdle);


    }

    public void pauseConnection(){
        hideDialog();
        h.postDelayed(disconnectAfterIdle,8000);

    }

    public void resumeConnection(){
        if(!isConnected()){
            connectToServer();
        }
        h.removeCallbacks(disconnectAfterIdle);
    }



    public void disconnect(){

        if(isConnected()){

            socket.close();

            connected = false;

            for(Request request: requests){

                request.cb.onError("disconnected");


            }

            requests.clear();

        }

    }



    private void connectToServer(){

        if(hasSSID()){
            if(showConnectingDialog){
                showDialog();
            }

            // Connect to websocket server
            Uri uri = Uri.parse(getBaseUrl());

            final AsyncHttpRequest req = new AsyncHttpRequest(uri,"GET",new Headers().add("Cookie","flood-ssid="+getSSID()+";"));

            AsyncHttpClient.getDefaultInstance().websocket(req, null, new AsyncHttpClient.WebSocketConnectCallback() {
                @Override
                public void onCompleted(Exception ex, WebSocket webSocket) {
                    if(ex != null){
                        Log.e("ApiBase.java",ex.getMessage());
                    }else{
                        connected = true;
                        socket = webSocket;

                        hideDialog();

                        if(readyCallback != null){
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    readyCallback.onConnected();
                                }
                            });

                        }

                        for(String pending: pendingRequests){

                            socket.send(pending);

                        }

                        pendingRequests.clear();



                        webSocket.setClosedCallback(new CompletedCallback() {
                            @Override
                            public void onCompleted(Exception ex) {
                                if(ex != null) {
                                    connectToServer();
                                }
                            }
                        });

                        webSocket.setStringCallback(new WebSocket.StringCallback() {
                            @Override
                            public void onStringAvailable(final String s) {

                                try{

                                    final JSONObject data = new JSONObject(s);

                                    final String messageType = data.getString("messageType");
                                    final String requestId = data.getString("requestId");

                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                switch (messageType) {

                                                    case "response":

                                                        for (Request request : requests) {
                                                            if (request.requestId.equals(requestId)) {
                                                                request.cb.onResult(data.getJSONObject("data").toString());
                                                            }
                                                        }

                                                        break;
                                                    case "error":

                                                        for (Request request : requests) {

                                                            if (request.requestId.equals(requestId)) {
                                                                request.cb.onError(data.getJSONObject("error").getString("errorCode"));
                                                            }
                                                        }

                                                        break;

                                                    case "done":

                                                        for(int i = 0; i < requests.size();i++){
                                                            if(requests.get(i).requestId.equals(requestId)){
                                                                requests.remove(i);
                                                                break;
                                                            }
                                                        }

                                                }
                                            }catch (Exception e){
                                                e.printStackTrace();
                                            }
                                        }
                                    });





                                }catch(Exception e){
                                    e.printStackTrace();
                                }

                            }
                        });
                    }
                }
            });

        }else{
            fetchSSID();
        }
    }



    public <T> void request(Request<T> request){
        Log.d("Request:",request.buildRequest());

        requests.add(request);

        if(isConnected()){
            try{
                socket.send(request.buildRequest());
                
                return;
            }catch(Exception e){

            }
        }

        
        pendingRequests.add(request.buildRequest());


    }

    public <T> void cancelRequest(Request<T> request){

        if(isConnected()){
            try{
                socket.send(request.cancelRequest());

                return;
            }catch(Exception e){

            }
        }

        pendingRequests.add(request.cancelRequest());

    }



    public void showDialog(){
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog = ProgressDialog.show(activity,activity.getString(R.string.connecting),activity.getString(R.string.connectingMessage),true,false);
            }
        });

    }

    public void hideDialog(){
        if(progressDialog != null){
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.hide();
                }
            });

        }
    }

    public void setReadyCallback(ReadyCallback cb){
        readyCallback = cb;

        if(connected){
            cb.onConnected();
        }
    }

    public static abstract class ReadyCallback{
        public abstract void onConnected();
    }

    /*
        Session methods
     */
    private void setSSID(String ssid){
        activity.getSharedPreferences(settings_name,0)
                .edit()
                .putString("ssid",ssid)
                .apply();
    }

    public void resetSSID(){
        activity
                .getSharedPreferences(settings_name,0)
                .edit()
                .remove("ssid")
                .apply();
    }

    public String getSSID(){
        return activity
                .getSharedPreferences(settings_name,0)
                .getString("ssid","");

    }

    private boolean hasSSID(){
        return activity.getSharedPreferences(settings_name,0).contains("ssid");
    }


    public boolean isConnected() {
        return connected;
    }

    private void fetchSSID(){

        AsyncHttpClient.getDefaultInstance().executeString(new AsyncHttpGet(getBaseUrl()+"main/about"),new AsyncHttpClient.StringCallback(){
            @Override
            public void onCompleted(Exception e, AsyncHttpResponse source, String result) {
                if(e != null){ e.printStackTrace(); }
                try{
                    String ssid = source.headers().get("Set-Cookie");
                    ssid = ssid.substring(11,47);
                    setSSID(ssid);
                    connectToServer();

                }catch (Exception parseException){
                    Log.e("ApiBase.java",parseException.getMessage());
                }
            }
        });
    }
}
