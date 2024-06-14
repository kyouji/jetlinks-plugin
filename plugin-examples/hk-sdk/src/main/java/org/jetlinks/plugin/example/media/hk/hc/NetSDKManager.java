package org.jetlinks.plugin.example.media.hk.hc;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.exception.BusinessException;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.File;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.sun.jna.Platform.isLinux;
import static com.sun.jna.Platform.isWindows;
import static org.jetlinks.plugin.example.media.hk.hc.HCNetSDK.*;

/**
 * @create 2020-07-27-10:42
 */
@Slf4j
public class NetSDKManager {
    static HCNetSDK hCNetSDK = null;
    public static FExceptionCallBack_Imp fExceptionCallBack;

    static class FExceptionCallBack_Imp implements HCNetSDK.FExceptionCallBack {
        public void invoke(int dwType,
                           int lUserID,
                           int lHandle,
                           Pointer pUser) {
            System.out.println("异常事件类型:" + dwType);
            return;
        }
    }

//    @SneakyThrows
//    public static void main(String[] args) {
//        NetSDKManager sdk = new NetSDKManager();
//        sdk.initSDKInstance(System.getProperty("user.dir") + "\\lib\\win");
//
//        int version = hCNetSDK.NET_DVR_GetSDKVersion();
//        log.info("版本：{}", version);
//
//        Tuple2<Integer, HCNetSDK.NET_DVR_DEVICEINFO_V40> loginResult = sdk.Login_V40(
//                "192.168.33.157", (short) 8000, "admin", "p@ssw0rd"
//        );
//        Integer userId = loginResult.getT1();
//        HCNetSDK.NET_DVR_DEVICEINFO_V40 deviceinfo = loginResult.getT2();
//        boolean online = sdk.getDeviceStatus(userId);
//        System.out.println("在线：" + online);
//        int channel = deviceinfo.struDeviceV30.byChanNum;
//
//        List<HCNetSDK.NET_DVR_PRESET_NAME> presetNames = sdk.queryPreset(userId);
//        for (HCNetSDK.NET_DVR_PRESET_NAME data : presetNames) {
//            log.info("preset: {}, {}", String.valueOf(data.wPresetNum), new String(data.byName, "gbk"));
//        }
//
//        HCNetSDK.NET_DVR_PU_STREAM_CFG streamConfig = sdk.getStreamConfig(userId);
//        log.info("推流配置信息：, lastError: {}", hCNetSDK.NET_DVR_GetLastError());
//
////        MediaSdkDeviceControl.Rtp rtp = new MediaSdkDeviceControl.Rtp(true, "192.168.32.243", 9100, "UDP");
////        boolean setRtp = sdk.setStreamConfig(userId, rtp, Charset.forName("gbk"));
//        Pointer pointer = streamConfig.getPointer();
//
//        IntByReference pInt = new IntByReference(0);
//        // NET_DVR_SET_PU_STREAMCFG = 186
//        boolean result = hCNetSDK.NET_DVR_SetDVRConfig(userId, 186, 0xFFFFFFFF,
//                                                       pointer, streamConfig.size());
//        log.info("修改推流配置信息：{}", result);
//        if (!result) {
//            log.error("设置推流配置失败，错误码：" + hCNetSDK.NET_DVR_GetLastError());
//        }
//
//        System.out.println(sdk.startRecord(userId, channel));
//        Thread.sleep(1000);
//        System.out.println(sdk.stopRecord(userId, channel));
//
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//        List<HCNetSDK.NET_DVR_FINDDATA_V30> records = sdk
//                .queryRecord(
//                        userId,
//                        channel,
//                        LocalDateTime.parse("2023-11-03 00:00:00", formatter),
//                        LocalDateTime.parse("2023-11-03 23:59:59", formatter));
//        log.info("录像数量：{}", records.size());
//
//        int rtspPort = sdk.queryRtspPort(userId);
//        log.info("rtsp port: {}", rtspPort);
//
//        HCNetSDK.NET_DVR_ACCESS_DEVICE_CHANNEL_INFO channelInfo = sdk.queryChannel(
//                userId,
//                deviceinfo.struDeviceV30.byChanNum,
//                "192.168.33.157",
//                "p@ssw0rd",
//                "admin",
//                (short) 8000
//        );
//        log.info("通道数量：{}", channelInfo.dwTotalChannelNum + 1);
//
////        LinkedList<Integer> channelNo = sdk.createDeviceTreeV40(userInfo.getUserId(), userInfo.getDeviceInfo());
////        log.info("channel: {}", channelNo);
//    }


