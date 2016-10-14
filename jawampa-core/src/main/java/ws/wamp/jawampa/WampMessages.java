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

package ws.wamp.jawampa;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class WampMessages
{

	interface WampMessageFactory
	{
		public WampMessage fromObjectArray( JsonArray messageNode ) throws WampError;
	}

	/**
	 * Base class for all messages
	 */
	public static abstract class WampMessage
	{

		/**
		 * A map which associates all message types which factories which can
		 * recreate them from received data.
		 */
		final static Map<Integer, WampMessageFactory> messageFactories;

		static
		{
			HashMap<Integer, WampMessageFactory> map = new HashMap<>();
			map.put( HelloMessage.ID, new HelloMessage.Factory() );
			map.put( WelcomeMessage.ID, new WelcomeMessage.Factory() );
			map.put( AbortMessage.ID, new AbortMessage.Factory() );
			map.put( ChallengeMessage.ID, new ChallengeMessage.Factory() );
			map.put( AuthenticateMessage.ID, new AuthenticateMessage.Factory() );
			map.put( GoodbyeMessage.ID, new GoodbyeMessage.Factory() );
			// map.put(MessageType.ID, new HeartbeatMessage.Factory());
			map.put( ErrorMessage.ID, new ErrorMessage.Factory() );
			map.put( PublishMessage.ID, new PublishMessage.Factory() );
			map.put( PublishedMessage.ID, new PublishedMessage.Factory() );
			map.put( SubscribeMessage.ID, new SubscribeMessage.Factory() );
			map.put( SubscribedMessage.ID, new SubscribedMessage.Factory() );
			map.put( UnsubscribeMessage.ID, new UnsubscribeMessage.Factory() );
			map.put( UnsubscribedMessage.ID, new UnsubscribedMessage.Factory() );
			map.put( EventMessage.ID, new EventMessage.Factory() );
			map.put( CallMessage.ID, new CallMessage.Factory() );
			// map.put(CancelMessage.ID, new CancelMessage.Factory());
			map.put( ResultMessage.ID, new ResultMessage.Factory() );
			map.put( RegisterMessage.ID, new RegisterMessage.Factory() );
			map.put( RegisteredMessage.ID, new RegisteredMessage.Factory() );
			map.put( UnregisterMessage.ID, new UnregisterMessage.Factory() );
			map.put( UnregisteredMessage.ID, new UnregisteredMessage.Factory() );
			map.put( InvocationMessage.ID, new InvocationMessage.Factory() );
			// map.put(InterruptMessage.ID, new InterruptMessage.Factory());
			map.put( YieldMessage.ID, new YieldMessage.Factory() );
			messageFactories = Collections.unmodifiableMap( map );
		}

		// Register all possible message types

		public static WampMessage fromObjectArray( JsonArray messageNode )
				throws WampError
		{
			if ( messageNode == null || messageNode.size() < 1//!messageNode.get(0).canConvertToInt())
					|| !( messageNode.get( 0 ).isJsonPrimitive() && ( (JsonPrimitive) messageNode.get( 0 ) ).isNumber() ) )
				throw new WampError( ApplicationError.INVALID_MESSAGE );

			int messageType = messageNode.get( 0 ).getAsInt();
			WampMessageFactory factory = messageFactories.get( messageType );
			if ( factory == null )
				return null; // We can't find the message type, so we skip it

			return factory.fromObjectArray( messageNode );
		}

		public abstract JsonArray toObjectArray()
				throws WampError;
	}

	/**
	 * Sent by a Client to initiate opening of a WAMP session to a Router
	 * attaching to a Realm. Format: [HELLO, Realm|uri, Details|dict]
	 */
	public static class HelloMessage extends WampMessage
	{
		public final static int ID = 1;
		public String     realm;
		public JsonObject details;

		public HelloMessage( String realm, JsonObject details )
		{
			this.realm = realm;
			this.details = details;
		}

		public JsonArray toObjectArray() throws WampError
		{
			JsonArray messageNode = new JsonArray();
			messageNode.add( ID );
			messageNode.add( realm );
			if ( details != null )
				messageNode.add( details );
			else
				messageNode.add( new JsonObject() );
			return messageNode;
		}

		static class Factory implements WampMessageFactory
		{
			@Override
			public WampMessage fromObjectArray( JsonArray messageNode ) throws WampError
			{
				if ( messageNode.size() != 3
						|| !( messageNode.get( 1 ).isJsonPrimitive() && ( (JsonPrimitive) messageNode.get( 1 ) ).isString() )
						|| !messageNode.get( 2 ).isJsonObject() )
					throw new WampError( ApplicationError.INVALID_MESSAGE );

				String realm = messageNode.get( 1 ).getAsString();
				JsonObject details = messageNode.get( 2 ).getAsJsonObject();
				return new HelloMessage( realm, details );
			}
		}
	}

	/**
	 * Sent by a Router to accept a Client. The WAMP session is now open.
	 * Format: [WELCOME, Session|id, Details|dict]
	 */
	public static class WelcomeMessage extends WampMessage
	{
		public final static int ID = 2;
		public long       sessionId;
		public JsonObject details;

		public WelcomeMessage( long sessionId, JsonObject details )
		{
			this.sessionId = sessionId;
			this.details = details;
		}

		public JsonArray toObjectArray() throws WampError
		{
			JsonArray messageNode = new JsonArray();
			messageNode.add( ID );
			messageNode.add( sessionId );
			if ( details != null )
				messageNode.add( details );
			else
				messageNode.add( new JsonObject() );
			return messageNode;
		}

		static class Factory implements WampMessageFactory
		{
			@Override
			public WampMessage fromObjectArray( JsonArray messageNode ) throws WampError
			{
				if ( messageNode.size() != 3
						|| !( messageNode.get( 1 ).isJsonPrimitive() && ( (JsonPrimitive) messageNode.get( 1 ) ).isNumber() )
						|| !messageNode.get( 2 ).isJsonObject() )
					throw new WampError( ApplicationError.INVALID_MESSAGE );

				long sessionId = messageNode.get( 1 ).getAsLong();
				JsonObject details = messageNode.get( 2 ).getAsJsonObject();
				return new WelcomeMessage( sessionId, details );
			}
		}
	}

	/**
	 * Sent by a Peer to abort the opening of a WAMP session. No response is
	 * expected. [ABORT, Details|dict, Reason|uri]
	 */
	public static class AbortMessage extends WampMessage
	{
		public final static int ID = 3;
		public JsonObject details;
		public String     reason;

		public AbortMessage( JsonObject details, String reason )
		{
			this.details = details;
			this.reason = reason;
		}

		public JsonArray toObjectArray() throws WampError
		{
			JsonArray messageNode = new JsonArray();
			messageNode.add( ID );
			if ( details != null )
				messageNode.add( details );
			else
				messageNode.add( new JsonObject() );
			messageNode.add( reason );
			return messageNode;
		}

		static class Factory implements WampMessageFactory
		{
			@Override
			public WampMessage fromObjectArray( JsonArray messageNode )
					throws WampError
			{
				if ( messageNode.size() != 3
						|| !messageNode.get( 1 ).isJsonObject()
						|| !( messageNode.get( 2 ).isJsonPrimitive() && ( (JsonPrimitive) messageNode.get( 2 ) ).isString() ) )
					throw new WampError( ApplicationError.INVALID_MESSAGE );

				JsonObject details = messageNode.get( 1 ).getAsJsonObject();
				String reason = messageNode.get( 2 ).getAsString();
				return new AbortMessage( details, reason );
			}
		}
	}

	/**
	 * During authenticated session establishment, a Router sends a challenge message.
	 * Format: [CHALLENGE, AuthMethod|string, Extra|dict]
	 */
	public static class ChallengeMessage extends WampMessage
	{
		public final static int ID = 4;
		public String     authMethod;
		public JsonObject extra;

		public ChallengeMessage( String authMethod, JsonObject extra )
		{
			this.authMethod = authMethod;
			this.extra = extra;
		}

		public JsonArray toObjectArray() throws WampError
		{
			JsonArray messageNode = new JsonArray();
			messageNode.add( ID );
			messageNode.add( authMethod );
			if ( extra != null )
				messageNode.add( extra );
			else
				messageNode.add( new JsonObject() );
			return messageNode;
		}

		static class Factory implements WampMessageFactory
		{
			@Override
			public WampMessage fromObjectArray( JsonArray messageNode ) throws WampError
			{
				if ( messageNode.size() != 3
						|| !( messageNode.get( 1 ).isJsonPrimitive() && ( (JsonPrimitive) messageNode.get( 1 ) ).isString() )
						|| !messageNode.get( 2 ).isJsonObject() )
					throw new WampError( ApplicationError.INVALID_MESSAGE );

				String authMethod = messageNode.get( 1 ).getAsString();
				JsonObject extra = messageNode.get( 2 ).getAsJsonObject();
				return new ChallengeMessage( authMethod, extra );
			}
		}
	}

	/**
	 * A Client having received a challenge is expected to respond by sending a signature or token.
	 * Format: [AUTHENTICATE, Signature|string, Extra|dict]
	 */
	public static class AuthenticateMessage extends WampMessage
	{
		public final static int ID = 5;
		public String     signature;
		public JsonObject extra;

		public AuthenticateMessage( String signature, JsonObject extra )
		{
			this.signature = signature;
			this.extra = extra;
		}

		public JsonArray toObjectArray() throws WampError
		{
			JsonArray messageNode = new JsonArray();
			messageNode.add( ID );
			messageNode.add( signature );
			if ( extra != null )
				messageNode.add( extra );
			else
				messageNode.add( new JsonObject() );
			return messageNode;
		}

		static class Factory implements WampMessageFactory
		{
			@Override
			public WampMessage fromObjectArray( JsonArray messageNode ) throws WampError
			{
				if ( messageNode.size() != 3
						|| !( messageNode.get( 1 ).isJsonPrimitive() && ( (JsonPrimitive) messageNode.get( 1 ) ).isString() )
						|| !messageNode.get( 2 ).isJsonObject() )
					throw new WampError( ApplicationError.INVALID_MESSAGE );

				String signature = messageNode.get( 1 ).getAsString();
				JsonObject extra = messageNode.get( 2 ).getAsJsonObject();
				return new AuthenticateMessage( signature, extra );
			}
		}
	}

	/**
	 * Sent by a Peer to close a previously opened WAMP session. Must be echo'ed
	 * by the receiving Peer. Format: [GOODBYE, Details|dict, Reason|uri]
	 */
	public static class GoodbyeMessage extends WampMessage
	{
		public final static int ID = 6;
		public JsonObject details;
		public String     reason;

		public GoodbyeMessage( JsonObject details, String reason )
		{
			this.details = details;
			this.reason = reason;
		}

		public JsonArray toObjectArray() throws WampError
		{
			JsonArray messageNode = new JsonArray();
			messageNode.add( ID );
			if ( details != null )
				messageNode.add( details );
			else
				messageNode.add( new JsonObject() );
			messageNode.add( reason );
			return messageNode;
		}

		static class Factory implements WampMessageFactory
		{
			@Override
			public WampMessage fromObjectArray( JsonArray messageNode )
					throws WampError
			{
				if ( messageNode.size() != 3
						|| !messageNode.get( 1 ).isJsonObject()
						|| !( messageNode.get( 2 ).isJsonPrimitive() && ( (JsonPrimitive) messageNode.get( 2 ) ).isString() ) )
					throw new WampError( ApplicationError.INVALID_MESSAGE );

				JsonObject details = messageNode.get( 1 ).getAsJsonObject();
				String reason = messageNode.get( 2 ).getAsString();
				return new GoodbyeMessage( details, reason );
			}
		}
	}

	/**
	 * Error reply sent by a Peer as an error response to different kinds of
	 * requests. Possible formats: [ERROR, REQUEST.Type|int, REQUEST.Request|id,
	 * Details|dict, Error|uri] [ERROR, REQUEST.Type|int, REQUEST.Request|id,
	 * Details|dict, Error|uri, Arguments|list] [ERROR, REQUEST.Type|int,
	 * REQUEST.Request|id, Details|dict, Error|uri, Arguments|list,
	 * ArgumentsKw|dict]
	 */
	public static class ErrorMessage extends WampMessage
	{
		public final static int ID = 8;
		public int        requestType;
		public long       requestId;
		public JsonObject details;
		public String     error;
		public JsonArray  arguments;
		public JsonObject argumentsKw;

		public ErrorMessage( int requestType, long requestId,
							 JsonObject details, String error, JsonArray arguments,
							 JsonObject argumentsKw )
		{
			this.requestType = requestType;
			this.requestId = requestId;
			this.details = details;
			this.error = error;
			this.arguments = arguments;
			this.argumentsKw = argumentsKw;
		}

		public JsonArray toObjectArray() throws WampError
		{
			JsonArray messageNode = new JsonArray();
			messageNode.add( ID );
			messageNode.add( requestType );
			messageNode.add( requestId );
			if ( details != null )
				messageNode.add( details );
			else
				messageNode.add( new JsonObject() );
			messageNode.add( error );
			if ( arguments != null )
				messageNode.add( arguments );
			else if ( argumentsKw != null )
				messageNode.add( new JsonArray() );
			if ( argumentsKw != null )
				messageNode.add( argumentsKw );
			return messageNode;
		}

		static class Factory implements WampMessageFactory
		{
			@Override
			public WampMessage fromObjectArray( JsonArray messageNode ) throws WampError
			{
				if ( messageNode.size() < 5 || messageNode.size() > 7
						|| !( messageNode.get( 1 ).isJsonPrimitive() && ( (JsonPrimitive) messageNode.get( 1 ) ).isNumber() )
						|| !( messageNode.get( 2 ).isJsonPrimitive() && ( (JsonPrimitive) messageNode.get( 2 ) ).isNumber() )
						|| !messageNode.get( 3 ).isJsonObject()
						|| !( messageNode.get( 4 ).isJsonPrimitive() && ( (JsonPrimitive) messageNode.get( 2 ) ).isString() ) )
					throw new WampError( ApplicationError.INVALID_MESSAGE );

				int requestType = messageNode.get( 1 ).getAsInt();
				long requestId = messageNode.get( 2 ).getAsLong();
				JsonObject details = messageNode.get( 3 ).getAsJsonObject();
				String error = messageNode.get( 4 ).getAsString();
				JsonArray arguments = null;
				JsonObject argumentsKw = null;

				if ( messageNode.size() >= 6 )
				{
					if ( !messageNode.get( 5 ).isJsonArray() )
						throw new WampError( ApplicationError.INVALID_MESSAGE );
					arguments = (JsonArray) messageNode.get( 5 );
					if ( messageNode.size() >= 7 )
					{
						if ( !messageNode.get( 6 ).isJsonObject() )
							throw new WampError( ApplicationError.INVALID_MESSAGE );
						argumentsKw = (JsonObject) messageNode.get( 6 );
					}
				}

				return new ErrorMessage( requestType, requestId, details, error,
						arguments, argumentsKw );
			}
		}
	}

	/**
	 * Sent by a Publisher to a Broker to publish an event. Possible formats:
	 * [PUBLISH, Request|id, Options|dict, Topic|uri] [PUBLISH, Request|id,
	 * Options|dict, Topic|uri, Arguments|list] [PUBLISH, Request|id,
	 * Options|dict, Topic|uri, Arguments|list, ArgumentsKw|dict]
	 */
	public static class PublishMessage extends WampMessage
	{
		public final static int ID = 16;
		public long       requestId;
		public JsonObject options;
		public String     topic;
		public JsonArray  arguments;
		public JsonObject argumentsKw;

		public PublishMessage( long requestId, JsonObject options, String topic,
							   JsonArray arguments, JsonObject argumentsKw )
		{
			this.requestId = requestId;
			this.options = options;
			this.topic = topic;
			this.arguments = arguments;
			this.argumentsKw = argumentsKw;
		}

		public JsonArray toObjectArray() throws WampError
		{
			JsonArray messageNode = new JsonArray();
			messageNode.add( ID );
			messageNode.add( requestId );
			if ( options != null )
				messageNode.add( options );
			else
				messageNode.add( new JsonObject() );
			messageNode.add( topic );
			if ( arguments != null )
				messageNode.add( arguments );
			else if ( argumentsKw != null )
				messageNode.add( new JsonArray() );
			if ( argumentsKw != null )
				messageNode.add( argumentsKw );
			return messageNode;
		}

		static class Factory implements WampMessageFactory
		{
			@Override
			public WampMessage fromObjectArray( JsonArray messageNode ) throws WampError
			{
				if ( messageNode.size() < 4 || messageNode.size() > 6
						|| !( messageNode.get( 1 ).isJsonPrimitive() && ( (JsonPrimitive) messageNode.get( 1 ) ).isNumber() )
						|| !messageNode.get( 2 ).isJsonObject()
						|| !( messageNode.get( 3 ).isJsonPrimitive() && ( (JsonPrimitive) messageNode.get( 3 ) ).isString() ) )
					throw new WampError( ApplicationError.INVALID_MESSAGE );

				long requestId = messageNode.get( 1 ).getAsLong();
				JsonObject options = (JsonObject) messageNode.get( 2 );
				String topic = messageNode.get( 3 ).getAsString();
				JsonArray arguments = null;
				JsonObject argumentsKw = null;

				if ( messageNode.size() >= 5 )
				{
					if ( !messageNode.get( 4 ).isJsonArray() )
						throw new WampError( ApplicationError.INVALID_MESSAGE );
					arguments = (JsonArray) messageNode.get( 4 );
					if ( messageNode.size() >= 6 )
					{
						if ( !messageNode.get( 5 ).isJsonObject() )
							throw new WampError( ApplicationError.INVALID_MESSAGE );
						argumentsKw = (JsonObject) messageNode.get( 5 );
					}
				}

				return new PublishMessage( requestId, options, topic, arguments,
						argumentsKw );
			}
		}
	}

	/**
	 * Acknowledge sent by a Broker to a Publisher for acknowledged
	 * publications. [PUBLISHED, PUBLISH.Request|id, Publication|id]
	 */
	public static class PublishedMessage extends WampMessage
	{
		public final static int ID = 17;
		public long requestId;
		public long publicationId;

		public PublishedMessage( long requestId, long publicationId )
		{
			this.requestId = requestId;
			this.publicationId = publicationId;
		}

		public JsonArray toObjectArray() throws WampError
		{
			JsonArray messageNode = new JsonArray();
			messageNode.add( ID );
			messageNode.add( requestId );
			messageNode.add( publicationId );
			return messageNode;
		}

		static class Factory implements WampMessageFactory
		{
			@Override
			public WampMessage fromObjectArray( JsonArray messageNode ) throws WampError
			{
				if ( messageNode.size() != 3
						|| !( messageNode.get( 1 ).isJsonPrimitive() && ( (JsonPrimitive) messageNode.get( 1 ) ).isNumber() )
						|| !( messageNode.get( 2 ).isJsonPrimitive() && ( (JsonPrimitive) messageNode.get( 2 ) ).isNumber() ) )
					throw new WampError( ApplicationError.INVALID_MESSAGE );

				long requestId = messageNode.get( 1 ).getAsLong();
				long publicationId = messageNode.get( 2 ).getAsLong();

				return new PublishedMessage( requestId, publicationId );
			}
		}
	}

	/**
	 * Subscribe request sent by a Subscriber to a Broker to subscribe to a
	 * topic. [SUBSCRIBE, Request|id, Options|dict, Topic|uri]
	 */
	public static class SubscribeMessage extends WampMessage
	{
		public final static int ID = 32;
		public long       requestId;
		public JsonObject options;
		public String     topic;

		public SubscribeMessage( long requestId, JsonObject options, String topic )
		{
			this.requestId = requestId;
			this.options = options;
			this.topic = topic;
		}

		public JsonArray toObjectArray() throws WampError
		{
			JsonArray messageNode = new JsonArray();
			messageNode.add( ID );
			messageNode.add( requestId );
			if ( options != null )
				messageNode.add( options );
			else
				messageNode.add( new JsonObject() );
			messageNode.add( topic );
			return messageNode;
		}

		static class Factory implements WampMessageFactory
		{
			@Override
			public WampMessage fromObjectArray( JsonArray messageNode ) throws WampError
			{
				if ( messageNode.size() != 4
						|| !( messageNode.get( 1 ).isJsonPrimitive() && ( (JsonPrimitive) messageNode.get( 1 ) ).isNumber() )
						|| !messageNode.get( 2 ).isJsonObject()
						|| !( messageNode.get( 3 ).isJsonPrimitive() && ( (JsonPrimitive) messageNode.get( 3 ) ).isString() ) )
					throw new WampError( ApplicationError.INVALID_MESSAGE );

				long requestId = messageNode.get( 1 ).getAsLong();
				JsonObject options = (JsonObject) messageNode.get( 2 );
				String topic = messageNode.get( 3 ).getAsString();

				return new SubscribeMessage( requestId, options, topic );
			}
		}
	}

	/**
	 * Acknowledge sent by a Broker to a Subscriber to acknowledge a
	 * subscription. [SUBSCRIBED, SUBSCRIBE.Request|id, Subscription|id]
	 */
	public static class SubscribedMessage extends WampMessage
	{
		public final static int ID = 33;
		public long requestId;
		public long subscriptionId;

		public SubscribedMessage( long requestId, long subscriptionId )
		{
			this.requestId = requestId;
			this.subscriptionId = subscriptionId;
		}

		public JsonArray toObjectArray() throws WampError
		{
			JsonArray messageNode = new JsonArray();
			messageNode.add( ID );
			messageNode.add( requestId );
			messageNode.add( subscriptionId );
			return messageNode;
		}

		static class Factory implements WampMessageFactory
		{
			@Override
			public WampMessage fromObjectArray( JsonArray messageNode ) throws WampError
			{
				if ( messageNode.size() != 3
						|| !( messageNode.get( 1 ).isJsonPrimitive() && ( (JsonPrimitive) messageNode.get( 1 ) ).isNumber() )
						|| !( messageNode.get( 2 ).isJsonPrimitive() && ( (JsonPrimitive) messageNode.get( 2 ) ).isNumber() ) )
					throw new WampError( ApplicationError.INVALID_MESSAGE );

				long requestId = messageNode.get( 1 ).getAsLong();
				long subscriptionId = messageNode.get( 2 ).getAsLong();

				return new SubscribedMessage( requestId, subscriptionId );
			}
		}
	}

	/**
	 * Unsubscribe request sent by a Subscriber to a Broker to unsubscribe a
	 * subscription. [UNSUBSCRIBE, Request|id, SUBSCRIBED.Subscription|id]
	 */
	public static class UnsubscribeMessage extends WampMessage
	{
		public final static int ID = 34;
		public long requestId;
		public long subscriptionId;

		public UnsubscribeMessage( long requestId, long subsriptionId )
		{
			this.requestId = requestId;
			this.subscriptionId = subsriptionId;
		}

		public JsonArray toObjectArray() throws WampError
		{
			JsonArray messageNode = new JsonArray();
			messageNode.add( ID );
			messageNode.add( requestId );
			messageNode.add( subscriptionId );
			return messageNode;
		}

		static class Factory implements WampMessageFactory
		{
			@Override
			public WampMessage fromObjectArray( JsonArray messageNode ) throws WampError
			{
				if ( messageNode.size() != 3
						|| !( messageNode.get( 1 ).isJsonPrimitive() && ( (JsonPrimitive) messageNode.get( 1 ) ).isNumber() )
						|| !( messageNode.get( 2 ).isJsonPrimitive() && ( (JsonPrimitive) messageNode.get( 2 ) ).isNumber() ) )
					throw new WampError( ApplicationError.INVALID_MESSAGE );

				long requestId = messageNode.get( 1 ).getAsLong();
				long subscriptionId = messageNode.get( 2 ).getAsLong();

				return new UnsubscribeMessage( requestId, subscriptionId );
			}
		}
	}

	/**
	 * Acknowledge sent by a Broker to a Subscriber to acknowledge
	 * unsubscription. [UNSUBSCRIBED, UNSUBSCRIBE.Request|id]
	 */
	public static class UnsubscribedMessage extends WampMessage
	{
		public final static int ID = 35;
		public long requestId;

		public UnsubscribedMessage( long requestId )
		{
			this.requestId = requestId;
		}

		public JsonArray toObjectArray() throws WampError
		{
			JsonArray messageNode = new JsonArray();
			messageNode.add( ID );
			messageNode.add( requestId );
			return messageNode;
		}

		static class Factory implements WampMessageFactory
		{
			@Override
			public WampMessage fromObjectArray( JsonArray messageNode ) throws WampError
			{
				if ( messageNode.size() != 2
						|| !( messageNode.get( 1 ).isJsonPrimitive() && ( (JsonPrimitive) messageNode.get( 1 ) ).isNumber() ) )
					throw new WampError( ApplicationError.INVALID_MESSAGE );

				long requestId = messageNode.get( 1 ).getAsLong();

				return new UnsubscribedMessage( requestId );
			}
		}
	}

	/**
	 * Event dispatched by Broker to Subscribers for subscription the event was
	 * matching. [EVENT, SUBSCRIBED.Subscription|id, PUBLISHED.Publication|id,
	 * Details|dict] [EVENT, SUBSCRIBED.Subscription|id,
	 * PUBLISHED.Publication|id, Details|dict, PUBLISH.Arguments|list] [EVENT,
	 * SUBSCRIBED.Subscription|id, PUBLISHED.Publication|id, Details|dict,
	 * PUBLISH.Arguments|list, PUBLISH.ArgumentsKw|dict]
	 */
	public static class EventMessage extends WampMessage
	{
		public final static int ID = 36;
		public long       subscriptionId;
		public long       publicationId;
		public JsonObject details;
		public JsonArray  arguments;
		public JsonObject argumentsKw;

		public EventMessage( long subscriptionId, long publicationId,
							 JsonObject details, JsonArray arguments, JsonObject argumentsKw )
		{
			this.subscriptionId = subscriptionId;
			this.publicationId = publicationId;
			this.details = details;
			this.arguments = arguments;
			this.argumentsKw = argumentsKw;
		}

		public JsonArray toObjectArray() throws WampError
		{
			JsonArray messageNode = new JsonArray();
			messageNode.add( ID );
			messageNode.add( subscriptionId );
			messageNode.add( publicationId );
			if ( details != null )
				messageNode.add( details );
			else
				messageNode.add( new JsonObject() );
			if ( arguments != null )
				messageNode.add( arguments );
			else if ( argumentsKw != null )
				messageNode.add( new JsonArray() );
			if ( argumentsKw != null )
				messageNode.add( argumentsKw );
			return messageNode;
		}

		static class Factory implements WampMessageFactory
		{
			@Override
			public WampMessage fromObjectArray( JsonArray messageNode ) throws WampError
			{
				if ( messageNode.size() < 4 || messageNode.size() > 6
						|| !( messageNode.get( 1 ).isJsonPrimitive() && ( (JsonPrimitive) messageNode.get( 1 ) ).isNumber() )
						|| !( messageNode.get( 2 ).isJsonPrimitive() && ( (JsonPrimitive) messageNode.get( 2 ) ).isNumber() )
						|| !messageNode.get( 3 ).isJsonObject() )
					throw new WampError( ApplicationError.INVALID_MESSAGE );

				long subscriptionId = messageNode.get( 1 ).getAsLong();
				long publicationId = messageNode.get( 2 ).getAsLong();
				JsonObject details = (JsonObject) messageNode.get( 3 );
				JsonArray arguments = null;
				JsonObject argumentsKw = null;

				if ( messageNode.size() >= 5 )
				{
					if ( !messageNode.get( 4 ).isJsonArray() )
						throw new WampError( ApplicationError.INVALID_MESSAGE );
					arguments = (JsonArray) messageNode.get( 4 );
					if ( messageNode.size() >= 6 )
					{
						if ( !messageNode.get( 5 ).isJsonObject() )
							throw new WampError( ApplicationError.INVALID_MESSAGE );
						argumentsKw = (JsonObject) messageNode.get( 5 );
					}
				}

				return new EventMessage( subscriptionId, publicationId, details,
						arguments, argumentsKw );
			}
		}
	}

	/**
	 * Call as originally issued by the Caller to the Dealer. [CALL, Request|id,
	 * Options|dict, Procedure|uri] [CALL, Request|id, Options|dict,
	 * Procedure|uri, Arguments|list] [CALL, Request|id, Options|dict,
	 * Procedure|uri, Arguments|list, ArgumentsKw|dict]
	 */
	public static class CallMessage extends WampMessage
	{
		public final static int ID = 48;
		public long       requestId;
		public JsonObject options;
		public String     procedure;
		public JsonArray  arguments;
		public JsonObject argumentsKw;

		public CallMessage( long requestId, JsonObject options, String procedure,
							JsonArray arguments, JsonObject argumentsKw )
		{
			this.requestId = requestId;
			this.options = options;
			this.procedure = procedure;
			this.arguments = arguments;
			this.argumentsKw = argumentsKw;
		}

		public JsonArray toObjectArray() throws WampError
		{
			JsonArray messageNode = new JsonArray();
			messageNode.add( ID );
			messageNode.add( requestId );
			if ( options != null )
				messageNode.add( options );
			else
				messageNode.add( new JsonObject() );
			messageNode.add( procedure );
			if ( arguments != null )
				messageNode.add( arguments );
			else if ( argumentsKw != null )
				messageNode.add( new JsonArray() );
			if ( argumentsKw != null )
				messageNode.add( argumentsKw );
			return messageNode;
		}

		static class Factory implements WampMessageFactory
		{
			@Override
			public WampMessage fromObjectArray( JsonArray messageNode ) throws WampError
			{
				if ( messageNode.size() < 4 || messageNode.size() > 6
						|| !( messageNode.get( 1 ).isJsonPrimitive() && ( (JsonPrimitive) messageNode.get( 1 ) ).isNumber() )
						|| !messageNode.get( 2 ).isJsonObject()
						|| !( messageNode.get( 3 ).isJsonPrimitive() && ( (JsonPrimitive) messageNode.get( 3 ) ).isString() ) )
					throw new WampError( ApplicationError.INVALID_MESSAGE );

				long requestId = messageNode.get( 1 ).getAsLong();
				JsonObject options = (JsonObject) messageNode.get( 2 );
				String procedure = messageNode.get( 3 ).getAsString();
				JsonArray arguments = null;
				JsonObject argumentsKw = null;

				if ( messageNode.size() >= 5 )
				{
					if ( !messageNode.get( 4 ).isJsonArray() )
						throw new WampError( ApplicationError.INVALID_MESSAGE );
					arguments = (JsonArray) messageNode.get( 4 );
					if ( messageNode.size() >= 6 )
					{
						if ( !messageNode.get( 5 ).isJsonObject() )
							throw new WampError( ApplicationError.INVALID_MESSAGE );
						argumentsKw = (JsonObject) messageNode.get( 5 );
					}
				}

				return new CallMessage( requestId, options, procedure,
						arguments, argumentsKw );
			}
		}
	}

	/**
	 * Result of a call as returned by Dealer to Caller. [RESULT,
	 * CALL.Request|id, Details|dict] [RESULT, CALL.Request|id, Details|dict,
	 * YIELD.Arguments|list] [RESULT, CALL.Request|id, Details|dict,
	 * YIELD.Arguments|list, YIELD.ArgumentsKw|dict]
	 */
	public static class ResultMessage extends WampMessage
	{
		public final static int ID = 50;
		public long       requestId;
		public JsonObject details;
		public JsonArray  arguments;
		public JsonObject argumentsKw;

		public ResultMessage( long requestId, JsonObject details,
							  JsonArray arguments, JsonObject argumentsKw )
		{
			this.requestId = requestId;
			this.details = details;
			this.arguments = arguments;
			this.argumentsKw = argumentsKw;
		}

		public JsonArray toObjectArray() throws WampError
		{
			JsonArray messageNode = new JsonArray();
			messageNode.add( ID );
			messageNode.add( requestId );
			if ( details != null )
				messageNode.add( details );
			else
				messageNode.add( new JsonObject() );
			if ( arguments != null )
				messageNode.add( arguments );
			else if ( argumentsKw != null )
				messageNode.add( new JsonArray() );
			if ( argumentsKw != null )
				messageNode.add( argumentsKw );
			return messageNode;
		}

		static class Factory implements WampMessageFactory
		{
			@Override
			public WampMessage fromObjectArray( JsonArray messageNode ) throws WampError
			{
				if ( messageNode.size() < 3 || messageNode.size() > 5
						|| !( messageNode.get( 1 ).isJsonPrimitive() && ( (JsonPrimitive) messageNode.get( 1 ) ).isNumber() )
						|| !messageNode.get( 2 ).isJsonObject() )
					throw new WampError( ApplicationError.INVALID_MESSAGE );

				long requestId = messageNode.get( 1 ).getAsLong();
				JsonObject details = (JsonObject) messageNode.get( 2 );
				JsonArray arguments = null;
				JsonObject argumentsKw = null;

				if ( messageNode.size() >= 4 )
				{
					if ( !messageNode.get( 3 ).isJsonArray() )
						throw new WampError( ApplicationError.INVALID_MESSAGE );
					arguments = (JsonArray) messageNode.get( 3 );
					if ( messageNode.size() >= 5 )
					{
						if ( !messageNode.get( 4 ).isJsonObject() )
							throw new WampError( ApplicationError.INVALID_MESSAGE );
						argumentsKw = (JsonObject) messageNode.get( 4 );
					}
				}

				return new ResultMessage( requestId, details, arguments,
						argumentsKw );
			}
		}
	}

	/**
	 * A Callees request to register an endpoint at a Dealer. [REGISTER,
	 * Request|id, Options|dict, Procedure|uri]
	 */
	public static class RegisterMessage extends WampMessage
	{
		public final static int ID = 64;
		public long       requestId;
		public JsonObject options;
		public String     procedure;

		public RegisterMessage( long requestId, JsonObject options, String procedure )
		{
			this.requestId = requestId;
			this.options = options;
			this.procedure = procedure;
		}

		public JsonArray toObjectArray() throws WampError
		{
			JsonArray messageNode = new JsonArray();
			messageNode.add( ID );
			messageNode.add( requestId );
			if ( options != null )
				messageNode.add( options );
			else
				messageNode.add( new JsonObject() );
			messageNode.add( procedure );
			return messageNode;
		}

		static class Factory implements WampMessageFactory
		{
			@Override
			public WampMessage fromObjectArray( JsonArray messageNode ) throws WampError
			{
				if ( messageNode.size() != 4
						|| !( messageNode.get( 1 ).isJsonPrimitive() && ( (JsonPrimitive) messageNode.get( 1 ) ).isNumber() )
						|| !messageNode.get( 2 ).isJsonObject()
						|| !( messageNode.get( 3 ).isJsonPrimitive() && ( (JsonPrimitive) messageNode.get( 3 ) ).isString() ) )
					throw new WampError( ApplicationError.INVALID_MESSAGE );

				long requestId = messageNode.get( 1 ).getAsLong();
				JsonObject options = (JsonObject) messageNode.get( 2 );
				String procedure = messageNode.get( 3 ).getAsString();

				return new RegisterMessage( requestId, options, procedure );
			}
		}
	}

	/**
	 * Acknowledge sent by a Dealer to a Callee for successful registration.
	 * [REGISTERED, REGISTER.Request|id, Registration|id]
	 */
	public static class RegisteredMessage extends WampMessage
	{
		public final static int ID = 65;
		public long requestId;
		public long registrationId;

		public RegisteredMessage( long requestId, long registrationId )
		{
			this.requestId = requestId;
			this.registrationId = registrationId;
		}

		public JsonArray toObjectArray() throws WampError
		{
			JsonArray messageNode = new JsonArray();
			messageNode.add( ID );
			messageNode.add( requestId );
			messageNode.add( registrationId );
			return messageNode;
		}

		static class Factory implements WampMessageFactory
		{
			@Override
			public WampMessage fromObjectArray( JsonArray messageNode ) throws WampError
			{
				if ( messageNode.size() != 3
						|| !( messageNode.get( 1 ).isJsonPrimitive() && ( (JsonPrimitive) messageNode.get( 1 ) ).isNumber() )
						|| !( messageNode.get( 2 ).isJsonPrimitive() && ( (JsonPrimitive) messageNode.get( 2 ) ).isNumber() ) )
					throw new WampError( ApplicationError.INVALID_MESSAGE );

				long requestId = messageNode.get( 1 ).getAsLong();
				long registrationId = messageNode.get( 2 ).getAsLong();

				return new RegisteredMessage( requestId, registrationId );
			}
		}
	}

	/**
	 * A Callees request to unregister a previsouly established registration.
	 * [UNREGISTER, Request|id, REGISTERED.Registration|id]
	 */
	public static class UnregisterMessage extends WampMessage
	{
		public final static int ID = 66;
		public long requestId;
		public long registrationId;

		public UnregisterMessage( long requestId, long registrationId )
		{
			this.requestId = requestId;
			this.registrationId = registrationId;
		}

		public JsonArray toObjectArray() throws WampError
		{
			JsonArray messageNode = new JsonArray();
			messageNode.add( ID );
			messageNode.add( requestId );
			messageNode.add( registrationId );
			return messageNode;
		}

		static class Factory implements WampMessageFactory
		{
			@Override
			public WampMessage fromObjectArray( JsonArray messageNode )
					throws WampError
			{
				if ( messageNode.size() != 3
						|| !( messageNode.get( 1 ).isJsonPrimitive() && ( (JsonPrimitive) messageNode.get( 1 ) ).isNumber() )
						|| !( messageNode.get( 2 ).isJsonPrimitive() && ( (JsonPrimitive) messageNode.get( 2 ) ).isNumber() ) )
					throw new WampError( ApplicationError.INVALID_MESSAGE );

				long requestId = messageNode.get( 1 ).getAsLong();
				long registrationId = messageNode.get( 2 ).getAsLong();

				return new UnregisterMessage( requestId, registrationId );
			}
		}
	}

	/**
	 * Acknowledge sent by a Dealer to a Callee for successful unregistration.
	 * [UNREGISTERED, UNREGISTER.Request|id]
	 */
	public static class UnregisteredMessage extends WampMessage
	{
		public final static int ID = 67;
		public long requestId;

		public UnregisteredMessage( long requestId )
		{
			this.requestId = requestId;
		}

		public JsonArray toObjectArray() throws WampError
		{
			JsonArray messageNode = new JsonArray();
			messageNode.add( ID );
			messageNode.add( requestId );
			return messageNode;
		}

		static class Factory implements WampMessageFactory
		{
			@Override
			public WampMessage fromObjectArray( JsonArray messageNode ) throws WampError
			{
				if ( messageNode.size() != 2
						|| !( messageNode.get( 1 ).isJsonPrimitive() && ( (JsonPrimitive) messageNode.get( 1 ) ).isNumber() ) )
					throw new WampError( ApplicationError.INVALID_MESSAGE );

				long requestId = messageNode.get( 1 ).getAsLong();

				return new UnregisteredMessage( requestId );
			}
		}
	}

	/**
	 * Actual invocation of an endpoint sent by Dealer to a Callee. [INVOCATION,
	 * Request|id, REGISTERED.Registration|id, Details|dict] [INVOCATION,
	 * Request|id, REGISTERED.Registration|id, Details|dict,
	 * CALL.Arguments|list] [INVOCATION, Request|id, REGISTERED.Registration|id,
	 * Details|dict, CALL.Arguments|list, CALL.ArgumentsKw|dict]
	 */
	public static class InvocationMessage extends WampMessage
	{
		public final static int ID = 68;
		public long       requestId;
		public long       registrationId;
		public JsonObject details;
		public JsonArray  arguments;
		public JsonObject argumentsKw;

		public InvocationMessage( long requestId, long registrationId,
								  JsonObject details, JsonArray arguments, JsonObject argumentsKw )
		{
			this.requestId = requestId;
			this.registrationId = registrationId;
			this.details = details;
			this.arguments = arguments;
			this.argumentsKw = argumentsKw;
		}

		public JsonArray toObjectArray() throws WampError
		{
			JsonArray messageNode = new JsonArray();
			messageNode.add( ID );
			messageNode.add( requestId );
			messageNode.add( registrationId );
			if ( details != null )
				messageNode.add( details );
			else
				messageNode.add( new JsonObject() );
			if ( arguments != null )
				messageNode.add( arguments );
			else if ( argumentsKw != null )
				messageNode.add( new JsonArray() );
			if ( argumentsKw != null )
				messageNode.add( argumentsKw );
			return messageNode;
		}

		static class Factory implements WampMessageFactory
		{
			@Override
			public WampMessage fromObjectArray( JsonArray messageNode ) throws WampError
			{
				if ( messageNode.size() < 4 || messageNode.size() > 6
						|| !( messageNode.get( 1 ).isJsonPrimitive() && ( (JsonPrimitive) messageNode.get( 1 ) ).isNumber() )
						|| !( messageNode.get( 2 ).isJsonPrimitive() && ( (JsonPrimitive) messageNode.get( 2 ) ).isNumber() )
						|| !messageNode.get( 3 ).isJsonObject() )
					throw new WampError( ApplicationError.INVALID_MESSAGE );

				long requestId = messageNode.get( 1 ).getAsLong();
				long registrationId = messageNode.get( 2 ).getAsLong();
				JsonObject details = (JsonObject) messageNode.get( 3 );
				JsonArray arguments = null;
				JsonObject argumentsKw = null;

				if ( messageNode.size() >= 5 )
				{
					if ( !messageNode.get( 4 ).isJsonArray() )
						throw new WampError( ApplicationError.INVALID_MESSAGE );
					arguments = (JsonArray) messageNode.get( 4 );
					if ( messageNode.size() >= 6 )
					{
						if ( !messageNode.get( 5 ).isJsonObject() )
							throw new WampError( ApplicationError.INVALID_MESSAGE );
						argumentsKw = (JsonObject) messageNode.get( 5 );
					}
				}

				return new InvocationMessage( requestId, registrationId,
						details, arguments, argumentsKw );
			}
		}
	}

	/**
	 * Actual yield from an endpoint send by a Callee to Dealer. [YIELD,
	 * INVOCATION.Request|id, Options|dict] [YIELD, INVOCATION.Request|id,
	 * Options|dict, Arguments|list] [YIELD, INVOCATION.Request|id,
	 * Options|dict, Arguments|list, ArgumentsKw|dict]
	 */
	public static class YieldMessage extends WampMessage
	{
		public final static int ID = 70;
		public long       requestId;
		public JsonObject options;
		public JsonArray  arguments;
		public JsonObject argumentsKw;

		public YieldMessage( long requestId, JsonObject options,
							 JsonArray arguments, JsonObject argumentsKw )
		{
			this.requestId = requestId;
			this.options = options;
			this.arguments = arguments;
			this.argumentsKw = argumentsKw;
		}

		public JsonArray toObjectArray() throws WampError
		{
			JsonArray messageNode = new JsonArray();
			messageNode.add( ID );
			messageNode.add( requestId );
			if ( options != null )
				messageNode.add( options );
			else
				messageNode.add( new JsonObject() );
			if ( arguments != null )
				messageNode.add( arguments );
			else if ( argumentsKw != null )
				messageNode.add( new JsonArray() );
			if ( argumentsKw != null )
				messageNode.add( argumentsKw );
			return messageNode;
		}

		static class Factory implements WampMessageFactory
		{
			@Override
			public WampMessage fromObjectArray( JsonArray messageNode ) throws WampError
			{
				if ( messageNode.size() < 3 || messageNode.size() > 5
						|| !( messageNode.get( 1 ).isJsonPrimitive() && ( (JsonPrimitive) messageNode.get( 1 ) ).isNumber() )
						|| !messageNode.get( 2 ).isJsonObject() )
					throw new WampError( ApplicationError.INVALID_MESSAGE );

				long requestId = messageNode.get( 1 ).getAsLong();
				JsonObject options = (JsonObject) messageNode.get( 2 );
				JsonArray arguments = null;
				JsonObject argumentsKw = null;

				if ( messageNode.size() >= 4 )
				{
					if ( !messageNode.get( 3 ).isJsonArray() )
						throw new WampError( ApplicationError.INVALID_MESSAGE );
					arguments = (JsonArray) messageNode.get( 3 );
					if ( messageNode.size() >= 5 )
					{
						if ( !messageNode.get( 4 ).isJsonObject() )
							throw new WampError( ApplicationError.INVALID_MESSAGE );
						argumentsKw = (JsonObject) messageNode.get( 4 );
					}
				}

				return new YieldMessage( requestId, options, arguments,
						argumentsKw );
			}
		}
	}
}
