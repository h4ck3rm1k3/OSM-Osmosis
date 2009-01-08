// License: GPL. Copyright 2007-2008 by Brett Henderson and other contributors.
package com.bretth.osmosis.core.filter.v0_5;

import com.bretth.osmosis.core.container.v0_5.BoundContainer;
import com.bretth.osmosis.core.domain.v0_5.Bound;
import com.bretth.osmosis.core.domain.v0_5.Node;
import com.bretth.osmosis.core.filter.common.IdTrackerType;

/**
 * Provides a filter for extracting all entities that lie within a specific geographical box
 * identified by latitude and longitude coordinates.
 * 
 * @author Brett Henderson
 */
public class BoundingBoxFilter extends AreaFilter {
	private Bound bound; // use a Bound for the internal representation


	/**
	 * Creates a new instance with the specified geographical coordinates. When filtering, nodes
	 * right on the edge of the box will be included.
	 * 
	 * @param idTrackerType
	 *            Defines the id tracker implementation to use.
	 * @param left
	 *            The longitude marking the left edge of the bounding box.
	 * @param right
	 *            The longitude marking the right edge of the bounding box.
	 * @param top
	 *            The latitude marking the top edge of the bounding box.
	 * @param bottom
	 *            The latitude marking the bottom edge of the bounding box.
	 * @param completeWays
	 *            Include all nodes for ways which have at least one node inside the filtered area.
	 * @param completeRelations
	 *            Include all relations referenced by other relations which have members inside the
	 *            filtered area.
	 */
	public BoundingBoxFilter(IdTrackerType idTrackerType,
	        double left,
	        double right,
	        double top,
	        double bottom,
	        boolean completeWays,
	        boolean completeRelations) {
		super(idTrackerType, completeWays, completeRelations);
		this.bound = new Bound(right, left, top, bottom, "");
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void process(BoundContainer boundContainer) {
		Bound newBound;
		/*
		 * The order of calling intersect is important because the first non-empty origin string
		 * will be used for the resulting Bound, and we want the origin string from the pipeline
		 * Bound to be used.
		 */
		newBound = boundContainer.getEntity().intersect(bound);
		// intersect will return null if there is no overlapping area
		if (newBound != null) {
			// Send on a bound element clipped to the area
			super.process(new BoundContainer(newBound));
		}
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean isNodeWithinArea(Node node) {
		double latitude;
		double longitude;

		latitude = node.getLatitude();
		longitude = node.getLongitude();

		/*
		 * Check the node coordinates against the bounding box by comparing them to each "simple"
		 * bound.
		 */
		for (Bound b : bound.toSimpleBound()) {
			if (b.getTop() >= latitude
			        && b.getBottom() <= latitude
			        && b.getLeft() <= longitude
			        && b.getRight() >= longitude) {
				return true;
			}
		}
		return false;
	}
}