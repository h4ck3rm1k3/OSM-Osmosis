package crosby.counttags;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import org.openstreetmap.osmosis.core.pipeline.common.TaskConfiguration;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManager;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.pipeline.v0_6.SinkManager;

import crosby.binary.file.BlockOutputStream;



public class CountTagsFactory extends TaskManagerFactory {
		@Override
		protected TaskManager createTaskManagerImpl(TaskConfiguration taskConfig) {
			CountTags task = new CountTags(this.getIntegerArgument(taskConfig,"length",1024*1024));
			return new SinkManager(taskConfig.getId(), task, taskConfig.getPipeArgs());
		}
	}