    /**
     * 设备登录V30
     *
     * @param ip   设备IP
     * @param port SDK端口，默认设备的8000端口
     * @param user 设备用户名
     * @param psw  设备密码
     */
    public static int Login_V30(String ip,
                                short port,
                                String user,
                                String psw) {
        HCNetSDK.NET_DVR_DEVICEINFO_V30 m_strDeviceInfo = new HCNetSDK.NET_DVR_DEVICEINFO_V30();
        int lUserID = hCNetSDK.NET_DVR_Login_V30(ip, port, user, psw, m_strDeviceInfo);
        System.out.println("UsID:" + lUserID);
        if ((lUserID == -1) || (lUserID == 0xFFFFFFFF)) {
            System.out.println("登录失败，错误码为" + hCNetSDK.NET_DVR_GetLastError());
            return lUserID;
        } else {
            System.out.println(ip + ":设备登录成功！");
            return lUserID;
        }
    }

    /**
     * 设备登录V40 与V30功能一致
     *
     * @param ip   设备IP
     * @param port SDK端口，默认设备的8000端口
     * @param user 设备用户名
     * @param psw  设备密码
     */
    public Tuple2<Integer, HCNetSDK.NET_DVR_DEVICEINFO_V40> Login_V40(String ip,
                                                                      short port,
                                                                      String user,
                                                                      String psw) {
        //注册
        HCNetSDK.NET_DVR_USER_LOGIN_INFO m_strLoginInfo = new HCNetSDK.NET_DVR_USER_LOGIN_INFO();//设备登录信息
        HCNetSDK.NET_DVR_DEVICEINFO_V40 m_strDeviceInfo = new HCNetSDK.NET_DVR_DEVICEINFO_V40();//设备信息

        String m_sDeviceIP = ip;//设备ip地址
        m_strLoginInfo.sDeviceAddress = new byte[HCNetSDK.NET_DVR_DEV_ADDRESS_MAX_LEN];
        System.arraycopy(m_sDeviceIP.getBytes(), 0, m_strLoginInfo.sDeviceAddress, 0, m_sDeviceIP.length());

        String m_sUsername = user;//设备用户名
        m_strLoginInfo.sUserName = new byte[HCNetSDK.NET_DVR_LOGIN_USERNAME_MAX_LEN];
        System.arraycopy(m_sUsername.getBytes(), 0, m_strLoginInfo.sUserName, 0, m_sUsername.length());

        String m_sPassword = psw;//设备密码
        m_strLoginInfo.sPassword = new byte[HCNetSDK.NET_DVR_LOGIN_PASSWD_MAX_LEN];
        System.arraycopy(m_sPassword.getBytes(), 0, m_strLoginInfo.sPassword, 0, m_sPassword.length());

        m_strLoginInfo.wPort = port;
        m_strLoginInfo.bUseAsynLogin = false; //是否异步登录：0- 否，1- 是
//        m_strLoginInfo.byLoginMode=1;  //ISAPI登录
        m_strLoginInfo.write();

        int iUserID = hCNetSDK.NET_DVR_Login_V40(m_strLoginInfo, m_strDeviceInfo);
        if (iUserID < 0) {
            System.out.println("登录失败，错误码为" + hCNetSDK.NET_DVR_GetLastError());
            return null;
        } else {
            System.out.println(ip + ":设备登录成功！userId:" + iUserID);
//            Map<String ,Object> userInfo = new HashMap<>();
//            userInfo.put("userId", iUserID);
//            userInfo.put("charset", m_strDeviceInfo.byCharEncodeType);
            return Tuples.of(iUserID, m_strDeviceInfo);
        }
    }

    //设备在线状态监测
    public boolean getDeviceStatus(int lUserID) {
        return hCNetSDK.NET_DVR_RemoteControl(lUserID, HCNetSDK.NET_DVR_CHECK_USER_STATUS, null, 0);
    }

    public HCNetSDK.LPNET_DVR_LINK_ADDR realPlay(int userId , MediaSdkChannelConfig config) {
        int lLinkHandle = preview(userId, config);
        HCNetSDK.LPNET_DVR_LINK_ADDR lpLinkAddr = new HCNetSDK.LPNET_DVR_LINK_ADDR();
        hCNetSDK.NET_DVR_GetLinkAddr(lLinkHandle, 1, lpLinkAddr);
        return lpLinkAddr;
    }

