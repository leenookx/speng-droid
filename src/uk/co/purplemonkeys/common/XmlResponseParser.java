package uk.co.purplemonkeys.common;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.util.Log;

/**
 * Contains all parser creation and exception handling logic, for parsers
 * that consume an HttpEntity stream and produce a single object as a result.
 * 
 * XXX - does not provide a way to return a result before input is completely consumed
 * 
 * @author spapadim
 *
 */
public abstract class XmlResponseParser<T> 
{
	private final String TAG = "XmlResponseParser";
	private HttpEntity e;
	private T result;
	
	private XmlPullParserFactory mXmlParserFactory;
	
	public XmlResponseParser (HttpEntity e)  { this.e = e; } 

	public void setResult (T result) { this.result = result; }
	public T getResult ()  { return result; }
	
	/**
	 * Handle an XML event from the parser.
	 * @return  False if parsing is complete, true otherwise
	 */
	abstract public boolean onXmlEvent (XmlPullParser parser, int eventType) throws XmlPullParserException, IOException;
	
	public void onInit (XmlPullParser parser) { }
	
	public T parse () {
		try {
			XmlPullParser parser = getXmlParser(e);
			onInit(parser);
			int eventType = parser.getEventType();
			while (eventType != XmlPullParser.END_DOCUMENT) {
				if (!onXmlEvent(parser, eventType)) {
					break;
				}
				eventType = parser.next();
			}
		} catch (Throwable t) {
			Log.e(TAG, "XmlResponseParser exception", t);
			setResult(null);
	 	} finally {
	 		try {
	 			e.consumeContent();
	 		} catch (Throwable t2) { }
	 	}
	 	return getResult();
	}
	
	private final XmlPullParser getXmlParser (HttpEntity e) throws XmlPullParserException, IllegalStateException, IOException 
	{
		if (mXmlParserFactory == null) {
			mXmlParserFactory = XmlPullParserFactory.newInstance();
		}
		XmlPullParser parser = mXmlParserFactory.newPullParser();
		String charSet = EntityUtils.getContentCharSet(e);
		if (charSet == null) {
			charSet = HTTP.DEFAULT_CONTENT_CHARSET;
		}
		parser.setInput(e.getContent(), charSet);
		return parser;
	}
}