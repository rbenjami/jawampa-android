/*
 * Copyright 2014 Matthias Einwag
 *
 * The jawampa authors license this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package ws.wamp.jawampa.transport.netty;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.stream.JsonWriter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import ws.wamp.jawampa.WampMessages.WampMessage;
import ws.wamp.jawampa.WampSerialization;

import java.io.OutputStreamWriter;
import java.util.List;

public class WampSerializationHandler extends MessageToMessageEncoder<WampMessage> {
    
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(WampSerializationHandler.class);

    final WampSerialization serialization;
    
    public WampSerialization serialization() {
        return serialization;
    }
    
    public WampSerializationHandler(WampSerialization serialization) {
        this.serialization = serialization;
    }
    
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, WampMessage msg, List<Object> out) throws Exception {
        ByteBuf msgBuffer = Unpooled.buffer();
        ByteBufOutputStream outStream = new ByteBufOutputStream(msgBuffer);
        Gson gson = serialization.getGson();
        try {
            JsonWriter writer = new JsonWriter(new OutputStreamWriter(outStream, "UTF-8"));
            JsonArray node = msg.toObjectArray( gson );

            gson.toJson( node, writer );

            if (logger.isDebugEnabled()) {
                logger.debug("Serialized Wamp Message: {}", node.toString());
            }

        } catch (Exception e) {
            msgBuffer.release();
            return;
        }

        if (serialization.isText()) {
            TextWebSocketFrame frame = new TextWebSocketFrame(msgBuffer);
            out.add(frame);
        } else {
            BinaryWebSocketFrame frame = new BinaryWebSocketFrame(msgBuffer);
            out.add(frame);
        }
    }
}