    public HCNetSDK.LPNET_DVR_LINK_ADDR playback(int userId , MediaSdkChannelConfig config) {
        int lLinkHandle = preview(userId, config);
        HCNetSDK.LPNET_DVR_LINK_ADDR lpLinkAddr = new HCNetSDK.LPNET_DVR_LINK_ADDR();
        hCNetSDK.NET_DVR_GetLinkAddr(lLinkHandle, 2, lpLinkAddr);
        return lpLinkAddr;
    }

    public LinkedList<Integer> createDeviceTreeV40(int userId, HCNetSDK.NET_DVR_DEVICEINFO_V40 deviceinfo) {
        LinkedList<Integer> channelNo = new LinkedList<>();
        HCNetSDK.NET_DVR_DEVICEINFO_V30 strDeviceInfo = deviceinfo.struDeviceV30;
        IntByReference ibrBytesReturned = new IntByReference(0);// get ip param
        HCNetSDK.NET_DVR_IPPARACFG strIpparaCfg = new HCNetSDK.NET_DVR_IPPARACFG();
        Pointer lpIpParaConfig = strIpparaCfg.getPointer();
        boolean bRet = hCNetSDK.NET_DVR_GetDVRConfig(userId, HCNetSDK.NET_DVR_GET_IPPARACFG, 1, lpIpParaConfig,
                                                     strIpparaCfg.size(), ibrBytesReturned);
        log.info("获取设备信息：{}", bRet);
        strIpparaCfg.read();
        if (!bRet) {
            // device not support,has no ip camera
            for (int iChannum = 0; iChannum < strDeviceInfo.byChanNum; iChannum++) {
                log.info("通道号：{}", iChannum + strDeviceInfo.byStartChan + 32);
                channelNo.add(iChannum + strDeviceInfo.byStartChan + 32);
            }
        } else {
            // ip camera
            for (int iChannum = 0; iChannum < strDeviceInfo.byChanNum; iChannum++) {
                {
                    log.info("通道号：{}", iChannum + strDeviceInfo.byStartChan + 32);
                    channelNo.add(iChannum + strDeviceInfo.byStartChan + 32);
                }
            }
            for (int iChannum = 0; iChannum < strDeviceInfo.byIPChanNum; iChannum++) {
                log.info("通道号：{}", iChannum + strDeviceInfo.byStartChan + 32);
                channelNo.add(iChannum + strDeviceInfo.byStartChan + 32);
            }
        }
        return channelNo;
    }

    // 查询rtsp端口
    public int queryRtspPort(int lUserID) {
        HCNetSDK.NET_DVR_RTSPCFG rtspcfg = new HCNetSDK.NET_DVR_RTSPCFG();
        rtspcfg.dwSize = rtspcfg.size();

        hCNetSDK.NET_DVR_GetRtspConfig(lUserID, 0, rtspcfg, rtspcfg.size());
        return rtspcfg.wPort;
    }

    // 查询通道
    public HCNetSDK.NET_DVR_ACCESS_DEVICE_CHANNEL_INFO queryChannel(int iUserID,
                                                                    int dwChannel,
                                                                    String userName,
                                                                    String password,
                                                                    String ip,
                                                                    short port) {
        HCNetSDK.NET_DVR_ACCESS_DEVICE_INFO accessDevice = new HCNetSDK.NET_DVR_ACCESS_DEVICE_INFO();
        accessDevice.read();
        accessDevice.dwSize = accessDevice.size();
        System.arraycopy(userName.getBytes(), 0, accessDevice.sUserName, 0, userName.length());
        System.arraycopy(password.getBytes(), 0, accessDevice.szPassword, 0, password.length());
        HCNetSDK.NET_DVR_IPADDR ipaddr = new HCNetSDK.NET_DVR_IPADDR();
        System.arraycopy(ip.getBytes(), 0, ipaddr.sIpV4, 0, ip.length());
        accessDevice.wPort = port;
        //通道号说明：一般IPC/IPD通道号为1，32路以及以下路数的NVR的IP通道通道号从33开始，64路及以上路数的NVR的IP通道通道号从1开始。
        accessDevice.write();

        HCNetSDK.NET_DVR_ACCESS_DEVICE_CHANNEL_INFO info = new HCNetSDK.NET_DVR_ACCESS_DEVICE_CHANNEL_INFO();
        info.read();
        info.dwSize = info.size();
        info.write();
        IntByReference list = new IntByReference(0);
        boolean b_GetResult = hCNetSDK.NET_DVR_GetDeviceConfig(iUserID,
                                                               HCNetSDK.NET_DVR_GET_ACCESS_DEVICE_CHANNEL_INFO,
                                                               dwChannel,
                                                               accessDevice.getPointer(),
                                                               accessDevice.size(),
                                                               list.getPointer(),
                                                               info.getPointer(),
                                                               info.size());
        if (b_GetResult == false) {
            System.out.println("设备通道查询失败，错误码：" + hCNetSDK.NET_DVR_GetLastError());
        } else {
            info.read();
        }
        return info;
    }

