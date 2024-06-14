package org.jetlinks.plugin.example.media.hk.hc;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * rtp推流配置.
 *
 * @author zhangji 2023/11/29
 */
@Getter
@Setter
@AllArgsConstructor
public class Rtp {

    private boolean start;

    private String host;

    private int port;

    private String streamMode;
}
