package com.vzome.android.viewer;


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;

import com.dropbox.chooser.android.DbxChooser;


public class MainActivity extends Activity {
	
	public final static String EXTRA_MESSAGE = "com.vzome.model.file";

    // A request code's purpose is to match the result of a "startActivityForResult" with
    // the type of the original request.  Choose any value.
    private static final int READ_REQUEST_CODE = 1337;
    
    static final int DBX_CHOOSER_REQUEST = 0;  // You can change this if needed

    private DbxChooser mChooser;

    @Override
    protected void onCreate( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.activity_main );
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    /** Called when the user clicks the "choose a model" button */
    public void chooseModel( View view )
    {
    	mChooser = new DbxChooser( "bcb5b6h8p2wvrfb" );

        mChooser.forResultType( DbxChooser.ResultType.FILE_CONTENT )
        	.launch( MainActivity.this, DBX_CHOOSER_REQUEST );
    }
    
    @Override
    public void onActivityResult( int requestCode, int resultCode, Intent data )
    {
        if (requestCode == DBX_CHOOSER_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                DbxChooser.Result result = new DbxChooser.Result(data);
                Uri uri = result.getLink();
                String asUrl = uri.toString();

    	        Intent intent = new Intent( this, View3dActivity.class );
                intent.putExtra( EXTRA_MESSAGE, asUrl );
                startActivity(intent);

            } else {
                // Failed or was cancelled by the user.
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
