package com.easyhood.h265easypushduan;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * 功能：h264推流编码
 * 详细描述：
 * 作者：guan_qi
 * 创建日期：2023-03-21
 */
public class CodecLiveH264 extends Thread{
    public static final int NAL_I = 5;
    public static final int NAL_SPS = 7;
    private static final String TAG = "CodecLiveH264";
    private MediaProjection mediaProjection;
    private MediaCodec mediaCodec;
    private int width = 720;
    private int height = 1280;
    private byte[] sps_pps_buf;
    private VirtualDisplay virtualDisplay;
    private SocketLive socketLive;

    /**
     * 构造方法
     * @param socketLive SocketLive
     * @param mediaProjection MediaProjection
     */
    public CodecLiveH264(SocketLive socketLive, MediaProjection mediaProjection) {
        this.socketLive = socketLive;
        this.mediaProjection = mediaProjection;
    }

    /**
     * 开始直播投屏
     */
    public void startLive() {
        try {
            // mediacodec 中间联系人
            MediaFormat format = MediaFormat.createVideoFormat("video/avc", width, height);
            // 颜色调解器
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            // 设置比特率
            format.setInteger(MediaFormat.KEY_BIT_RATE, width * height);
            // 设置帧率
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 20);
            // 设置关键帧率
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            mediaCodec = MediaCodec.createEncoderByType("video/avc");
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            Surface surface = mediaCodec.createInputSurface();
            // 创建场地
            virtualDisplay = mediaProjection.createVirtualDisplay(
                    "-display",
                    width, height, 1,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, surface,
                    null, null);
        }catch (IOException e) {
            e.printStackTrace();
        }
        start();
    }

    @Override
    public void run() {
        super.run();
        mediaCodec.start();
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        while (true) {
            try {
                int outputBufferId = mediaCodec.dequeueOutputBuffer(bufferInfo,
                        10000);
                if (outputBufferId >= 0) {
                    //sps
                    ByteBuffer byteBuffer = mediaCodec.getOutputBuffer(outputBufferId);
                    dealFrame(byteBuffer, bufferInfo);
                    mediaCodec.releaseOutputBuffer(outputBufferId, false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 处理视频流
     * @param bb ByteBuffer
     * @param bufferInfo MediaCodec.BufferInfo
     */
    private void dealFrame (ByteBuffer bb, MediaCodec.BufferInfo bufferInfo) {
        int offset = 4;
        if (bb.get() == 0x01) {
            offset = 3;
        }
        int type = (bb.get(offset) & 0x1F);
        // sps 只会输出一份 非常宝贵
        if (NAL_SPS == type) {
            sps_pps_buf = new byte[bufferInfo.size];
            bb.get(sps_pps_buf);
        } else if (NAL_I == type) {
            final byte[] bytes = new byte[bufferInfo.size];
            // 45459
            bb.get(bytes);
            byte[] newBuf = new byte[sps_pps_buf.length + bytes.length];
            System.arraycopy(sps_pps_buf, 0, newBuf, 0, sps_pps_buf.length);
            System.arraycopy(bytes, 0, newBuf, sps_pps_buf.length, bytes.length);
            socketLive.sendData(newBuf);
        } else {
            final byte[] bytes = new byte[bufferInfo.size];
            bb.get(bytes);
            this.socketLive.sendData(bytes);
            Log.d(TAG, "dealFrame: 视频数据 = " + Arrays.toString(bytes));
        }
    }

    /**
     * 输出H264 16进制文本文档
     * @param array byte[]
     * @return sb.toString()
     */
    public String writeContent(byte[] array) {
        char[] HEX_CHAR_TABLE = {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
        };
        StringBuilder sb = new StringBuilder();
        for (byte b : array) {
            sb.append(HEX_CHAR_TABLE[(b & 0xf0) >> 4]);
            sb.append(HEX_CHAR_TABLE[b & 0x0f]);
        }
        FileWriter writer = null;
        try {
            // 打开一个写文件器，构造函数中的第二个参数true表示以追加形式写文件
            writer = new FileWriter(Environment.getExternalStorageDirectory() + "/codecH264.txt", true);
            writer.write(sb.toString());
            writer.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    /**
     * 输出h264文件
     * @param array byte[]
     */
    public void writeBytes(byte[] array) {
        FileOutputStream writer = null;
        try {
            // 打开一个写文件器，构造函数中的第二个参数true表示以追加形式写文件
            writer = new FileOutputStream(Environment.getExternalStorageDirectory() + "/codec.h264", true);
            writer.write(array);
            writer.write('\n');
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
