package crosby.keeptags;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.openstreetmap.osmosis.core.container.v0_6.BoundContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityProcessor;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.core.task.v0_6.SinkSource;

public class KeepTags implements SinkSource, EntityProcessor {
	Set<String> removetags;
	
	KeepTags(Set<String> tags) {
		this.removetags = tags;
	}

	@Override
	public void process(EntityContainer entityContainer) {
		entityContainer.process(this);
	}

	@Override
	public void complete() {
		sink.complete();
	}

	@Override
	public void release() {
		sink.release();
	}

	@Override
	public void setSink(Sink sink) {
		this.sink = sink;
	}

	@Override
	public void process(BoundContainer bound) {
		sink.process(bound);
	}

	void editTags(Collection<Tag> tags) {
		ArrayList<Tag> copy = new ArrayList<Tag>();
		copy.addAll(tags);
		for (Tag t : copy) {
			if (!removetags.contains(t.getKey())) {
				tags.remove(t);
			}
		}
	}
	
	@Override
	public void process(NodeContainer node) {
		node = node.getWriteableInstance();
		editTags(node.getEntity().getTags());
		sink.process(node);
	}

	@Override
	public void process(WayContainer way) {
		way = way.getWriteableInstance();
		editTags(way.getEntity().getTags());
		sink.process(way);
	}

	@Override
	public void process(RelationContainer relation) {
		relation = relation.getWriteableInstance();
		editTags(relation.getEntity().getTags());
		sink.process(relation);
	}
	
	Sink sink;
}
