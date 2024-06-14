package org.jetlinks.plugin.example.media.hk;

import com.sun.jna.Platform;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.DeviceOfflineMessage;
import org.jetlinks.core.message.DeviceOnlineMessage;
import org.jetlinks.core.message.function.FunctionInvokeMessage;
import org.jetlinks.core.message.property.ReportPropertyMessage;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.core.metadata.types.PasswordType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.plugin.core.PluginContext;
import org.jetlinks.plugin.example.media.hk.hc.HCNetSDK;
import org.jetlinks.plugin.example.media.hk.hc.NetSDKManager;
import org.jetlinks.plugin.internal.device.DeviceGatewayPlugin;
import org.jetlinks.plugin.internal.device.command.GetDeviceConfigMetadataCommand;
import org.jetlinks.plugin.internal.device.command.GetProductConfigMetadataCommand;
import org.jetlinks.plugin.internal.functional.FunctionalService;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.time.Duration;
import java.util.Map;

import static org.jetlinks.plugin.example.media.hk.SDKConfigConstants.*;

/**
 * 海康sdk视频设备插件.
 *
 * @author zhangji 2023/3/6
 */
@Slf4j
public class SdkDevicePlugin extends DeviceGatewayPlugin {

    static ConfigMetadata metadata = new DefaultConfigMetadata()
            .add(IP.getKey(), IP.getName(), new StringType().expand(required.value(true)))
            .add(USERNAME.getKey(), USERNAME.getName(), new StringType().expand(required.value(true)))
            .add(PASSWORD.getKey(), PASSWORD.getName(), new PasswordType().expand(required.value(true)))
            .add(ACCESS_PORT.getKey(), ACCESS_PORT.getName(), new IntType().expand(required.value(true)))
            .add(RTSP_PORT.getKey(), RTSP_PORT.getName(), new IntType().expand(required.value(true)));

    private static final NetSDKManager sdk = new NetSDKManager();

    private final FunctionalService mediaService;

    public SdkDevicePlugin(String id,
                           PluginContext context) {
        super(id, context);
        String libPath = context().workDir().getPath() + File.separator + "lib";
        // 复制库文件到工作目录
        copyLibFile(libPath);
        // 初始化sdk
        sdk.initSDKInstance(libPath);

        mediaService = context()
                .services()
                .getService(FunctionalService.class, "mediaService")
                .orElseThrow(() -> new UnsupportedOperationException("Unsupported FunctionalService: mediaService"));

        registerHandler(GetDeviceConfigMetadataCommand.createHandler(cmd -> this.getDeviceConfigMetadata(cmd.getDeviceId())));
        registerHandler(GetProductConfigMetadataCommand.createHandler(cmd -> this.getProductConfigMetadata(cmd.getProductId())));
    }

    @Override
    public Mono<ConfigMetadata> getDeviceConfigMetadata(String deviceId) {
        return Mono.just(metadata);
    }

    @Override
    public Publisher<? extends DeviceMessage> execute(DeviceMessage message) {
        if (message instanceof FunctionInvokeMessage) {
            FunctionInvokeMessage msg = ((FunctionInvokeMessage) message);
            return registry
                    .getDevice(message.getDeviceId())
                    .flatMap(device -> this
                            .login(device)
                            .flatMap(userId -> SdkFunction
                                    .valueOf(msg.getFunctionId())
                                    .invoke(sdk, device, msg, mediaService)));
        }
        return Mono.empty();
    }

    private void copyLibFile(String libPath) {
        try {
            if (Platform.isLinux()) {
                LibUtils.batchCopyFile("lib/linux/", libPath, true);
                LibUtils.batchCopyFile("lib/linux/HCNetSDKCom/", libPath + File.separator + "HCNetSDKCom", true);
            } else if (Platform.isWindows()) {
                LibUtils.batchCopyFile("lib/win/", libPath, true);
                LibUtils.batchCopyFile("lib/win/ClientDemoDll/", libPath + File.separator + "ClientDemoDll", true);
                LibUtils.batchCopyFile("lib/win/HCNetSDKCom/", libPath + File.separator + "HCNetSDKCom", true);
            }
        } catch (Exception e) {
            log.error("复制库文件到工作目录失败", e);
        }
    }

    @Override
    public Mono<Byte> getDeviceState(DeviceOperator device) {
        return this
                .login(device)
                .map(sdk::getDeviceStatus)
                .map(status -> status ? (byte) 1 : (byte) -1);
    }

    private Mono<Void> pollState() {
        return getPlatformDevices()
                .map(DeviceOperator::getDeviceId)
                .buffer(100)
                .flatMap(list -> Flux.fromIterable(list)
                                     .map(deviceId -> new DeviceInfo(deviceId, true, null))
                                     .flatMap(this::handleState)
                                     .onErrorResume(err -> {
                                         log.warn("poll device state error", err);
                                         return Mono.empty();
                                     }))
                .then();
    }

    private Mono<Void> handleState(DeviceInfo deviceInfo) {
        //在线
        if (deviceInfo.online) {
            //属性上报
            if (MapUtils.isNotEmpty(deviceInfo.properties)) {
                ReportPropertyMessage msg = new ReportPropertyMessage();
                msg.setDeviceId(deviceInfo.id);
                msg.setProperties(deviceInfo.getProperties());
                return handleMessage(msg);
            }

            DeviceOnlineMessage message = new DeviceOnlineMessage();
            message.setDeviceId(deviceInfo.id);
            return handleMessage(message);
        } else {
            DeviceOfflineMessage message = new DeviceOfflineMessage();
            message.setDeviceId(deviceInfo.id);
            return handleMessage(message);
        }
    }

    /**
     * 登录设备
     *
     * @param device 设备操作
     * @return 用户ID
     */
    private Mono<Integer> login(DeviceOperator device) {
        return device
                .getSelfConfig(USER_ID)
                .filter(sdk::getDeviceStatus)
                .onErrorResume(err -> {
                    log.error("sdk登录错误", err);
                    return Mono.error(err);
                })
                // 不存在用户ID，则发起登录
                .switchIfEmpty(doLogin(device));
    }

    private Mono<Integer> doLogin(DeviceOperator device) {
        return device
                .getSelfConfigs(IP, ACCESS_PORT, USERNAME, PASSWORD)
                .map(values -> sdk
                        .Login_V40(
                                values.getString(IP.getKey(), ""),
                                values.getNumber(ACCESS_PORT.getKey(), 0).shortValue(),
                                values.getString(USERNAME.getKey(), ""),
                                values.getString(PASSWORD.getKey(), "")))
                .flatMap(tp2 -> {
                    int userId = tp2.getT1();
                    HCNetSDK.NET_DVR_DEVICEINFO_V40 deviceInfo = tp2.getT2();
                    return device.setConfigs(
                                         USER_ID.value(userId),
                                         CHARSET.value(deviceInfo.byCharEncodeType),
                                         CHANNEL_NUM.value((int) deviceInfo.struDeviceV30.byChanNum)
                                 )
                                 .thenReturn(userId);
                });
    }

    @Override
    protected Mono<Void> doStart() {

        //启动时定时拉取状态
        this.context()
            .scheduler()
            .interval("pull_device_state",
                      Mono.defer(this::pollState),
                      Duration.ofSeconds(10));

        return super.doStart();
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DeviceInfo {
        private String id;
        private boolean online;

        private Map<String, Object> properties;
    }
}
