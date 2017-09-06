package tk.giesecke.myhomecontrol.security;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class LoadImage extends AsyncTask<String, Void, Bitmap> {

	public LoadImage(Listener listener) {

		listenerLoadImage = listener;
	}

	public interface Listener{

		void onImageLoaded(Bitmap bitmap);
		void onError();
	}

	private final Listener listenerLoadImage;
	@Override
	protected Bitmap doInBackground(String... args) {

		try {

			return BitmapFactory.decodeStream((InputStream)new URL(args[0]).getContent());

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	protected void onPostExecute(Bitmap bitmap) {

		if (bitmap != null) {

			listenerLoadImage.onImageLoaded(bitmap);

		} else {

			listenerLoadImage.onError();
		}
	}
}
