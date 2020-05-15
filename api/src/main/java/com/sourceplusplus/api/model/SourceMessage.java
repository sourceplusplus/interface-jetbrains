package com.sourceplusplus.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;

import java.io.Serializable;

/**
 * Base message for all Source++ API messages.
 *
 * @author <a href="mailto:brandon@srcpl.us">Brandon Fergerson</a>
 * @version 0.2.6
 * @since 0.1.0
 */
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public interface SourceMessage extends Serializable {

    static MessageCodec messageCodec(Class type) {
        return new MessageCodec() {

            @Override
            public void encodeToWire(Buffer buffer, Object o) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public Object decodeFromWire(int pos, Buffer buffer) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public Object transform(Object o) {
                return o;
            }

            @Override
            public String name() {
                return type.getSimpleName();
            }

            @Override
            public byte systemCodecID() {
                return -1;
            }
        };
    }
}
