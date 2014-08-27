package com.cardiomood.android.tools;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;

public class ReachabilityTest extends AsyncTask<Void, Void, Boolean> {
    
    public interface Callback {
        void onReachabilityTestPassed();
        
        void onReachabilityTestFailed();
    }
    
    private Context mContext;
    
    private String mHostname;
    
    private int mServicePort;
    
    private Callback mCallback;
    
    public ReachabilityTest(Context context, String hostname, int port, Callback callback) {
        mContext = context.getApplicationContext(); // Avoid leaking the Activity!
        mHostname = hostname;
        mServicePort = port;
        mCallback = callback;
    }
    
    @Override
    protected Boolean doInBackground(Void... args) {
        if (isConnected(mContext)) {
            InetAddress address = resolve(mHostname);
            if (address != null) {
                if (canConnect(address, mServicePort)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        if (mCallback != null) {
            if (result) {
                mCallback.onReachabilityTestPassed();
            }
            else {
                mCallback.onReachabilityTestFailed();
            }
        }
    }
    
    private boolean isConnected(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
    
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        }
        
        return false;
    }
    
    private InetAddress resolve(String hostname) {
        try {
            return InetAddress.getByName(hostname);
        }
        catch (UnknownHostException e) {
            return null;
        }
    }
    
    private boolean canConnect(InetAddress address, int port) {
        Socket socket = new Socket();
    
        SocketAddress socketAddress = new InetSocketAddress(address, port);
        
        try {
            socket.connect(socketAddress, 2000);
        }
        catch (IOException e) {
            return false; 
        }
        finally {
            if (socket.isConnected()) {
                try {
                    socket.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
        return true;
    }
    
}