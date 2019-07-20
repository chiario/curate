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
import android.os.Handler;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.curate.R;
import com.example.curate.utils.Animations;

public class ItemTouchHelperCallbacks {

	private RecyclerView.Adapter mAdapter;

	private Context mContext;

	public ItemTouchHelperCallbacks(RecyclerView.Adapter adapter, Context context) {
		mAdapter = adapter;
		mContext = context;
	}

	public ItemTouchHelper.SimpleCallback deleteCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
		@Override
		public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
			return false;
		}

		@Override
		public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
			((QueueAdapter) mAdapter).onItemRemove(viewHolder);
		}

		@Override
		public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
			View itemView = viewHolder.itemView;
			if(!isCurrentlyActive) {
				c.drawColor(mContext.getResources().getColor(R.color.colorBackground));
			}
			if(dX > 0) {
				int height = itemView.getBottom() - itemView.getTop();
				int width = height / 3;
				Drawable d = mContext.getDrawable(R.drawable.ic_circle_cancel);
				d.setTint(mContext.getResources().getColor(R.color.colorSecondary));
				Rect iconDest = new Rect(itemView.getLeft() + width , itemView.getTop() + width, itemView.getLeft()+ 2*width,itemView.getBottom() - width);
				d.setBounds(iconDest);
				d.draw(c);
			}
			super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
		}
	};

	public ItemTouchHelper.SimpleCallback likeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
		@Override
		public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
			return false;
		}

		@Override
		public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
			QueueAdapter.ViewHolder queueViewHolder = (QueueAdapter.ViewHolder) viewHolder;
			((QueueAdapter) mAdapter).onItemLike(viewHolder);
			((QueueAdapter) mAdapter).notifyItemChanged(viewHolder.getAdapterPosition());
			// TODO: Why is this animation not working?
			Handler handler = new Handler();
			handler.post(new Animations.PressRunnable(queueViewHolder.clItem,
					queueViewHolder.ibLike.getX(), queueViewHolder.ibLike.getY()));
		}

		@Override
		public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
			View itemView = viewHolder.itemView;
			if(!isCurrentlyActive) {
				c.drawColor(mContext.getResources().getColor(R.color.colorBackground));
			}
			int height = itemView.getBottom() - itemView.getTop();
			int width = height / 3;
			if(dX < 0) {
				Drawable d = mContext.getDrawable(R.drawable.ic_favorite);
				d.setTint(mContext.getResources().getColor(R.color.colorAccent));
				Rect iconDest = new Rect(itemView.getRight() - 2*width , itemView.getTop() + width, itemView.getRight() - width,itemView.getBottom() - width);
				d.setBounds(iconDest);
				d.draw(c);
			}
			super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
		}
	};

	public ItemTouchHelper.SimpleCallback addCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
		@Override
		public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
			return false;
		}

		@Override
		public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
			SearchAdapter.ViewHolder searchViewHolder = (SearchAdapter.ViewHolder) viewHolder;
			((SearchAdapter) mAdapter).onItemAdd(viewHolder);
			mAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
		}

		@Override
		public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
			if(((SearchAdapter.ViewHolder) viewHolder).ibLike.isSelected()) {
				super.onChildDraw(c, recyclerView, viewHolder, 0, dY, actionState, isCurrentlyActive);
				return;
			}
			View itemView = viewHolder.itemView;
			if(!isCurrentlyActive) {
				c.drawColor(mContext.getResources().getColor(R.color.colorBackground));
			}
			int height = itemView.getBottom() - itemView.getTop();
			int width = height / 3;
			if(dX < 0) {
				Drawable d = mContext.getDrawable(R.drawable.ic_favorite);
				d.setTint(mContext.getResources().getColor(R.color.colorAccent));
				Rect iconDest = new Rect(itemView.getRight() - 2*width , itemView.getTop() + width, itemView.getRight() - width,itemView.getBottom() - width);
				d.setBounds(iconDest);
				d.draw(c);
			}
			super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
		}
	};
}