    public HCNetSDK.NET_DVR_PU_STREAM_CFG getStreamConfig(int lUserID) {
        HCNetSDK.NET_DVR_PU_STREAM_CFG config = new HCNetSDK.NET_DVR_PU_STREAM_CFG();
        config.dwSize = config.size();
        config.write();
        Pointer pointer = config.getPointer();

        IntByReference pInt = new IntByReference(0);
        // NET_DVR_PU_STREAM_CFG = 187
        boolean result = hCNetSDK.NET_DVR_GetDVRConfig(lUserID, 187, 0xFFFFFFFF,
                                                       pointer, config.size(), pInt);
        if (!result) {
            log.error("获取推流配置失败，错误码：" + hCNetSDK.NET_DVR_GetLastError());
        }
        config.read();
        return config;
    }

    public boolean setStreamConfig(int lUserID, Rtp rtp, Charset charset) {
        HCNetSDK.NET_DVR_PU_STREAM_CFG config = new HCNetSDK.NET_DVR_PU_STREAM_CFG();
        config.dwSize = config.size();
        config.struStreamMediaSvrCfg.byValid = (byte) (rtp.isStart() ? 1 : 0);
        config.struStreamMediaSvrCfg.struDevIP.sIpV4 = rtp.getHost().getBytes(charset);
        config.struStreamMediaSvrCfg.wDevPort = (short) rtp.getPort();
        config.struStreamMediaSvrCfg.byTransmitType = (byte) ("UDP".equalsIgnoreCase(rtp.getStreamMode()) ? 1 : 0);


        config.struDevChanInfo.byChannel = 1;
        config.struDevChanInfo.byTransProtocol = 1;
        config.struDevChanInfo.byTransMode = 0;


        config.write();
        Pointer pointer = config.getPointer();

        IntByReference pInt = new IntByReference(0);
        // NET_DVR_SET_PU_STREAMCFG = 186
        boolean result = hCNetSDK.NET_DVR_SetDVRConfig(lUserID, 186, 0xFFFFFFFF,
                                                       pointer, config.size());
        if (!result) {
            log.error("设置推流配置失败，错误码：" + hCNetSDK.NET_DVR_GetLastError());
            return false;
        }
        return true;
    }

    // 查询预置位
    public List<HCNetSDK.NET_DVR_PRESET_NAME> queryPreset(int lUserID) {

        HCNetSDK.NET_DVR_PRESET_NAME presetName = new HCNetSDK.NET_DVR_PRESET_NAME();
        presetName.dwSize = presetName.size();
        presetName.write();
        Pointer pointer = presetName.getPointer();

        IntByReference pInt = new IntByReference(0);
        boolean result = hCNetSDK.NET_DVR_GetDVRConfig(lUserID, HCNetSDK.NET_DVR_GET_PRESET_NAME, 0xFFFFFFFF,
                                                       pointer, presetName.size(), pInt);
        System.out.println(pInt.getValue());
//        HCNetSDK.NET_DVR_PRESET_NAME[] presetNames = (HCNetSDK.NET_DVR_PRESET_NAME[])presetName.toArray(10);
//        Pointer[] pointers = new Pointer[300];
//        for (int i = 0; i < presetNames.length; i++) {
//            pointers[i] = presetNames[i].getPointer();
//        }
//        pointer.read(0, pointers, 0, 300);
        if (!result) {
            log.error("获取预置位失败，错误码：" + hCNetSDK.NET_DVR_GetLastError());
        }

        presetName.read();

        // TODO: 2023/10/20 返回列表。如何从一个pointer读取结构体数组？
        return Arrays.asList(presetName);
    }

