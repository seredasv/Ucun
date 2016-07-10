package com.nasctech.ucun.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nasctech.ucun.R;
import com.nasctech.ucun.utils.DBHelper;
import com.nasctech.ucun.utils.GridClickListener;
import com.nasctech.ucun.utils.Utils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

//TODO refactor using Picasso
public class GridAdapter extends BaseAdapter {
    private List<Integer> items;
    private List<String> itemNames;
    private LayoutInflater inflater;
    private Context c;
    private LruCache<String, Bitmap> memoryCache;
    private GridClickListener listener;

    public GridAdapter(Context context) {
        inflater = LayoutInflater.from(context);
        c = context;
        initCache();
        listener = (GridClickListener) c;
        itemNames = new ArrayList<>();
        items = new ArrayList<>();
    }

    private static boolean cancelPotentialWork(String data, ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final String bitmapData = bitmapWorkerTask.getData();
            if (bitmapData != data) {
                // Cancel previous task
                bitmapWorkerTask.cancel(true);
            } else {
                // The same work is already in progress
                return false;
            }
        }
        // No task associated with the ImageView, or an existing task was cancelled
        return true;
    }

    private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }

    private void initNames() {
        itemNames.clear();
        for (int i = 0; i < items.size(); i++) {
            String itemName = DBHelper.getVideoNameById(items.get(i));
            itemNames.add(itemName);
            Log.d("list", "id " + items.get(i) + " name " + itemName);
        }
    }

    public void setList(List<Integer> items) {
        this.items.clear();
        this.items.addAll(items);
        initNames();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Integer getItem(int i) {
        return items.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        final ViewHolder holder;
        final Integer item = items.get(i);
        if (view == null) {
            view = inflater.inflate(R.layout.gridview_item, viewGroup, false);
            holder = new ViewHolder();
            holder.text = (TextView) view.findViewById(R.id.text);
            holder.image = (ImageView) view.findViewById(R.id.picture);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("click", "id click " + String.valueOf(item));
                listener.onGridClick(item);
            }
        });
        String itemName = itemNames.get(i);
        loadBitmap(Utils.PATH + itemName, holder.image, item);
        holder.text.setText(itemName.substring(0, itemName.indexOf(".mp4")).toUpperCase());
        return view;
    }

    private void initCache() {
        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;
        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    private void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null)
            memoryCache.put(key, bitmap);
    }

    private Bitmap getBitmapFromMemCache(String key) {
        return memoryCache.get(key);
    }

    private void loadBitmap(String videoName, ImageView imageView, Integer id) {
        final Bitmap bitmap = getBitmapFromMemCache(videoName);
        if (bitmap != null)
            imageView.setImageBitmap(bitmap);
        else if (cancelPotentialWork(videoName, imageView)) {
            final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
            final AsyncDrawable asyncDrawable =
                    new AsyncDrawable(c.getResources(), null, task); //no placeholder yet
            imageView.setImageDrawable(asyncDrawable);
            task.execute(videoName, String.valueOf(id));
        }
    }

    static class ViewHolder {
        TextView text;
        ImageView image;
    }

    static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap,
                             BitmapWorkerTask bitmapWorkerTask) {
            super(res, bitmap);
            bitmapWorkerTaskReference =
                    new WeakReference<>(bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }

    private class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;
        private String data;

        public BitmapWorkerTask(ImageView imageView) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<>(imageView);
        }

        public String getData() {
            return data;
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(String... params) {
            data = params[0];
            Bitmap bitmap = io.vov.vitamio.ThumbnailUtils.createVideoThumbnail(c, data, io.vov.vitamio.provider.MediaStore.Video.Thumbnails.MICRO_KIND);
            if (bitmap != null)
                addBitmapToMemoryCache(data, bitmap);
            return bitmap;
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled())
                bitmap = null;

            if (imageViewReference != null && bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                final BitmapWorkerTask bitmapWorkerTask =
                        getBitmapWorkerTask(imageView);
                if (this == bitmapWorkerTask && imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
            //not clickable if can't read video

            else {
                imageViewReference.get().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(c, "Can't play this video", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }
}

