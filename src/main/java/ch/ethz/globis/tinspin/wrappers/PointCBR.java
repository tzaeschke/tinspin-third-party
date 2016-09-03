/*
 * Copyright 2011-2016 ETH Zurich. All Rights Reserved.
 *
 * This software is the proprietary information of ETH Zurich.
 * Use is subject to license terms.
 */
package ch.ethz.globis.tinspin.wrappers;

import org.ardverk.collection.IntArrayKeyAnalyzer;
import org.ardverk.collection.PatriciaTrie;

import ch.ethz.globis.phtree.util.BitTools;
import ch.ethz.globis.tinspin.wrappers.Candidate;

/**
 * Wrapper for CritBit tree by:
 * https://github.com/rkapsi/patricia-trie
 */
public class PointCBR extends Candidate {

	private PatriciaTrie<int[], Object> cbr;
	
	private final int DIM;
	private final int DEPTH = 64;
	private final int N;
	
	private double[] data;
	
	private PointCBR(int DIM, int N) {
		this.DIM = DIM;
		this.N = N;
	}
	
	public static PointCBR create(int DIM, int N) {
		return new PointCBR(DIM, N);
	}
	
	@Override
	public void load(double[] data, int idxDim) {
		this.data = data;
		cbr = new PatriciaTrie<int[], Object>(new IntArrayKeyAnalyzer(idxDim, DEPTH));
		long[] buf = new long[idxDim];
		Object dummy = new Object();
		for (int i = 0; i < N; i++) {
			for (int d = 0; d < idxDim; d++) {
				buf[d] = BitTools.toSortableLong(data[idxDim*i+d]); 
			}
			
			int[] key = BitTools.merge(DEPTH, buf);
			if (cbr.put(key, dummy) != null) {
				System.out.println("Dupl!");
			}
		}
	}
	
	@Override
	public long[][] preparePointQuery(double[][] qA) {
		long[][] r = new long[qA.length][DIM];
		for (int i = 0; i < qA.length; i++) {
			BitTools.toSortableLong(qA[i], r[i]);
		}
		return r;
	}

	@Override
	public int pointQuery(Object qA) {
		int n = 0;
		long[][] a = (long[][]) qA;
		for (long[] q: a) {
			//log("q=" + q);
			int[] key = BitTools.merge(DEPTH, q);
			if (cbr.containsKey(key)) {
				n++;
			}
		}
		return n;
	}

	@Override
	public boolean supportsWindowQuery() {
		return N <= 10000;
	}

	@Override
	public int query(double[] min, double[] max) {
		long[] lower = new long[DIM];
		long[] upper = new long[DIM];
		for (int i = 0; i < DIM; i++) {
			lower[i] = BitTools.toSortableLong(min[i]);
			upper[i] = BitTools.toSortableLong(max[i]);
		}

		int[] fromKey;
		int[] toKey;
		fromKey = BitTools.merge(DEPTH, lower);
		toKey = BitTools.merge(DEPTH, upper);
		//Iterator<long[]> it = cbr.prefixMap(prefix);
		int n = 0;
		for (int[] key: cbr.subMap(fromKey, toKey).keySet()) {
			long[] val = BitTools.split(DIM, DEPTH, key);
			boolean match = true;
			for (int d = 0; d < DIM; d++) {
				if (val[d] < lower[d] || val[d] > upper[d]) {
					match = false;
					break;
				}
			}
			if (match) {
				n++;
			}
		}
//		log("N=" + N);
//		log("n=" + n);
//		log("q=" + lower + " / " + upper);
		return n;
	}

	@Override
	public int unload() {
		int n = 0;
		long[] e = new long[DIM];
		for (int i = 0; i < N>>1; i++) {
			n += cbr.remove(getEntryInt(e, i)) != null ? 1 : 0;
			n += cbr.remove(getEntryInt(e, N-i-1)) != null ? 1 : 0;
		}
		return n;
	}

	private int[] getEntryInt(long[] buf, int pos) {
		for (int d = 0; d < DIM; d++) {
			buf[d] = BitTools.toSortableLong(data[pos*DIM+d]); 
		}
		
		int[] key = BitTools.merge(DEPTH, buf);
		return key;
	}

	
	public static void main(String[] args) {
		int N = 1*1000*1000;
		int DIM = 3;
		Candidate x = create(DIM, N);
		x.run(x, N, DIM);
	}
	
	@Override
	public void release() {
		// nothing to be done
	}

	@Override
	public int update(double[][] updateTable) {
//		Object dummy = new Object();
//		int n = 0;
//		for (int i = 0; i < updateTable.length; ) {
//			int[] p1 = toKey(updateTable[i++]);
//			int[] p2 = toKey(updateTable[i++]);
//			if (cbf.remove(p1) != null) {
//				cbf.put(p2, dummy);
//				n++;
//			}
//		}
//		return n;
		throw new UnsupportedOperationException();
	}
}
