package com.example.filetransferapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class IPAddressUtil {

    @SuppressLint("DefaultLocale")
    public static String getIPAddress(Context context) {

        //public static String getIPAddress(Context context) {    // update this if below uncomment
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network = cm.getActiveNetwork();

        LinkProperties linkProperties = cm.getLinkProperties(network);

        if (linkProperties != null) {
            for (LinkAddress address : linkProperties.getLinkAddresses()) {
                InetAddress inetAddress = address.getAddress();
                if (!inetAddress.isLoopbackAddress() && inetAddress.getHostAddress().contains(".")) {
                    return inetAddress.getHostAddress();
                }
            }
        }
        return null;

    }

    public static String getLocalIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface networkInterface : interfaces) {
                if (!networkInterface.isLoopback() && networkInterface.isUp()) {
                    List<java.net.InetAddress> addresses = Collections.list(networkInterface.getInetAddresses());
                    for (java.net.InetAddress address : addresses) {
                        if (!address.isLoopbackAddress() && address instanceof java.net.Inet4Address) {
                            return address.getHostAddress();
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }
}
