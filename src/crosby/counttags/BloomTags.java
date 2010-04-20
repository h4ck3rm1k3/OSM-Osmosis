package crosby.counttags;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

public class BloomTags {
	BloomTags(int filtersize) {
		blooms = new HashMap<String,BloomFilter>();
		this.filtersize = filtersize;
	}

	void process(Collection<Tag> tags) {
		for (Tag i : tags)
			process(i);
	}
	
	void process(Tag t) {
		if (!blooms.containsKey(t.getKey()))
			blooms.put(t.getKey(),new BloomFilter(filtersize));
			
			blooms.get(t.getKey()).put(t.getValue());
	}
	final int filtersize;	
	Map<String,BloomFilter> blooms;

	public String toString() {
		String out = "";

		Comparator<String> comparator = new Comparator<String>() {
			@Override
			public int compare(String s1, String s2) {
				int diff = blooms.get(s2).getUnique()-blooms.get(s1).getUnique();
				diff += (blooms.get(s2).getCount()-blooms.get(s1).getCount())/20;
				return diff;
			}
		};

		String keys[] = blooms.keySet().toArray(new String[0]);
		Arrays.sort(keys,comparator);
				
		
		for (String key : keys) {
			out += String.format ("%s for %s\n",blooms.get(key).toString(),key);
		}
		return out;
	}
}
