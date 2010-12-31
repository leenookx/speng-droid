package uk.co.purplemonkeys.spengler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;

class ShareLinkTask extends AsyncTask<Void, Void, Object> 
{
	private final static String TAG = "SharedLinkTask";
	
    private String _url;
    private String _description;
    private String _keywords;
    private DefaultHttpClient mClient = createGzipHttpClient();
    
    String _mUserError = "Error creating submission. Please try again.";
   
    ShareLinkTask(String url, String description, String keywords)
    {
    	_url = url;
    	_description = description;
    	_keywords = keywords;
    }
   
    @Override
    public Void doInBackground(Void... unused_params) 
    {
    	HttpEntity entity = null;
    	
        try 
        {
        	// Construct data
        	JSONObject post_params = new JSONObject();
        	post_params.put("auth_code", "abc123");
        	
        	JSONObject sub = new JSONObject();
        	sub.put("url", _url);
        	sub.put("description", _description);
        	sub.put("keywords", _keywords);
        	
        	post_params.put("links", sub);
        	
        	HttpPost httppost = new HttpPost("http://192.168.230.178:3000/links");
        	
        	// The progress dialog is non-cancelable, so set a shorter timeout than system's
        	HttpParams params = httppost.getParams();
        	HttpConnectionParams.setConnectionTimeout(params, 30000);
        	HttpConnectionParams.setSoTimeout(params, 30000);
   
        	StringEntity s = new StringEntity(post_params.toString());
        	s.setContentEncoding("UTF-8");
        	s.setContentType("application/json");
        	httppost.setEntity(s);
            
        	// Perform the HTTP POST request
        	HttpResponse response = mClient.execute(httppost);
        	String status = response.getStatusLine().toString();
            if (!status.contains("OK"))
            	throw new HttpException(status);
           
            entity = response.getEntity();

            BufferedReader in = new BufferedReader(new InputStreamReader(entity.getContent()));
            String line = in.readLine();
            in.close();
            if (line == null || "".equals(line)) {
            	throw new HttpException("No content returned from reply POST");
            }

            entity.consumeContent();           
        } 
        catch (Exception e) {
        	if (entity != null) {
        		try {
        			entity.consumeContent();
        		} 
        		catch (Exception e2) {
        			Log.e(TAG, "entity.consumeContent()", e2);
        		}
        	}
        	
        	Log.e(TAG, "ShareLinkTask", e);
        }
        
        return null;
    }
    
    @Override
    public void onPreExecute() 
    {
    }
   
    @Override
    public void onPostExecute(Object result) 
    {
    	// TODO: We need to dismiss the dialog at this point...
    }
    
    private static DefaultHttpClient createGzipHttpClient() {
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
    		public void process(final HttpResponse response,
    							final HttpContext context) throws HttpException, IOException {
    			HttpEntity entity = response.getEntity();
    			Header ceheader = entity.getContentEncoding();
    			if (ceheader != null) {
    				HeaderElement[] codecs = ceheader.getElements();
    				for (int i = 0; i < codecs.length; i++) {
    					if (codecs[i].getName().equalsIgnoreCase("gzip")) {
    						response.setEntity(
    								new GzipDecompressingEntity(response.getEntity()));
    						return;
    					}
    				}
    			}
    		}
    	});
    	
    	return httpclient;
    }
    
    static class GzipDecompressingEntity extends HttpEntityWrapper {
    	public GzipDecompressingEntity(final HttpEntity entity) {
    		super(entity);
    	}
        
    	@Override
        public InputStream getContent() throws IOException, IllegalStateException {
            // the wrapped entity's getContent() decides about repeatability
            InputStream wrappedin = wrappedEntity.getContent();
            return new GZIPInputStream(wrappedin);
        }
    	
        @Override
        public long getContentLength() {
            // length of ungzipped content is not known
            return -1;
        }
    }
}