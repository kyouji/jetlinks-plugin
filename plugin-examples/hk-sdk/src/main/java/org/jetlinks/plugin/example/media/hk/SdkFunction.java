package org.jetlinks.plugin.example.media.hk;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.function.FunctionInvokeMessage;
import org.jetlinks.core.message.function.FunctionInvokeMessageReply;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.plugin.example.media.hk.hc.HCNetSDK;
import org.jetlinks.plugin.example.media.hk.hc.NetSDKManager;
import org.jetlinks.plugin.internal.functional.FunctionalService;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.jetlinks.sdk.server.media.MediaInfo;
import org.jetlinks.sdk.server.media.ProxyMediaStreamCommand;
import org.jetlinks.sdk.server.media.StopProxyMediaStreamCommand;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sun.nio.cs.ext.GBK;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;

import static org.jetlinks.plugin.example.media.hk.SDKConfigConstants.*;

@Getter
@AllArgsConstructor
public enum SdkFunction {
    SyncChannel("同步通道") {
        @Override
        public Mono<FunctionInvokeMessageReply> invoke(NetSDKManager sdk,
                                                       DeviceOperator device,
                                                       FunctionInvokeMessage msg,
                                                       FunctionalService mediaService) {
            return device
                    .getConfigs(USER_ID, IP, ACCESS_PORT, USERNAME, PASSWORD, CHANNEL_NUM)
                    .map(values -> {
                        HCNetSDK.NET_DVR_ACCESS_DEVICE_CHANNEL_INFO info = sdk
                                .queryChannel(
                                        values.getNumber(USER_ID.getKey(), DEFAULT_USER_ID).intValue(),
                                        values.getNumber(CHANNEL_NUM.getKey(), DEFAULT_CHANNEL_NUM).intValue(),
                                        values.getString(USERNAME.getKey(), ""),
                                        values.getString(PASSWORD.getKey(), ""),
                                        values.getString(IP.getKey(), ""),
                                        values.getNumber(ACCESS_PORT.getKey(), DEFAULT_ACCESS_PORT).shortValue()
                                );
                        List<Map<String, Object>> channelList = new ArrayList<>();

                        for (int i = 0; i <= info.dwTotalChannelNum; i++) {
                            int index = i + 1;
                            Map<String, Object> channel = new HashMap<>();
                            channel.put("channelId", String.valueOf(index));
                            channel.put("name", String.valueOf(index));
                            channel.put("deviceId", device.getDeviceId());
                            channelList.add(channel);
                        }
                        return channelList;
                    })
                    .flatMapMany(Flux::fromIterable)
                    .collectList()
                    .map(msg.newReply()::success);
        }
    },
    GetRealplayStream("获取预览地址") {
        @Override
        public Mono<FunctionInvokeMessageReply> invoke(NetSDKManager sdk,
                                                       DeviceOperator device,
                                                       FunctionInvokeMessage msg,
                                                       FunctionalService mediaService) {
            return device
                    .getConfigs(IP, RTSP_PORT, USERNAME, PASSWORD, USER_ID)
                    // 地址格式：rtsp://{username}:{password}@{ip}:{port}/{codec}/{channel}/{subtype}/av_stream
                    .map(values -> String.format(
                            "rtsp://%s:%s@%s:%d/%s/%d/%s/av_stream",
                            values.getString(USERNAME.getKey(), ""),
                            values.getString(PASSWORD.getKey(), ""),
                            values.getString(IP.getKey(), ""),
                            values.getNumber(
                                          RTSP_PORT.getKey(),
                                          sdk.queryRtspPort(values.getNumber(USER_ID.getKey(), DEFAULT_USER_ID).intValue()))
                                  .intValue(),
                            getArg(PlayStream.ARG_CODEC, msg, CastUtils::castString).orElse(DEFAULT_CODEC),
                            getArg(PlayStream.ARG_CHANNEL, msg, CastUtils::castNumber)
                                    .orElse(DEFAULT_CHANNEL)
                                    .intValue(),
                            getArg(PlayStream.ARG_SUB_TYPE, msg, CastUtils::castString).orElse(DEFAULT_SUB_TYPE)
                    ))
                    .flatMap(streamUrl -> proxyMediaStream(
                            mediaService,
                            getRequireArg(PlayStream.ARG_STREAM_ID, msg, CastUtils::castString),
                            streamUrl,
                            getArg(PlayStream.ARG_TARGET, msg, CastUtils::castString).orElse(""),
                            getArg(PlayStream.ARG_LOCAL_PLAYER, msg, CastUtils::castBoolean).orElse(true)
                    ))
                    .map(msg.newReply()::success);
        }
    },
    GetPlaybackStream("获取回放地址") {
        @Override
        public Mono<FunctionInvokeMessageReply> invoke(NetSDKManager sdk,
                                                       DeviceOperator device,
                                                       FunctionInvokeMessage msg,
                                                       FunctionalService mediaService) {
            return device
                    .getConfigs(IP, RTSP_PORT, USERNAME, PASSWORD)
                    .map(values -> {
                        // 地址格式：rtsp://username:password@ip:port/Streaming/tracks/{channel}?starttime={starttime}&endtime={endtime}
                        String channel = (String) msg.getInput("channel").orElse("1");
                        String url = String.format(
                                "rtsp://%s:%s@%s:%d/Streaming/tracks/%s",
                                values.getString(USERNAME.getKey(), ""),
                                values.getString(PASSWORD.getKey(), ""),
                                values.getString(IP.getKey(), ""),
                                values.getNumber(
                                              RTSP_PORT.getKey(),
                                              sdk.queryRtspPort(values
                                                                        .getNumber(USER_ID.getKey(), DEFAULT_USER_ID)
                                                                        .intValue()))
                                      .intValue(),
                                channel
                        );
                        String start = getArg(Record.ARG_START, msg, SdkFunction::formatDateToUTC).orElse(null);
                        String end = getArg(Record.ARG_END, msg, SdkFunction::formatDateToUTC).orElse(null);
                        if (start != null) {
                            url = url + "?starttime=" + start;
                        }
                        if (end != null) {
                            if (start != null) {
                                url = url + "&endtime=" + end;
                            } else {
                                url = url + "?endtime=" + end;
                            }
                        }
                        return url;
                    })
                    .flatMap(streamUrl -> proxyMediaStream(
                            mediaService,
                            getRequireArg(PlayStream.ARG_STREAM_ID, msg, CastUtils::castString),
                            streamUrl,
                            getArg(PlayStream.ARG_TARGET, msg, CastUtils::castString).orElse(""),
                            getArg(PlayStream.ARG_LOCAL_PLAYER, msg, CastUtils::castBoolean).orElse(true)
                    ))
                    .map(msg.newReply()::success);
        }
    },
    StopProxyMediaStream("停止推流") {
        @Override
        public Mono<FunctionInvokeMessageReply> invoke(NetSDKManager sdk,
                                                       DeviceOperator device,
                                                       FunctionInvokeMessage msg,
                                                       FunctionalService mediaService) {
            StopProxyMediaStreamCommand cmd = new StopProxyMediaStreamCommand()
                    .withStreamId(getRequireArg(PlayStream.ARG_STREAM_ID, msg, CastUtils::castString));
            return mediaService
                    .execute(cmd)
                    .thenReturn(msg.newReply().success());
        }
    },
    QueryPreset("查询预置位") {
        @Override
        public Mono<FunctionInvokeMessageReply> invoke(NetSDKManager sdk,
                                                       DeviceOperator device,
                                                       FunctionInvokeMessage msg,
                                                       FunctionalService mediaService) {
            return getCharset(device)
                    .flatMapMany(charset -> device
                            .getConfig(USER_ID)
                            .flatMapMany(userId -> Flux
                                    .fromIterable(sdk.queryPreset(userId))
                                    .map(data -> buildPreset(data, charset))))
                    .collectList()
                    .map(msg.newReply()::success);
        }

        @SneakyThrows
        private Map<String, Object> buildPreset(HCNetSDK.NET_DVR_PRESET_NAME data, Charset charset) {
            Map<String, Object> preset = new HashMap<>();
            preset.put("id", String.valueOf(data.wPresetNum));
            preset.put("name", new String(data.byName, charset).trim());

            Map<String, Object> others = new HashMap<>();
            others.put("wPanPos", data.wPanPos);
            others.put("wTiltPos", data.wTiltPos);
            others.put("wZoomPos", data.wZoomPos);
            preset.put("others", others);
            return preset;
        }
    },
    Preset("预置位操作") {
        @Override
        public Mono<FunctionInvokeMessageReply> invoke(NetSDKManager sdk,
                                                       DeviceOperator device,
                                                       FunctionInvokeMessage msg,
                                                       FunctionalService mediaService) {
            return device
                    .getConfig(USER_ID)
                    .map(userId -> sdk
                            .presetControl(
                                    userId,
                                    getArg(Ptz.ARG_CHANNEL, msg, CastUtils::castNumber)
                                            .orElse(DEFAULT_CHANNEL)
                                            .intValue(),
                                    getArg(Ptz.ARG_PRESET_OPERATION, msg, CastUtils::castString).orElse(""),
                                    getArg(Ptz.ARG_PRESET_INDEX, msg, CastUtils::castNumber).orElse(1).intValue()
                            ))
                    .map(result -> msg.newReply().success(result.booleanValue()));
        }
    },
    PTZ("云台控制") {
        @Override
        @SuppressWarnings("unchecked")
        public Mono<FunctionInvokeMessageReply> invoke(NetSDKManager sdk,
                                                       DeviceOperator device,
                                                       FunctionInvokeMessage msg,
                                                       FunctionalService mediaService) {
            return device
                    .getConfigs(USER_ID, LAST_PTZ_COMMAND)
                    .map(values -> sdk
                            .ptzControl(
                                    values.getNumber(USER_ID.getKey(), DEFAULT_USER_ID).intValue(),
                                    getArg(Ptz.ARG_CHANNEL, msg, CastUtils::castNumber)
                                            .orElse(DEFAULT_CHANNEL)
                                            .intValue(),
                                    getRequireArg(Ptz.ARG_PTZ_COMMAND, msg, obj -> ((Map<String, Integer>) obj)),
                                    values.getNumber(LAST_PTZ_COMMAND.getKey(), DEFAULT_LAST_PTZ_COMMAND).intValue()
                            ))
                    .flatMap(result -> {
                        if (result.isSuccess()) {
                            return device
                                    .setConfig(LAST_PTZ_COMMAND.getKey(), result.getCommand())
                                    .thenReturn(msg.newReply().success(result.isSuccess()));
                        }
                        return Mono.just(msg.newReply().success(result.isSuccess()));
                    });
        }
    },
    StartRecord("开始录像") {
        @Override
        public Mono<FunctionInvokeMessageReply> invoke(NetSDKManager sdk,
                                                       DeviceOperator device,
                                                       FunctionInvokeMessage msg,
                                                       FunctionalService mediaService) {
            int channelId = getArg(Ptz.ARG_CHANNEL, msg, CastUtils::castNumber).orElse(DEFAULT_CHANNEL).intValue();
            return device
                    .getConfig(USER_ID)
                    .map(userId -> record(sdk, userId, channelId, true))
                    .map(result -> msg.newReply().success(result));
        }
    },
    StopRecord("停止录像") {
        @Override
        public Mono<FunctionInvokeMessageReply> invoke(NetSDKManager sdk,
                                                       DeviceOperator device,
                                                       FunctionInvokeMessage msg,
                                                       FunctionalService mediaService) {
            int channelId = getArg(Ptz.ARG_CHANNEL, msg, CastUtils::castNumber).orElse(DEFAULT_CHANNEL).intValue();
            return device
                    .getConfig(USER_ID)
                    .map(userId -> record(sdk, userId, channelId, false))
                    .map(result -> msg.newReply().success(result));
        }
    },
    QueryRecordList("查询录像文件") {
        @Override
        public Mono<FunctionInvokeMessageReply> invoke(NetSDKManager sdk,
                                                       DeviceOperator device,
                                                       FunctionInvokeMessage msg,
                                                       FunctionalService mediaService) {
            int channelId = getArg(Ptz.ARG_CHANNEL, msg, CastUtils::castNumber).orElse(DEFAULT_CHANNEL).intValue();
            return device
                    .getConfig(USER_ID)
                    .flatMap(userId -> getCharset(device)
                            .flatMapMany(charset -> Mono
                                    .fromSupplier(() -> sdk
                                            .queryRecord(
                                                    userId,
                                                    channelId,
                                                    getArg(Record.ARG_START, msg, SdkFunction::parseDate).orElse(null),
                                                    getArg(Record.ARG_END, msg, SdkFunction::parseDate).orElse(null)
                                            ))
                                    .flatMapMany(Flux::fromIterable)
                                    .map(data -> {
                                        Map<String, Object> record = new HashMap<>();
                                        record.put("deviceId", device.getDeviceId());
                                        record.put("channelId", channelId);
                                        record.put("name", new String(data.sFileName, charset).trim());
                                        record.put("startTime", data.struStartTime.toTimestamp());
                                        record.put("endTime", data.struStopTime.toTimestamp());
                                        record.put("fileSize", data.dwFileSize);
                                        return record;
                                    }))
                            .collectList())
                    .map(msg.newReply()::success);
        }
    };

