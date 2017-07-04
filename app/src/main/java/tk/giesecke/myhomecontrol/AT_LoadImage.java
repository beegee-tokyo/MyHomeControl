package tk.giesecke.myhomecontrol;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

class AT_LoadImage extends AsyncTask<String, Void, Bitmap> {

	AT_LoadImage(Listener listener) {

		listenerLoadImage = listener;
	}

	interface Listener{

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
