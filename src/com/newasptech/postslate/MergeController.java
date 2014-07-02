package com.newasptech.postslate;

import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CancellationException;
import java.util.logging.Logger;

import javax.swing.ProgressMonitor;

public class MergeController {
	@SuppressWarnings("unused")
	private static Logger _l = Logger.getLogger("com.newasptech.postslate.MergeController");

	private String outputDir;
	private String container;
	private boolean separate, copyAllVideoStreams, copyCameraAudioStream, copyOtherStreams;
	private float vshift;
	private String vcodec;
	private String acodec;
	private Workspace workspace;
	
	public MergeController(String _outputDir, String _container, boolean _separate,
			boolean _copyAllVideoStreams, boolean _copyCameraAudioStream, boolean _copyOtherStreams,
			float _vshift, String _vcodec, String _acodec, Workspace _workspace) {
		outputDir = _outputDir;
		container = _container;
		separate = _separate;
		copyAllVideoStreams = _copyAllVideoStreams;
		copyCameraAudioStream = _copyCameraAudioStream;
		copyOtherStreams = _copyOtherStreams;
		vshift = _vshift;
		vcodec = _vcodec;
		acodec = _acodec;
		workspace = _workspace;
	}
	
	/** Merge all video/audio pairs from the workspace. */
	public void mergeAll(ProgressMonitor m) throws Exception {
		try {
			List<AVPair> contents = workspace.contents();
			int i = 0;
			for (Iterator<AVPair> pPair = contents.iterator(); pPair.hasNext();) {
				AVPair pair = pPair.next();
				AVClip vClip = pair.video(), aClip = pair.audio();
				if (vClip == null || aClip == null) continue;
				if (m != null) {
					if (m.isCanceled()) throw new CancellationException();
					m.setNote(String.format("%s / %s", vClip.getName(), aClip.getName()));
					m.setProgress(Math.round(100.0f * i++ / contents.size()));
				}
				merge(vClip, aClip);
			}
		}
		finally {
			if (m != null) m.close();
		}
	}
	
	/** Merge a single pair of video and audio clips. Return the path to the merged file. */
	public static final String AUDIO_MERGE_CONTAINER = "wav";
	public String merge(AVClip vFile, AVClip aFile) {
		AVEngine e = workspace.getSession().getAVEngine();
		float vClipLen = vFile.getMeta().findFirst(e.metaKeyName(AVEngine.MetaKey.CODEC_TYPE), e.metaValueName(AVEngine.MetaValue.CODEC_TYPE_VIDEO)).get(e.metaKeyName(AVEngine.MetaKey.DURATION)).fValue(),
				aClipLen = aFile.getMeta().findFirst(e.metaKeyName(AVEngine.MetaKey.CODEC_TYPE), e.metaValueName(AVEngine.MetaValue.CODEC_TYPE_AUDIO)).get(e.metaKeyName(AVEngine.MetaKey.DURATION)).fValue();
		TrimValues bounds = new TrimValues(vClipLen, vFile.getOffset(), vshift, aClipLen, aFile.getOffset());
		String videoMergeFile = mergeTarget(vFile, container, outputDir),
				audioMergeFile = mergeTarget(aFile, AUDIO_MERGE_CONTAINER, outputDir);
		SortedSet<Integer> vIndexSet = streamIndices(vFile, e.metaKeyName(AVEngine.MetaKey.CODEC_TYPE), e.metaValueName(AVEngine.MetaValue.CODEC_TYPE_VIDEO), !copyAllVideoStreams);
		SortedSet<Integer> aIndexSet = streamIndices(aFile, e.metaKeyName(AVEngine.MetaKey.CODEC_TYPE), e.metaValueName(AVEngine.MetaValue.CODEC_TYPE_AUDIO), !copyAllVideoStreams);
		if (copyCameraAudioStream) {
			vIndexSet.addAll(streamIndices(vFile, e.metaKeyName(AVEngine.MetaKey.CODEC_TYPE), e.metaValueName(AVEngine.MetaValue.CODEC_TYPE_AUDIO), false));
		}
		if (copyOtherStreams) {
			vIndexSet.addAll(streamIndices(vFile, e.metaKeyName(AVEngine.MetaKey.CODEC_TYPE), e.metaValueName(AVEngine.MetaValue.CODEC_TYPE_DATA), false));
		}
		AVClip vClip = new AVClip(vFile, bounds.getVideoStart(), bounds.getDuration(), vIndexSet);
		AVClip aClip = new AVClip(aFile, bounds.getAudioStart(), bounds.getDuration(), aIndexSet);
		if (vcodec == null && acodec == null) {
			if (separate) {
				e.repackage(workspace.getVideoDir(), vClip, null, null, videoMergeFile);
			}
			else {
				e.repackage(workspace.getVideoDir(), vClip, workspace.getAudioDir(), aClip, videoMergeFile);
			}
		}
		else {
			if (separate) {
				e.transcode(workspace.getVideoDir(), vClip, null, null, vcodec, null, videoMergeFile);
			}
			else {
				e.transcode(workspace.getVideoDir(), vClip, workspace.getAudioDir(), aClip, vcodec, acodec, videoMergeFile);
			}
		}
		if (separate)
			e.repackage(null, null, workspace.getAudioDir(), aClip, audioMergeFile);
		return videoMergeFile;
	}
	
	/** Return the output filename for a merge operation */
	private static String mergeTarget(AVFileRef vFile, String outputContainer, String outputDir) {
		StringBuffer s = new StringBuffer(outputDir);
		s.append(System.getProperty("file.separator"));
		String name =vFile.getName(); 
		s.append(name.substring(0, 1 + name.lastIndexOf('.')));
		s.append(outputContainer);
		return s.toString();
	}
	
	private static SortedSet<Integer> streamIndices(AVFileRef avf, String key, String value, boolean firstOnly) {
		SortedSet<Integer> retval = new TreeSet<Integer>();
		if (firstOnly) {
			int i = avf.getMeta().findFirstIndex(key, value);
			retval.add(Integer.valueOf(i));
		}
		else {
			retval.addAll(avf.getMeta().findAllIndices(key, value));
		}
		return retval;
	}
}