    // 预置位控制
    public boolean presetControl(int userId, int channel, String operation,
                                 Integer presetIndex) {
        int dwPTZPresetCmd = getPTZPresetCmd(operation);
        // 不用启动预览
        return hCNetSDK.NET_DVR_PTZPreset_Other(
                userId, channel, dwPTZPresetCmd, presetIndex
        );
    }

    private int getPTZPresetCmd(String operation) {
        switch (operation) {
            case "DEL":
                return HCNetSDK.CLE_PRESET;
            case "SET":
                return HCNetSDK.SET_PRESET;
            case "CALL":
                return HCNetSDK.GOTO_PRESET;
            default:
                throw new BusinessException("不支持的预置位操作：{}", operation);
        }
    }

    // 云台控制
    public PtzCommandInfo ptzControl(int userId, int channel, Map<String, Integer> commandMap, int lastCmd) {
        PtzCommandInfo ptzCommand = getPTZCmd(commandMap, lastCmd);

        ptzCommand.success = hCNetSDK.NET_DVR_PTZControlWithSpeed_Other(
                userId, channel, ptzCommand.command, ptzCommand.stop, ptzCommand.speed
        );
        return ptzCommand;
    }

    @Getter
    @Setter
    public static class PtzCommandInfo {

        int command;
        // 0：启动，1：停止
        int stop;
        // 范围[1 - 7]
        int speed;

        boolean success;
    }

    /**
     * 获取控制指令
     * @param ptzCommand 指令集合
     * @param lastCmd 缓存的上一次指令
     * @return 指令
     */
    private PtzCommandInfo getPTZCmd(Map<String, Integer> ptzCommand, int lastCmd) {
        PtzCommandInfo ptzCommandInfo = new PtzCommandInfo();
        Set<PtzCommand> directions = ptzCommand
                .keySet()
                .stream()
                .map(String::toUpperCase)
                .map(PtzCommand::valueOf)
                .collect(Collectors.toSet());


        if (directions.contains(PtzCommand.STOP)) {
            ptzCommandInfo.stop = 1;
            ptzCommandInfo.command = lastCmd;
        } else {
            ptzCommandInfo.command = doGetPtzCmd(directions);
        }
        ptzCommandInfo.speed = ptzCommand.values().stream().findFirst().map(value -> value * 7 / 180).orElse(1);
        return ptzCommandInfo;
    }

    private int doGetPtzCmd(Set<PtzCommand> directions) {
        // 只实现了最多2个方向的控制
        if (directions.size() <= 2) {
            if (directions.contains(PtzCommand.UP)) {
                if (directions.contains(PtzCommand.LEFT)) {
                    return UP_LEFT;
                }
                if (directions.contains(PtzCommand.RIGHT)) {
                    return UP_RIGHT;
                }
                return TILT_UP;
            }
            if (directions.contains(PtzCommand.DOWN)) {
                if (directions.contains(PtzCommand.LEFT)) {
                    return DOWN_LEFT;
                }
                if (directions.contains(PtzCommand.RIGHT)) {
                    return DOWN_RIGHT;
                }
                return TILT_DOWN;
            }
            if (directions.contains(PtzCommand.LEFT)) {
                return PAN_LEFT;
            }
            if (directions.contains(PtzCommand.RIGHT)) {
                return PAN_RIGHT;
            }
            if (directions.contains(PtzCommand.ZOOM_IN)) {
                return ZOOM_IN;
            }
            if (directions.contains(PtzCommand.ZOOM_OUT)) {
                return ZOOM_OUT;
            }
        }

        throw new BusinessException("暂不支持此指令：" + directions);
    }

    public enum PtzCommand {
        //上
        UP,
        //下
        DOWN,
        //左
        LEFT,
        //右
        RIGHT,
        //放大
        ZOOM_IN,
        //缩小
        ZOOM_OUT,
        //停止
        STOP;
    }

    // 预览
    private int preview(int userId, MediaSdkChannelConfig config) {
        // 预览设置
        HCNetSDK.NET_DVR_PREVIEWINFO streamInfo = new HCNetSDK.NET_DVR_PREVIEWINFO();
        streamInfo.hPlayWnd = null; //null表示不播放视频
        streamInfo.lChannel = Integer.parseInt(config.getChannel()); //通道号
        streamInfo.dwStreamType = (int) config.getExtraConfig("dwStreamType", 0); //码流类型
        streamInfo.dwLinkMode = (int) config.getExtraConfig("dwLinkMode", 3); //流传输模式
        streamInfo.bBlocked = 1; //阻塞取流
        // 开始预览
        return hCNetSDK.NET_DVR_RealPlay_V40(userId, streamInfo, null, null);
    }

