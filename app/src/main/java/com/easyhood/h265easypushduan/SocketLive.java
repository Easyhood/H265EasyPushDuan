package com.easyhood.h265easypushduan;

import android.media.projection.MediaProjection;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

/**
 * 功能：WebSocket连接管理
 * 详细描述：
 * 作者：guan_qi
 * 创建日期：2023-03-21
 */
public class SocketLive {
    private static final String TAG = "SocketLive";
    // 另外一台设备的socket ---》发送数据
    private WebSocket webSocket;
    CodecLiveH264 codecLiveH264;

    /**
     * 开始连接socket 并开始录屏
     * @param mediaProjection MediaProjection
     */
    public void start(MediaProjection mediaProjection) {
        webSocketServer.start();
        codecLiveH264 = new CodecLiveH264(this, mediaProjection);
        codecLiveH264.startLive();
    }

    /**
     * 发送socket数据
     * @param bytes byte[]
     */
    public void sendData(byte[] bytes) {
        if (webSocket != null && webSocket.isOpen()) {
            webSocket.send(bytes);
        }
    }

    /**
     * WebSocketServer 实体对象
     */
    private WebSocketServer webSocketServer = new WebSocketServer() {
        @Override
        public void onOpen(WebSocket webSocket, ClientHandshake handshake) {
            SocketLive.this.webSocket = webSocket;
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {

        }

        @Override
        public void onMessage(WebSocket conn, String message) {

        }

        @Override
        public void onError(WebSocket conn, Exception ex) {

        }

        @Override
        public void onStart() {

        }
    };

}
