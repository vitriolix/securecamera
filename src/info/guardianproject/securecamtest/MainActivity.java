package info.guardianproject.securecamtest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import info.guardianproject.iocipher.File;
import info.guardianproject.iocipher.Pipes;
import info.guardianproject.iocipher.StreamFile;
import info.guardianproject.iocipher.VirtualFileSystem;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private static final String TAG = "MainActivity";
	VirtualFileSystem mVfs = null;
	StreamFile imageStreamFile = null;
	Button mButtonTakePic;
	Button mButtonCopyFileOut;
	
	static {
		System.loadLibrary("iocipher");
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		String dbFile;
		
		mButtonTakePic = (Button) findViewById(R.id.btnTakePic);
		mButtonTakePic.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				takePic();
			}
		});
		
		mButtonCopyFileOut = (Button) findViewById(R.id.btnCopyFileOut);
		mButtonCopyFileOut.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				copyFileOut(MainActivity.this, "/storage/emulated/legacy/DCIM/foo.jpg"); // FIXME debug: copy file to sd on launch so we can see it
			}
		});
		
		((Button) findViewById(R.id.btnShowPic)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				displayImageFromVFS();
			}
		});
		
		
//		copyAssets();
		
		dbFile = getDir("vfs", MODE_PRIVATE).getAbsolutePath() + "/myfiles.db";
		mVfs = new VirtualFileSystem(dbFile);
		// TODO don't use a hard-coded password! prompt for the password
		mVfs.mount("foo"); // FIXME need real password
		
		dumpAllVFSFilePaths();

		//vfs.unmount(); // FIXME why unmount here?