    // 开始录像
    public boolean startRecord(int userId, int channel) {
        // 录像类型：0- 手动，1- 报警，2- 回传，3- 信号，4- 移动，5- 遮挡
        return hCNetSDK.NET_DVR_StartDVRRecord(userId, channel, 0);
    }

    // 停止录像
    public boolean stopRecord(int userId, int channel) {
        return hCNetSDK.NET_DVR_StopDVRRecord(userId, channel);
    }


    // 查询录像文件
    public List<HCNetSDK.NET_DVR_FINDDATA_V30> queryRecord(int userId, int channel, LocalDateTime start, LocalDateTime stop) {

        HCNetSDK.NET_DVR_FILECOND param = new HCNetSDK.NET_DVR_FILECOND();
        param.lChannel = channel;
        param.struStartTime = HCNetSDK.NET_DVR_TIME.of(start);
        param.struStopTime = HCNetSDK.NET_DVR_TIME.of(stop);
        param.dwFileType = 0XFF; //要查找的文件类型：0xFF－全部；0－定时录像；1—移动侦测；2－接近报警；3－出钞报警；4－进钞报警；5—命令触发；6－手动录像；7－震动报警；8-环境报警；9-智能报警
        param.dwIsLocked = 0XFF;

        int findHandle = hCNetSDK.NET_DVR_FindFile_V30(userId, param);
        if (findHandle == -1) {
            throw new BusinessException("查询录像失败。code：" + hCNetSDK.NET_DVR_GetLastError());
        }

        List<HCNetSDK.NET_DVR_FINDDATA_V30> list = new ArrayList<>();
        int status;
        do {
            HCNetSDK.NET_DVR_FINDDATA_V30 data = new HCNetSDK.NET_DVR_FINDDATA_V30();
            status = hCNetSDK.NET_DVR_FindNextFile_V30(findHandle, data);
            if (status == NET_DVR_FILE_SUCCESS) {
                data.read();
                list.add(data);
            }
        } while (status == NET_DVR_FILE_SUCCESS || status == NET_DVR_ISFINDING);

        hCNetSDK.NET_DVR_FindClose_V30(findHandle);

        return list;
    }

    public boolean initSDKInstance(String libPath) {
        if (hCNetSDK == null) {
            synchronized (HCNetSDK.class) {
                if (!CreateSDKInstance(libPath)) {
                    System.out.println("Load SDK fail");
                    return false;
                }
            }
        }
        //SDK初始化，一个程序进程只需要调用一次
        hCNetSDK.NET_DVR_Init();
        if (fExceptionCallBack == null) {
            fExceptionCallBack = new FExceptionCallBack_Imp();
        }
        Pointer pUser = null;
        if (!hCNetSDK.NET_DVR_SetExceptionCallBack_V30(0, 0, fExceptionCallBack, pUser)) {
            return false;
        }
        System.out.println("设置异常消息回调成功");

        //启用SDK写日志
        hCNetSDK.NET_DVR_SetLogToFile(3, libPath + File.separator + "sdkLog", false);

        return true;
    }

    /**
     * 动态库加载
     *
     * @return
     */
    private static boolean CreateSDKInstance(String libPath) {
        if (hCNetSDK == null) {
            synchronized (HCNetSDK.class) {
                String strDllPath = "";
                try {
                    if (isWindows()) {
                        //win系统加载库路径
                        strDllPath = libPath + File.separator + "HCNetSDK.dll";
                    } else if (isLinux()) {
                        //Linux系统加载库路径
                        strDllPath = libPath + File.separator + "libhcnetsdk.so";
                    }

                    hCNetSDK = (HCNetSDK) Native.loadLibrary(strDllPath, HCNetSDK.class);
                } catch (Exception ex) {
                    System.out.println("loadLibrary: " + strDllPath + " Error: " + ex.getMessage());
                    return false;
                }
            }
        }
        return true;
    }

    @Getter
    @Setter
    public static class DeviceInfo {

        private int charset;

    }
}


