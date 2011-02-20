package uk.co.purplemonkeys.spengler.articlefeed;

import uk.co.purplemonkeys.spengler.R;
import uk.co.purplemonkeys.spengler.providers.Article.Articles;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class ArticlesList extends ListActivity 
{
	private String[] ARTICLES_PROJECTION = new String[] 
           {
               Articles._ID, 
               Articles.TITLE,
               Articles.URL
       	};
	private Context _context;
	
	
	@Override
	protected void onCreate(Bundle icicle) 
	{
		super.onCreate(icicle);
		setContentView(R.layout.main);
        
		fillData();
		
//		listView = (ListView) this.findViewById(R.id.ListView);
//		Cursor cur = managedQuery(Articles.CONTENT_URI, PROJECTION, null, null, null);
//		SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
//				android.R.layout.activity_list_item,
//				cur,
//				new String[] {Articles.TITLE},
//				new int[] {R.id.ListView});
//		adapter.setViewResource(android.R.layout.simple_list_item_1);
//		listView.setAdapter(adapter);
	}
	
	@Override
    protected void onListItemClick(ListView l, View v, int position, long id) 
	{
        super.onListItemClick(l, v, position, id);
        
        Cursor articlesCursor = (Cursor) l.getAdapter().getItem(position);

    	int f1 = articlesCursor.getColumnIndex(Articles.URL);
    	String article_url = articlesCursor.getString( f1 );
        
        Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(article_url));
        startActivity(myIntent);
    }
	
	private void fillData() 
	{
		Cursor taskCursor = managedQuery(Articles.CONTENT_URI, ARTICLES_PROJECTION, null, null, null);
		if (!taskCursor.moveToFirst())
		{
		}
		else
		{
			SimpleCursorAdapter taskAdaptor = new SimpleCursorAdapter(_context, 
				android.R.layout.simple_spinner_item,
				taskCursor,
				new String[] {Articles.TITLE},
				new int[] {android.R.id.text1});
			taskAdaptor.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			ListView _spTasks = (ListView)findViewById( R.id.ListView );
			_spTasks.setAdapter( taskAdaptor );
		}
	}
}