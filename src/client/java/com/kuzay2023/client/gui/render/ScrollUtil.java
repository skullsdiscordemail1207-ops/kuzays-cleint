package com.kuzay2023.client.gui.render;

import net.minecraft.client.gui.DrawContext;

/**
 * Utility for rendering and managing scrollable lists with scrollbars.
 * Handles mouse wheel scrolling and drag-to-scroll functionality.
 */
public class ScrollUtil {
	private final int maxVisibleItems;
	private int totalItems;
	private int scrollOffset;
	private boolean isDraggingScrollbar;
	private int lastMouseY;

	public ScrollUtil(int maxVisibleItems) {
		this.maxVisibleItems = Math.max(1, maxVisibleItems);
		this.totalItems = 0;
		this.scrollOffset = 0;
		this.isDraggingScrollbar = false;
		this.lastMouseY = 0;
	}

	/**
	 * Update the total number of items in the list.
	 */
	public void setTotalItems(int totalItems) {
		this.totalItems = Math.max(0, totalItems);
		clampScrollOffset();
	}

	/**
	 * Get the current scroll offset (first visible item index).
	 */
	public int getScrollOffset() {
		return scrollOffset;
	}

	/**
	 * Get the last visible item index (inclusive).
	 */
	public int getLastVisibleIndex() {
		return Math.min(scrollOffset + maxVisibleItems - 1, totalItems - 1);
	}

	/**
	 * Check if scrolling is needed.
	 */
	public boolean isScrollNeeded() {
		return totalItems > maxVisibleItems;
	}

	/**
	 * Get the height of the scrollbar thumb based on visible items ratio.
	 */
	public int getScrollbarThumbHeight(int trackHeight) {
		if (!isScrollNeeded()) {
			return trackHeight;
		}
		return Math.max(20, (int) ((float) maxVisibleItems / totalItems * trackHeight));
	}

	/**
	 * Get the Y position of the scrollbar thumb based on scroll offset.
	 */
	public int getScrollbarThumbY(int trackY, int trackHeight) {
		if (!isScrollNeeded()) {
			return trackY;
		}
		int thumbHeight = getScrollbarThumbHeight(trackHeight);
		int maxThumbY = trackHeight - thumbHeight;
		int thumbY = (int) ((float) scrollOffset / (totalItems - maxVisibleItems) * maxThumbY);
		return trackY + thumbY;
	}

	/**
	 * Handle mouse wheel scrolling.
	 * Delta is typically 1 for scroll up, -1 for scroll down.
	 */
	public void handleMouseScroll(int delta) {
		if (!isScrollNeeded()) {
			return;
		}
		scrollOffset -= delta * 3; // Scroll 3 items per wheel click
		clampScrollOffset();
	}

	/**
	 * Start dragging the scrollbar.
	 */
	public void startDraggingScrollbar(int mouseY) {
		if (isScrollNeeded()) {
			isDraggingScrollbar = true;
			lastMouseY = mouseY;
		}
	}

	/**
	 * Stop dragging the scrollbar.
	 */
	public void stopDraggingScrollbar() {
		isDraggingScrollbar = false;
	}

	/**
	 * Update scrollbar drag position.
	 */
	public void updateDraggedScrollbar(int mouseY, int trackY, int trackHeight) {
		if (!isDraggingScrollbar || !isScrollNeeded()) {
			return;
		}

		int delta = mouseY - lastMouseY;
		lastMouseY = mouseY;

		int thumbHeight = getScrollbarThumbHeight(trackHeight);
		int maxThumbY = trackHeight - thumbHeight;

		if (maxThumbY <= 0) {
			return;
		}

		// Calculate new scroll offset based on thumb position
		int relativeY = mouseY - trackY - (thumbHeight / 2);
		int newScrollOffset = (int) ((float) relativeY / maxThumbY * (totalItems - maxVisibleItems));
		scrollOffset = newScrollOffset;
		clampScrollOffset();
	}

	/**
	 * Check if the mouse is over the scrollbar track.
	 */
	public boolean isMouseOverScrollbar(int mouseX, int mouseY, int scrollbarX, int scrollbarY, int scrollbarWidth, int scrollbarHeight) {
		return mouseX >= scrollbarX && mouseX <= scrollbarX + scrollbarWidth
			&& mouseY >= scrollbarY && mouseY <= scrollbarY + scrollbarHeight;
	}

	/**
	 * Check if the mouse is over the scrollbar thumb.
	 */
	public boolean isMouseOverScrollbarThumb(int mouseX, int mouseY, int scrollbarX, int scrollbarY, int scrollbarWidth, int scrollbarHeight) {
		if (!isScrollNeeded() || !isMouseOverScrollbar(mouseX, mouseY, scrollbarX, scrollbarY, scrollbarWidth, scrollbarHeight)) {
			return false;
		}

		int thumbHeight = getScrollbarThumbHeight(scrollbarHeight);
		int thumbY = getScrollbarThumbY(scrollbarY, scrollbarHeight);
		return mouseY >= thumbY && mouseY <= thumbY + thumbHeight;
	}

	/**
	 * Render the scrollbar.
	 */
	public void renderScrollbar(DrawContext context, int scrollbarX, int scrollbarY, int scrollbarWidth, int scrollbarHeight) {
		if (!isScrollNeeded()) {
			return;
		}

		// Draw track background
		context.fill(scrollbarX, scrollbarY, scrollbarX + scrollbarWidth, scrollbarY + scrollbarHeight, 0xFF3D3D3D);

		// Draw thumb
		int thumbHeight = getScrollbarThumbHeight(scrollbarHeight);
		int thumbY = getScrollbarThumbY(scrollbarY, scrollbarHeight);
		int thumbColor = isDraggingScrollbar ? 0xFFAAAAAA : 0xFF808080;
		context.fill(scrollbarX, thumbY, scrollbarX + scrollbarWidth, thumbY + thumbHeight, thumbColor);

		// Draw outline
		context.fill(scrollbarX, thumbY, scrollbarX + 1, thumbY + thumbHeight, 0xFFB0B0B0);
		context.fill(scrollbarX + scrollbarWidth - 1, thumbY, scrollbarX + scrollbarWidth, thumbY + thumbHeight, 0xFF4D4D4D);
	}

	/**
	 * Reset scroll to top.
	 */
	public void reset() {
		scrollOffset = 0;
	}

	private void clampScrollOffset() {
		if (totalItems <= maxVisibleItems) {
			scrollOffset = 0;
		} else {
			scrollOffset = Math.max(0, Math.min(scrollOffset, totalItems - maxVisibleItems));
		}
	}

	/**
	 * Check if scrollbar is currently being dragged.
	 */
	public boolean isDragging() {
		return isDraggingScrollbar;
	}
}
