package com.ucsf.core.services;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.ucsf.core.data.AbstractConnection;
import com.ucsf.core.data.DataManager;
import com.ucsf.core.data.Entry;
import com.ucsf.core.data.JSON;
import com.ucsf.core.services.Messages.Event;
import com.ucsf.core.services.Messages.Message;
import com.ucsf.core.services.Messages.RequestMessage;
import com.ucsf.core.services.Messages.RequestReplyMessage;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * Service responsible of the communication between phone and watch. Allows exchange of events,
 * requests and data. Events are sent once and may not be received. Requests are sent as long no
 * answer is received until they time out. Data are sent once but you can detect when the
 * transaction fails.
 *
 * @author  Julien Jacquemot
 * @version 1.0
 */
public abstract class DeviceInterface extends WearableListenerService {
    private static final String                         TAG                    = "ucsf:DataLayerListener";
    private static final long                           REQUEST_TIMER_PERIOD   = 5000;   // 5 seconds
    private static final long                           REQUEST_TIMEOUT_PERIOD = 300000; // 5 minutes
    private static final Map<Messages.Request, Request> mPendingRequests       = new HashMap<>();
    private static final Connection                     mConnection            = new Connection();

    /** Class allowing multiple Google API connections. */
    protected static class Connection extends AbstractConnection {
        private GoogleApiClient mGoogleApiClient = null;

        @Override
        protected void openConnection(Context context) throws Exception {
            if (mGoogleApiClient == null)
                mGoogleApiClient = new GoogleApiClient.Builder(context)
                        .addApi(Wearable.API)
                        .build();
            ConnectionResult res = mGoogleApiClient.blockingConnect(30, TimeUnit.SECONDS);
            if (!res.isSuccess())
                throw new Exception("Google Api Client connection timed out!");
        }

        @Override
        protected void closeConnection() {
            mGoogleApiClient.disconnect();
        }

        public GoogleApiClient getClient() {
            return mGoogleApiClient;
        }
    }