    private final String text;

    public abstract Mono<FunctionInvokeMessageReply> invoke(NetSDKManager sdk,
                                                            DeviceOperator device,
                                                            FunctionInvokeMessage msg,
                                                            FunctionalService mediaService);

    /**
     * 代理推送视频流
     * <p>
     * source示例: rtsp://admin:admin@127.0.0.1/h264/ch1/live_stream
     * target示例：rtp://127.0.0.1:7000/storage/stream_01.mp4?transport=udp
     *
     * @param streamId    流ID
     * @param source      来源地址
     * @param target      目标地址
     * @param localPlayer 是否本地播放
     * @return 流媒体信息
     */
    private static Mono<MediaInfo> proxyMediaStream(FunctionalService mediaService,
                                                    String streamId,
                                                    String source,
                                                    String target,
                                                    boolean localPlayer) {
        source = source.contains("?") ?
                source + "&streamId=" + streamId :
                source + "?streamId=" + streamId;
        ProxyMediaStreamCommand proxyMediaStreamCommand = new ProxyMediaStreamCommand()
                .withSource(source)
                .withTarget(target)
                .withLocalPlayer(localPlayer);
        return mediaService.execute(proxyMediaStreamCommand);
    }

    private static Mono<Boolean> record(NetSDKManager sdk, int userId, int channel, boolean start) {
        return Mono.fromSupplier(() -> {
            if (start) {
                return sdk.startRecord(userId, channel);
            } else {
                return sdk.stopRecord(userId, channel);
            }
        });
    }

