package com.freedom.chain.model.p2p;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * @description: p2p通讯消息
 * @author: freedom
 * @create: 2025-11-19
 **/
@Getter
@Setter
public class Message implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 消息类型
     */
    private int type;
    /**
     * 消息内容
     */
    private String data;

    public Message() {
    }

    public Message(int type) {
        this.type = type;
    }

    public Message(int type, String data) {
        this.type = type;
        this.data = data;
    }

}
