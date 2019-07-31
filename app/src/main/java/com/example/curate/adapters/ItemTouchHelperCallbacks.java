package com.example.curate.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RotateDrawable;
import android.os.Handler;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.resource.bitmap.Rotate;
import com.example.curate.R;

import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;

public class ItemTouchHelperCallbacks {

	private RecyclerView.Adapter mAdapter;

	private Context mContext;


	// Drawables are cached for performance
	private final ColorDrawable background = new ColorDrawable();
	private Drawable mAdd;
	private Drawable mLike;
	private Drawable mRemove;

	public ItemTouchHelperCallbacks(RecyclerView.Adapter adapter, Context context) {
		mAdapter = adapter;
		mContext = context;

		// Set the drawables in constructor to avoid having to do so in the onChildDraw
		mAdd = mContext.getDrawable(R.drawable.ic_add);
		mLike = mContext.getDrawable(R.drawable.ic_favorite);
		mRemove = mContext.getDrawable(R.drawable.ic_circle_cancel);
		mAdd.setTint(mContext.getResources().getColor(R.color.white));
		mLike.setTint(mContext.getResources().getColor(R.color.white));
		mRemove.setTint(mContext.getResources().getColor(R.color.white));

	}

	/**
	 * The call back for deleting items from the QueueAdapter
	 */
	public ItemTouchHelper.SimpleCallback deleteCallback =
			new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
		@Override
		public boolean isItemViewSwipeEnabled() {
			return true;
		}

		@Override
		public boolean onMove(@NonNull RecyclerView recyclerView,
		                      @NonNull RecyclerView.ViewHolder viewHolder,
		                      @NonNull RecyclerView.ViewHolder target) {
			return false;
		}

		@Override
		public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
			((QueueAdapter) mAdapter).onItemSwipedRemove(viewHolder);
		}

		@Override
		public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
		                        @NonNull RecyclerView.ViewHolder viewHolder, float dX,
		                        float dY, int actionState, boolean isCurrentlyActive) {

			// Check for positive displacement
			if(dX > 0) {
				View itemView = viewHolder.itemView;
				// Draw icon on left side
				int width = (itemView.getBottom() - itemView.getTop())/3;
				mRemove.setBounds(itemView.getLeft() + width, itemView.getTop() + width,
						itemView.getLeft() + 2 * width, itemView.getBottom() - width);
				mRemove.draw(c);
			} else {
				mRemove.setBounds(0,0,0,0);
				mRemove.draw(c);
			}

			super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
		}
	};
	/**
	 * The call back for liking items from the QueueAdapter
	 */
	public ItemTouchHelper.SimpleCallback likeCallback =
			new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
		@Override
		public boolean isItemViewSwipeEnabled() {
			return true;
		}

		@Override
		public boolean onMove(@NonNull RecyclerView recyclerView,
		                      @NonNull RecyclerView.ViewHolder viewHolder,
		                      @NonNull RecyclerView.ViewHolder target) {
			return false;
		}

		@Override
		public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
			((QueueAdapter) mAdapter).onItemSwipedLike(viewHolder);
			mAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
		}

		@Override
		public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
		                        @NonNull RecyclerView.ViewHolder viewHolder, float dX,
		                        float dY, int actionState, boolean isCurrentlyActive) {

			drawOnRight(viewHolder, dX, c, mLike);

			super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
		}
	};

	/**
	 * The call back for adding items from the SearchAdapter
	 */
	public ItemTouchHelper.SimpleCallback addCallback =
			new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
		@Override
		public boolean isItemViewSwipeEnabled() {
			return true;
		}

		@Override
		public boolean onMove(@NonNull RecyclerView recyclerView,
		                      @NonNull RecyclerView.ViewHolder viewHolder,
		                      @NonNull RecyclerView.ViewHolder target) {
			return false;
		}

		@Override
		public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
			if(viewHolder instanceof SearchAdapter.SectionViewHolder) return;
			((SearchAdapter) mAdapter).onItemSwipedAdd(viewHolder);
			mAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
		}

		@Override
		public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
		                        @NonNull RecyclerView.ViewHolder viewHolder, float dX,
		                        float dY, int actionState, boolean isCurrentlyActive) {

			// Don't swipe the section viewholders or the songs in queue
			if (viewHolder instanceof SearchAdapter.SectionViewHolder ||
					viewHolder.getItemViewType() == SearchAdapter.TYPE_SONG_IN_QUEUE) {
				super.onChildDraw(c, recyclerView, viewHolder,
						0, dY, actionState, isCurrentlyActive);
				return;
			}

			drawOnRight(viewHolder, dX, c, mAdd);

			super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
		}

	};


	/**
	 * Draws the provided icon on the right side of the ViewHolder, with a colored background
	 * @param viewHolder - The viewholder being swiped
	 * @param dX - the x displacement of the viewholder
	 * @param c - the canvas to draw on
	 * @param icon - the icon to draw
	 */
	private void drawOnRight(RecyclerView.ViewHolder viewHolder, float dX, Canvas c, Drawable icon) {
		View itemView = viewHolder.itemView;

		if(dX < 0) {
			// Draw background
			background.setColor(mContext.getResources().getColor(R.color.colorAccent));
			background.setBounds(itemView.getRight(), itemView.getTop(),
					itemView.getRight() + (int) dX, itemView.getBottom());
			background.draw(c);

			// Draw icon
			int width = (itemView.getBottom() - itemView.getTop())/3;
			icon.setBounds(itemView.getRight() - 2 * width , itemView.getTop() + width,
					itemView.getRight() - width,itemView.getBottom() - width);
			icon.draw(c);
		} else {
			background.setBounds(0,0,0,0);
			background.draw(c);
		}
	}
}