    private static Mono<Charset> getCharset(DeviceOperator device) {
        return device
                .getConfig(CHARSET)
                .defaultIfEmpty(Byte.valueOf("0"))
                .map(charset -> {
                    switch (charset) {
                        case 0:
                            return Charset.defaultCharset();
                        case 1:
                            return Charset.forName("GB2312");
                        case 2:
                            return new GBK();
                        case 3:
                            return Charset.forName("BIG5");
                        case 6:
                            return StandardCharsets.UTF_8;
                        case 7:
                            return StandardCharsets.ISO_8859_1;
                        default:
                            return Charset.defaultCharset();
                    }
                });
    }

    static final DateTimeType UtcDateTimeType = new DateTimeType()
            .timeZone(ZoneOffset.UTC)
            // 海康查询录像的时间格式，Z表示UTC时区
            .format("yyyyMMdd'T'HHmmss'Z'");

    static String formatDateToUTC(Object date) {
        // 转换后的时间（系统默认时区）
        Date convert = DateTimeType.GLOBAL.convert(date);
        // 转为UTC时区
        // 海康播放录像的url默认是UTC时间。使用时区（+08）会因为GET请求的URL解码而无法正确解析
        return UtcDateTimeType.format(convert);
    }

    static LocalDateTime parseDate(Object date) {
        if (date == null) {
            return null;
        }
        return DateTimeType.GLOBAL
                .convert(date)
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}