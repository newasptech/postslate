package com.newasptech.postslate;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ViewController {
	private static Logger _l = Logger.getLogger("com.newasptech.postslate.ViewController");
	private float videoShift;
	private String container;
	private Workspace workspace;
	public enum ViewType {
		CLAP,
		FULL,
		VIDEO,
		AUDIO;
		public static ViewType fromString(String a) {
			ViewType viewType = CLAP;
			if (a.equalsIgnoreCase(CLAP.toString()))
				viewType = ViewType.CLAP;
			else if (a.equalsIgnoreCase(FULL.toString()))
				viewType = ViewType.FULL;
			else if (a.equalsIgnoreCase(AUDIO.toString()))
				viewType = ViewType.AUDIO;
			else if (a.equalsIgnoreCase(VIDEO.toString()))
				viewType = ViewType.VIDEO;
			else
				assert(false);
			return viewType;
		}
	};

	/** Constructor
	 * @param _videoShift video time shift as fractional seconds
	 * @param _container merged file container format (e.g., "mov")
	 * @param _workspace workspace 
	 * */
	public ViewController(float _videoShift, String _container, Workspace _workspace) {
		videoShift = _videoShift;
		container = _container;
		workspace = _workspace;
	}

	public void view(File avFile, ViewType target, int width, int height, int x, int y)
		throws Exception {
		AVClip vclip = null, aclip = null;
		AVClipNDir cd = workspace.findClip(avFile), associate = null;
		try {
			associate = findAssociate(cd);
			if (cd.dir.getType() == AVDirRef.Type.VIDEO) {
				vclip = cd.clip;
				aclip = associate.clip;
			}
			else {
				aclip = cd.clip;
				vclip = associate.clip;
			}
		}
		catch(NoSuchElementException nsee) {}
		if (vclip == null) {
			target = ViewType.AUDIO;
			_l.log(Level.FINE, "There is no video, so the view target will be " + target);
		}
		else if (aclip == null) {
			target = ViewType.VIDEO;
			_l.log(Level.FINE, "There is no audio, so the view target will be " + target);
		}
		AVClip playClip = null;
		switch(target) {
		case CLAP:
			previewClap(vclip, aclip, videoShift, container, width, height, x, y);
			break;
		case FULL:
			previewMerge(vclip, aclip, videoShift, container, System.getProperty("java.io.tmpdir"),
					width, height, x, y);
			break;
		case VIDEO:
			playClip = new AVClip(vclip, 0.0f, AVClip.duration(vclip,
					AVEngine.MetaValue.CODEC_TYPE_VIDEO, workspace.getSession().getAVEngine()));
			showClip(workspace.getVideoDir(), playClip, width, height, x, y, true);
			break;
		case AUDIO:
			playClip = new AVClip(aclip, 0.0f, AVClip.duration(aclip,
					AVEngine.MetaValue.CODEC_TYPE_AUDIO, workspace.getSession().getAVEngine()));
			showClip(workspace.getAudioDir(), playClip, width, height, x, y, true);
			break;
		}
	}
	
	public void view(File avFile, float start, float duration, int width, int height, int x, int y,
			boolean cancelCurrent)
		throws Exception {
		AVClipNDir cd = workspace.findClip(avFile);
		AVClip playClip = new AVClip(cd.clip, start, duration);
		showClip(cd.dir, playClip, width, height, x, y, cancelCurrent);
	}
	
	private void previewMerge(AVClip vFile, AVClip aFile, float vshift, String container,
			String workdir, int width, int height, int x, int y) throws IOException {
		boolean allVideo = false, allAudio = false, copyOther = false;
		String vcodec = null, acodec = null;
		MergeController mc = new MergeController(workdir, getContainer(container),
				false, allVideo, allAudio, copyOther, vshift, vcodec, acodec, workspace);
		String previewFilePath = mc.merge(vFile, aFile);
		File previewFile = new File(previewFilePath);
		AVEngine e = workspace.getSession().getAVEngine();
		TrimValues bounds = new TrimValues(AVClip.duration(vFile, AVEngine.MetaValue.CODEC_TYPE_VIDEO, e),
				vFile.getOffset(), vshift, AVClip.duration(aFile, AVEngine.MetaValue.CODEC_TYPE_AUDIO, e),
				aFile.getOffset());
		AVClip pClip = new AVClip(new AVFileRef(previewFile, null), 0.0f, bounds.getDuration() + POST_VIDEO_PADDING);
		showClip(new AVDirRef(AVDirRef.Type.VIDEO, previewFile.getParent(), null, null), pClip,
				width, height, x, y, true);
		previewFile.deleteOnExit();
	}
	
	private static float POST_VIDEO_PADDING = 0.5f;
	private void previewClap(AVClip vFile, AVClip aFile, float vshift, String container,
			int width, int height, int x, int y) throws IOException {
		// Normally, PRE_CLAP is a fixed value, but what if the clap comes less than
		// that interval after the start of the clip?  Adjust if needed.
		Config cfg = workspace.getSession().getConfig();
		AVEngine e = workspace.getSession().getAVEngine();
		float usePreClap = Math.min(Math.min(vFile.getOffset(),
				aFile.getOffset()), cfg.fvalue(Config.PRE_CLAP));
		AVClip vClip = new AVClip(vFile, vFile.getOffset() + vshift - usePreClap,
				usePreClap+cfg.fvalue(Config.POST_CLAP),
				new int[]{vFile.getMeta().findFirstIndex(e.metaKeyName(AVEngine.MetaKey.CODEC_TYPE),
				e.metaValueName(AVEngine.MetaValue.CODEC_TYPE_VIDEO))});
		AVClip aClip = new AVClip(aFile, aFile.getOffset() - usePreClap,
				usePreClap+cfg.fvalue(Config.POST_CLAP),
				new int[]{aFile.getMeta().findFirstIndex(e.metaKeyName(AVEngine.MetaKey.CODEC_TYPE),
				e.metaValueName(AVEngine.MetaValue.CODEC_TYPE_AUDIO))});
		File previewFile = File.createTempFile("preview", "." + getContainer(container));
		previewFile.deleteOnExit();
		e.repackage(workspace.getVideoDir(), vClip, workspace.getAudioDir(), aClip, previewFile.toString());
		AVClip pClip = new AVClip(new AVFileRef(previewFile, null),
				0.0f, usePreClap + cfg.fvalue(Config.POST_CLAP) + POST_VIDEO_PADDING);
		showClip(new AVDirRef(AVDirRef.Type.VIDEO, previewFile.getParent(), null, null), pClip,
				width, height, x, y, true);
	}
	
	private void showClip(AVDirRef dir, AVClip clip, int width, int height, int x, int y,
			boolean cancelCurrent) {
		workspace.getSession().getAVEngine().play(dir, clip, width, height, x, y, cancelCurrent);
	}

	private AVClipNDir findAssociate(AVClipNDir cnd) {
		for (Iterator<AVPair> pp = workspace.contents().iterator(); pp.hasNext();) {
			AVPair p = pp.next();
			switch (cnd.dir.getType()) {
			case VIDEO:
				if (p.video() != null && 0 == p.video().compareTo(cnd.clip)) {
					return new AVClipNDir(p.audio(), workspace.getAudioDir());
				}
				break;
			case AUDIO:
				if (p.audio() != null && 0 == p.audio().compareTo(cnd.clip)) {
					return new AVClipNDir(p.video(), workspace.getVideoDir());
				}
				break;
			}
		}
		throw new NoSuchElementException();
	}
	
	private String getContainer(String c) {
		if (c != null && c.length() > 0)
			return c;
		return workspace.getSession().getConfig().getProperty(Config.MERGE_FORMAT);
	}
}
