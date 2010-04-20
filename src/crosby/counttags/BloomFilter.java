package crosby.counttags;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

public class BloomFilter {
	private final int SETTHRESHOLD = 4096;  // should be length/8/1000
	public BloomFilter(int length) {
		this.length = length;
		set = new HashSet<String>();
	}

	Set<String> set;
	int unique_in_set=0;
	
	public void put(String key) {
		putcount++;

		if (set != null) {
			// First, we store in a set if there aren't many keys
			if (set.contains(key))
				return;
			set.add(key);
			unique_in_set++;
			
			if (unique_in_set > length/1024) {
				bits = new byte[length/8];
				for (String s :set) {
					setBit(s);
				}
				set = null;
			}
		} else {
			// No set, a lot of keys.
			setBit(key);
		}
	}

	private void setBit(String key) {
		int bit = getHash(key)%length;
		bits[bit/8] = (byte) (bits[bit/8] | (1<<(bit%8)));
	}

	public String toString() {
		int unique = getUnique();
		if (unique < length/2)
			return String.format("estimating %d unique of %d keys",unique,putcount);
		else 
			return String.format("estimating >%d unique of %d keys",length/2,putcount);
	}
	
	public int getCount() {
		return (int) putcount;
	}
	
	public int getUnique() {
		if (set != null) 
			return unique_in_set;
		else
			return countOnes();
	}
	
	private int countOnes() {
		int total = 0;
		for (int i=0 ; i < bits.length ; i++) {
			byte b = bits[i];
			byte temp=b;
			temp = (byte) ((temp&0x55) + ((temp&0xaa)>>1));
			temp = (byte) ((temp&0x33) + ((temp&0xcc)>>2));
			temp = (byte) ((temp&0x0f) + ((temp&0xf0)>>4));
			total += temp;
		}
		return total;
	}
	
	private int getHash(String key) {
		try {
			MessageDigest md=MessageDigest.getInstance("SHA-256");
			md.update(key.getBytes());
			byte buf[] = md.digest();
			int out=0;
			for (int i=0 ; i < 4 ; i++) {
				out *= 256; out += buf[i];
			}
			out= out & 0x7fffffff;
			return out;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return -1;
	}
	
	final int length;
	byte bits[];
	long putcount = 0;
}
