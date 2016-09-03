/*
 * Copyright 2011-2016 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
package ch.ethz.globis.tinspin.wrappers.util;


public class PR_Entry {

	private final double[] data;
	
	@SuppressWarnings("unused")
	private PR_Entry() {
		data = null;
		//for ZooDB
	}
	
	/**
	 * Point constructor.
	 * @param xyz
	 */
	public PR_Entry(double[] xyz) {
		data = new double[xyz.length];
		System.arraycopy(xyz, 0, data, 0, xyz.length);
	}

	/**
	 * Range constructor.
	 * @param xyz1
	 * @param xyz2
	 */
	public PR_Entry(double[] xyz1, double[] xyz2) {
		data = new double[xyz1.length*2];
		System.arraycopy(xyz1, 0, data, 0, xyz1.length);
		System.arraycopy(xyz2, 0, data, xyz1.length, xyz2.length);
	}

	public double[] data() {
		return data;
	}

}
