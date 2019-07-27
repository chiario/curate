package com.example.curate.adapters;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.curate.R;

import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;

public class ItemTouchHelperCallbacks {

	private RecyclerView.Adapter mAdapter;

	private Context mContext;

	private final ColorDrawable background = new ColorDrawable(); // Cached for performance

	private Drawable mAdd;
	private Drawable mLike;
	private Drawable mRemove;

	public ItemTouchHelperCallbacks(RecyclerView.Adapter adapter, Context context) {
		mAdapter = adapter;
		mContext = context;

		mAdd = mContext.getDrawable(R.drawable.ic_add);
		mLike = mContext.getDrawable(R.drawable.ic_favorite);
		mRemove = mContext.getDrawable(R.drawable.ic_circle_cancel);
		mAdd.setTint(mContext.getResources().getColor(R.color.white));
		mLike.setTint(mContext.getResources().getColor(R.color.white));
		mRemove.setTint(mContext.getResources().getColor(R.color.white));

	}

	public ItemTouchHelper.SimpleCallback deleteCallback =
			new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
		@Override
		public boolean isItemViewSwipeEnabled() {
			return !((QueueAdapter) mAdapter).isSwiping();
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

			if (viewHolder.getAdapterPosition() == NO_POSITION ||
					((QueueAdapter) mAdapter).isSwiping()) {
				super.onChildDraw(c, recyclerView, viewHolder,
						0, dY, actionState, isCurrentlyActive);
				return;
			}

			// Check for positive displacement
			if(dX > 0) {
				View itemView = viewHolder.itemView;

				// Draw background
				background.setColor(mContext.getResources().getColor(R.color.darkBlue));
				background.setBounds(0, itemView.getTop(),
						itemView.getLeft() + (int) dX, itemView.getBottom());
				background.draw(c);

				// Draw icon
				int width = (itemView.getBottom() - itemView.getTop())/3;
				mRemove.setBounds(itemView.getLeft() + width, itemView.getTop() + width,
						itemView.getLeft() + 2 * width, itemView.getBottom() - width);
				mRemove.draw(c);
			}
			super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
		}
	};

	public ItemTouchHelper.SimpleCallback likeCallback =
			new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
		@Override
		public boolean isItemViewSwipeEnabled() {
			return !((QueueAdapter) mAdapter).isSwiping();
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
			if (viewHolder.getAdapterPosition() == NO_POSITION ||
					((QueueAdapter) mAdapter).isSwiping()) {
				super.onChildDraw(c, recyclerView, viewHolder,
						0, dY, actionState, isCurrentlyActive);
				return;
			}
			View itemView = viewHolder.itemView;
			if(dX < 0) {
				// Draw background
				background.setColor(mContext.getResources().getColor(R.color.colorAccent));
				background.setBounds(itemView.getRight(), itemView.getTop(), itemView.getRight() + (int) dX, itemView.getBottom());
				background.draw(c);

				// Draw icon
				int width = (itemView.getBottom() - itemView.getTop())/3;
				mLike.setBounds(itemView.getRight() - 2 * width , itemView.getTop() + width,
						itemView.getRight() - width,itemView.getBottom() - width);
				mLike.draw(c);
			}
			super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
		}
	};

	public ItemTouchHelper.SimpleCallback addCallback =
			new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
		@Override
		public boolean isItemViewSwipeEnabled() {
			return !((SearchAdapter) mAdapter).isSwiping();
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
		}

		@Override
		public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
		                        @NonNull RecyclerView.ViewHolder viewHolder, float dX,
		                        float dY, int actionState, boolean isCurrentlyActive) {
			if (viewHolder instanceof SearchAdapter.SectionViewHolder ||
					viewHolder.getAdapterPosition() < ((SearchAdapter) mAdapter).mAddToQueuePosition ||
					viewHolder.getAdapterPosition() == NO_POSITION ||
					((SearchAdapter) mAdapter).isSwiping()) {
				super.onChildDraw(c, recyclerView, viewHolder,
						0, dY, actionState, isCurrentlyActive);
				return;
			}
			View itemView = viewHolder.itemView;
			super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

			if(dX < 0) {
				// Draw background
				background.setColor(mContext.getResources().getColor(R.color.colorAccent));
				background.setBounds(itemView.getRight(), itemView.getTop(),
						itemView.getRight() + (int) dX, itemView.getBottom());
				background.draw(c);

				// Draw icon
				int width = (itemView.getBottom() - itemView.getTop())/3;
				mAdd.setBounds(itemView.getRight() - 2 * width , itemView.getTop() + width,
						itemView.getRight() - width,itemView.getBottom() - width);
				mAdd.draw(c);
			}

		}

	};

	private void drawBackground(Canvas c, int colorID, View itemView) {
		background.setColor(mContext.getResources().getColor(colorID));
		background.setBounds(itemView.getLeft(), itemView.getTop(),
				itemView.getRight(), itemView.getBottom());
		background.draw(c);
	}
}
