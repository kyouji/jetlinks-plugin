package org.jetlinks.plugin.example.media.hk;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.plugin.core.Description;
import org.jetlinks.plugin.core.PluginContext;
import org.jetlinks.plugin.core.Version;
import org.jetlinks.plugin.core.VersionRange;
import org.jetlinks.plugin.internal.device.DeviceGatewayPlugin;
import org.jetlinks.plugin.internal.device.DeviceGatewayPluginDriver;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Collections;

/**
 * 海康sdk视频设备插件驱动.
 *
 * @author zhangji 2023/3/6
 */
@Slf4j
public class SdkDevicePluginDriver implements DeviceGatewayPluginDriver {

    static Version version_1_0 = new Version(1, 0, 0, true);

    @Nonnull
    @Override
    public final Description getDescription() {
        return Description.of(
                "hc-sdk",
                "通过SDK接入",
                "基于海康威视设备SDK的实现。",
                version_1_0,
                VersionRange.of(Version.platform_2_0, Version.platform_latest),
                //告诉平台,此插件需要的配置信息
                Collections.singletonMap(
                        PLUGIN_CONFIG_METADATA,
                        new DefaultConfigMetadata()
                )
        );
    }

    @Nonnull
    @Override
    public Mono<? extends DeviceGatewayPlugin> createPlugin(@Nonnull String pluginId,
                                                            @Nonnull PluginContext context) {
        return Mono.just(new SdkDevicePlugin(pluginId, context));
    }

}
