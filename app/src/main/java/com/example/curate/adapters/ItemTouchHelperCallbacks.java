package com.example.curate.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ScaleDrawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.curate.R;

public class ItemTouchHelperCallbacks {

	private QueueAdapter mAdapter;

	private Context mContext;

	private int lastDirection;

	private int mWidth;

	public interface Adapter {
		void onItemDismiss(int position);
	}

	public ItemTouchHelperCallbacks(QueueAdapter adapter, int width, Context context) {
		mAdapter = adapter;
		mContext = context;
		mWidth = width;
	}

	public ItemTouchHelper.SimpleCallback fullCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
		@Override
		public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
			return false;
		}

		@Override
		public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
			if(direction == ItemTouchHelper.RIGHT) {
				mAdapter.onItemRemove(viewHolder);
			}
			else if (direction == ItemTouchHelper.LEFT) {
				mAdapter.onItemLike(viewHolder);
				mAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
			}
		}

		@Override
		public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
			View itemView = viewHolder.itemView;
			float newDx = dX;
			if(!isCurrentlyActive) {
				c.drawColor(mContext.getResources().getColor(R.color.colorBackground));
			}

			int height = itemView.getBottom() - itemView.getTop();
			int width = height / 3;

			if(dX > 0) {
				Drawable d = mContext.getDrawable(R.drawable.ic_circle_cancel);
				d.setTint(mContext.getResources().getColor(R.color.colorSecondary));
				Rect iconDest = new Rect(itemView.getLeft() + width , itemView.getTop() + width, itemView.getLeft()+ 2*width,itemView.getBottom() - width);
				d.setBounds(iconDest);
				d.draw(c);
			}
			else if(dX < 0) {
				Drawable d = mContext.getDrawable(R.drawable.ic_favorite);
				d.setTint(mContext.getResources().getColor(R.color.colorAccent));
				Rect iconDest = new Rect(itemView.getRight() - 2*width , itemView.getTop() + width, itemView.getRight() - width,itemView.getBottom() - width);
				d.setBounds(iconDest);
				d.draw(c);
			}
			super.onChildDraw(c, recyclerView, viewHolder, newDx, dY, actionState, isCurrentlyActive);
		}
	};
}
