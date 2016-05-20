import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.UUID;

public class Request<Result>{
    public String messageType = "request";
    public String namespace;
    public String action;
    public String requestId;

    public Object params;

    public transient  RequestCallback cb;
    private static Gson gson = new GsonBuilder().create();

    public Request(String namespace,String action,RequestCallback resultCallback,Object params){
        this.namespace = namespace;
        this.action = action;
        this.cb = resultCallback;
        this.requestId = UUID.randomUUID().toString();
        this.params = params;

    }

    public Request(String namespace,String action,RequestCallback resultCallback){
        this.namespace = namespace;
        this.action = action;
        this.cb = resultCallback;
        this.requestId = UUID.randomUUID().toString();
        this.params = new EmptyParams();

    }


    public String buildRequest(){ return gson.toJson(this); }

    public String cancelRequest(){
        // Build a new cancelRequest object
        CancelRequest cancelRequest = new CancelRequest();
        cancelRequest.requestId = this.requestId;
        // Return the json representation
        return gson.toJson(cancelRequest);

    }

    private class EmptyParams{
    }

    private class CancelRequest{
        public String requestId;
        public String messageType = "cancelRequest";
    }

    private class ResponseEnvelope{

        public String messageType;
        public String responseId;
        public Error error;


        private class Error{
            private String errorCode;
            private String descrition;
        }
    }

    public static abstract class RequestCallback{
        public abstract void onResult(String result);
        public abstract void onError(String error);
    }
}