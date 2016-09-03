/*
 * Copyright 2011-2016 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
package ch.ethz.globis.tinspin.wrappers.util;

import org.khelekore.prtree.MBRConverter;

public class PR_PR_PointConverter implements MBRConverter<PR_Entry> {
	private final int DIM;
	public PR_PR_PointConverter(int DIM) {
		this.DIM = DIM;
	}

	@Override
	public int getDimensions () {
		return DIM;
	}

	@Override
	public double getMin (int axis, PR_Entry t) {
		//return axis == 0 ? t.getMinX () : t.getMinY ();
		return t.data()[axis];
	}

	@Override
	public double getMax (int axis, PR_Entry t) {
		//return axis == 0 ? t.getMaxX () : t.getMaxY ();
		return t.data()[axis];
	}
}