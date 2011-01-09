package uk.co.purplemonkeys.common.http;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.protocol.HttpContext;


public class HttpCommon 
{

    public static DefaultHttpClient createGzipHttpClient() 
    {
    	BasicHttpParams params = new BasicHttpParams();
    	SchemeRegistry schemeRegistry = new SchemeRegistry();
    	schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
    	ClientConnectionManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
    	
    	DefaultHttpClient httpclient = new DefaultHttpClient(cm, params);
    	
    	httpclient.addRequestInterceptor(new HttpRequestInterceptor() 
    	{
    		public void process(final HttpRequest request,
    	                    	final HttpContext context) throws HttpException, IOException {
    			if (!request.containsHeader("Accept-Encoding")) {
    				request.addHeader("Accept-Encoding", "gzip");
    			}
    		}
    	});
    	
    	httpclient.addResponseInterceptor(new HttpResponseInterceptor() 
    	{
    		public void process(final HttpResponse response, final HttpContext context) throws HttpException, IOException 
    		{
    			HttpEntity entity = response.getEntity();
    			Header ceheader = entity.getContentEncoding();
    			if (ceheader != null) 
    			{
    				HeaderElement[] codecs = ceheader.getElements();
    				for (int i = 0; i < codecs.length; i++) 
    				{
    					if (codecs[i].getName().equalsIgnoreCase("gzip")) 
    					{
    						response.setEntity(new GzipDecompressingEntity(response.getEntity()));
    						return;
    					}
    				}
    			}
    		}
    	});
    	
    	return httpclient;
    }
}
