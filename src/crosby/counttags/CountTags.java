package crosby.counttags;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openstreetmap.osmosis.core.container.v0_6.BoundContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityProcessor;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Bound;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.OsmUser;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

import crosby.binary.BinarySerializer;
import crosby.binary.Osmformat;
import crosby.binary.StringTable;
import crosby.binary.Osmformat.Relation.MemberType;
import crosby.binary.file.BlockOutputStream;
import crosby.binary.file.FileBlock;

public class CountTags implements Sink {

	final BloomTags wayTags,nodeTags,relationTags;
	final int filtersize;
	public CountTags(int filtersize) {
		this.filtersize = filtersize;
		nodeTags = new BloomTags(filtersize);
		wayTags = new BloomTags(filtersize);
		relationTags = new BloomTags(filtersize);
	}

	
	private Processor processor = new Processor();

	public class Processor implements EntityProcessor {
		@Override
		public void process(BoundContainer bound) {
		}

		@Override
		public void process(NodeContainer node) {
			nodeTags.process(node.getEntity().getTags());
		}

		@Override
		public void process(WayContainer way) {
			wayTags.process(way.getEntity().getTags());
		}

		@Override
		public void process(RelationContainer relation) {
			relationTags.process(relation.getEntity().getTags());
		}
	}
	
	public void process(EntityContainer entityContainer) {
		entityContainer.process(processor);
	}
	
	@Override
	public void complete() {
		System.out.println("---------- NODES ----------");
		System.out.println(nodeTags.toString());
		System.out.println("---------- WAYS ----------");
		System.out.println(wayTags.toString());
		System.out.println("---------- RELATIONS ----------");
		System.out.println(relationTags.toString());
	}

	@Override
	public void release() {
	}
}