//		cleanup();
//		viewPic();
//		viewPicRealFile();
//		viewPicPipe0();
		
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mVfs != null) {
			mVfs.unmount();
		}
	}
	
	void dumpAllVFSFilePaths() {
		// debug log info
		Collection<File> all = new ArrayList<File>();
		File rootFile = new File("/");
//		for (String s: rootFile.list()) {
//			Log.d(TAG, "filenames: " + s);
//		}
	    addTree(rootFile, all);
		for (File f: all) {
			Log.d(TAG, "file: " + f + "            (size: " + f.length() + " bytes)");
		}
	}
	
	void cleanup() {
		Log.d(TAG, "cleanup...");
		if (imageStreamFile != null) 
			imageStreamFile.close();
	}
	
	void displayImageFromVFS() {
		try {
			File imageFile = new File("/storage/emulated/legacy/DCIM/foo.jpg");
			ImageView iv = ((ImageView)findViewById(R.id.imageView1));
			InputStream is;
			is = new info.guardianproject.iocipher.FileInputStream(imageFile);
			Bitmap bm = BitmapFactory.decodeStream(is);
			int nh = (int) ( bm.getHeight() * (512.0 / bm.getWidth()) );
			Bitmap scaled = Bitmap.createScaledBitmap(bm, 512, nh, true);
			iv.setImageBitmap(scaled);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	void viewPic() {
		StreamFile imageStreamFile = new StreamFile("/storage/emulated/legacy/DCIM/foo.jpg");
		imageStreamFile.startReadFromVFS();
		String path = imageStreamFile.getAbsolutePath();
//		String path = "/storage/emulated/legacy/Android/data/info.guardianproject.securecamtest/files/foo.jpg";
		Log.d(TAG, "display test image from vfs. real path: " + path + "; vfs path: " + imageStreamFile.getVirtualPath());
		Intent intent = new Intent();
		intent.setAction(android.content.Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.fromFile(new File(path)), "image/jpeg");
		startActivity(intent);
	}
	
	void viewPicRealFile() {
		java.io.File file = new java.io.File("/data/data/info.guardianproject.securecamtest/asset.jpg");
		String path = file.getAbsolutePath();
//		String path = "/storage/emulated/legacy/Android/data/info.guardianproject.securecamtest/files/foo.jpg";
		Log.d(TAG, "display test image from real file. real path: " + path);
		Intent intent = new Intent();
		intent.setAction(android.content.Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.fromFile(new File(path)), "image/jpeg");
		startActivity(intent);
	}
	
	void viewPicPipe0() {
		java.io.File file = new java.io.File("/data/data/info.guardianproject.securecamtest/pipe0.jpg");
		String path = file.getAbsolutePath();
//		String path = "/storage/emulated/legacy/Android/data/info.guardianproject.securecamtest/files/foo.jpg";
		Log.d(TAG, "display test image from real file. real path: " + path);
		Intent intent = new Intent();
		intent.setAction(android.content.Intent.ACTION_VIEW);
		intent.setDataAndType(Uri.fromFile(new File(path)), "image/jpeg");
		startActivity(intent);
	}
	
	void takePic() {
		Intent imageIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		File imagesFolder = new File(Environment.getExternalStorageDirectory(), "MyImages");
		//File imagesFolder = new File(Environment.getExternalStorageDirectory(), "MyImages");
		//java.io.File imagesFolder = new java.io.File("/data/data/info.guardianproject.securecamtest/");
//		java.io.File imagesFolder = new java.io.File("/storage/sdcard0/DCIM/");
//		java.io.File imagesFolder = new java.io.File(Environment.DIRECTORY_DCIM);
		imagesFolder.mkdirs();
		
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    File imageFile = new File(imagesFolder, "securecam_"+ timeStamp + ".jpg");
	    imageStreamFile = new StreamFile(imageFile.getAbsolutePath());
		Log.d(TAG, "vfs file: " + imageFile.getAbsolutePath());
		Uri uriStreamFile = Uri.fromFile(imageStreamFile.getJavaIoFile());
		Log.d(TAG, "pipe uri: " + uriStreamFile);
		imageStreamFile.startWriteToVFS();
		
//		java.io.File file = new java.io.File(imageFile.getAbsolutePath());
//		imageIntent.putExtra(MediaStore.EXTRA_OUTPUT, file);
		
		imageIntent.putExtra(MediaStore.EXTRA_OUTPUT, uriStreamFile);
		startActivityForResult(imageIntent, 0);
		// FIXME close imageStreamFile in result handler
		
		/*
		Pipes.createfifonative();
		Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		java.io.File dir=
		    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);

		java.io.File output=new File(dir, "CameraContentDemo.jpeg");
		i.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(output));
		Log.d("foo", "uri: " + Uri.fromFile(output));

		startActivityForResult(i, 0);
		*/
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}


	public static void addTree(File file, Collection<File> all) {
		File[] children = file.listFiles();
		if (children != null) {
			for (File child : children) {
				all.add(child);
				addTree(child, all);
			}
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		cleanup();
		displayImageFromVFS();
	}
	
	// for debugging iocipher, it copies a file out to the sd
	// FIXME IOCipher make sure to remove in prod version
	public static void copyFileOut(Context context, String vfsFilename) {
		try {
			File file = new File(vfsFilename);
			Log.d(TAG, "copyFileOut file: " + file);
			if (file.exists()) {
				Log.d(TAG, "copyFileOut file exists, file.length(): " + file.length());
				
				java.io.File outFile = new File(context.getExternalFilesDir(null), file.getName());
				Log.d(TAG, "copyFileOut outFile: " + outFile);
				
				byte[] buf = new byte[512]; // optimize the size of buffer to your need
			    int num;
				InputStream is = new info.guardianproject.iocipher.FileInputStream(file);
				OutputStream os = new java.io.FileOutputStream(outFile);
				int copied = 0;
				while ((num = is.read(buf)) != -1) {
					os.write(buf, 0, num);
					copied += num;
				}
				Log.d(TAG, "copied: " + copied + " bytes");
				is.close();
				os.flush();
				os.close();
				Toast.makeText(context, "copied file to " + outFile.getAbsolutePath(), Toast.LENGTH_LONG);
			} else {
				Log.d(TAG, "copyFileOut file doesn't exists");
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void copyAssets() {
	    AssetManager assetManager = getAssets();
	    String[] files = null;
	    try {
	        files = assetManager.list("");
	    } catch (IOException e) {
	        Log.e("tag", "Failed to get asset file list.", e);
	    }
	    for(String filename : files) {
	        InputStream in = null;
	        OutputStream out = null;
	        try {
	          in = assetManager.open(filename);
	          File outFile = new File("/data/data/info.guardianproject.securecamtest/", filename);
	          out = new java.io.FileOutputStream(outFile);
	          copyFile(in, out);
	          in.close();
	          in = null;
	          out.flush();
	          out.close();
	          out = null;
	        } catch(IOException e) {
	            Log.e("tag", "Failed to copy asset file: " + filename, e);
	        }       
	    }
	}
	private void copyFile(InputStream in, OutputStream out) throws IOException {
	    byte[] buffer = new byte[1024];
	    int read;
	    while((read = in.read(buffer)) != -1){
	      out.write(buffer, 0, read);
	    }
	}
}
