package org.jetlinks.plugin.example.media.hk;

import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.core.message.function.FunctionInvokeMessage;

import java.util.Optional;
import java.util.function.Function;

/**
 * 配置的静态变量.
 *
 * @author zhangji 2024/6/12
 */
public interface SDKConfigConstants {

    /* 产品设备的配置项 */
    ConfigKey<String> IP = ConfigKey.of("ip", "设备ip", String.class);

    ConfigKey<String> USERNAME = ConfigKey.of("user", "用户名", String.class);

    ConfigKey<String> PASSWORD = ConfigKey.of("pwd", "密码", String.class);

    ConfigKey<Integer> ACCESS_PORT = ConfigKey.of("access_port", "接口访问端口", Integer.TYPE);

    int DEFAULT_ACCESS_PORT = 8000;

    ConfigKey<Integer> RTSP_PORT = ConfigKey.of("rtsp_port", "RTSP推流端口", Integer.TYPE);

    /* 设备缓存项 */
    ConfigKey<String> LAST_PTZ_COMMAND = ConfigKey.of("last_ptz_command", "上一次云台控制指令", String.class);

    int DEFAULT_LAST_PTZ_COMMAND = -1;

    ConfigKey<Integer> USER_ID = ConfigKey.of("user_id", "用户ID", Integer.TYPE);

    int DEFAULT_USER_ID = 1;

    ConfigKey<Byte> CHARSET = ConfigKey.of("charset", "字符集", Byte.TYPE);

    ConfigKey<Integer> CHANNEL_NUM = ConfigKey.of("channel_num", "通道数量", Integer.TYPE);

    int DEFAULT_CHANNEL_NUM = 1;

    /* 通用配置 */
    ConfigKey<Boolean> required = ConfigKey.of("required", "是否必填", Boolean.TYPE);

    /* 功能输入参数默认值 */
    String DEFAULT_CODEC = "codec";
    int DEFAULT_CHANNEL = 1;
    String DEFAULT_SUB_TYPE = "main";

    /**
     * 云台控制参数
     */
    interface Ptz {
        String ARG_CHANNEL = "channel";
        String ARG_PRESET_OPERATION = "presetOperation";
        String ARG_PRESET_INDEX = "presetIndex";
        String ARG_PTZ_COMMAND = "ptzCommand";
    }

    /**
     * 播放参数
     */
    interface PlayStream {
        String ARG_CHANNEL = "channel";
        String ARG_CODEC = "codec";
        String ARG_SUB_TYPE = "subType";
        String ARG_STREAM_ID = "streamId";
        String ARG_TARGET = "target";
        String ARG_LOCAL_PLAYER = "localPlayer";
    }

    /**
     * 录像参数
     */
    interface Record {
        String ARG_START = "start";
        String ARG_END = "end";
    }

    static <T> T getRequireArg(String arg, FunctionInvokeMessage message, Function<Object, T> mapper) {
        return getArg(arg, message, mapper)
                .orElseThrow(() -> new IllegalArgumentException("[" + arg + "]不能为空"));
    }

    static <T> Optional<T> getArg(String arg, FunctionInvokeMessage message, Function<Object, T> mapper) {
        return message
                .getInput(arg)
                .map(mapper);
    }
}