    /**
     * Sends a {@link Messages.Message message} containing the given data. A message can either an
     * event, a request or a request answer.
     */
    private static void sendMessage(final Context context, final Message message, JSONObject data) {
        final String dataString = data.toString();
        final byte[] dataContent = dataString.getBytes();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try (Connection instance = openConnection(context)) {
                    // Get nodes
                    List<Node> nodes =
                            Wearable.NodeApi.getConnectedNodes(instance.getClient()).await().getNodes();

                    // Send the message
                    Log.d(TAG, String.format("Sending to %d nodes the message %s:%s",
                            nodes.size(), message.getTag(), dataString));

                    for (Node node : nodes) {
                        Wearable.MessageApi.sendMessage(instance.getClient(), node.getId(), message.getTag(),
                                dataContent).setResultCallback(
                                new ResultCallback<MessageApi.SendMessageResult>() {
                                    @Override
                                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                                        if (!sendMessageResult.getStatus().isSuccess()) {
                                            Log.e(TAG, String.format("Failed to send message '%s' with status code: %s",
                                                    message.getTag(), sendMessageResult.getStatus().getStatusCode()));
                                        } else {
                                            Log.d(TAG, String.format("Message '%s' successfully sent.", message.getTag()));
                                        }
                                    }
                                });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to send a message the Google Api: ", e);
                }

                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Sends an event containing the given value. This value is identified by the key
     * {@link Messages#VALUE}.
     */
    protected static void sendEvent(Context context, Event event, Serializable value) {
        sendEvent(context, event, new Entry(Messages.VALUE, value));
    }

    /**
     * Sends an event containing the given entries.
     */
    public static void sendEvent(Context context, Event event, Entry... entries) {
        sendEvent(context, event, JSON.create(entries));
    }

    /**
     * Sends an event containing the given entries.
     */
    protected static void sendEvent(Context context, Event event, JSONObject data) {
        sendMessage(context, event, data);
    }

    /**
     * Sends a request for the given message until an answer is received. Returns the request id.
     * If a request for the same message is already running, cancels it first, i.e. only one
     * request for a given message is running at the same time. The given value is identified by
     * the key {@link Messages#VALUE}.
     */
    protected static void sendRequest(Context context, Messages.Request request,
                                      RequestListener listener, Serializable value) {
        sendRequest(context, request, listener, new Entry(Messages.VALUE, value));
    }

    /**
     * Sends a request for the given message until an answer is received. Returns the request id.
     * If a request for the same message is already running, cancels it first, i.e. only one
     * request for a given message is running at the same time.
     */
    protected static void sendRequest(Context context, Messages.Request request,
                                      RequestListener listener, Entry... entries) {
        sendRequest(context, request, listener, JSON.create(entries));
    }

    /**
     * Sends a request for the given message until an answer is received. Returns the request id.
     * If a request for the same message is already running, cancels it first, i.e. only one
     * request for a given message is running at the same time.
     */
    protected static void sendRequest(Context context, Messages.Request request,
                                      RequestListener listener, JSONObject data) {
        try {
            cancelRequest(request);
            new Request(context, request, data, listener).send();
        } catch (Exception e) {
            Log.e(TAG, String.format("Failed to send request for message '%s':", request.getTag()), e);
        }
    }

    /**
     * Cancels the given request
     */
    public static void cancelRequest(Messages.Request request) {
        synchronized (mPendingRequests) {
            Request pendingRequest = mPendingRequests.get(request);
            if (pendingRequest != null)
                pendingRequest.cancel();
        }
    }

    /**
     * Replies to the given request. The given value is identified by the key
     * {@link Messages#VALUE}.
     */
    protected static void replyToRequest(Context context, Messages.Request request, Serializable value) {
        replyToRequest(context, request, new Entry(Messages.VALUE, value));
    }

    /**
     * Replies to the given request.
     */
    protected static void replyToRequest(Context context, Messages.Request request, Entry... entries) {
        replyToRequest(context, request, JSON.create(entries));
    }

    /**
     * Replies to the given request.
     */
    protected static void replyToRequest(Context context, Messages.Request request, JSONObject data) {
        try {
            sendMessage(context, new RequestReplyMessage(request), data);
        } catch (Exception e) {
            Log.e(TAG, String.format("Failed to reply to the request for message '%s':",
                    request.getTag()), e);
        }
    }

    /**
     * Formats the table entry into a valid unique message path.
     */
    protected static String createTableEntryPath(DataManager.Table table, DataManager.Cursor cursor)
    {
        return String.format("/%s/%s", table.tag, cursor.getInt(DataManager.KEY_ROW_ID));
    }

    /**
     * Parses the given message path and return the corresponding table name and entry row id.
     * If the path is ill-formed, throws an excpetion.
     */
    protected static Pair<String, Integer> parseMessagePath(String path) throws Exception {
        String[] tags = path.split("/");
        if (tags.length != 3)
            throw new Exception("Invalid path format: " + path);

        String table = tags[1];
        int    rowId = Integer.valueOf(tags[2]);

        return new Pair<>(table, rowId);
    }

    /**
     * Pushes the given data through the Google API.
     */
    protected static void sendData(Connection connection, PutDataMapRequest data,
                                   ResultCallback<DataApi.DataItemResult> callback)
            throws Exception
    {
        Wearable.DataApi.putDataItem(connection.getClient(), data.asPutDataRequest())
                .setResultCallback(callback);
    }

    /**
     * Removes the given data from the Google API pending list.
     */
    protected static void removeData(Connection connection, Uri uri) throws Exception {
        Wearable.DataApi.deleteDataItems(connection.getClient(), uri);
    }

    /**
     * Opens the connection to the Google API. Doesn't need to be explicitly called if you used
     * the provided interface.
     */
    protected static Connection openConnection(Context context) throws Exception {
        return (Connection) mConnection.open(context);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        // Read the event data
        JSONObject data = null;
        byte[] eventData = messageEvent.getData();
        if (eventData != null) {
            String messageContent = new String(eventData);
            try {
                data = new JSONObject(messageContent);
            } catch (Exception e) {
                Log.e(TAG, String.format("Failed to read message '%s': ", messageContent), e);
                return;
            }
        }

        // Broadcast the message
        String messageTag = messageEvent.getPath();
        try {
            if (Messages.isRequestMessage(messageTag)) {
                Messages.Request request = Messages.getRequestFromRequestMessage(messageTag);
                if (request != Messages.Request.INVALID)
                    onRequestReceived(request, data);
                else
                    Log.e(TAG, String.format("Invalid request message '%s'!", messageTag));
            } else if (Messages.isRequestReplyMessage(messageTag)) {
                Messages.Request request = Messages.getRequestFromRequestReplyMessage(messageTag);
                if (request != Messages.Request.INVALID) {
                    synchronized (mPendingRequests) {
                        Request pendingRequest = mPendingRequests.get(request);
                        if (pendingRequest != null)
                            pendingRequest.complete(data);
                    }
                } else
                    Log.e(TAG, String.format("Invalid request message '%s'!", messageTag));
            } else {
                Event event = Event.fromTag(messageTag);
                if (event != Event.INVALID)
                    onEventReceived(event, data);
                else
                    Log.e(TAG, String.format("Invalid event '%s'!", messageTag));
            }
        } catch (Exception e) {
            Log.e(TAG, "An error occurred while processing message: ", e);
        }
    }

    /**
     * Method called when an event is received.
     */
    protected abstract void onEventReceived(Event event, JSONObject data) throws Exception;

    /**
     * Method called when a request is received.
     */
    protected abstract void onRequestReceived(Messages.Request request, JSONObject data) throws Exception;

    /**
     * Interface used when sending a request.
     */
    public interface RequestListener {
        /**
         * Method called when an answer to the request is received.
         * @param request Type of the method that received an answer.
         * @param data    Data included in the request answer.
         */
        void requestProcessed(Messages.Request request, JSONObject data) throws Exception;

        /**
         * Method called when the request times out without receiving an answer.
         * @param request Type of the timed out request.
         */
        void requestTimeout(Messages.Request request) throws Exception;

        /**
         * Method called if the request is cancelled, i.e. when a new request of the same type is
         * started or when an explicit call to {@link DeviceInterface#cancelRequest(Messages.Request)}
         * is made.
         * @param request Type of the cancelled request.
         */
        void requestCancelled(Messages.Request request) throws Exception;
    }

    /** Internal representation of a request. */
    private static class Request {
        public final Context         context;
        public final RequestMessage  message;
        public final JSONObject      data;
        public final Timer           timer;
        public final RequestListener listener;
        public final long            firstTry;

        public Request(Context context, Messages.Request request, JSONObject data,
                       RequestListener listener)
        {
            this.context  = context;
            this.message  = new RequestMessage(request);
            this.data     = data;
            this.listener = listener;
            this.timer    = new Timer();
            this.firstTry = System.currentTimeMillis();
        }

        public void send() {
            synchronized (mPendingRequests) {
                mPendingRequests.put(message.request, this);
            }

            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (System.currentTimeMillis() - firstTry > REQUEST_TIMEOUT_PERIOD) {
                        try {
                            stopTimer();
                            listener.requestTimeout(message.request);
                        } catch (Exception e) {
                            Log.e(TAG, String.format("Message '%s' timed out with errors: ",
                                    message.request.getTag()), e);
                        }
                    } else
                        sendMessage(context, message, data);
                }
            }, 0, REQUEST_TIMER_PERIOD);
        }

        public void cancel() {
            try {
                stopTimer();
                listener.requestCancelled(message.request);
            } catch (Exception e) {
                Log.e(TAG, String.format("Message '%s' cancelled with errors: ",
                        message.request.getTag()), e);
            }
        }

        public void complete(JSONObject data) {
            try {
                stopTimer();
                listener.requestProcessed(message.request, data);
            } catch (Exception e) {
                Log.e(TAG, String.format("Failed to process request '%s' answer: ",
                        message.request.getTag()), e);
            }
        }

        private void stopTimer() {
            timer.cancel();
            synchronized (mPendingRequests) {
                mPendingRequests.remove(message.request);
            }
        }
    }

}
