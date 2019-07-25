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
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ScaleDrawable;
import android.os.Handler;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.curate.R;
import com.example.curate.utils.Animations;

import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;

public class ItemTouchHelperCallbacks {

	private RecyclerView.Adapter mAdapter;

	private Context mContext;

	private ColorDrawable background = new ColorDrawable(); // Cached for performance

	public ItemTouchHelperCallbacks(RecyclerView.Adapter adapter, Context context) {
		mAdapter = adapter;
		mContext = context;
	}

	public ItemTouchHelper.SimpleCallback deleteCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
		@Override
		public boolean isItemViewSwipeEnabled() {
			return !((QueueAdapter) mAdapter).isUpdating();
		}

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
			if(((QueueAdapter.ViewHolder) viewHolder).isDeleted) {
				return;
			}

			if (viewHolder.getAdapterPosition() == NO_POSITION || ((QueueAdapter) mAdapter).isUpdating()) {
				super.onChildDraw(c, recyclerView, viewHolder, 0, dY, actionState, isCurrentlyActive);
				return;
			}
			View itemView = viewHolder.itemView;
			if(dX > 0) {
				int height = itemView.getBottom() - itemView.getTop();
				int width = height / 3;
				background.setColor(mContext.getResources().getColor(R.color.darkBlue));
				background.setBounds(0, itemView.getTop(), itemView.getLeft() + (int) dX, itemView.getBottom());
				background.draw(c);
				Drawable d = mContext.getDrawable(R.drawable.ic_circle_cancel);
				d.setTint(mContext.getResources().getColor(R.color.white));
				d.setBounds(itemView.getLeft() + width, itemView.getTop() + width, itemView.getLeft()+ 2*width, itemView.getBottom() - width);
				d.draw(c);
			}
			super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
		}
	};

	public ItemTouchHelper.SimpleCallback likeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
		@Override
		public boolean isItemViewSwipeEnabled() {
			return !((QueueAdapter) mAdapter).isUpdating();
		}

		@Override
		public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
			return false;
		}

		@Override
		public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
			((QueueAdapter) mAdapter).onItemLike(viewHolder);
			((QueueAdapter) mAdapter).notifyItemChanged(viewHolder.getAdapterPosition());
		}

		@Override
		public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
			if (viewHolder.getAdapterPosition() == NO_POSITION || ((QueueAdapter) mAdapter).isUpdating()) {
				super.onChildDraw(c, recyclerView, viewHolder, 0, dY, actionState, isCurrentlyActive);
				return;
			}
			View itemView = viewHolder.itemView;
			if(!isCurrentlyActive || viewHolder.getAdapterPosition() == NO_POSITION) {
				c.drawColor(mContext.getResources().getColor(R.color.darkGray));
				if(viewHolder.getAdapterPosition() == NO_POSITION) return;
			}
			int height = itemView.getBottom() - itemView.getTop();
			int width = height / 3;
			if(dX < 0) {
				background.setColor(mContext.getResources().getColor(R.color.colorAccent));
				background.setBounds(itemView.getRight(), itemView.getTop(), itemView.getRight() + (int) dX, itemView.getBottom());
				background.draw(c);
				Drawable d = mContext.getDrawable(R.drawable.ic_favorite);
				d.setTint(mContext.getResources().getColor(R.color.white));
				d.setBounds(itemView.getRight() - 2*width , itemView.getTop() + width, itemView.getRight() - width,itemView.getBottom() - width);
				d.draw(c);
			}
			super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
		}
	};

	public ItemTouchHelper.SimpleCallback addCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
		@Override
		public boolean isItemViewSwipeEnabled() {
			return !((SearchAdapter) mAdapter).isUpdating();
		}

		@Override
		public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
			return false;
		}

		@Override
		public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
			SearchAdapter.ViewHolder searchViewHolder = (SearchAdapter.ViewHolder) viewHolder;
			((SearchAdapter) mAdapter).onItemAdd(viewHolder.getAdapterPosition());
		}

		@Override
		public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
			if (((SearchAdapter.ViewHolder) viewHolder).ibLike.isSelected() || viewHolder.getAdapterPosition() == NO_POSITION || ((SearchAdapter) mAdapter).isUpdating()) {
				super.onChildDraw(c, recyclerView, viewHolder, 0, dY, actionState, isCurrentlyActive);
				return;
			}
			View itemView = viewHolder.itemView;
			super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
			int height = itemView.getBottom() - itemView.getTop();
			int width = height / 3;
			if(dX < 0) {
				background.setColor(mContext.getResources().getColor(R.color.colorAccent));
				background.setBounds(itemView.getRight(), itemView.getTop(), itemView.getRight() + (int) dX, itemView.getBottom());
				background.draw(c);
				Drawable d = mContext.getDrawable(R.drawable.ic_add);
				d.setTint(mContext.getResources().getColor(R.color.white));
				d.setBounds(itemView.getRight() - 2*width , itemView.getTop() + width, itemView.getRight() - width,itemView.getBottom() - width);
				d.draw(c);
			}

		}

	};
}
