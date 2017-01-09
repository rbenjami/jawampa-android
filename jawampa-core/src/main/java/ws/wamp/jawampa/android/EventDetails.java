package ws.wamp.jawampa.android;

public class EventDetails<T>
{
	final T      message;
	final String topic;

	public EventDetails( T msg, String topic )
	{
		this.message = msg;
		this.topic = topic;
	}

	public T message()
	{
		return message;
	}

	public String topic()
	{
		return topic;
	}
}
