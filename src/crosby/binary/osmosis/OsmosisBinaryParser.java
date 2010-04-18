package crosby.binary.osmosis;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstreetmap.osmosis.core.container.v0_6.BoundContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityProcessor;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.common.TimestampContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.CommonEntityData;
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
import org.openstreetmap.osmosis.core.task.v0_6.Source;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import crosby.binary.Osmformat;
import crosby.binary.Osmformat.DenseNodes;
import crosby.binary.file.BlockInputStream;
import crosby.binary.file.FileBlock;

public class OsmosisBinaryParser implements FileBlock.Adaptor {
		@Override
		public void handleBlock(FileBlock message) {
			// TODO Auto-generated method stub
			try {
				Osmformat.PrimitiveBlock primblock=Osmformat.PrimitiveBlock.parseFrom(message.getData());
				parse(primblock);
			} catch (InvalidProtocolBufferException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new Error("ParseError"); // TODO
			}

			
		}

		// TODO: Later make this adaptor abstract so that skipBlock can be overridden.
		@Override
		public boolean skipBlock(FileBlock block) {
			if (block.getType().equals("OSMDATA"))
				return true;
			if (block.getType().equals("OSMBBOX"))
				return true;
			return false;
		}
		

	static OsmUser getUser(Osmformat.Info info,String strings[]) {
		if (info.hasUid() && info.hasUserSid())
			return new OsmUser((int)info.getUid(),strings[(int) info.getUserSid()]);
		else
			return OsmUser.NONE;
	}

	static Date getDate(Osmformat.Info info) {
		if (info.hasTimestamp()) 
			return new Date(info.getTimestamp());
		else
			return null;
	}
	
	public CommonEntityData parseInfo(long changeset_id, long id) {
		OsmUser user = OsmUser.NONE;
		Date timestamp = null;
		
		return new CommonEntityData(				
				id, -1, timestamp, user, changeset_id);
	}

	final int NOVERSION = -1;
	final int NOCHANGESET = -1;
	
	public void parseDense(Osmformat.DenseNodes nodes, String strings[], int granularity) {
		double multiplier = granularity*.000000001;

		long last_id = 0, last_lat = 0, last_lon = 0;
		for (int i=0 ; i < nodes.getIdCount(); i++) {
			Node tmp;
			List<Tag> tags = null;
			long lat = nodes.getLat(i)+last_lat; last_lat = lat;
			long lon = nodes.getLon(i)+last_lon; last_lon = lon;
			long id =  nodes.getId(i)+last_id; last_id = id;
			double latf = lat*multiplier, lonf = lon*multiplier;
			if (nodes.getInfoCount()>0) {
				Osmformat.Info info = nodes.getInfo(i);
				tmp = new Node(id,info.getVersion(), getDate(info), getUser(info,strings),
						info.getChangeset(),tags,latf,lonf);
			} else if (nodes.getChangesetIdCount() > 0) {
				tmp=new Node(id,NOVERSION,(Date)null,OsmUser.NONE,
						nodes.getChangesetId(i), tags, latf,lonf);
			} else {
				tmp=new Node(id,NOVERSION,(Date)null,OsmUser.NONE,
						NOCHANGESET, tags, latf, lonf);
			}
			sink.process(new NodeContainer(tmp));
		}
	}
	
