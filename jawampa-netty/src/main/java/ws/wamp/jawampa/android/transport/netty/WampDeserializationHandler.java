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

package ws.wamp.jawampa.android.transport.netty;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.stream.JsonReader;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import ws.wamp.jawampa.android.WampMessages.WampMessage;
import ws.wamp.jawampa.android.WampSerialization;

import java.io.InputStreamReader;
import java.util.List;

public class WampDeserializationHandler extends MessageToMessageDecoder<WebSocketFrame>
{

	private static final InternalLogger logger = InternalLoggerFactory.getInstance( WampDeserializationHandler.class );

	enum ReadState
	{
		Closed,
		Reading,
		Error
	}

	ReadState readState = ReadState.Closed;

	final WampSerialization serialization;

	public WampSerialization serialization()
	{
		return serialization;
	}

	public WampDeserializationHandler( WampSerialization serialization )
	{
		this.serialization = serialization;
	}

	@Override
	public void handlerAdded( ChannelHandlerContext ctx )
	{
		readState = ReadState.Reading;
	}

	@Override
	public void channelActive( ChannelHandlerContext ctx )
	{
		readState = ReadState.Reading;
		ctx.fireChannelActive();
	}

	@Override
	public void channelInactive( ChannelHandlerContext ctx )
	{
		readState = ReadState.Closed;
		ctx.fireChannelInactive();
	}

	@Override
	protected void decode( ChannelHandlerContext ctx, WebSocketFrame frame, List<Object> out )
			throws Exception
	{
		if ( readState != ReadState.Reading ) return;

		ByteBuf content;
		Gson gson = serialization.getGson();
		if ( frame instanceof TextWebSocketFrame )
		{
			// Only want Text frames when text subprotocol
			if ( !serialization.isText() )
				throw new IllegalStateException( "Received unexpected TextFrame" );
			content = frame.content();
		}
		else if ( frame instanceof BinaryWebSocketFrame )
		{
			// Only want Binary frames when binary subprotocol
			if ( serialization.isText() )
				throw new IllegalStateException( "Received unexpected BinaryFrame" );
			content = frame.content();
		}
		else if ( frame instanceof PongWebSocketFrame )
		{
			// System.out.println("WebSocket Client received pong");
			return;
		}
		else if ( frame instanceof CloseWebSocketFrame )
		{
			// System.out.println("WebSocket Client received closing");
			readState = ReadState.Closed;
			return;
		}
		else
			throw new IllegalStateException( "Received unknown Frame" );

		// If we receive an invalid frame on of the following functions will throw
		// This will lead Netty to closing the connection
		JsonReader jsonReader = gson.newJsonReader( new InputStreamReader( new ByteBufInputStream( content ) ) );
		JsonArray arr = gson.fromJson( jsonReader, JsonArray.class );

		if ( logger.isDebugEnabled() )
		{
			logger.debug( "Deserialized Wamp Message: {}", arr.toString() );
		}

		WampMessage recvdMessage = WampMessage.fromObjectArray( arr );
		out.add( recvdMessage );
	}

	@Override
	public void exceptionCaught( ChannelHandlerContext ctx, Throwable cause )
	{
		// We caught an exception.
		// Most probably because we received an invalid message
		readState = ReadState.Error;
		ctx.writeAndFlush( Unpooled.EMPTY_BUFFER )
		   .addListener( ChannelFutureListener.CLOSE );
	}
}