	public void parseNodes(List<Osmformat.Node> nodes, String strings[], int granularity) {
		double multiplier = granularity*.000000001;
		for (Osmformat.Node i : nodes) {
			List<Tag> tags = new ArrayList<Tag>();
			for (int j=0 ; j < i.getKeysCount(); j++)
				tags.add(new Tag(strings[i.getKeys(j)],strings[i.getVals(j)]));
			
			//long id, int version, Date timestamp, OsmUser user, 
			//long changesetId, Collection<Tag> tags,
			//double latitude, double longitude
			Node tmp;
			long id = i.getId();
			double latf = multiplier*i.getLat();
			double lonf = multiplier*i.getLon();
			
			if (i.hasInfo()) {
				Osmformat.Info info = i.getInfo();
				tmp = new Node(id,info.getVersion(), getDate(info), getUser(info,strings),
						info.getChangeset(),tags,latf,lonf);
			} else if (i.hasChangesetId()) {
				tmp=new Node(id,NOVERSION,(Date)null,OsmUser.NONE,
						i.getChangesetId(), tags, latf,lonf);
			} else {
				tmp=new Node(id,NOVERSION,(Date)null,OsmUser.NONE,
						NOCHANGESET, tags, latf, lonf);
			}
			sink.process(new NodeContainer(tmp));

		}
	}
	public void parseWays(List<Osmformat.Way> ways, String strings[]) {
		for (Osmformat.Way i : ways) {
			List<Tag> tags = new ArrayList<Tag>();
			for (int j=0 ; j < i.getKeysCount(); j++)
				tags.add(new Tag(strings[i.getKeys(j)],strings[i.getVals(j)]));

			long last_id=0;
			List<WayNode> nodes= new ArrayList<WayNode>();
			for (long j : i.getRefsList()) {
				nodes.add(new WayNode(j+last_id));
				last_id = j+last_id;
			}

			long id = i.getId();

			//long id, int version, Date timestamp, OsmUser user, 
			//long changesetId, Collection<Tag> tags,
			//List<WayNode> wayNodes
			Way tmp;
			if (i.hasInfo()) {
				Osmformat.Info info = i.getInfo();
				tmp = new Way(id, info.getVersion(), getDate(info), getUser(info,strings),
						info.getChangeset(),tags,
						nodes);
			} else if (i.hasChangesetId()) {
				tmp=new Way(id,NOVERSION,(Date)null,OsmUser.NONE,
						i.getChangesetId(), tags,
						nodes);
			} else {
				tmp=new Way(id,NOVERSION,(Date)null,OsmUser.NONE,
						NOCHANGESET, tags,
						nodes);
			}
			sink.process(new WayContainer(tmp));
		}
	}
	public void parseRelations(List<Osmformat.Relation> rels, String strings[]) {
		for (Osmformat.Relation i : rels) {
			List<Tag> tags = new ArrayList<Tag>();
			for (int j=0 ; j < i.getKeysCount(); j++)
				tags.add(new Tag(strings[i.getKeys(j)],strings[i.getVals(j)]));

			long id = i.getId();
			
			long last_mid=0;
			List<RelationMember> nodes= new ArrayList<RelationMember>();
			for (int j =0; j < i.getMemidsCount() ; j++) {
				long mid = last_mid + i.getMemids(j);
				last_mid = mid;
				String role = strings[i.getRolesSid(j)];
				EntityType etype=null;
				
				if (i.getTypes(j) == Osmformat.Relation.MemberType.NODE)
					etype = EntityType.Node;
				else if (i.getTypes(j) == Osmformat.Relation.MemberType.WAY)
					etype = EntityType.Way;
				else if (i.getTypes(j) == Osmformat.Relation.MemberType.RELATION)
					etype = EntityType.Relation;
				else
					assert false; // TODO; Illegal file?

				nodes.add(new RelationMember(mid,etype,role));
			}
			//long id, int version, TimestampContainer timestampContainer, OsmUser user, 
			//long changesetId, Collection<Tag> tags, 
			//List<RelationMember> members
			Relation tmp;
			if (i.hasInfo()) {
				Osmformat.Info info = i.getInfo();
				tmp = new Relation(id, info.getVersion(), getDate(info), getUser(info,strings),
						info.getChangeset(),tags,
						nodes);
			} else if (i.hasChangesetId()) {
				tmp=new Relation(id,NOVERSION,(Date)null,OsmUser.NONE,
						i.getChangesetId(), tags,
						nodes);
			} else {
				tmp=new Relation(id,NOVERSION,(Date)null,OsmUser.NONE,
						NOCHANGESET, tags,
						nodes);
			}
			sink.process(new RelationContainer(tmp));
		}
	}
	
	public void parse(Osmformat.PrimitiveBlock block) {
		Osmformat.StringTable stablemessage = block.getStringtable();
		String strings[]=new String[stablemessage.getSCount()];

		for (int i=0 ; i < strings.length ; i++) {
			strings[i]=stablemessage.getS(i).toStringUtf8();
		}
		
		for (Osmformat.PrimitiveGroup groupmessage : block.getPrimitivegroupList()) {
			// Exactly one of these should trigger.
			parseNodes(groupmessage.getNodesList(),strings,block.getGranularity());
			parseWays(groupmessage.getWaysList(),strings);
			parseRelations(groupmessage.getRelationsList(),strings);
			if (groupmessage.hasDense()) 
				parseDense(groupmessage.getDense(),strings,block.getGranularity());
		}
	}
		
	public void setSink(Sink sink_) {
		sink = sink_;		
	}
	
	private Sink sink;
}
